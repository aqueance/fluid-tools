/*
 * Copyright (c) 2006-2012 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fluidity.deployment.maven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.fluidity.foundation.Archives;
import org.fluidity.foundation.ServiceProviders;
import org.fluidity.foundation.Streams;
import org.fluidity.foundation.Utility;

import org.apache.maven.plugin.logging.Log;

/**
 * Common JAR processing utilities for standalone archive processing plugins.
 *
 * @author Tibor Varga
 */
public final class ArchivesSupport extends Utility {

    public static final String META_INF = Archives.META_INF.concat("/");

    private ArchivesSupport() { }

    /**
     * Iterates through the {@link File}s returned by the given {@link Feed feed} and loads the manifest attributes and service provider files into the given
     * maps, respectively.
     *
     * @param attributes the map to store JAR manifest attributes of the input JARs.
     * @param providers  the map to store contents of service provider files encountered in the input JARs.
     * @param buffer     the buffer to use when copying data.
     * @param log        the Maven log to use.
     * @param feed       the provider of the list of input JARs to load.
     *
     * @throws IOException when JAR processing fails.
     */
    public static void load(final Map<String, Attributes> attributes,
                            final Map<String, String[]> providers,
                            final byte[] buffer,
                            final Log log,
                            final Feed feed) throws IOException {
        for (File input; (input = feed.next()) != null; ) {
            Archives.read(input.toURI().toURL(), new Archives.Reader() {
                public boolean matches(final URL url, final JarEntry entry) throws IOException {

                    // read all entries except the MANIFEST
                    return feed.include(entry) && !entry.getName().equals(JarFile.MANIFEST_NAME);
                }

                public boolean read(final URL url, final JarEntry entry, final InputStream stream) throws IOException {
                    final String entryName = entry.getName();

                    if (!attributes.containsKey(entryName)) {
                        if (entryName.equals(Archives.INDEX_NAME)) {
                            log.warn(String.format("JAR index ignored in %s", url));
                            return true;
                        } else if (entryName.startsWith(Archives.META_INF) && entryName.toUpperCase().endsWith(".SF")) {
                            throw new IOException(String.format("JAR signatures not supported in %s", url));
                        } else if (entryName.startsWith(ServiceProviders.LOCATION) && !entry.isDirectory()) {
                            final String[] list = Streams.load(stream, "UTF-8", buffer, false).split("[\n\r]+");
                            final String[] present = providers.get(entryName);

                            if (present == null) {
                                providers.put(entryName, list);
                            } else {
                                final String[] combined = Arrays.copyOf(present, present.length + list.length);
                                System.arraycopy(list, 0, combined, present.length, list.length);
                                providers.put(entryName, combined);
                            }

                            return true;
                        }

                        attributes.put(entryName, entry.getAttributes());
                    } else if (!entry.isDirectory()) {
                        log.warn(String.format("Duplicate entry: %s", entryName));
                    }

                    return true;
                }
            });
        }
    }

    /**
     * Iterates through the {@link File}s returned by the given {@link Feed feed} and stores all entries found therein in the output JAR stream,
     * except the JAR manifest, JAR indexes, signatures, and whatever else is not {@linkplain ArchivesSupport.Feed#include(JarEntry) included} by the
     * <code>feed</code>. The service providers in the given map loaded by {@link #load(Map, Map, byte[], Log, ArchivesSupport.Feed)} are saved as well.
     *
     * @param output   the JAR output stream to add entries to.
     * @param buffer   the buffer to use when {@linkplain Streams#copy(java.io.InputStream, java.io.OutputStream, byte[], boolean, boolean) copying} data.
     * @param services the service provider map computed by {@link #load(Map, Map, byte[], Log, ArchivesSupport.Feed)}.
     * @param feed     the provider of the list of JAR inputs to expand.
     *
     * @throws IOException when JAR processing fails.
     */
    public static void expand(final JarOutputStream output, final byte[] buffer, final Map<String, String[]> services, final Feed feed) throws IOException {
        final Set<String> copied = new HashSet<String>();

        for (final Map.Entry<String, String[]> entry : services.entrySet()) {
            final String entryName = entry.getKey();

            if (!copied.contains(entryName)) {
                copied.add(entryName);

                final StringBuilder contents = new StringBuilder();

                for (final String line : entry.getValue()) {
                    contents.append(line).append('\n');
                }

                output.putNextEntry(new JarEntry(entryName));
                Streams.store(output, contents.toString(), "UTF-8", buffer, false);
            }
        }

        for (File input; (input = feed.next()) != null; ) {
            final JarFile jar = new JarFile(input);

            try {
                for (final Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements(); ) {
                    final JarEntry entry = entries.nextElement();
                    final String entryName = entry.getName();

                    final boolean done = copied.contains(entryName);
                    final boolean manifest = entryName.equals(JarFile.MANIFEST_NAME) || entryName.equals(META_INF);
                    final boolean index = entryName.equals(Archives.INDEX_NAME);
                    final boolean signature = entryName.startsWith(Archives.META_INF) && entryName.toUpperCase().endsWith(".SF");

                    if (!done && !manifest && !index && !signature && feed.include(entry)) {
                        copied.add(entryName);
                        output.putNextEntry(entry);
                        Streams.copy(jar.getInputStream(entry), output, buffer, true, false);
                    }
                }
            } finally {
                jar.close();
            }
        }
    }

    /**
     * Adds the given JAR manifest entries to the given JAR manifest.
     *
     * @param entries  the entries.
     * @param manifest the manifest.
     */
    public static void include(final Map<String, Attributes> entries, final Manifest manifest) {
        final Map<String, Attributes> attributes = manifest.getEntries();

        for (final Map.Entry<String, Attributes> entry : entries.entrySet()) {
            final Attributes value = entry.getValue();

            if (value != null) {
                final String key = entry.getKey();
                final Attributes list = attributes.get(key);

                if (list == null) {
                    attributes.put(key, value);
                } else {
                    for (final Map.Entry<Object, Object> item : value.entrySet()) {
                        list.put(item.getKey(), item.getValue());
                    }
                }
            }
        }
    }

    /**
     * Provides {@link JarFile} input files to {@link ArchivesSupport#load(Map, Map, byte[], Log, ArchivesSupport.Feed)} and {@link
     * ArchivesSupport#expand(JarOutputStream, byte[], Map, ArchivesSupport.Feed)}.
     *
     * @author Tibor Varga
     */
    public interface Feed {

        /**
         * Returns the next JAR {@link File} to {@link ArchivesSupport#load(Map, Map, byte[], Log, ArchivesSupport.Feed)} or {@link
         * ArchivesSupport#expand(JarOutputStream, byte[], Map, ArchivesSupport.Feed)}.
         *
         * @return a {@link File} object or <code>null</code> if there is no more.
         *
         * @throws IOException when constructing the return object fails.
         */
        File next() throws IOException;

        /**
         * Tells if the given entry should be included when {@linkplain ArchivesSupport#load(Map, Map, byte[], Log, ArchivesSupport.Feed) loading} or
         * {@linkplain ArchivesSupport#expand(JarOutputStream, byte[], Map, ArchivesSupport.Feed) expanding} JAR entries.
         *
         * @param entry the entry to consider for inclusion.
         *
         * @return <code>true</code> if the given entry should be included, <code>false</code> otherwise.
         */
        boolean include(JarEntry entry);
    }
}

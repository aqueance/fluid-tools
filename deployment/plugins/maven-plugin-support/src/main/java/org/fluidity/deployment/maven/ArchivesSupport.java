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

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
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
 * @author Tibor Varga
 */
public class ArchivesSupport extends Utility {

    private ArchivesSupport() { }

    /**
     * Iterates through the {@link JarFile}s returned by the given {@link Feed feed} and expands it into the given {@link JarOutputStream output}.
     *
     * @param output the JAR output stream to expand the JAR inputs into.
     * @param buffer the buffer to use when copying data.
     * @param log    the Maven log to use.
     * @param feed   the provider of the list of JAR inputs to expand.
     *
     * @return the map of JAR manifest entries encountered in the input JAR inputs.
     *
     * @throws IOException when JAR handling fails.
     */
    public static Map<String, Attributes> expand(final JarOutputStream output, final byte[] buffer, final Log log, final Feed feed) throws IOException {
        final Map<String, Attributes> attributesMap = new HashMap<String, Attributes>();
        final Map<String, String[]> providerMap = new HashMap<String, String[]>();

        JarFile input;
        while ((input = feed.next()) != null) {
            try {
                for (final Enumeration<JarEntry> entries = input.entries(); entries.hasMoreElements(); ) {
                    final JarEntry entry = entries.nextElement();
                    final String entryName = entry.getName();

                    if (!attributesMap.containsKey(entryName)) {

                        // copy all entries except the MANIFEST
                        if (!entryName.equals(JarFile.MANIFEST_NAME)) {
                            if (entryName.equals(Archives.INDEX_NAME)) {
                                log.warn(String.format("JAR index ignored in %s", input.getName()));
                                continue;
                            } else if (entryName.startsWith(Archives.META_INF) && entryName.toUpperCase().endsWith(".SF")) {
                                throw new IOException(String.format("JAR signatures not supported in %s", input.getName()));
                            } else if (entryName.startsWith(ServiceProviders.LOCATION) && !entry.isDirectory()) {
                                final String[] list = Streams.load(input.getInputStream(entry), "UTF-8", buffer).split("[\n\r]+");
                                final String[] present = providerMap.get(entryName);

                                if (present == null) {
                                    providerMap.put(entryName, list);
                                } else {
                                    final String[] combined = Arrays.copyOf(present, present.length + list.length);
                                    System.arraycopy(list, 0, combined, present.length, list.length);
                                    providerMap.put(entryName, combined);
                                }

                                continue;
                            }

                            output.putNextEntry(entry);
                            Streams.copy(input.getInputStream(entry), output, buffer, false);
                            attributesMap.put(entryName, entry.getAttributes());
                        }
                    } else if (!entry.isDirectory()) {
                        log.warn(String.format("Duplicate entry: %s", entryName));
                    }
                }
            } finally {
                try {
                    input.close();
                } catch (final IOException ignored) {
                    // ignored
                }
            }
        }

        for (final Map.Entry<String, String[]> entry : providerMap.entrySet()) {
            final StringBuilder contents = new StringBuilder();

            for (final String line : entry.getValue()) {
                contents.append(line).append('\n');
            }

            output.putNextEntry(new JarEntry(entry.getKey()));
            Streams.store(output, contents.toString(), "UTF-8", buffer, false);
        }

        return attributesMap;
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
     * Provides {@link JarFile} input files to {@link ArchivesSupport#expand(JarOutputStream, byte[], Log, ArchivesSupport.Feed)}.
     */
    public interface Feed {

        /**
         * Returns the next {@link JarFile} to {@link ArchivesSupport#expand(JarOutputStream, byte[], Log, ArchivesSupport.Feed)}.
         *
         * @return a {@link JarFile} object or <code>null</code> if there is no more.
         *
         * @throws IOException when constructing the return object fails.
         */
        JarFile next() throws IOException;
    }
}

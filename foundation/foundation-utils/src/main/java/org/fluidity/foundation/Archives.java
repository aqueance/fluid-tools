/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.foundation;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * Convenience methods to browse and read JAR archives.
 */
public final class Archives extends Utility {

    private Archives() { }

    /**
     * Allows searching for and reading nested JAR files as in a JAR file. This method, as compared to {@link #readEntries(URL, EntryReader)}, supplies the
     * provided <code>reader</code> with an independent stream for each entry rather than the stream tied to the supplied JAR file that the receiver should
     * close.
     *
     * @param jar    the URL of the JAR file.
     * @param reader the object that filters and reads the JAR entries.
     *
     * @return the number of entries read.
     *
     * @throws IOException when something goes wrong reading the JAR file.
     */
    public static int readNestedEntries(final URL jar, final EntryReader reader) throws IOException {
        assert jar != null;
        final JarInputStream container = new JarInputStream(jar.openStream(), false);

        int count = 0;
        try {
            JarEntry entry;
            while ((entry = container.getNextJarEntry()) != null) {
                try {
                    if (!entry.isDirectory()) {
                        if (reader.matches(entry)) {
                            ++count;

                            if (!reader.read(entry, new JarInputStream(container, false))) {
                                break;
                            }
                        }
                    }
                } finally {
                    try {
                        container.closeEntry();
                    } catch (final IOException e) {
                        // ignore
                    }
                }
            }
        } finally {
            try {
                container.close();
            } catch (final IOException e) {
                // ignore
            }
        }

        return count;
    }

    /**
     * Allows reading entries from a JAR file. This method, as compared to {@link #readNestedEntries(URL, EntryReader)}, supplies the
     * provided <code>reader</code> the stream tied to the supplied JAR file that the receiver should not close.
     *
     * @param jar    the URL of the JAR file.
     * @param reader the object that reads the JAR entries.
     *
     * @return the number of entries read.
     *
     * @throws IOException when something goes wrong reading the JAR file.
     */
    public static int readEntries(final URL jar, final EntryReader reader) throws IOException {
        assert jar != null;
        final JarInputStream container = new JarInputStream(jar.openStream(), false);

        int count = 0;
        try {
            JarEntry entry;
            while ((entry = container.getNextJarEntry()) != null) {
                try {
                    if (!entry.isDirectory()) {
                        if (reader.matches(entry)) {
                            ++count;

                            if (!reader.read(entry, container)) {
                                break;
                            }
                        }
                    }
                } finally {
                    try {
                        container.closeEntry();
                    } catch (final IOException e) {
                        // ignore
                    }
                }
            }
        } finally {
            try {
                container.close();
            } catch (final IOException e) {
                // ignore
            }
        }

        return count;
    }

    /**
     * Loads manifest attributes from the JAR file where the given class was loaded from.
     *
     * @param type the class whose source JAR is to be processed.
     * @param names  the list of attribute names to load.
     *
     * @return an array of strings, each being the value of the attribute name at the same index in the <code>names</code> parameter.
     */
    public static String[] manifestAttributes(final Class<?> type, final String... names) {
        final URL url = ClassLoaders.findClassResource(type);

        if (url == null) {
            throw new IllegalArgumentException(String.format("Can't find class loader for %s", type));
        }

        try {
            final URL jar = jarFile(url).getJarFileURL();

            if (jar != null) {
                final Attributes manifest = loadManifest(jar).getMainAttributes();

                final List<String> list = new ArrayList<String>();
                for (final String name : names) {
                    list.add(manifest.getValue(name));
                }

                return list.toArray(new String[list.size()]);
            } else {
                throw new IllegalArgumentException(String.format("Class %s was not loaded from a JAR file: %s", type.getName(), url));
            }
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Loads the JAR manifest from the given JAR file URL.
     *
     * @param url the JAR file URL.
     *
     * @return the JAR manifest.
     *
     * @throws IOException if reading the URL contents fails.
     */
    private static Manifest loadManifest(final URL url) throws IOException {
        final JarInputStream stream = new JarInputStream(url.openStream());

        try {
            return stream.getManifest();
        } finally {
            stream.close();
        }
    }

    /**
     * Loads the JAR manifest from the given JAR input stream.
     *
     * @param stream the JAR input stream to load the manifest from.
     *
     * @return the JAR manifest.
     *
     * @throws IOException if loading the input stream fails.
     */
    public static Manifest loadManifest(final InputStream stream) throws IOException {
        final Manifest manifest = new Manifest();

        try {
            manifest.read(stream);
        } finally {
            stream.close();
        }

        return manifest;
    }

    /**
     * Returns a <code>JarURLConnection</code> for the given JAR URL (an URL into a JAR file) that allows {@link JarURLConnection#getAttributes() loading the
     * JAR manifest} or finding the {@link JarURLConnection#getJarFileURL() base JAR file URL} of the given URL.
     *
     * @param url the URL to interpret as a JAR URL.
     *
     * @return the JAR URL connection or <code>null</code> if the provided URL is not a JAR URL.
     */
    public static JarURLConnection jarFile(final URL url) {
        try {
            final URLConnection connection = url.openConnection();
            return connection instanceof JarURLConnection ? ((JarURLConnection) connection) : null;
        } catch (final IOException e) {
            return null;
        }
    }

    /**
     * Filters and reads entries in a JAR file. Used by {@link Archives#readEntries(URL, EntryReader)} and {@link Archives#readNestedEntries(URL,
     * EntryReader)}.
     */
    public interface EntryReader {

        /**
         * Tells if the {@link #read(JarEntry, JarInputStream)} method should be invoked with the given entry.
         *
         * @param entry the entry to decide about; never <code>null</code>.
         *
         * @return <code>true</code> if the given entry should be passed to the {@link #read(JarEntry, JarInputStream)} method, <code>false</code> if not.
         *
         * @throws IOException when something goes wrong reading the JAR file.
         */
        boolean matches(JarEntry entry) throws IOException;

        /**
         * Reads the given entry.
         *
         * @param entry  the entry to read.
         * @param stream the stream containing the entry's content.
         *
         * @return <code>true</code> if further searching is needed, <code>false</code> if search should terminate.
         *
         * @throws IOException when something goes wrong reading the JAR file.
         */
        boolean read(JarEntry entry, JarInputStream stream) throws IOException;
    }
}

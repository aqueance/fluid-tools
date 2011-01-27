/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.fluidity.foundation;

import java.io.IOException;
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
 * Convenience methods to traverse jar streams
 */
public final class JarStreams {

    private JarStreams() {
        throw new UnsupportedOperationException("No instance allowed");
    }

    /**
     * Allows the caller to search for and read entries in a jar file.
     *
     * @param jar    the URL of the jar file.
     * @param reader the object that filters and reads the jar entries.
     *
     * @throws java.io.IOException when something goes wrong reading the jar file.
     */
    public static void readEntries(final URL jar, final JarEntryReader reader) throws IOException {
        final JarInputStream container = new JarInputStream(jar.openStream(), false);

        try {
            JarEntry entry;
            while ((entry = container.getNextJarEntry()) != null) {
                try {
                    if (!entry.isDirectory()) {
                        if (reader.matches(entry)) {
                            final JarInputStream stream = new JarInputStream(container, false);

                            if (!reader.read(entry, stream)) {
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
    }

    /**
     * Loads manifest attributes from the jar file where the given class was loaded from.
     *
     * @param aClass the class whose source jar is to be processed.
     * @param names  the list of attribute names to load.
     *
     * @return an array of Strings, each being the value of the attribute name at the same index.
     */
    public static String[] manifestAttributes(final Class<?> aClass, final String... names) {
        final URL url = ClassLoaders.findClassResource(aClass);

        try {
            final URLConnection connection = url.openConnection();

            if (connection instanceof JarURLConnection) {
                final Attributes manifest = loadManifest(((JarURLConnection) connection).getJarFileURL()).getMainAttributes();

                final List<String> list = new ArrayList<String>();
                for (final String name : names) {
                    list.add(manifest.getValue(name));
                }

                return list.toArray(new String[list.size()]);
            } else {
                throw new IllegalArgumentException(String.format("Class %s was not loaded from a jar file: %s", aClass.getName(), url));
            }
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static Manifest loadManifest(final URL url) throws IOException {
        final JarInputStream stream = new JarInputStream(url.openStream());

        try {
            return stream.getManifest();
        } finally {
            stream.close();
        }
    }

    /**
     * Filters and reads entries in a jar file.
     */
    public static interface JarEntryReader {

        /**
         * Tells if the {@link #read(JarEntry, JarInputStream)} method should be invoked with the given entry.
         *
         * @param entry the entry to decide about; never <code>null</code>.
         *
         * @return <code>true</code> if the given entry should be passed to the {@link #read(JarEntry, JarInputStream)} method, <code>false</code> if not.
         *
         * @throws java.io.IOException when something goes wrong reading the jar file.
         */
        boolean matches(JarEntry entry) throws IOException;

        /**
         * Reads the given entry.
         *
         * @param entry  the entry to read.
         * @param stream the stream containing the entry's contnt.
         *
         * @return <code>true</code> if further searching is needed, <code>false</code> if search should terminate.
         *
         * @throws java.io.IOException when something goes wrong reading the jar file.
         */
        boolean read(JarEntry entry, JarInputStream stream) throws IOException;
    }
}

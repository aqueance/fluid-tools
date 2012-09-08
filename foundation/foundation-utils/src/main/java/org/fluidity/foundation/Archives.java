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

import org.fluidity.foundation.jarjar.Handler;

/**
 * Convenience methods to work with JAR archives.
 * <h3>Usage Example</h3>
 * <pre>
 * {@link Archives#readEntries(java.net.URL, org.fluidity.foundation.Archives.Entry) Archives.readEntries}({@linkplain Archives#containing(Class) Archives.containing}(getClass()), new <span class="hl1">Archives.Entry</span>() {
 *   public boolean <span class="hl1">matches</span>(final {@linkplain JarEntry} entry) throws {@linkplain IOException} {
 *     return true;
 *   }
 *
 *   public boolean <span class="hl1">read</span>(final {@linkplain JarEntry} entry, final {@linkplain InputStream} stream) throws {@linkplain IOException} {
 *     System.out.println(entry.{@linkplain JarEntry#getName() getName}());
 *     return true;
 *   }
 * });
 *
 * </pre>
 *
 * @author Tibor Varga
 */
@SuppressWarnings("UnusedDeclaration")
public final class Archives extends Utility {

    /**
     * Name of the <code>META-INF</code> directory in JAR files.
     */
    public static final String META_INF = "META-INF";

    /**
     * Name of the <code>WEB-INF</code> directory in WAR files.
     */
    public static final String WEB_INF = "WEB-INF";

    /**
     * Name of the <code>APP-INF</code> directory in WAR files.
     */
    public static final String APP_INF = "APP-INF";

    /**
     * Name of the JAR index file in JAR files.
     */
    public static final String INDEX_NAME = String.format("%s/INDEX.LIST", META_INF);

    /**
     * The JAR manifest attribute to list the embedded JAR files within a Java archive.
     */
    private static final String NESTED_DEPENDENCIES = "Nested-Dependencies";

    private Archives() { }

    /**
     * Returns the JAR manifest attribute listing the embedded dependency paths for the given name.
     *
     * @param name the name of the dependency list; may be <code>null</code>
     *
     * @return the JAR manifest entry listing the embedded dependency paths for the given name.
     */
    public static String nestedDependencies(final String name) {
        return name == null || name.isEmpty() ? NESTED_DEPENDENCIES : String.format("%s-%s", NESTED_DEPENDENCIES, name);
    }

    /**
     * Reads entries from a JAR file.
     *
     * @param jar         the URL of the JAR file.
     * @param reader      the object that reads the JAR entries.
     *
     * @return the number of entries read.
     *
     * @throws IOException when something goes wrong reading the JAR file.
     */
    public static int readEntries(final URL jar, final Entry reader) throws IOException {
        assert jar != null;
        final InputStream stream = jar.openStream();

        try {
            return readEntries(stream, reader);
        } finally {
            try {
                stream.close();
            } catch (final IOException e) {
                // ignore
            }
        }
    }

    /**
     * Reads entries from a JAR file. The input stream will <em>not</em> be {@linkplain InputStream#close() closed} by this method.
     *
     * @param input  the stream to load the JAR file from.
     * @param reader the object that reads the JAR entries.
     *
     * @return the number of entries read.
     *
     * @throws IOException when something goes wrong reading the JAR file.
     */
    public static int readEntries(final InputStream input, final Entry reader) throws IOException {
        assert input != null;
        final JarInputStream stream = new JarInputStream(input, false);

        int count = 0;
        JarEntry entry;
        while ((entry = stream.getNextJarEntry()) != null) {
            try {
                if (!entry.isDirectory()) {
                    if (reader.matches(entry)) {
                        ++count;

                        if (!reader.read(entry, stream)) {
                            break;
                        }
                    }
                }
            } finally {
                try {
                    stream.closeEntry();
                } catch (final IOException e) {
                    // ignore
                }
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
     * @return an array of strings, each being the value of the attribute name at the same index in the <code>names</code> parameter or <code>null</code>.
     */
    public static String[] manifestAttributes(final Class<?> type, final String... names) {
        return manifestAttributes(ClassLoaders.findClassResource(type),
                                  String.format("Can't find class loader for %s", type),
                                  String.format("Class %s was not loaded from a JAR file: %s", type.getName(), ClassLoaders.findClassResource(type)),
                                  names);
    }

    /**
     * Loads manifest attributes from the JAR file where the given resource was loaded from.
     *
     * @param url   the resource whose source JAR is to be processed.
     * @param names the list of attribute names to load.
     *
     * @return an array of strings, each being the value of the attribute name at the same index in the <code>names</code> parameter or <code>null</code>.
     */
    public static String[] manifestAttributes(final URL url, final String... names) {
        return manifestAttributes(url, "Resource URL is null", String.format("Resource was not loaded from a JAR file: %s", url), names);
    }

    private static String[] manifestAttributes(final URL url, final String nullURL, final String notJAR, final String... names) {
        if (url == null) {
            throw new IllegalArgumentException(nullURL);
        }

        try {
            final JarURLConnection connection = jarFile(url);
            final URL jar = connection == null ? url : connection.getJarFileURL();

            if (jar != null) {
                final Manifest manifest = loadManifest(jar);
                final Attributes attributes = manifest == null ? null : manifest.getMainAttributes();

                final List<String> list = new ArrayList<String>();
                for (final String name : names) {
                    list.add(attributes == null ? null : attributes.getValue(name));
                }

                return list.toArray(new String[list.size()]);
            } else {
                throw new IllegalArgumentException(notJAR);
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
    public static Manifest loadManifest(final URL url) throws IOException {
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
     * Returns a <code>JarURLConnection</code> for the given JAR URL (an URL into a JAR file) that allows {@linkplain JarURLConnection#getAttributes() loading
     * the JAR manifest} or finding the {@linkplain JarURLConnection#getJarFileURL() base JAR file URL} of the given URL.
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
     * Returns the JAR file that contains the given type.
     *
     * @param type   the Java class to find.
     *
     * @return the JAR URL containing the given type.
     *
     * @throws IllegalArgumentException if the given type is not loaded from a JAR file.
     */
    public static URL containing(final Class<?> type) throws IllegalArgumentException {
        final URL source = ClassLoaders.findClassResource(type);
        final URL jar = jarFile(source).getJarFileURL();

        if (jar == null) {
            throw new IllegalArgumentException(String.format("Class %s was not loaded from a JAR file: %s", type.getName(), source));
        } else {
            return jar;
        }
    }

    /**
     * Used by {@link Archives#readEntries(URL, Archives.Entry) Archives.readEntries()} to select and read entries in a JAR file.
     * <h3>Usage</h3>
     * See {@link Archives}.
     *
     * @author Tibor Varga
     */
    public interface Entry {

        /**
         * Tells if the {@link #read(JarEntry, InputStream) Entry.read()} method should be invoked with the given entry.
         *
         * @param entry the entry to decide about; never <code>null</code>.
         *
         * @return <code>true</code> if the given entry should be passed to the {@link #read(JarEntry, InputStream) Entry.read()} method, <code>false</code> if
         * not.
         *
         * @throws IOException when something goes wrong reading the JAR file.
         */
        boolean matches(JarEntry entry) throws IOException;

        /**
         * Reads the given entry.
         *
         * @param entry  the entry to read.
         * @param stream the stream containing the entry's content; must <em>not</em> be {@link InputStream#close() closed} by the receiver.
         *
         * @return <code>true</code> if further searching is needed, <code>false</code> if search should terminate.
         *
         * @throws IOException when something goes wrong reading the JAR file.
         */
        boolean read(JarEntry entry, InputStream stream) throws IOException;
    }

    /**
     * Convenience methods to handle nested JAR archives.
     *
     * @author Tibor Varga
     */
    public static final class Nested extends Utility {

        /**
         * The URL protocol understood by this embedded JAR URL handler.
         */
        public static final String PROTOCOL = Handler.PROTOCOL;

        /**
         * The path component delimiter in a valid embedded JAR URL.
         */
        public static final String DELIMITER = Handler.DELIMITER;

        private Nested() { }

        /**
         * Creates a URL that will target an entry in a JAR archive nested in other JAR archives, at any level.
         *
         * @param root  the URL of the outermost (root) JAR archive.
         * @param file  optional file path inside the nested JAR archive; may be <code>null</code>.
         * @param paths the list of JAR archive paths relative to the preceding JAR archive in the list, or the <code>root</code> archive in case of the first
         *              path.
         *
         * @return a "jar:" URL to target the given <code>file</code> in a nested JAR archive.
         *
         * @throws IOException if URL handling fails.
         */
        public static URL formatURL(final URL root, final String file, final String... paths) throws IOException {
            return Handler.formatURL(root, file, paths);
        }

        /**
         * Returns the root URL of the given URL returned by a previous call to {@link #formatURL(URL, String, String...)}. The returned URL can then be fed back
         * to {@link #formatURL(URL, String, String...)} to target other nested JAR archives.
         *
         * @param url the URL to return the root of.
         *
         * @return the root URL.
         *
         * @throws IOException when processing the URL fails.
         */
        public static URL rootURL(final URL url) throws IOException {
            return Handler.rootURL(url);
        }

        /**
         * Unloads a AR archive identified by its URL that was previously loaded to cache nested JAR archives found within. The protocol of the URL must either be
         * "jar" or "jarjar", as produced by {@link org.fluidity.foundation.jarjar.Handler#formatURL(java.net.URL, String, String...)}.
         *
         * @param url the URL to the JAR archive to unload.
         */
        public static void unload(final URL url) throws IOException {
            Handler.unload(url);
        }
    }
}

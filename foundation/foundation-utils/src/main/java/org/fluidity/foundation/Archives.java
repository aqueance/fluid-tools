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
import java.util.Collection;
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
     * Reads the main attributes from the manifest of the JAR file where the given class was loaded from.
     *
     * @param type the class whose source JAR is to be processed.
     * @param names  the list of attribute names to load.
     *
     * @return an array of strings, each being the value of the attribute name at the same index in the <code>names</code> parameter or <code>null</code>.
     */
    public static String[] mainAttributes(final Class<?> type, final String... names) throws IOException {
        return attributes(ClassLoaders.findClassResource(type), String.format("Can't find class loader for %s", type), names);
    }

    /**
     * Returns the main attributes with the given names from manifest of the JAR file identified by the given URL.
     *
     * @param url   the URL, pointing either to a JAR resource or an archive itself.
     * @param names the list of attribute names to load.
     *
     * @return an array of strings, each being the value of the attribute name at the same index in the <code>names</code> parameter or <code>null</code>.
     *
     * @throws IllegalStateException when the given URL is not a JAR file
     * @throws IOException           when an I/O error occurs when accessing its manifest
     */
    public static String[] mainAttributes(final URL url, final String... names) throws IOException {
        return attributes(url, "Resource URL is null", names);
    }

    private static String[] attributes(final URL url, final String nullURL, final String... names) throws IOException {
        if (url == null) {
            throw new IllegalStateException(nullURL);
        }

        final URL jar = jar(url);
        final Manifest manifest = loadManifest(jar == null ? url : jar);    // not a JAR resource, assume it's the archive itself
        final Attributes attributes = manifest == null ? null : manifest.getMainAttributes();

        final List<String> list = new ArrayList<String>();

        for (final String name : names) {
            list.add(attributes == null ? null : attributes.getValue(name));
        }

        return list.toArray(new String[list.size()]);
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
     * Returns the <code>URL</code> for the Java archive that the given JAR URL points into.
     *
     * @param url the URL to interpret as a JAR URL.
     *
     * @return the <code>URL</code> for the Java archive that the given JAR URL points into.
     */
    private static URL jar(final URL url) {
        try {
            final URLConnection connection = url.openConnection();
            return connection instanceof JarURLConnection ? ((JarURLConnection) connection).getJarFileURL() : null;
        } catch (final IOException e) {
            return null;
        }
    }

    /**
     * Returns the JAR file that contains the given type.
     *
     * @param type the Java class to find.
     *
     * @return the JAR URL containing the given type.
     *
     * @throws IllegalStateException if the given type is not loaded from a JAR file.
     */
    public static URL containing(final Class<?> type) throws IllegalStateException {
        final URL source = ClassLoaders.findClassResource(type);
        return containing(source, String.format("Class %s was not loaded from a JAR file: %s", type.getName(), source));
    }

    /**
     * Returns the URL for JAR file that the given URL is relative to.
     *
     * @param url the nested URL.
     *
     * @return the URL for JAR file that the given URL is relative to.
     *
     * @throws IllegalStateException if the given URL is not relative to a JAR file.
     */
    public static URL containing(final URL url) throws IllegalStateException {
        return containing(url, String.format("%s is not a JAR URL", url));
    }

    /**
     * Returns the root JAR file that the given URL is relative to.
     *
     * @param url    the nested URL.
     * @param notJAR the error to report if the given URL is not a JAR resource URL.
     *
     * @return the root JAR file that the given URL is relative to.
     *
     * @throws IllegalStateException if the given URL is not relative to a JAR file.
     */
    private static URL containing(final URL url, final String notJAR) throws IllegalStateException {
        final URL jar = jar(url);

        if (jar == null) {
            throw new IllegalStateException(notJAR);
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
     * <h3>Usage</h3>
     * TODO
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

        /**
         * Returns the JAR manifest attribute listing the embedded dependency paths for the given name.
         *
         * @param name the name of the dependency list; may be <code>null</code>
         *
         * @return the JAR manifest entry listing the embedded dependency paths for the given name.
         */
        public static String attribute(final String name) {
            return name == null || name.isEmpty() ? NESTED_DEPENDENCIES : String.format("%s-%s", NESTED_DEPENDENCIES, name);
        }

        /**
         * Returns the list of URLs pointing to the named list of embedded archives.
         *
         * @param name the name of the dependency list; may be <code>null</code>
         *
         * @return the list of URLs pointing to the named list of embedded archives.
         *
         * @throws IllegalStateException if there is no dependency list with the given name.
         * @throws IOException           when I/O error occurs when accessing the archive.
         */
        public static Collection<URL> dependencies(final String name) throws IOException, IllegalStateException  {
            return dependencies(Archives.containing(Archives.class), name, false);
        }

        /**
         * Returns the list of URLs pointing to the named list of embedded archives.
         *
         * @param archive the URL to the Java archive to inspect
         * @param name    the name of the dependency list; may be <code>null</code>
         * @param jar     if <code>true</code>, the returned URLs will be JAR URLs ready to append paths contained therein; if <code>false</code>, the returned
         *                URLs will point to the nested JAR files themselves.
         *
         * @return the list of URLs pointing to the named list of embedded archives.
         *
         * @throws IllegalStateException if there is no dependency list with the given name.
         * @throws IOException           when I/O error occurs when accessing the archive.
         */
        public static Collection<URL> dependencies(final URL archive, final String name, final boolean jar) throws IOException, IllegalStateException  {
            final String list = attribute(name);
            final String dependencies = Archives.mainAttributes(archive, list)[0];

            if (dependencies == null) {
                throw new IllegalStateException(String.format("%s is not defined in the archive manifest of %s", list, archive));
            }

            final List<URL> urls = new ArrayList<URL>();

            for (final String dependency : dependencies.split(" ")) {
                final URL url = Handler.formatURL(archive, null, dependency);
                urls.add(jar ? url : Archives.containing(url));
            }

            return urls;
        }
    }
}

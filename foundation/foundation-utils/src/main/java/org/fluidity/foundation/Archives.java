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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
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
     * Name of the <code>APP-INF</code> directory in EAR files.
     */
    @SuppressWarnings("UnusedDeclaration")
    public static final String APP_INF = "APP-INF";

    /**
     * Name of the JAR index file in JAR files.
     */
    public static final String INDEX_NAME = String.format("%s/INDEX.LIST", META_INF);

    /**
     * The JAR manifest attribute to list the embedded JAR files within a Java archive.
     */
    private static final String NESTED_DEPENDENCIES = "Nested-Dependencies";

    /**
     * The JAR resource URL protocol.
     */
    public static final String PROTOCOL = "jar:";
    /**
     * The JAR resource URL path component delimiter in a valid URL.
     */
    public static final String DELIMITER = "!/";

    private Archives() { }

    /**
     * Reads entries from a JAR file.
     *
     * @param jar    the URL of the JAR file.
     * @param reader the object that reads the JAR entries.
     *
     * @return the number of entries read.
     *
     * @throws IOException when something goes wrong reading the JAR file.
     */
    public static int readEntries(final URL jar, final Entry reader) throws IOException {
        assert jar != null;
        final InputStream stream = jar.openStream();

        try {
            return Archives.readEntries(stream, reader);
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
     * @param type  the class whose source JAR is to be processed.
     * @param names the list of attribute names to load.
     *
     * @return an array of strings, each being the value of the attribute name at the same index in the <code>names</code> parameter or <code>null</code>.
     */
    public static String[] mainAttributes(final Class<?> type, final String... names) throws IOException {
        return Archives.attributes(ClassLoaders.findClassResource(type), names);
    }

    /**
     * Returns the main attributes with the given names from manifest of the JAR file identified by the given URL.
     *
     * @param url   the URL, pointing either to a JAR resource or an archive itself.
     * @param names the list of attribute names to load.
     *
     * @return an array of strings, each being the value of the attribute name at the same index in the <code>names</code> parameter or <code>null</code>; may
     *         be <code>null</code> if the <code>url</code> parameter is <code>null</code>.
     *
     * @throws IOException when an I/O error occurs when accessing its manifest
     */
    public static String[] mainAttributes(final URL url, final String... names) throws IOException {
        return url == null ? null : Archives.attributes(url, names);
    }

    private static String[] attributes(final URL url, final String... names) throws IOException {
        final Manifest manifest = Archives.loadManifest(url);
        final Attributes attributes = manifest == null ? null : manifest.getMainAttributes();

        final String[] list = new String[names.length];

        if (attributes != null) {
            for (int i = 0, limit = names.length; i < limit; i++) {
                list[i] = attributes.getValue(names[i]);
            }
        }

        return list;
    }

    /**
     * Loads the JAR manifest from the given URL. Supports URLs pointing to a Java archive, JAR resource URLs pointing to any resource in a Java archive, URLs
     * pointing to a {@link JarFile#MANIFEST_NAME}, and URLs from which {@link JarFile#MANIFEST_NAME} can be loaded.
     *
     * @param url the URL.
     *
     * @return the JAR manifest.
     *
     * @throws IOException when reading the URL contents fails.
     */
    public static Manifest loadManifest(final URL url) throws IOException {
        final URL jar = Archives.containing(url);
        Manifest manifest = jar == null ? null : jarManifest(jar);

        if (manifest == null) {
            final InputStream stream = manifestStream(url);
            manifest = new Manifest();

            try {
                manifest.read(stream);
            } finally {
                stream.close();
            }

            if (manifest.getMainAttributes().isEmpty() && manifest.getEntries().isEmpty()) {
                return null;
            }
        }

        return manifest;
    }

    private static InputStream manifestStream(final URL url) throws IOException {
        InputStream stream;

        try {
            stream = new URL(String.format("%s%s%s%s", PROTOCOL, url.toExternalForm(), DELIMITER, JarFile.MANIFEST_NAME)).openStream();
        } catch (final IOException e) {
            stream = new URL(new URL(url, "/"), JarFile.MANIFEST_NAME).openStream();
        }

        return new BufferedInputStream(stream);
    }

    private static Manifest jarManifest(final URL url) throws IOException {
        final JarInputStream stream = new JarInputStream(url.openStream(), false);

        try {
            return stream.getManifest();
        } finally {
            stream.close();
        }
    }

    /**
     * Returns the URL for the JAR file that contains the given class.
     *
     * @param type the Java class to find.
     *
     * @return the JAR URL containing the given class; may be <code>null</code> if the given class is not loaded from a JAR file.
     *
     * @throws IOException when the given URL cannot be accessed.
     */
    public static URL containing(final Class<?> type) throws IOException {
        return Archives.containing(ClassLoaders.findClassResource(type));
    }

    /**
     * Returns the URL for JAR file that the given URL is relative to.
     *
     * @param url the nested URL.
     *
     * @return the URL for JAR file that the given URL is relative to; may be <code>null</code> if the given URL is not relative to a JAR file.
     *
     * @throws IOException when the given URL cannot be accessed.
     */
    public static URL containing(final URL url) throws IOException {
        final URLConnection connection = url.openConnection();
        return connection instanceof JarURLConnection ? ((JarURLConnection) connection).getJarFileURL() : null;
    }

    /**
     * Returns the URL for the JAR file that loaded this class.
     *
     * @return the URL for the JAR file that loaded this class.
     *
     * @throws IOException when the given URL cannot be accessed.
     */
    public static URL rootURL() throws IOException {
        return Archives.containing(Archives.class);
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
         *         not.
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
     * <pre>
     * for (final URL <span class="hl2">url</span> : <span class="hl1">Archives.Nested</span>.<span class="hl1">{@linkplain #dependencies(String) dependencies}</span>("widgets")) {
     *
     *     // make sure to select only those archives that have the
     *     // mandatory "Widget-Name" manifest attribute
     *     if ({@linkplain Archives}.{@linkplain Archives#mainAttributes(java.net.URL, String...) mainAttributes}(<span class="hl2">url</span>, "Widget-Name")[0] != null) {
     *         &hellip;
     *     }
     * }
     * </pre>
     *
     * @author Tibor Varga
     */
    public static final class Nested extends Utility {

        /**
         * The URL protocol understood by the embedded JAR URL handler.
         */
        public static final String PROTOCOL = Handler.PROTOCOL;

        /**
         * The path component delimiter in a valid embedded JAR URL.
         */
        public static final String DELIMITER = Handler.DELIMITER;

        private Nested() { }

        /**
         * Creates a URL to either a JAR archive nested in other JAR archives at any level, or an entry therein, depending on the absence or presence of the
         * <code>file</code> parameter, respectively.
         *
         * @param root  the URL of the (possibly nested) JAR archive.
         * @param file  optional file path inside the nested JAR archive; may be <code>null</code>.
         * @param paths the list of JAR archive paths relative to the preceding JAR archive in the list, or the <code>root</code> archive in case of the first
         *              path; may be empty.
         *
         * @return either a <code>jarjar:</code> or a <code>jar:</code> URL to either a JAR archive nested in other JAR archives at any level, or the given
         *         <code>file</code> entry therein, respectively.
         *
         * @throws IOException when URL handling fails.
         */
        public static URL formatURL(final URL root, final String file, final String... paths) throws IOException {
            return Handler.formatURL(root, file, paths);
        }

        /**
         * Returns the root URL of the given URL returned by a previous call to {@link #formatURL(URL, String, String...) formatURL()}. The returned URL can
         * then be fed back to {@link #formatURL(URL, String, String...) formatURL()} to target other nested JAR archives.
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
         * Unloads a AR archive identified by its URL that was previously loaded to cache nested JAR archives found within. The protocol of the URL must either
         * be "jar" or "jarjar", as produced by {@link org.fluidity.foundation.jarjar.Handler#formatURL(java.net.URL, String, String...)}.
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
         * Returns the list of URLs pointing to the named list of embedded archives. The returned URLs can then be appended further nested archives or
         * resources using the {@link Archives.Nested#formatURL(URL, String, String...) formatURL} method.
         *
         * @param name the name of the dependency list; may be <code>null</code>
         *
         * @return the list of URLs pointing to the named list of embedded archives; may be empty.
         *
         * @throws IOException when I/O error occurs when accessing the archive.
         */
        public static Collection<URL> dependencies(final String name) throws IOException {
            return Archives.Nested.dependencies(Archives.rootURL(), name);
        }

        /**
         * Returns the list of URLs pointing to the named list of embedded archives. The returned URLs can then be appended further nested archives or
         * resources using the {@link Archives.Nested#formatURL(URL, String, String...) formatURL} method.
         *
         * @param archive the URL to the Java archive to inspect
         * @param name    the name of the dependency list; may be <code>null</code>
         *
         * @return the list of URLs pointing to the named list of embedded archives; may be empty.
         *
         * @throws IOException when I/O error occurs when accessing the archive.
         */
        public static Collection<URL> dependencies(final URL archive, final String name) throws IOException {
            final String dependencies = archive == null ? null : Archives.mainAttributes(archive, Archives.Nested.attribute(name))[0];

            final Collection<URL> urls = new ArrayList<URL>();

            if (dependencies != null) {
                for (final String dependency : dependencies.split(" ")) {
                    urls.add(Handler.formatURL(archive, null, dependency));
                }
            }

            return urls;
        }
    }
}

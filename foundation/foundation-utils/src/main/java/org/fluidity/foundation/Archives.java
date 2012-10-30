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
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.fluidity.foundation.jarjar.Handler;

/**
 * Convenience methods to work with Java archives.
 * <h3>Usage Example</h3>
 * <pre>
 * {@link Archives#read(boolean, URL, Archives.Entry) Archives.read}({@linkplain Archives#containing(Class) Archives.containing}(getClass()), new <span class="hl1">Archives.Entry</span>() {
 *   public boolean <span class="hl1">matches</span>(final {@linkplain URL} url, final {@linkplain JarEntry} entry) throws {@linkplain IOException} {
 *     &hellip;
 *     return true;
 *   }
 *
 *   public boolean <span class="hl1">read</span>(final {@linkplain URL} url, final {@linkplain JarEntry} entry, final {@linkplain InputStream} stream) throws {@linkplain IOException} {
 *     &hellip;
 *     return true;
 *   }
 * });
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
    public static final String PROTOCOL = "jar";
    /**
     * The JAR resource URL path component delimiter in a valid URL.
     */
    public static final String DELIMITER = "!/";

    private Archives() { }

    /**
     * Reads entries from a JAR file. If present, the archive manifest is read first after the archive has been {@linkplain
     * JarInputStream#JarInputStream(InputStream, boolean) verified} but its {@linkplain JarEntry entry details} will be restricted to those available in a
     * {@link ZipEntry}.
     *
     * @param cached tells whether a previously cached archive, if any, should be used (value <code>true</code>), or a newly loaded one (value
     *               <code>false</code>).
     * @param url    the URL of the Java archives.
     * @param reader the reader to process the archive entries.
     *
     * @return the number of entries read.
     *
     * @throws IOException when something goes wrong reading the JAR file.
     */
    public static int read(final boolean cached, final URL url, final Entry reader) throws IOException {
        assert url != null;
        final byte[] data = Streams.load(Archives.open(cached, url), new byte[16384], true);
        final InputStream content = new ByteArrayInputStream(data);

        try {
            int count = 0;
            final JarInputStream jar = new JarInputStream(content, true);

            ZipEntry manifest;

            final ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(data));
            try {
                manifest = zip.getNextEntry();

                if (manifest != null) {
                    final String name = manifest.getName();

                    if (name.equals(META_INF.concat("/"))) {
                        zip.closeEntry();
                        manifest = zip.getNextEntry();
                    }

                    if (manifest != null && !name.equals(JarFile.MANIFEST_NAME)) {
                        manifest = null;
                    }

                    if (manifest != null) {
                        final JarEntry entry = new JarEntry(manifest);

                        if (reader.matches(url, entry)) {
                            ++count;

                            if (!reader.read(url, entry, new OpenInputStream(zip))) {
                                return count;
                            }
                        }
                    }
                }
            } finally {
                zip.close();
            }

            final InputStream stream = new OpenInputStream(jar);

            for (JarEntry entry; (entry = jar.getNextJarEntry()) != null; ) {
                try {
                    if (!entry.isDirectory()) {
                        if (reader.matches(url, entry)) {
                            ++count;

                            if (!reader.read(url, entry, stream)) {
                                return count;
                            }
                        }
                    }
                } finally {
                    jar.closeEntry();
                }
            }

            return count;
        } finally {
            content.close();
        }
    }

    /**
     * Opens an {@link InputStream} to the contents of the given URL.
     *
     * @param cached tells whether a previously cached archive, if any, should be used (value <code>true</code>), or a newly loaded one (value
     *               <code>false</code>).
     * @param url    the URL to open.
     *
     * @return an {@link InputStream}; never <code>null</code>.
     *
     * @throws IOException if the stream cannot be open.
     */
    public static InputStream open(final boolean cached, final URL url) throws IOException {
        final URLConnection connection = connection(url);
        connection.setUseCaches(cached);
        return connection.getInputStream();
    }

    private static URLConnection connection(final URL url) throws IOException {
        return url.openConnection();    // TODO: proxy?
    }

    /**
     * Returns the main attributes with the given names from manifest of the JAR file identified by the given URL.
     *
     * @param cached tells whether a previously cached archive, if any, should be used (value <code>true</code>), or a newly loaded one (value
     *               <code>false</code>).
     * @param url    the URL, pointing either to a JAR resource or an archive itself.
     * @param names  the list of attribute names to load.
     *
     * @return an array of strings, each being the value of the attribute name at the same index in the <code>names</code> parameter or <code>null</code>;
     *         never <code>null</code>.
     *
     * @throws IOException when an I/O error occurs when accessing its manifest
     */
    public static String[] attributes(final boolean cached, final URL url, final String... names) throws IOException {
        final String[] list = new String[names.length];

        if (url != null) {
            final Manifest manifest = Archives.manifest(cached, url);
            final Attributes attributes = manifest == null ? null : manifest.getMainAttributes();

            if (attributes != null) {
                for (int i = 0, limit = names.length; i < limit; i++) {
                    list[i] = attributes.getValue(names[i]);
                }
            }
        }

        return list;
    }

    /**
     * Returns the main attributes with the given names from manifest of the JAR file identified by the given URL.
     *
     * @param cached tells whether a previously cached archive, if any, should be used (value <code>true</code>), or a newly loaded one (value
     *               <code>false</code>).
     * @param url    the URL, pointing either to a JAR resource or an archive itself.
     * @param names  the list of attribute names to load.
     *
     * @return an array of strings, each being the value of the attribute name at the same index in the <code>names</code> parameter or <code>null</code>;
     *         never <code>null</code>.
     *
     * @throws IOException when an I/O error occurs when accessing its manifest
     */
    public static String[] attributes(final boolean cached, final URL url, final Attributes.Name... names) throws IOException {
        final String[] list = new String[names.length];

        if (url != null) {
            final Manifest manifest = Archives.manifest(cached, url);
            final Attributes attributes = manifest == null ? null : manifest.getMainAttributes();

            if (attributes != null) {
                for (int i = 0, limit = names.length; i < limit; i++) {
                    list[i] = attributes.getValue(names[i]);
                }
            }
        }

        return list;
    }

    /**
     * Loads the JAR manifest from the given URL. Supports URLs pointing to a Java archive, URLs pointing to any resource in a Java archive, URLs
     * pointing to a {@link JarFile#MANIFEST_NAME}, and URLs from which {@link JarFile#MANIFEST_NAME} can be loaded.
     *
     * @param cached tells whether a previously cached archive, if any, should be used (value <code>true</code>), or a newly loaded one (value
     *               <code>false</code>).
     * @param url    the URL.
     *
     * @return the JAR manifest; may be <code>null</code> if not found or has neither main nor entry attributes.
     *
     * @throws IOException when reading the URL contents fails.
     */
    public static Manifest manifest(final boolean cached, final URL url) throws IOException {
        final URL jar = Archives.containing(url);
        Manifest manifest = jar == null ? null : jarManifest(cached, jar);

        if (manifest == null) {
            manifest = new Manifest();

            InputStream stream;

            try {
                stream = manifestStream(cached, url);
            } catch (final IOException e) {
                stream = null;
            }

            if (stream != null) {
                try {
                    manifest.read(stream);
                } finally {
                    stream.close();
                }
            }

            if (manifest.getMainAttributes().isEmpty() && manifest.getEntries().isEmpty()) {
                return null;
            }
        }

        return manifest;
    }

    private static InputStream manifestStream(final boolean cached, final URL url) throws IOException {
        InputStream stream;

        try {
            stream = Archives.open(cached, new URL(String.format("%s:%s%s%s", PROTOCOL, url.toExternalForm(), DELIMITER, JarFile.MANIFEST_NAME)));
        } catch (final IOException e) {
            try {
                stream = Archives.open(cached, new URL(new URL(url, "/"), JarFile.MANIFEST_NAME));
            } catch (final IOException ignored) {
                return null;
            }
        }

        return new BufferedInputStream(stream);
    }

    private static Manifest jarManifest(final boolean cached, final URL url) throws IOException {
        final JarInputStream stream = new JarInputStream(Archives.open(cached, url), false);

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
     * Returns the URL for the Java archive that the given URL is relative to. The returned URL, when not <code>null</code> can be used with a {@link
     * URLClassLoader}.
     *
     * @param url the nested URL.
     *
     * @return the URL for the Java archive that the given URL is relative to; may be <code>null</code> if the given URL is not relative to a Java archive.
     *
     * @throws IOException when the Java URL cannot be created.
     */
    public static URL containing(final URL url) throws IOException {
        if (url != null && Archives.PROTOCOL.equals(url.getProtocol())) {
            final String file = url.getFile();
            final int delimiter = file.indexOf(DELIMITER);

            return new URL(delimiter == -1 ? file : file.substring(0, delimiter));
        }

        return null;
    }

    /**
     * Returns the URL for the JAR file that loaded this class.
     *
     * @return the URL for the JAR file that loaded this class.
     *
     * @throws IOException when the given URL cannot be accessed.
     */
    public static URL root() throws IOException {
        return Archives.containing(Archives.class);
    }

    /**
     * Used by {@link Archives#read(boolean, URL, Archives.Entry) Archives.read()} to select and read entries in a JAR file. The reader will not be invoked for
     * directory entries.
     * <h3>Usage</h3>
     * See {@link Archives}.
     *
     * @author Tibor Varga
     */
    public interface Entry {

        /**
         * Tells if the {@link #read(URL, JarEntry, InputStream) read()} method should be invoked with the given entry.
         *
         * @param url   the URL passed to the originating {@link Archives#read(boolean, URL, Archives.Entry) Archives.read()} call.
         * @param entry the entry in <code>url</code> to decide about; never <code>null</code>.
         *
         * @return <code>true</code> if the given entry should be passed to the {@link #read(URL, JarEntry, InputStream) read()} method, <code>false</code> if
         *         not.
         *
         * @throws IOException when something goes wrong reading the JAR file.
         */
        boolean matches(URL url, JarEntry entry) throws IOException;

        /**
         * Reads the given entry.
         *
         * @param url    the URL passed to the originating {@link Archives#read(boolean, URL, Archives.Entry) Archives.read()} call.
         * @param entry  the entry in <code>url</code> to read.
         * @param stream the stream containing the entry's content; must <em>not</em> be {@link InputStream#close() closed} by the receiver.
         *
         * @return <code>true</code> if further searching is needed, <code>false</code> if search should terminate.
         *
         * @throws IOException when something goes wrong reading the JAR file.
         */
        boolean read(URL url, JarEntry entry, InputStream stream) throws IOException;
    }

    /**
     * Convenience methods to handle nested Java archives.
     * <h3>Usage</h3>
     * <pre>
     * for (final URL <span class="hl2">url</span> : <span class="hl1">Archives.Nested</span>.<span class="hl1">{@linkplain #dependencies(String) dependencies}</span>("widgets")) {
     *
     *     // make sure to select only those archives that have the
     *     // mandatory "Widget-Name" manifest attribute
     *     if ({@linkplain Archives}.{@linkplain Archives#attributes(boolean, URL, String...) attributes}(<span class="hl2">url</span>, "Widget-Name")[0] != null) {
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
         * Creates a URL to either a JAR archive nested in other JAR archives at any level, or a resource entry in such a potentially nested archive, depending
         * on whether the last item in the <code>path</code> list is <code>null</code> or not, respectively.
         *
         * @param root the URL of the (possibly already nested) JAR archive.
         * @param path the list of entry names within the preceding archive entry in the list, or the <code>root</code> archive in case of the first
         *             path; may be empty, in which case the <code>root</code> URL is returned. If the last item is <code>null</code>, the returned URL will
         *             point to a nested archive; if the last item is <i>not</i> <code>null</code>, the returned URL will point to a JAR resource inside a
         *             (potentially nested) archive.
         *
         * @return either a <code>jarjar:</code> pointing to a nested archive, or a <code>jar:</code> URL pointing to a JAR resource.
         *
         * @throws IOException when URL handling fails.
         */
        public static URL formatURL(final URL root, final String... path) throws IOException {
            return Handler.formatURL(root, path);
        }

        /**
         * Returns the root URL of the given URL returned by a previous call to {@link #formatURL(URL, String...) formatURL()}. The returned URL can
         * then be fed back to {@link #formatURL(URL, String...) formatURL()} to target other nested Java archives.
         *
         * @param url the URL to return the root of.
         *
         * @return the root URL, which may be the given <code>url</code>; never <code>null</code>.
         *
         * @throws IOException when processing the URL fails.
         */
        public static URL rootURL(final URL url) throws IOException {
            return Handler.rootURL(url);
        }

        /**
         * Unloads a AR archive identified by its URL that was previously loaded to cache nested archives found within. The protocol of the URL must either be
         * "jar" or "jarjar", as produced by {@link Handler#formatURL(URL, String...)}.
         *
         * @param url the URL to the Java archive to unload.
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
         * Returns the list of URLs pointing to the named list of embedded archives. The returned URLs can then be extended with further nested archives or
         * resources using the {@link Archives.Nested#formatURL(URL, String...) formatURL} method.
         *
         * @param name the name of the dependency list; may be <code>null</code>
         *
         * @return the list of URLs pointing to the named list of embedded archives; may be empty.
         *
         * @throws IOException when I/O error occurs when accessing the archive.
         */
        public static Collection<URL> dependencies(final String name) throws IOException {
            return Archives.Nested.dependencies(Archives.root(), name);
        }

        /**
         * Returns the list of URLs pointing to the named list of embedded archives. The returned URLs can then be appended further nested archives or
         * resources using the {@link Archives.Nested#formatURL(URL, String...) formatURL} method.
         *
         * @param archive the URL to the Java archive to inspect
         * @param name    the name of the dependency list; may be <code>null</code>
         *
         * @return the list of URLs pointing to the named list of embedded archives; may be empty.
         *
         * @throws IOException when I/O error occurs when accessing the archive.
         */
        public static Collection<URL> dependencies(final URL archive, final String name) throws IOException {
            final String dependencies = Archives.attributes(true, archive, Archives.Nested.attribute(name))[0];
            final Collection<URL> urls = new ArrayList<URL>();

            if (dependencies != null) {
                for (final String dependency : dependencies.split(" ")) {
                    urls.add(Handler.formatURL(archive, dependency, null));
                }
            }

            return urls;
        }
    }

    /**
     * Rejects calls to {@link #close()} and delegates all other methods to an actual stream.
     *
     * @author Tibor Varga
     */
    private static class OpenInputStream extends FilterInputStream {

        OpenInputStream(final InputStream delegate) {
            super(delegate);
        }

        @Override
        public void close() throws IOException {
            throw new IOException("Entry content stream cannot be closed");
        }
    }
}

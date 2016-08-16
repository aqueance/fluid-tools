/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.fluidity.foundation.jarjar.Handler;

import static org.fluidity.foundation.Command.Process;

/**
 * Convenience methods to work with Java archives.
 * <h3>Usage Example</h3>
 * <pre>
 * {@link Archives#read(URL, boolean, Entry) Archives.read}({@linkplain Archives#containing(Class) Archives.containing}(getClass()), (url, entry) -&gt; {
 *     &hellip;
 *
 *     return (_url, _entry, stream) -&gt; {
 *         &hellip;
 *         return true;
 *     };
 * });
 * </pre>
 *
 * @author Tibor Varga
 */
@SuppressWarnings({ "WeakerAccess", "ThrowFromFinallyBlock" })
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
     * Name of the <code>OSGI-INF</code> directory in OSGi bundles.
     */
    public static final String OSGI_INF = "OSGI-INF";

    /**
     * Name of the JAR index file in JAR files.
     */
    public static final String INDEX_NAME = String.format("%s/INDEX.LIST", META_INF);

    /**
     * The name of the main manifest attribute that, when present, points to the security policy file for the archive.
     */
    public static final String SECURITY_POLICY = "Security-Policy";

    /**
     * The JAR resource URL protocol.
     */
    public static final String PROTOCOL = "jar";

    /**
     * The JAR resource URL path component delimiter in a valid URL.
     */
    public static final String DELIMITER = "!/";

    /**
     * The file URL protocol.
     */
    public static final String FILE = "file";

    private Archives() { }

    /**
     * Reads entries from a JAR file. If the archive manifest is the first entry in the archive, its {@linkplain JarEntry entry details} will be restricted to
     * those available in a {@link ZipEntry}.
     *
     * @param url     the URL of the Java archive; this will be passed to the {@link Entry} methods.
     * @param cached  tells whether a previously cached archive, if any, should be used (<code>true</code>), or a newly loaded one (<code>false</code>).
     * @param matcher the processor to send the entries to.
     *
     * @return the number of entries read.
     *
     * @throws IOException when something goes wrong reading the JAR file.
     */
    public static int read(final URL url, final boolean cached, final Entry matcher) throws IOException {
        assert url != null;

        try (final InputStream input = Archives.open(url, cached)) {
            return Archives.read(input, url, matcher);
        }
    }

    /**
     * Reads entries from a JAR file contained in the given stream. If the archive manifest is the first entry in the archive, its {@linkplain JarEntry entry
     * details} will be restricted to those available in a {@link ZipEntry}.
     *
     * @param input   the archive's content; the stream will <b>not</b> be automatically {@linkplain InputStream#close() closed}.
     * @param url     the URL archive is coming from; this will be passed to the {@link Entry} methods.
     * @param matcher the processor to send the entries to.
     *
     * @return the number of entries read.
     *
     * @throws IOException when something goes wrong reading the JAR file.
     */
    public static int read(final InputStream input, final URL url, final Entry matcher) throws IOException {
        return read(IOStreams.load(input, new byte[16384]), url, matcher);
    }

    /**
     * Reads entries from a JAR file contained in the given byte array. If the archive manifest is the first entry in the archive, its {@linkplain JarEntry
     * entry details} will be restricted to those available in a {@link ZipEntry}.
     *
     * @param data    the archive's content.
     * @param url     the URL archive is coming from; this will be passed to the {@link Entry#matches(URL, JarEntry)} method.
     * @param matcher the processor to send the entries to.
     *
     * @return the number of entries read.
     *
     * @throws IOException when something goes wrong reading the JAR file.
     */
    public static int read(final byte[] data, final URL url, final Entry matcher) throws IOException {
        int count = 0;

        // Can be close to your heart's content, so this can safely be outside the try-with-resource constructs
        final ByteArrayInputStream input = new ByteArrayInputStream(data);

        // Use a ZIP stream to access the JAR manifest, if present

        try (final ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry manifest = zip.getNextEntry();

            if (manifest != null) {
                if (manifest.getName().equals(META_INF.concat("/"))) {
                    zip.closeEntry();
                    manifest = zip.getNextEntry();
                }

                if (manifest != null && !manifest.getName().equals(JarFile.MANIFEST_NAME)) {
                    manifest = null;
                }

                if (manifest != null) {
                    final JarEntry entry = new JarEntry(manifest);
                    final Entry.Reader reader = matcher.matches(url, entry);

                    if (reader != null) {
                        ++count;

                        if (!reader.read(url, entry, zip)) {
                            return count;
                        }
                    }
                }
            }
        }

        input.reset();

        // The JAR stream hides the manifest entry, if present
        try (final JarInputStream jar = new JarInputStream(input, true)) {
            for (JarEntry entry; (entry = jar.getNextJarEntry()) != null; ) {
                try {
                    if (!entry.isDirectory()) {
                        final Entry.Reader reader = matcher.matches(url, entry);

                        if (reader != null) {
                            ++count;

                            if (!reader.read(url, entry, jar)) {
                                return count;
                            }
                        }
                    }
                } finally {
                    jar.closeEntry();
                }
            }
        }

        return count;
    }

    /**
     * Opens an {@link InputStream} for the contents of the given URL.
     *
     * @param url    the URL to open.
     * @param cached tells whether a previously cached archive, if any, should be used (<code>true</code>), or a newly loaded one (<code>false</code>).
     *
     * @return an {@link InputStream}; never <code>null</code>.
     *
     * @throws IOException if the stream cannot be open.
     */
    public static InputStream open(final URL url, final boolean cached) throws IOException {
        final byte[] data = cached ? Handler.cached(url) : null;
        return data != null ? new ByteArrayInputStream(data) : connect(url, cached).getInputStream();
    }

    /**
     * Creates a connection to the given URL.
     *
     * @param url    the URL to connect to.
     * @param cached tells whether a previously cached archive, if any, should be used (<code>true</code>), or a newly loaded one (<code>false</code>).
     *
     * @return an {@link URLConnection}; never <code>null</code>.
     *
     * @throws IOException when getting the connection fails.
     */
    public static URLConnection connect(final URL url, final boolean cached) throws IOException {
        final URLConnection connection = url.openConnection();
        connection.setUseCaches(cached);
        return connection;
    }

    /**
     * Returns the main attributes with the given names from manifest of the JAR file identified by the given URL.
     *
     * @param url    the URL, pointing either to a JAR resource or an archive itself.
     * @param cached tells whether a previously cached archive, if any, should be used (<code>true</code>), or a newly loaded one (<code>false</code>).
     * @param names  the list of attribute names to load.
     *
     * @return an array of strings, each being the value of the attribute name at the same index in the <code>names</code> parameter or <code>null</code>; never
     * <code>null</code>.
     *
     * @throws IOException when an I/O error occurs when accessing its manifest
     */
    public static String[] attributes(final URL url, final boolean cached, final String... names) throws IOException {
        final String[] list = new String[names.length];

        if (url != null) {
            final Attributes attributes = Archives.manifest(url, cached).getMainAttributes();

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
     * @param url    the URL, pointing either to a JAR resource or an archive itself.
     * @param cached tells whether a previously cached archive, if any, should be used (<code>true</code>), or a newly loaded one (<code>false</code>).
     * @param names  the list of attribute names to load.
     *
     * @return an array of strings, each being the value of the attribute name at the same index in the <code>names</code> parameter or <code>null</code>; never
     * <code>null</code>.
     *
     * @throws IOException when an I/O error occurs when accessing its manifest
     */
    public static String[] attributes(final URL url, final boolean cached, final Attributes.Name... names) throws IOException {
        final String[] list = new String[names.length];

        if (url != null) {
            final Attributes attributes = Archives.manifest(url, cached).getMainAttributes();

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
     * @param url    the URL.
     * @param cached tells whether a previously cached archive, if any, should be used (<code>true</code>), or a newly loaded one (<code>false</code>).
     *
     * @return the JAR manifest; may be <code>null</code> if not found or has neither main nor entry attributes.
     *
     * @throws IOException when reading the URL contents fails.
     */
    public static Manifest manifest(final URL url, final boolean cached) throws IOException {
        final String protocol = url.getProtocol();
        InputStream stream;

        if (url.toExternalForm().endsWith(JarFile.MANIFEST_NAME)) {

            // the URL points to the manifest file
            stream = Archives.open(url, cached);
        } else if (Nested.PROTOCOL.equals(protocol)) {

            // the URL points to a nested archive
            stream = Archives.open(Nested.formatURL(url, JarFile.MANIFEST_NAME), cached);
        } else if (Archives.FILE.equals(protocol) && localFile(url).isDirectory()) {

            // directories don't have manifest files
            stream = null;
        } else {
            try {

                // assume URL is an archive and see if it contains a manifest
                stream = Archives.open(Nested.formatURL(url, JarFile.MANIFEST_NAME), cached);
            } catch (final IOException e) {

                // apparently not an archive, give up
                try {

                    // try simple relative URL for the manifest
                    stream = Archives.open(new URL(url, JarFile.MANIFEST_NAME), cached);
                } catch (final FileNotFoundException ignored) {

                    // relative URL not found
                    stream = null;
                }
            }
        }

        final Manifest manifest = new Manifest();

        if (stream != null) {
            try {
                manifest.read(stream);
            } finally {
                stream.close();
            }
        }

        return manifest;
    }

    /**
     * Returns the local file underlying the given non-nested archive URL. This method first tries to URL-decode the path and
     * if the file exists, returns that; otherwise it returns the file at the URL's path without URL decoding.
     *
     * @param url the URL.
     *
     * @return the local file underlying the URL; may be <code>null</code>.
     */
    public static File localFile(final URL url) {
        assert url != null;
        assert FILE.equals(url.getProtocol()) : url;
        final String path = url.getPath().replace('/', File.separatorChar);

        try {
            final File file = new File(URLDecoder.decode(path, Strings.UTF_8.name()));
            boolean found = false;

            try {
                found = file.exists();
            } catch (final AccessControlException e) {
                // ignore
            }

            return found ? file : new File(path);
        } catch (final UnsupportedEncodingException e) {
            assert false : e;
            return null;
        }
    }

    /**
     * Returns the URL for the JAR file that contains the given class. If the URL does not locate an archive, an attempt is made to find the root URL for the
     * class.
     * <p>
     * The caller must have the {@link RuntimePermission} <code>"getClassLoader"</code> permission.
     *
     * @param type the Java class to find.
     *
     * @return the JAR URL containing the given class.
     */
    public static URL containing(final Class<?> type) {
        final URL resource = ClassLoaders.findClassResource(type);
        final URL url = Archives.containing(resource);

        if (url == null) {
            final StringBuilder relative = new StringBuilder();

            for (int i = 0, limit = type.getName().split("\\.").length - 1; i < limit; i++) {
                relative.append("../");
            }

            return validURL(() -> new URL(resource, relative.toString()));
        } else {
            return url;
        }
    }

    /**
     * Returns the URL for the Java archive that the given URL is relative to. The returned URL, if not <code>null</code>, can be used with a {@link
     * java.net.URLClassLoader}.
     *
     * @param url the nested URL.
     *
     * @return the URL for the Java archive that the given URL is relative to; may be <code>null</code> if the given URL is not relative to a Java archive.
     */
    public static URL containing(final URL url) {
        final String protocol = url == null ? null : url.getProtocol();

        if (protocol != null) {
            if (Archives.PROTOCOL.equals(protocol)) {
                final String file = url.getFile();
                final int delimiter = file.indexOf(DELIMITER);

                return validURL(() -> new URL(delimiter == -1 ? file : file.substring(0, delimiter)));
            } else if (Nested.PROTOCOL.equals(protocol)) {
                final String file = url.getFile();
                final int delimiter = file.lastIndexOf(Nested.DELIMITER);

                final String containing = delimiter == -1 ? file : file.substring(0, delimiter);
                return validURL(() -> containing.contains(Nested.DELIMITER) ? Archives.parseURL(String.format("%s:%s", Nested.PROTOCOL, containing)) : new URL(containing));
            }
        }

        return null;
    }

    /**
     * Parses the given URL specification and returns the parsed URL.
     *
     * @param specification the archive URL in text form.
     *
     * @return an {@link URL} with the given specification.
     *
     * @throws MalformedURLException when the specification does not make for a valid URL.
     */
    public static URL parseURL(final String specification) throws MalformedURLException {
        if (specification.startsWith(Nested.PROTOCOL.concat(":"))) {
            return Handler.parseURL(specification);
        } else {
            return new URL(specification);
        }
    }

    /**
     * Returns the resource path in the given URL relative to the given <code>archive</code>. The path will consist of all nested archives relative to the
     * given <code>archive</code> leading to a resource, the name of which will be the last element of the returned array.
     *
     * @param url     the URL, based on the <code>archive</code>, that refers a resource.
     * @param archive the archive underlying the URL; normally returned by one or more (nested) invocations of {@link #containing(URL)}.
     *
     * @return a resource name relative to the given <code>archive</code> in the given URL; may be <code>null</code>.
     */
    public static String[] resourcePath(final URL url, final URL archive) {
        if (url == null) {
            return null;
        }

        final String protocol = url.getProtocol();
        final String stem = archive.toExternalForm();

        final String[] path = relative(protocol, stem, url.toExternalForm());
        return path == null ? relative(protocol, stem, url.getFile()) : path;
    }

    /**
     * Asserts that the created URL is valid and returns it.
     *
     * @param factory The factory to create the URL.
     *
     * @return The created URL.
     */
    private static URL validURL(final Process<URL, IOException> factory) {
        try {
            return factory.run();
        } catch (final IOException e) {
            throw new AssertionError(e);
        }
    }

    private static final String[] NO_STRING = new String[0];

    private static String[] relative(final String protocol, final String archive, final String url) {
        if (url.startsWith(archive)) {
            if (url.equals(archive)) {
                return NO_STRING;
            } else if (Nested.PROTOCOL.equals(protocol)) {
                return url.substring(archive.length() + Nested.DELIMITER.length()).split(Nested.DELIMITER);
            } else {
                final int delimiter = url.indexOf('/', archive.length());
                return new String[] { delimiter < 0 ? "/" : url.substring(delimiter + 1) };
            }
        }

        return null;
    }

    /**
     * Returns the URL for the JAR file that loaded this class.
     * <p>
     * The caller must have the {@link RuntimePermission} <code>"getClassLoader"</code> permission.
     *
     * @return the URL for the JAR file that loaded this class.
     */
    public static URL root() {
        return Archives.containing(Archives.class);
    }

    /**
     * Creates a relative URL from the given <code>base</code> URL and a <code>resource</code> in it, using the optional <code>factory</code> if necessary.
     *
     * @param base     the base URL to form the relative URL to.
     * @param resource the resource in the base URL.
     * @param factory  the URL stream handler to use; may be <code>null</code>.
     *
     * @return a relative URL.
     *
     * @throws MalformedURLException if the relative URL cannot be formed.
     */
    public static URL relativeURL(final URL base, final String resource, final URLStreamHandlerFactory factory) throws MalformedURLException {
        try {
            return new URL(base, resource);
        } catch (final MalformedURLException e) {
            final int colon = resource.indexOf(':');
            final URLStreamHandler handler = colon == -1 || factory == null ? null : factory.createURLStreamHandler(resource.substring(colon));

            if (handler != null) {
                return new URL(base, resource, handler);
            } else {
                throw e;
            }
        }
    }

    /**
     * Selects and reads entries in a JAR file. The reader will not be invoked for directory entries. Used by {@link Archives#read(URL, boolean, Entry)}, {@link
     * Archives#read(byte[], URL, Entry)}, and {@link Archives#read(InputStream, URL, Entry)}.
     * <h3>Usage</h3>
     * See {@link Archives}.
     *
     * @author Tibor Varga
     */
    @FunctionalInterface
    public interface Entry {

        /**
         * Determines on an entry by entry basis whether to read an archive identified by its URL.
         *
         * @param url   the URL passed to the originating {@link Archives#read(URL, boolean, Entry) Archives.read()} call.
         * @param entry the entry in <code>url</code> to decide about; never <code>null</code>.
         *
         * @return An entry reader if the given entry should be read, <code>null</code> otherwise.
         *
         * @throws IOException when something goes wrong reading the JAR file.
         */
        Reader matches(URL url, JarEntry entry) throws IOException;

        /**
         * Reads one entry from an archive. The object is returned by {@link Entry#matches(URL, JarEntry)}.
         *
         * @author Tibor Varga
         */
        @FunctionalInterface
        interface Reader {

            /**
             * Reads the given entry.
             *
             * @param url    the URL passed to the originating {@link Archives#read(URL, boolean, Entry) Archives.read()} call.
             * @param entry  the entry in <code>url</code> to read.
             * @param stream the stream containing the entry's content; this stream must <b>not</b> be {@link InputStream#close() closed}.
             *
             * @return <code>true</code> if further searching is needed, <code>false</code> if search should terminate.
             *
             * @throws IOException when something goes wrong reading the JAR file.
             */
            boolean read(URL url, JarEntry entry, InputStream stream) throws IOException;
        }
    }

    /**
     * Convenience methods to handle nested Java archives.
     * <h3>Usage</h3>
     * <pre>
     * for (final URL <span class="hl2">url</span> : <span class="hl1">Archives.Nested</span>.<span class="hl1">{@linkplain #dependencies(boolean, String) dependencies}</span>(true, "widgets")) {
     *
     *     // make sure to select only those archives that have the
     *     // mandatory "Widget-Name" manifest attribute
     *     if ({@linkplain Archives}.{@linkplain Archives#attributes(URL, boolean, String...) attributes}(<span class="hl2">url</span>, "Widget-Name")[0] != null) {
     *         &hellip;
     *     }
     * }
     * </pre>
     *
     * @author Tibor Varga
     */
    public static final class Nested extends Utility {

        /**
         * The JAR manifest attribute to list the embedded JAR files within a Java archive.
         */
        private static final String DEPENDENCIES = "Nested-Dependencies";

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
         * Creates a URL to a resources in a JAR archive nested in other JAR archives at any level.
         *
         * @param root  the URL of the (possibly already nested) JAR archive.
         * @param path  the list of entry names within the preceding archive entry in the list, or the <code>root</code> archive in case of the first
         *              path; may be empty, in which case the <code>root</code> URL is returned. If the last item is <code>null</code>, the returned URL will point
         *              to a nested archive; if the last item is <i>not</i> <code>null</code>, the returned URL will point to a JAR resource inside a (potentially
         *              nested) archive.
         *
         * @return a <code>jarjar:</code> pointing either to a nested archive, or a JAR resource within a nested archive.
         *
         * @throws IOException when URL handling fails.
         */
        public static URL formatURL(final URL root, final String... path) throws IOException {
            return Handler.formatURL(root, path);
        }

        /**
         * Takes a JAR resource URL that happens to point to a nested archive, say <code>jar:file:/tmp/archive.jar!/nested.jar</code>, and converts that URL to
         * a nested archive URL pointing to the same resource that can be used to access the contents of that nested archive.
         *
         * @param archive the JAR resource URL of the (possibly already nested) JAR archive.
         *
         * @return a <code>jarjar:</code> pointing either to a nested archive, or a JAR resource within a nested archive.
         *
         * @throws IOException when URL handling fails.
         */
        public static URL nestedURL(final URL archive) throws IOException {
            final String protocol = archive == null ? null : archive.getProtocol();

            if (protocol == null || !Archives.PROTOCOL.equals(protocol)) {
                return archive;
            }

            final String parts[] = archive.getFile().split(Archives.DELIMITER);

            if (parts.length < 2) {
                return archive;
            }

            final String path[] = new String[parts.length - 1];
            System.arraycopy(parts, 1, path, 0, path.length);

            return Handler.formatURL(new URL(parts[0]), (String[]) path);
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
         * Returns the JAR manifest attribute listing the embedded dependency paths for the given name.
         *
         * @param name the name of the dependency list; may be <code>null</code>
         *
         * @return the JAR manifest entry listing the embedded dependency paths for the given name.
         */
        public static String attribute(final String name) {
            return name == null || name.isEmpty() ? DEPENDENCIES : String.format("%s-%s", DEPENDENCIES, name);
        }

        /**
         * Returns the list of URLs pointing to the named list of embedded archives. The returned URLs can then be extended with further nested archives or
         * resources using the {@link Archives.Nested#formatURL(URL, String...) formatURL} method.
         * <p>
         * The caller must have the {@link RuntimePermission} <code>"getClassLoader"</code> permission.
         *
         * @param cached tells whether a previously cached archive, if any, should be used (<code>true</code>), or a newly loaded one (<code>false</code>).
         * @param name   the name of the dependency list; may be <code>null</code>
         *
         * @return the list of URLs pointing to the named list of embedded archives; may be empty.
         *
         * @throws IOException when I/O error occurs when accessing the archive.
         */
        public static Collection<URL> dependencies(final boolean cached, final String name) throws IOException {
            return Nested.dependencies(cached, Archives.root(), name);
        }

        /**
         * Returns the list of URLs pointing to the named list of embedded archives. The returned URLs can then be appended further nested archives or
         * resources using the {@link Archives.Nested#formatURL(URL, String...) formatURL} method.
         *
         * @param cached  tells whether a previously cached archive, if any, should be used (<code>true</code>), or a newly loaded one (<code>false</code>).
         * @param archive the URL to the Java archive to inspect
         * @param name    the name of the dependency list; may be <code>null</code>
         *
         * @return the list of URLs pointing to the named list of embedded archives; may be empty.
         *
         * @throws IOException when I/O error occurs when accessing the archive.
         */
        public static Collection<URL> dependencies(final boolean cached, final URL archive, final String name) throws IOException {
            final String dependencies = Archives.attributes(archive, cached, Nested.attribute(name))[0];
            final Collection<URL> urls = new ArrayList<>();

            if (dependencies != null) {
                for (final String dependency : dependencies.split(" ")) {
                    urls.add(Handler.formatURL(archive, dependency));
                }
            }

            return urls;
        }

        /**
         * Returns the list of custom nested dependency names in the given archive. The returned names can then be fed to {@link #dependencies(boolean,
         * String)}.
         * <p>
         * The caller must have the {@link RuntimePermission} <code>"getClassLoader"</code> permission.
         *
         * @param cached tells whether a previously cached archive, if any, should be used (<code>true</code>), or a newly loaded one (<code>false</code>).
         *
         * @return a possibly empty list of nested dependencies; never <code>null</code>.
         *
         * @throws IOException when reading the archive fails.
         */
        public static String[] list(final boolean cached) throws IOException {
            return list(Archives.manifest(Archives.root(), cached));
        }

        /**
         * Returns the list of custom nested dependency names in the given archive. The returned names can then be fed to {@link #dependencies(boolean, URL,
         * String)}.
         *
         * @param archive the archive to look for nested dependencies in.
         *
         * @param cached  tells whether a previously cached archive, if any, should be used (<code>true</code>), or a newly loaded one (<code>false</code>).
         * @return a possibly empty list of nested dependencies; never <code>null</code>.
         *
         * @throws IOException when reading the archive fails.
         */
        public static String[] list(final URL archive, final boolean cached) throws IOException {
            return list(Archives.manifest(archive, cached));
        }

        /**
         * Returns the list of custom nested dependency names in the given archive. The returned names can then be fed to {@link #dependencies(boolean, URL,
         * String)}.
         *
         * @param manifest the manifest entries to check for nested dependency headers.
         *
         * @return a possibly empty list of nested dependencies; never <code>null</code>.
         */
        public static String[] list(final Manifest manifest) {
            final List<String> list = new ArrayList<>();

            final String prefix = DEPENDENCIES.concat("-");
            final int length = prefix.length();

            for (final Object name : manifest.getMainAttributes().keySet()) {
                final String key = name != null ? name.toString() : null;

                if (key != null && key.startsWith(prefix)) {
                    list.add(key.substring(length));
                }
            }

            return Lists.asArray(String.class, list);
        }
    }

    /**
     * Archives cache related functions.
     * <h3>Usage</h3>
     * <h4>Automatic Cache Eviction</h4>
     * <pre>
     * final <span class="hl3">String[]</span> values = <span class="hl1">Archives.Cache</span>.{@linkplain #access(Command.Process) access}(new {@linkplain Command.Process}&lt;<span class="hl3">String[]</span>, <span class="hl3">IOException</span>&gt;() {
     *   public <span class="hl3">String[]</span> run() throws <span class="hl3">IOException</span> {
     *     &hellip; // cache access
     *
     *     return …;
     *   }
     * });
     * </pre>
     * <h4>Capturing Cache Contents</h4>
     * <pre>
     * final <span class="hl3">Object</span> <span class="hl2">context</span> = <span class="hl1">Archives.Cache</span>.{@linkplain #access(Command.Process) access}(new {@linkplain Command.Process}&lt;<span class="hl3">Object</span>, RuntimeException&gt;() {
     *   public <span class="hl3">Object</span> run() {
     *     &hellip; // cache access
     *
     *     return <span class="hl1">Archives.Cache</span>.{@linkplain #capture(boolean) capture}(true);
     *   }
     * });
     *
     * &hellip;
     *
     * final <span class="hl3">String[]</span> values = <span class="hl1">Archives.Cache</span>.{@linkplain #access(Object, Command.Process) access}(<span class="hl2">context</span>, new {@linkplain Command.Process}&lt;<span class="hl3">String[]</span>, <span class="hl3">IOException</span>&gt;() {
     *   public <span class="hl3">String[]</span> run() throws <span class="hl3">IOException</span> {
     *     &hellip; // computation
     *
     *     return …;
     *   }
     * });
     * </pre>
     *
     * @author Tibor Varga
     */
    public static class Cache extends Utility {

        private Cache() { }

        /**
         * Isolates the effects on the caching of nested archives of the given <code>command</code> from the rest of the application. The isolated cache is
         * inherited by threads created by <code>command</code> but it will not be stable outside a call to this method by a new thread made while this call was
         * still in scope.
         *
         * @param command the command that potentially accesses nested archives.
         * @param <T>     the return type of the command.
         * @param <E>     the exception type thrown by the command.
         *
         * @return whatever the command returns.
         *
         * @throws E the exception that the given command can potentially throw.
         */
        public static <T, E extends Exception> T access(final Process<T, E> command) throws E {
            return Handler.access(command);
        }

        /**
         * Captures the current content of the archives cache.
         *
         * @param active if <code>true</code>, only the active items are retained from the cache, all else is dropped; if <code>false</code>, all items will be
         *               retained. Active items are those that have been accessed in the nearest enclosing invocation of {@link #access(Object,
         *               Command.Process)}.
         *
         * @return a cache context.
         */
        public static Object capture(final boolean active) {
            return Handler.capture(active);
        }

        /**
         * Isolates the effects on the caching of nested archives of the given <code>command</code> from the rest of the application. The isolated cache is
         * inherited by threads created by <code>command</code> but it will not be stable outside a call to this method by a new thread made while this call was
         * still in scope.
         * <p>
         * The initial content of the cache will consist of what has been {@linkplain #capture captured} in the given <code>context</code> and whatever else is
         * in the cache at the point of invocation.
         *
         * @param context the cache contents captured using a previous {@link #capture} call.
         * @param command the command that potentially accesses nested archives.
         * @param <T>     the return type of the command.
         * @param <E>     the exception type thrown by the command.
         *
         * @return whatever the command returns.
         *
         * @throws E the exception that the given command can potentially throw.
         */
        public static <T, E extends Exception> T access(final Object context, final Process<T, E> command) throws E {
            return Handler.access(context, command);
        }
    }
}

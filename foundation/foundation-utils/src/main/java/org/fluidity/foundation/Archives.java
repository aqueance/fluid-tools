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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
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
     * @param cached tells whether a previously cached archive, if any, should be used (<code>true</code>), or a newly loaded one (<code>false</code>).
     * @param url    the URL of the Java archive; this will be passed to the {@link Entry} methods.
     * @param reader the reader to process the archive entries.
     *
     * @return the number of entries read.
     *
     * @throws IOException when something goes wrong reading the JAR file.
     */
    public static int read(final boolean cached, final URL url, final Entry reader) throws IOException {
        assert url != null;
        return Archives.read(Archives.open(cached, url), url, reader);
    }

    /**
     * Reads entries from a JAR file contained in the given stream. If the archive manifest is the first entry in the archive, its {@linkplain
     * JarEntry entry details} will be restricted to those available in a {@link ZipEntry}.
     *
     * @param input  the archive's content; the stream will be {@linkplain InputStream#close() closed} after reading it.
     * @param url    the URL archive is coming from; this will be passed to the {@link Entry} methods.
     * @param reader the reader to process the archive entries.
     *
     * @return the number of entries read.
     *
     * @throws IOException when something goes wrong reading the JAR file.
     */
    public static int read(final InputStream input, final URL url, final Entry reader) throws IOException {
        return read(Streams.load(input, new byte[16384], true), url, reader);
    }

    /**
     * Reads entries from a JAR file contained in the given byte array. If the archive manifest is the first entry in the archive, its {@linkplain
     * JarEntry entry details} will be restricted to those available in a {@link ZipEntry}.
     *
     * @param data   the archive's content.
     * @param url    the URL archive is coming from; this will be passed to the {@link Entry} methods.
     * @param reader the reader to process the archive entries.
     *
     * @return the number of entries read.
     *
     * @throws IOException when something goes wrong reading the JAR file.
     */
    public static int read(final byte[] data, final URL url, final Entry reader) throws IOException {
        final InputStream content = new ByteArrayInputStream(data);

        int count = 0;
        final JarInputStream jar = new JarInputStream(content, true);

        ZipEntry manifest;

        final ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(data));
        manifest = zip.getNextEntry();

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

                if (reader.matches(url, entry)) {
                    ++count;

                    if (!reader.read(url, entry, new OpenInputStream(zip))) {
                        return count;
                    }
                }
            }
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
    }

    /**
     * Opens an {@link InputStream} to the contents of the given URL.
     *
     * @param cached tells whether a previously cached archive, if any, should be used (<code>true</code>), or a newly loaded one (<code>false</code>).
     * @param url    the URL to open.
     *
     * @return an {@link InputStream}; never <code>null</code>.
     *
     * @throws IOException if the stream cannot be open.
     */
    public static InputStream open(final boolean cached, final URL url) throws IOException {
        final byte[] data = cached ? Handler.cached(url) : null;
        return data != null ? new ByteArrayInputStream(data) : connection(cached, url).getInputStream();
    }

    /**
     * Creates a connection to the given URL.
     *
     * @param cached tells whether a previously cached archive, if any, should be used (<code>true</code>), or a newly loaded one (<code>false</code>).
     * @param url    the URL to connect to.
     *
     * @return an {@link URLConnection}; never <code>null</code>.
     *
     * @throws IOException when getting the connection fails.
     */
    public static URLConnection connection(final boolean cached, final URL url) throws IOException {
        final URLConnection connection = url.openConnection();
        connection.setUseCaches(cached);
        return connection;
    }

    /**
     * Returns the main attributes with the given names from manifest of the JAR file identified by the given URL.
     *
     * @param cached tells whether a previously cached archive, if any, should be used (<code>true</code>), or a newly loaded one (<code>false</code>).
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
            final Attributes attributes = Archives.manifest(cached, url).getMainAttributes();

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
     * @param cached tells whether a previously cached archive, if any, should be used (<code>true</code>), or a newly loaded one (<code>false</code>).
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
            final Attributes attributes = Archives.manifest(cached, url).getMainAttributes();

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
     * @param cached tells whether a previously cached archive, if any, should be used (<code>true</code>), or a newly loaded one (<code>false</code>).
     * @param url    the URL.
     *
     * @return the JAR manifest; may be <code>null</code> if not found or has neither main nor entry attributes.
     *
     * @throws IOException when reading the URL contents fails.
     */
    public static Manifest manifest(final boolean cached, final URL url) throws IOException {
        final String protocol = url.getProtocol();
        InputStream stream;

        if (url.toExternalForm().endsWith(JarFile.MANIFEST_NAME)) {

            // the URL points to the manifest file
            stream = Archives.open(cached, url);
        } else if (Nested.PROTOCOL.equals(protocol)) {

            // the URL points to a nested archive
            stream = Archives.open(cached, Nested.formatURL(url, JarFile.MANIFEST_NAME));
        } else if (Archives.FILE.equals(protocol) && localFile(url).isDirectory()) {

            // directories don't have manifest files
            stream = null;
        } else {
            try {

                // assume URL is an archive and see if it contains a manifest
                stream = Archives.open(cached, Nested.formatURL(url, JarFile.MANIFEST_NAME));
            } catch (final IOException e) {

                // apparently not an archive, give up
                try {

                    // try simple relative URL for the manifest
                    stream = Archives.open(cached, new URL(url, JarFile.MANIFEST_NAME));
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
            final File file = new File(URLDecoder.decode(path, "UTF-8"));
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
     * <p/>
     * The caller must have the {@link RuntimePermission} <code>"getClassLoader"</code> permission.
     *
     * @param type the Java class to find.
     *
     * @return the JAR URL containing the given class.
     *
     * @throws IOException when the given URL cannot be accessed.
     */
    public static URL containing(final Class<?> type) throws IOException {
        final URL resource = ClassLoaders.findClassResource(type);
        final URL url = Archives.containing(resource);

        if (url == null) {
            final StringBuilder relative = new StringBuilder();

            for (int i = 0, limit = type.getName().split("\\.").length - 1; i < limit; i++) {
                relative.append("../");
            }

            return new URL(resource, relative.toString());
        } else {
            return url;
        }
    }

    /**
     * Returns the URL for the Java archive that the given URL is relative to. The returned URL, when not <code>null</code> can be used with a {@link
     * java.net.URLClassLoader}.
     *
     * @param url the nested URL.
     *
     * @return the URL for the Java archive that the given URL is relative to; may be <code>null</code> if the given URL is not relative to a Java archive.
     *
     * @throws MalformedURLException when the Java URL cannot be created.
     */
    public static URL containing(final URL url) throws MalformedURLException {
        final String protocol = url == null ? null : url.getProtocol();

        if (protocol != null) {
            if (Archives.PROTOCOL.equals(protocol)) {
                final String file = url.getFile();
                final int delimiter = file.indexOf(DELIMITER);

                return new URL(delimiter == -1 ? file : file.substring(0, delimiter));
            } else if (Nested.PROTOCOL.equals(protocol)) {
                final String file = url.getFile();
                final int delimiter = file.lastIndexOf(Nested.DELIMITER);

                final String containing = delimiter == -1 ? file : file.substring(0, delimiter);
                return containing.contains(Nested.DELIMITER) ? Archives.parseURL(String.format("%s:%s", Nested.PROTOCOL, containing)) : new URL(containing);
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
     * @throws MalformedURLException when the Java URL cannot be created.
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
     * <p/>
     * The caller must have the {@link RuntimePermission} <code>"getClassLoader"</code> permission.
     *
     * @return the URL for the JAR file that loaded this class.
     *
     * @throws IOException when the given URL cannot be accessed.
     */
    public static URL root() throws IOException {
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
         * @param stream the stream containing the entry's content; ignores {@link InputStream#close()} calls.
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
     * for (final URL <span class="hl2">url</span> : <span class="hl1">Archives.Nested</span>.<span class="hl1">{@linkplain #dependencies(boolean, String) dependencies}</span>(true, "widgets")) {
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
         * <p/>
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
            final String dependencies = Archives.attributes(cached, archive, Nested.attribute(name))[0];
            final Collection<URL> urls = new ArrayList<URL>();

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
         * <p/>
         * The caller must have the {@link RuntimePermission} <code>"getClassLoader"</code> permission.
         *
         * @param cached tells whether a previously cached archive, if any, should be used (<code>true</code>), or a newly loaded one (<code>false</code>).
         *
         * @return a possibly empty list of nested dependencies; never <code>null</code>.
         *
         * @throws IOException when reading the archive fails.
         */
        public static String[] list(final boolean cached) throws IOException {
            return list(Archives.manifest(cached, Archives.root()));
        }

        /**
         * Returns the list of custom nested dependency names in the given archive. The returned names can then be fed to {@link #dependencies(boolean, URL,
         * String)}.
         *
         * @param cached  tells whether a previously cached archive, if any, should be used (<code>true</code>), or a newly loaded one (<code>false</code>).
         * @param archive the archive to look for nested dependencies in.
         *
         * @return a possibly empty list of nested dependencies; never <code>null</code>.
         *
         * @throws IOException when reading the archive fails.
         */
        public static String[] list(final boolean cached, final URL archive) throws IOException {
            return list(Archives.manifest(cached, archive));
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
            final List<String> list = new ArrayList<String>();

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

        /**
         * Unloads from the cache the root JAR archive that contains the archive identified by the given URL.
         *
         * @param url the URL identifying the Java archive to unload.
         */
        public static void unload(final URL url) {
            Handler.unload(url);
        }
    }

    /**
     * Archives cache related functions.
     * <h3>Usage</h3>
     * TODO
     *
     * @author Tibor Varga
     */
    public static class Cache extends Utility {

        private Cache() { }

        /**
         * Isolates the effects on the caching of nested archives of the given <code>command</code> from the rest of the application. The isolated cache is
         * inherited by threads created by <code>command</code> but it will not be stable outside a call to this method by a new thread made while this call
         * was still in scope.
         *
         * @param command the command that potentially accesses nested archives.
         * @param <T>     the return type of the command.
         * @param <E>     the exception type thrown by the command.
         *
         * @return whatever the command returns.
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
         * inherited by threads created by <code>command</code> but it will not be stable outside a call to this method by a new thread made while this call
         * was still in scope.
         * <p/>
         * The initial content of the cache will consist of what has been {@linkplain #capture captured} in the given <code>context</code> and
         * whatever else is in the cache at the point of invocation.
         *
         * @param context the cache contents captured using a previous {@link #capture} call.
         * @param command the command that potentially accesses nested archives.
         * @param <T>     the return type of the command.
         * @param <E>     the exception type thrown by the command.
         *
         * @return whatever the command returns.
         */
        public static <T, E extends Exception> T access(final Object context, final Process<T, E> command) throws E {
            return Handler.access(context, command);
        }
    }

    /**
     * Ignores calls to {@link #close()} and delegates all other methods to an actual stream.
     *
     * @author Tibor Varga
     */
    private static class OpenInputStream extends FilterInputStream {

        OpenInputStream(final InputStream delegate) {
            super(delegate);
        }

        @Override
        public void close() throws IOException {
            // ignored
        }
    }
}

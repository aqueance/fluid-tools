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

package org.fluidity.foundation.jarjar;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.AccessControlException;
import java.security.Permission;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.fluidity.foundation.Archives;
import org.fluidity.foundation.ClassLoaders;
import org.fluidity.foundation.Streams;
import org.fluidity.foundation.Strings;

/**
 * {@linkplain URLStreamHandler Stream protocol handler} to work with resources inside JAR archives embedded in other JAR archives, ad infinitum.
 * <p/>
 * When loaded, it adds its enclosing package to the protocol handler package list to make itself known. This stream protocol handler makes it possible for an
 * ordinary {@link java.net.URLClassLoader} to work with JAR archives embedded in other JAR archives, enabling packaged Java applications without loss of
 * functionality such as signed JAR files, etc.
 * <p/>
 * To use this stream handler, all you need to do is {@linkplain #formatURL(URL, String, String...) create} URLs that map to this stream protocol handler. For
 * example, if you have a JAR archive named <code>my-archive.jar</code> that embeds another JAR archive, <code>my-dependency.jar</code>, the following will
 * create an URL that can then be fed to an URL class loader:
 * <pre>
 * final URL embedded = Handler.formatURL(new File("my-archive.jar").toURI().toURL(), null, "my-dependency.jar");
 * </pre>
 *
 * @author Tibor Varga
 */
public final class Handler extends URLStreamHandler {

    /**
     * The URL protocol understood by this handler. The value is computed to be the last component of the containing package.
     */
    public static final String PROTOCOL;

    /**
     * The path component delimiter in a valid URL.
     */
    public static final String DELIMITER = "!:/";      // must not be the one used by the JAR handler

    private static final String PROTOCOL_HANDLERS_PROPERTY = "java.protocol.handler.pkgs";

    static {
        assert !DELIMITER.equals(Archives.DELIMITER);

        final String canonicalName = Handler.class.getName();
        final int dot = canonicalName.lastIndexOf('.', canonicalName.length() - Handler.class.getSimpleName().length() - 2);

        if (dot < 0) {
            throw new IllegalStateException(String.format("Class %s must be in a package at least two levels deep", Handler.class.getName()));
        }

        final String packageName = canonicalName.substring(0, dot);

        try {
            final String property = System.getProperty(PROTOCOL_HANDLERS_PROPERTY);

            if (property == null || !Arrays.asList(property.split("|")).contains(packageName)) {
                System.setProperty(PROTOCOL_HANDLERS_PROPERTY, property == null ? packageName : String.format("%s|%s", packageName, property));
            }
        } catch (final AccessControlException e) {
            // fine, we'll just assume our parent package has been registered already
        }

        PROTOCOL = canonicalName.substring(dot + 1, canonicalName.lastIndexOf('.'));
    }

    /**
     * Used by the URL stream handler framework. Do not call this constructor directly.
     */
    public Handler() { }

    @Override
    protected URLConnection openConnection(final URL url) throws IOException {
        return new Connection(url);
    }

    @Override
    protected void parseURL(final URL url, final String spec, final int start, final int limit) {
        final String string = spec.substring(start, limit);
        final int delimiter = string.indexOf(DELIMITER);

        if (delimiter < 0) {
            setURL(url, PROTOCOL, string, -1, null, null, "", null, null);
        } else {
            setURL(url, url.getProtocol(), string.substring(0, delimiter), -1, null, null, string.substring(delimiter), null, null);
        }
    }

    @Override
    protected String toExternalForm(final URL url) {
        try {
            return String.format("%s:%s%s", url.getProtocol(), new URL(url.getHost()).toExternalForm(), url.getPath());
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

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
        if (root == null) {
            return null;
        }

        final String stem = root.toExternalForm();
        final Strings.Listing specification = Strings.delimited(DELIMITER);

        if (PROTOCOL.equals(root.getProtocol())) {
            specification.set(stem);
        } else {
            final URLConnection connection = root.openConnection();

            if (connection instanceof JarURLConnection) {

                // we have a JAR URL but we need the enclosed URL and the JAR file path relative to the URL, separately
                final URL url = ((JarURLConnection) connection).getJarFileURL();

                if (PROTOCOL.equals(url.getProtocol())) {
                    specification.set(url.toExternalForm());
                } else {
                    final String path = root.getPath().split(Archives.DELIMITER, -1)[1];      // parsing a JAR URL, not our own URL

                    specification.set(PROTOCOL).append(':').append(url.toExternalForm());
                    specification.add(path);
                }
            } else {
                specification.set(PROTOCOL).append(':').append(stem);
            }
        }

        for (final String path : paths) {
            specification.add(ClassLoaders.absoluteResourceName(path));
        }

        if (!specification.toString().contains(DELIMITER)) {
            specification.set(stem);
        }

        if (file != null) {
            specification.surround(Archives.PROTOCOL, Archives.DELIMITER);
            specification.append(file);
        }

        try {
            return new URL(specification.toString());
        } catch (final MalformedURLException e) {
            return new URL(null, specification.toString(), Singleton.INSTANCE);
        }
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
        final URL jarjar = unwrap(url);
        return PROTOCOL.equals(jarjar.getProtocol()) ? new URL(jarjar.getHost()) : url;
    }

    /**
     * Unloads the contents of the given URL from the caches.
     *
     * @param url the URL to unload the contents of.
     *
     * @throws IOException when processing the URL fails.
     */
    public static void unload(final URL url) throws IOException {
        final URL jarjar = unwrap(url);

        if (PROTOCOL.equals(jarjar.getProtocol())) {
            contents.remove(new URL(jarjar.getHost()));
        }
    }

    private static URL unwrap(final URL url) throws IOException {
        return url.openConnection() instanceof JarURLConnection ? new URL(url.getFile()) : url;
    }

    private static final Map<URL, Map<String, byte[]>> contents = new HashMap<URL, Map<String, byte[]>>();

    static byte[] contents(final URL url) throws IOException {
        final byte[] data = load(url).get(url.getPath());
        return data == null ? null : data;
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    static Map<String, byte[]> load(final URL url) throws IOException {
        assert url != null;

        final URL root = new URL(url.getHost());
        Map<String, byte[]> content = contents.get(root);

        if (content == null) {
            content = new HashMap<String, byte[]>();

            synchronized (content) {
                synchronized (contents) {
                    if (contents.containsKey(root)) {
                        content = contents.get(root);

                        synchronized (content) {
                            return content;
                        }
                    } else {
                        contents.put(root, content);
                    }
                }

                final URLConnection connection = root.openConnection();
                connection.setUseCaches(true);

                load(content, new HashMap<Metadata, String>(), new byte[1024 * 1024], "", new JarInputStream(connection.getInputStream()));

                return content;
            }
        } else {
            synchronized (content) {
                return content;
            }
        }
    }

    /**
     * @author Tibor Varga
     */
    private static class Metadata {

        private final String name;
        private final long size;
        private final long crc;

        Metadata(final String name, final long size, final long crc) {
            assert name != null;
            final String[] components = name.split("/");
            this.name = components[components.length - 1];
            this.size = size;
            this.crc = crc;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Metadata that = (Metadata) o;
            return size == that.size && crc == that.crc && name.equals(that.name);
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + (int) (size ^ (size >>> 32));
            result = 31 * result + (int) (crc ^ (crc >>> 32));
            return result;
        }

        @Override
        public String toString() {
            return String.format("%s (%d: %d)", name, size, crc);
        }
    }

    private static void load(final Map<String, byte[]> content, final Map<Metadata, String> meta, byte[] buffer, final String base, final JarInputStream stream) throws IOException {
        try {
            for (JarEntry next; (next = stream.getNextJarEntry()) != null; stream.closeEntry()) {
                final String name = next.getName();
                final String entry = String.format("%s%s%s", base, DELIMITER, name);

                final byte[] bytes = Streams.load(stream, buffer, false);

                if (new JarInputStream(new ByteArrayInputStream(bytes)).getManifest() != null) {
                    final Metadata metadata = new Metadata(name, next.getSize(), next.getCrc());
                    final String reference = meta.get(metadata);

                    if (reference == null) {
                        meta.put(metadata, entry);
                        content.put(entry, bytes);

                        load(content, meta, buffer, entry, new JarInputStream(new ByteArrayInputStream(bytes), false));
                    } else {
                        content.put(entry, content.get(reference));

                        final Map<String, byte[]> entries = new HashMap<String, byte[]>();
                        final String prefix = reference.concat(DELIMITER);

                        for (final Map.Entry<String, byte[]> candidate : content.entrySet()) {
                            final String key = candidate.getKey();

                            if (key.startsWith(prefix)) {
                                entries.put(entry.concat(key.substring(reference.length())), candidate.getValue());
                            }
                        }

                        content.putAll(entries);
                    }
                }
            }
        } finally {
            try {
                stream.close();
            } catch (final IOException e) {
                // ignore
            }
        }
    }

    /**
     * Lazy instantiation of a {@link Handler} object.
     *
     * @author Tibor Varga
     */
    private static class Singleton {

        public static final Handler INSTANCE = new Handler();
    }

    /**
     * An URL connection for the "jarjar:" URL protocol as required by the URL stream protocol handler framework.
     *
     * @author Tibor Varga
     */
    private static class Connection extends URLConnection {

        private final URLConnection root;

        Connection(final URL url) throws IOException {
            super(url);

            // the host part of our root URL itself is an URL
            this.root = new URL(getURL().getHost()).openConnection();
        }

        @Override
        public void connect() throws IOException {
            root.connect();
        }

        @Override
        public Permission getPermission() throws IOException {
            return root.getPermission();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (!getDoInput()) {
                throw new IllegalStateException(String.format("Input stream disabled on %s", url));
            }

            if (getUseCaches()) {
                final byte[] contents = contents(url);

                if (contents == null) {
                    throw new FileNotFoundException(url.toExternalForm());
                } else {
                    return new ByteArrayInputStream(contents);
                }
            } else {
                final JarInputStream container = new JarInputStream(root.getInputStream());

                // each successive path is nested in the stream at the previous index
                final String[] paths = url.getPath().split(DELIMITER);

                // first stream is the container
                JarInputStream stream = container;

                // the first path is an empty string since spec starts with a ! and we split around !s
                for (int i = 1, pathCount = paths.length; i < pathCount; i++) {
                    final String file = paths[i];
                    final String directory = file.endsWith("/") ? file : file.concat("/");

                    JarEntry next;
                    while ((next = stream.getNextJarEntry()) != null) {
                        final String name = next.getName();

                        if (!next.isDirectory()) {
                            if (file.equals(name)) {
                                if (i == pathCount - 1) {

                                    // caller must close the stream, which closes its input, closing all streams we just opened here
                                    return stream;
                                } else {

                                    // descend to entry
                                    stream = new JarInputStream(stream);
                                    break;
                                }
                            }
                        } else if (directory.equals(name)) {
                            try {
                                stream.close();
                            } catch (final IOException e) {
                                // ignore
                            }

                            throw new IOException(String.format("%s is a directory", url.toExternalForm()));
                        }

                        stream.closeEntry();
                    }
                }

                try {
                    stream.close();
                } catch (final IOException e) {
                    // ignore
                }

                throw new FileNotFoundException(url.toExternalForm());
            }
        }

        @Override
        public void setDoOutput(final boolean value) {
            if (value) {
                throw new IllegalStateException("Output not supported on nested URLs");
            }
        }
    }
}

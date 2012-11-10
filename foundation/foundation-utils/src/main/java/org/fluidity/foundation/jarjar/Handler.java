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
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.fluidity.foundation.Archives;
import org.fluidity.foundation.ClassLoaders;
import org.fluidity.foundation.Deferred;
import org.fluidity.foundation.Lists;
import org.fluidity.foundation.Streams;

import static org.fluidity.foundation.Command.Job;
import static org.fluidity.foundation.Command.Process;

/**
 * {@linkplain URLStreamHandler Stream protocol handler} to work with resources inside JAR archives embedded in other JAR archives, ad infinitum.
 * <p/>
 * When loaded, it adds its enclosing package to the protocol handler package list to make itself known. This stream protocol handler makes it possible for an
 * ordinary {@link java.net.URLClassLoader} to work with JAR archives embedded in other JAR archives, enabling packaged Java applications without loss of
 * functionality such as signed JAR files, etc.
 * <p/>
 * To use this stream handler, all you need to do is {@linkplain #formatURL(URL, String...) create} URLs that map to this stream protocol handler. For
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

    private static final String PACKAGE;

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

        PACKAGE = canonicalName.substring(0, dot);
        PROTOCOL = canonicalName.substring(dot + 1, canonicalName.lastIndexOf('.'));
    }

    private static final Deferred.Reference<Void> registration = Deferred.reference(new Deferred.Factory<Void>() {
        public Void create() {
            try {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    public Void run() {
                        final String property = System.getProperty(PROTOCOL_HANDLERS_PROPERTY);

                        if (property == null || !Arrays.asList(property.split("\\|")).contains(PACKAGE)) {
                            System.setProperty(PROTOCOL_HANDLERS_PROPERTY, property == null ? PACKAGE : String.format("%s|%s", PACKAGE, property));
                        }

                        return null;
                    }
                });
            } catch (final AccessControlException e) {
                // fine, we'll just assume our parent package has been registered already
            }

            return null;
        }
    });

    private static void initialize() {
        registration.get(); // guaranteed to run at most once
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
    protected URLConnection openConnection(final URL url, final Proxy proxy) throws IOException {
        return new Connection(url);
    }

    @Override
    protected void parseURL(final URL url, final String spec, final int start, final int limit) {

        /*
         * This method handles URL composition. The base URL is "url" while the one to compose with it is spec.substring(start, limit), which never contains
         * a protocol.
         *
         * The following cases are possible:
         *  * relative path: not supported, considered illegal
         *  * absolute path: supported
         *  * query string: handled by caller
         */

        assert PROTOCOL.equals(url.getProtocol()) : url;

        final String base = url.getPath();
        final String extra = spec.substring(start, limit);

        final String path;

        if (base != null) {
            final int delimiter1 = base.lastIndexOf(DELIMITER);
            final int delimiter2 = extra.lastIndexOf(DELIMITER);
            final int length = DELIMITER.length();

            final String paths[] = {
                    delimiter1 < 0 ? base : base.substring(delimiter1 + length),
                    delimiter2 < 0 ? extra : extra.substring(delimiter1 + length),
            };

            try {
                final URL relative = new URL(PROTOCOL, null, -1, paths[0]);
                super.parseURL(relative, paths[1], 0, paths[1].length());

                path = base.substring(0, delimiter1 + length).concat(relative.getPath());
            } catch (final MalformedURLException e) {
                throw new IllegalArgumentException(String.format("%s: %s", base, extra), e);
            }
        } else {
            path = extra;
        }

        final int delimiter = path.indexOf(DELIMITER);
        assert delimiter > 0 : path;

        final String enclosed = path.substring(0, delimiter);

        try {
            final URL valid = new URL(enclosed);

            if (PROTOCOL.equals(valid.getProtocol())) {
                throw new IllegalArgumentException(String.format("%s URLs may not enclose a %1$s URL", PROTOCOL));
            }
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException(String.format("%s URLs must enclose a valid URL", PROTOCOL));
        }

        setURL(url, PROTOCOL, null, -1, null, null, path, null, null);
    }

    /**
     * Returns the part of the nested archive URL that identifies the outermost enclosing archive.
     */
    static String enclosedURL(final URL url) {
        final String path = url.getFile();
        final int delimiter = path.indexOf(DELIMITER);
        return delimiter < 0 ? path : path.substring(0, delimiter);
    }

    @Override
    protected String toExternalForm(final URL url) {
        return String.format("%s:%s%s", url.getProtocol(), enclosedURL(url), Cache.path(url));
    }

    /**
     * Creates a URL to a resource in a JAR archive nested in other JAR archives at any level.
     *
     * @param root the URL of the (possibly already nested) JAR archive.
     * @param path the list of entry names within the preceding archive entry in the list, or the <code>root</code> archive in case of the first
     *             path; may be empty, in which case the <code>root</code> URL is returned. If the last item is <code>null</code>, the returned URL will point
     *             to a nested archive; if the last item is <i>not</i> <code>null</code>, the returned URL will point to a JAR resource inside a (potentially
     *             nested) archive.
     *
     * @return a <code>jarjar:</code> pointing either to a nested archive, or a JAR resource within a nested archive.
     *
     * @throws IOException when URL handling fails.
     */
    public static URL formatURL(final URL root, final String... path) throws IOException {
        Handler.initialize();

        if (root == null) {
            return null;
        } else if (path == null || path.length == 0) {
            return root;
        }

        final String stem = root.toExternalForm();
        final Lists.Delimited specification = Lists.delimited(DELIMITER);

        if (PROTOCOL.equals(root.getProtocol())) {
            specification.set(stem);
        } else {
            final URL url = Archives.containing(root);

            if (url != null) {
                if (PROTOCOL.equals(url.getProtocol())) {
                    specification.set(url.toExternalForm());
                } else {
                    specification.set(PROTOCOL).append(':').append(url.toExternalForm());
                }

                // find the JAR resource, if any
                final String[] parts = Cache.path(root).split(Archives.DELIMITER);

                if (parts.length > 1) {
                    specification.add(parts[1]);
                }
            } else {
                specification.set(PROTOCOL).append(':').append(stem);
            }
        }

        for (final String part : path) {
            if (part == null) {
                throw new MalformedURLException("Path list may not contain null values");
            }

            specification.add(ClassLoaders.absoluteResourceName(part));
        }

        if (!specification.toString().contains(DELIMITER)) {
            specification.set(stem);
        }

        try {
            return new URL(specification.toString());
        } catch (final MalformedURLException e) {
            return new URL(null, specification.toString(), Singleton.INSTANCE);
        }
    }

    /**
     * Returns the root URL of the given URL returned by a previous call to {@link #formatURL(URL, String...)}. The returned URL can then be fed back
     * to {@link #formatURL(URL, String...)} to target other nested JAR archives.
     *
     * @param url the URL to return the root of.
     *
     * @return the root URL.
     *
     * @throws IOException when processing the URL fails.
     */
    public static URL rootURL(final URL url) throws IOException {
        final URL jarjar = unwrap(url);
        return PROTOCOL.equals(jarjar.getProtocol()) ? new URL(enclosedURL(jarjar)) : url;
    }

    /**
     * Unloads the contents of the given URL from the caches.
     *
     * @param url the URL to unload the contents of.
     */
    public static void unload(final URL url) {
        Cache.unload(url);
    }

    /**
     * Isolates the effects on the caching of nested archives of the given <code>command</code> from the rest of the application. The isolated cache is
     * inherited by threads created by <code>command</code> but it will not be stable outside a call to this method or {@link
     * #access(org.fluidity.foundation.Command.Process)} by a new thread made while this call was still in scope.
     *
     * @param command the command that potentially accesses nested archives.
     * @param <E>     the exception type thrown by the command.
     */
    public static <E extends Exception> void access(final Job<E> command) throws E {
        Cache.access(command);
    }

    /**
     * Isolates the effects on the caching of nested archives of the given <code>command</code> from the rest of the application. The isolated cache is
     * inherited by threads created by <code>command</code> but it will not be stable outside a call to this method or {@link
     * #access(org.fluidity.foundation.Command.Job)} by a new thread made while this call was still in scope.
     *
     * @param command the command that potentially accesses nested archives.
     * @param <T>     the return type of the command.
     * @param <E>     the exception type thrown by the command.
     *
     * @return whatever the command returns.
     */
    public static <T, E extends Exception> T access(final Process<T, E> command) throws E {
        return Cache.access(command);
    }

    /**
     * Returns the cached contents of the given archive, loading it first if necessary.
     *
     * @param url the URL of the archive.
     *
     * @return the contents of the given archive, or <code>null</code> if the URL does not point to an archive.
     *
     * @throws IOException when loading the archive fails.
     */
    public static byte[] cached(final URL url) throws IOException {
        final Cache.Archive archive = Cache.archive(url);
        return PROTOCOL.equals(url.getProtocol()) ? archive.entry(Cache.path(url)) : archive.root();
    }

    private static URL unwrap(final URL url) throws IOException {
        final URL root = Archives.containing(url);
        return root == null ? url : root;
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
            final URL enclosed = new URL(enclosedURL(getURL()));
            assert !PROTOCOL.equals(enclosed.getProtocol()) : getURL();

            this.root = enclosed.openConnection();
        }

        @Override
        public void connect() throws IOException {
            root.connect();
        }

        @Override
        public Permission getPermission() throws IOException {
            return new AccessPermission(root.getPermission());
        }

        @Override
        public void setUseCaches(final boolean flag) {
            super.setUseCaches(flag);
            root.setUseCaches(flag);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (!getDoInput()) {
                throw new IllegalStateException(String.format("Input stream disabled on %s", url));
            }

            if (getUseCaches()) {
                final byte[] contents = Cache.contents(url);

                if (contents == null) {
                    throw new FileNotFoundException(url.toExternalForm());
                } else {
                    return new ByteArrayInputStream(contents);
                }
            } else {
                final byte[] buffer = new byte[16384];
                final InputStream found[] = { null };

                Archives.read(root.getInputStream(), url, new Archives.Entry() {

                    // each successive path is nested in the archive at the previous index
                    private final String[] paths = Cache.path(url).split(DELIMITER);

                    // the first path is ignored since that is the enclosing archive
                    private int index = 1;
                    private String file = paths[index];
                    private String directory = directory(file);

                    public boolean matches(final URL url, final JarEntry entry) throws IOException {
                        final String name = entry.getName();

                        if (entry.isDirectory() && name.equals(directory)) {
                            throw new IOException(String.format("Nested entry '%s' is a directory, URL is invalid: %s", name, url.toExternalForm()));
                        }

                        return file.equals(name);
                    }

                    public boolean read(final URL url, final JarEntry entry, final InputStream stream) throws IOException {
                        if (++index == paths.length) {
                            found[0] = new ByteArrayInputStream(Streams.copy(stream, new ByteArrayOutputStream(), buffer, false, false).toByteArray());
                        } else {
                            file = paths[index];
                            directory = directory(file);

                            Archives.read(stream, url, this);
                        }

                        return false;
                    }

                    private String directory(final String name) {
                        return name.endsWith("/") ? name : name.concat("/");
                    }
                });

                if (found[0] == null) {
                    throw new FileNotFoundException(url.toExternalForm());
                } else {
                    return found[0];
                }
            }
        }

        @Override
        public void setDoOutput(final boolean value) {
            if (value) {
                throw new IllegalStateException("Output not supported on nested URLs");
            }
        }
    }

    /**
     * Caches archive contents.
     *
     * @author Tibor Varga
     */
    static final class Cache {

        private static final String ROOT = "";

        /**
         * Represents the contents of a cached archive.
         *
         * @author Tibor Varga
         */
        static final class Archive {

            private final String archive;
            private final Map<String, byte[]> contents;
            private final int prefix;

            private Archive(final String archive, final Map<String, byte[]> contents) {
                this.archive = archive;
                this.contents = contents;
                this.prefix = archive.length() + 1;
            }

            /**
             * Returns the content of a named archive entry.
             *
             * @param name the name of the entry.
             *
             * @return a byte array; may be <code>null</code>.
             */
            @SuppressWarnings("StringBufferReplaceableByString")
            public byte[] entry(final String name) {
                return contents.get(name.isEmpty()
                                    ? archive
                                    : new StringBuilder(prefix + name.length()).append(archive).append(DELIMITER).append(name).toString());
            }

            /**
             * Returns the contents of the archive itself.
             *
             * @return the contents of the archive itself.
             */
            public byte[] root() {
                return entry(ROOT);
            }
        }

        /**
         * Returns the part of the nested archive URL that identifies the list of archives, each nested in the outermost archive.
         */
        static String path(final URL url) {
            final String path = url.getFile();
            final int delimiter = path.indexOf(DELIMITER);
            return delimiter < 0 ? path : path.substring(delimiter);
        }

        /**
         * Isolates the effects on the caching of nested archives of the given <code>command</code> from the rest of the application. The isolated cache is
         * inherited by threads created by <code>command</code> but it will not be stable outside a call to this method or {@link
         * #access(org.fluidity.foundation.Command.Process)} by a new thread made while this call was still in scope.
         *
         * @param command the command that potentially accesses nested archives.
         * @param <E>     the exception type thrown by the command.
         */
        static <E extends Exception> void access(final Job<E> command) throws E {
            access(new Process<Void, E>() {
                public Void run() throws E {
                    command.run();
                    return null;
                }
            });
        }

        /**
         * Isolates the effects on the caching of nested archives of the given <code>command</code> from the rest of the application. The isolated cache is
         * inherited by threads created by <code>command</code> but it will not be stable outside a call to this method or {@link
         * #access(org.fluidity.foundation.Command.Job)} by a new thread made while this call was still in scope.
         *
         * @param command the command that potentially accesses nested archives.
         * @param <T>     the return type of the command.
         * @param <E>     the exception type thrown by the command.
         *
         * @return whatever the command returns.
         */
        static <T, E extends Exception> T access(final Process<T, E> command) throws E {
            final Context saved = context.get();
            UUID id;

            synchronized (localCache) {
                for (id = UUID.randomUUID(); localCache.containsKey(id); id = UUID.randomUUID()) {
                    // empty
                }

                final Map<String, Map<String, byte[]>> map;

                synchronized (globalCache) {
                    map = new HashMap<String, Map<String, byte[]>>(globalCache);
                }

                localCache.put(id, map);

                Context local = saved;
                for (; local != null; local = local.last) {
                    map.putAll(localCache.get(local.id));
                }
            }

            context.set(new Context(id, saved));

            try {
                return command.run();
            } finally {
                context.set(saved);

                synchronized (localCache) {
                    localCache.remove(id);
                }
            }
        }

        static byte[] contents(final URL url) throws IOException {
            return load(new URL(enclosedURL(url))).get(path(url));
        }

        static Archive archive(final URL url) throws IOException {
            final boolean nested = PROTOCOL.equals(url.getProtocol());
            return new Archive(nested ? path(url) : ROOT, load(nested ? new URL(enclosedURL(url)) : url));
        }

        @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
        static Map<String, byte[]> load(final URL root) throws IOException {
            assert root != null;
            final String key = root.toExternalForm();

            final Map<String, Map<String, byte[]>> cache = localCache();
            Map<String, byte[]> content = cache.get(key);

            if (content == null) {
                content = new HashMap<String, byte[]>();

                synchronized (content) {
                    synchronized (cache) {
                        if (cache.containsKey(key)) {
                            content = cache.get(key);

                            synchronized (content) {
                                return content;
                            }
                        } else {
                            cache.put(key, content);
                        }
                    }

                    final URLConnection connection = root.openConnection();
                    connection.setUseCaches(true);

                    final byte[] buffer = new byte[1024 * 1024];
                    final byte[] data = Streams.load(connection.getInputStream(), buffer, true);

                    content.put(ROOT, data);

                    final ZipInputStream stream = new ZipInputStream(new ByteArrayInputStream(data));
                    try {
                        load(content, new HashMap<Metadata, String>(), buffer, ROOT, stream, stream.getNextEntry());
                    } catch (final IOException e) {
                        stream.close();
                    }

                    return content;
                }
            } else {
                synchronized (content) {
                    return content;
                }
            }
        }

        private static Map<String, Map<String, byte[]>> localCache() {
            final Context local = context.get();
            final UUID id = local == null ? null : local.id;
            assert local == null | id != null;

            final Map<String, Map<String, byte[]>> cache;

            synchronized (localCache) {
                cache = id == null ? globalCache : localCache.get(id);
            }

            assert cache != null;
            return cache;
        }

        private static void load(final Map<String, byte[]> content,
                                 final Map<Metadata, String> meta,
                                 final byte[] buffer,
                                 final String base,
                                 final ZipInputStream stream,
                                 final ZipEntry first) throws IOException {
            for (ZipEntry next = first; next != null; stream.closeEntry(), next = stream.getNextEntry()) {
                final String name = next.getName();

                if (!name.endsWith("/")) {
                    final String entry = String.format("%s%s%s", base, DELIMITER, name);
                    final byte[] bytes = Streams.load(stream, buffer, false);

                    final ZipInputStream nested = new ZipInputStream(new ByteArrayInputStream(bytes));
                    try {
                        final ZipEntry check = nested.getNextEntry();

                        if (check != null) {
                            final Metadata metadata = new Metadata(name, next.getSize(), next.getCrc());
                            final String reference = meta.get(metadata);

                            if (reference == null) {
                                meta.put(metadata, entry);
                                content.put(entry, bytes);

                                load(content, meta, buffer, entry, nested, check);
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
                        } else {
                            content.put(entry, bytes);
                        }
                    } finally {
                        nested.close();
                    }
                }
            }
        }

        /**
         * Unloads the contents of the given URL from the caches.
         *
         * @param url the URL to unload the contents of.
         */
        static void unload(final URL url) {
            if (PROTOCOL.equals(url.getProtocol())) {
                localCache().remove(enclosedURL(url));
            }
        }

        static boolean loaded(final URL url, final boolean local) {
            return (local ? localCache() : globalCache).containsKey(enclosedURL(url));
        }

        /**
         * @author Tibor Varga
         */
        private static class Context {

            public final UUID id;
            public final Context last;

            private Context(final UUID id, final Context last) {
                this.id = id;
                this.last = last;
            }
        }

        private static final ThreadLocal<Context> context = new InheritableThreadLocal<Context>() {
            @Override
            protected Context childValue(final Context parent) {
                return parent == null ? null : new Context(parent.id, null);
            }
        };

        private static final Map<UUID, Map<String, Map<String, byte[]>>> localCache = new HashMap<UUID, Map<String, Map<String, byte[]>>>();
        private static final Map<String, Map<String, byte[]>> globalCache = new HashMap<String, Map<String, byte[]>>();

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
    }
}

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
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.fluidity.foundation.Archives;
import org.fluidity.foundation.ClassLoaders;
import org.fluidity.foundation.Command;
import org.fluidity.foundation.Deferred;
import org.fluidity.foundation.Exceptions;
import org.fluidity.foundation.Lists;
import org.fluidity.foundation.Security;
import org.fluidity.foundation.Streams;

import static org.fluidity.foundation.Command.Process;

/**
 * {@linkplain URLStreamHandler Stream protocol handler} to work with resources inside JAR archives embedded in other JAR archives, ad infinitum.
 * <p>
 * When loaded, it adds its enclosing package to the protocol handler package list to make itself known. This stream protocol handler makes it possible for an
 * ordinary {@link java.net.URLClassLoader} to work with JAR archives embedded in other JAR archives, enabling packaged Java applications without loss of
 * functionality such as signed JAR files, etc.
 * <p>
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

    private static final Deferred.Reference<Void> registration = Deferred.shared(() -> {
        try {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                final String property = System.getProperty(PROTOCOL_HANDLERS_PROPERTY);

                if (property == null || !Arrays.asList(property.split("\\|")).contains(PACKAGE)) {
                    System.setProperty(PROTOCOL_HANDLERS_PROPERTY, property == null ? PACKAGE : String.format("%s|%s", PACKAGE, property));
                }

                return null;
            });
        } catch (final AccessControlException e) {
            // fine, we'll just assume our parent package has been registered already
        }

        return null;
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
                final URL relative = new URL(PROTOCOL, null, -1, paths[0], this);
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

        if (enclosed.startsWith(PROTOCOL.concat(":"))) {
            throw new IllegalArgumentException(String.format("%s URLs may not enclose a %1$s URL", PROTOCOL));
        }

        setURL(url, PROTOCOL, null, -1, null, null, path, null, null);
    }

    /**
     * Returns the part of the nested archive URL that identifies the outermost enclosing archive.
     */
    private static String enclosedURL(final URL url) {
        final String path = url.getFile();
        final int delimiter = path.indexOf(DELIMITER);
        return delimiter < 0 ? path : path.substring(0, delimiter);
    }

    @Override
    protected String toExternalForm(final URL url) {
        return String.format("%s:%s%s", url.getProtocol(), enclosedURL(url), Cache.path(url));
    }

    /**
     * Parses the given nested URL specification and returns the parsed URL.
     *
     * @param specification the nested archive URL in text form.
     *
     * @return an {@link URL} with the given specification.
     *
     * @throws MalformedURLException when the Java URL cannot be created.
     */
    public static URL parseURL(final String specification) throws MalformedURLException {
        try {
            return !Security.CONTROLLED
                   ? new URL(null, specification, Singleton.INSTANCE)
                   : AccessController.doPrivileged((PrivilegedExceptionAction<URL>) () -> new URL(null, specification, Singleton.INSTANCE));
        } catch (final PrivilegedActionException e) {
            throw (MalformedURLException) e.getCause();
        }
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
     * @throws MalformedURLException when URL formatting fails.
     */
    public static URL formatURL(final URL root, final String... path) throws MalformedURLException {
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

        return parseURL(specification.toString());
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
        return PROTOCOL.equals(url.getProtocol()) ? new URL(enclosedURL(url)) : url;
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
     * inherited by threads created by <code>command</code> but it will not be stable outside a call to this method  by a new thread made while this call was
     * still in scope.
     *
     * @param command the command that potentially accesses nested archives.
     * @param <T>     the return type of the command.
     * @param <E>     the exception type thrown by the command.
     *
     * @return whatever the command returns.
     *
     * @throws E as is thrown by the command.
     */
    public static <T, E extends Exception> T access(final Process<T, E> command) throws E {
        return Cache.access(command);
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
        return Cache.capture(active);
    }

    /**
     * Isolates the effects on the caching of nested archives of the given <code>command</code> from the rest of the application. The isolated cache is
     * inherited by threads created by <code>command</code> but it will not be stable outside a call to this method by a new thread made while this call
     * was still in scope.
     * <p>
     * The initial content of the cache will consist of what has been {@linkplain #capture(boolean) captured} in the given <code>context</code> and
     * whatever else is in the cache at the point of invocation.
     *
     * @param context the cache contents captured using a previous {@link #capture(boolean)} call.
     * @param command the command that potentially accesses nested archives.
     * @param <T>     the return type of the command.
     * @param <E>     the exception type thrown by the command.
     *
     * @return whatever the command returns.
     *
     * @throws E as is thrown by the command.
     */
    public static <T, E extends Exception> T access(final Object context, final Process<T, E> command) throws E {
        return Cache.access(context, command);
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
        return Cache.contents(url);
    }

    static URL relativeURL(final URL root, final String resource) throws MalformedURLException {
        return directory(root.getPath()) ? new URL(root, resource) : Handler.formatURL(root, resource);
    }

    private static boolean directory(final String name) {
        return name.isEmpty() || name.endsWith("/") || name.endsWith("\\");
    }

    /**
     * Lazy instantiation of a {@link Handler} object.
     *
     * @author Tibor Varga
     */
    private static class Singleton {

        static final Handler INSTANCE = new Handler();
    }

    /**
     * An URL connection for the "jarjar:" URL protocol as required by the URL stream protocol handler framework.
     *
     * @author Tibor Varga
     */
    private static class Connection extends URLConnection {

        private static final String CONTENT_LENGTH = "content-length";
        private static final String CONTENT_TYPE = "content-type";
        private static final String LAST_MODIFIED = "last-modified";
        private static final String UNKNOWN_TYPE = "application/octet-stream";

        private static final String[] FIELDS = new String[] { CONTENT_TYPE, CONTENT_LENGTH, LAST_MODIFIED };

        private final URLConnection root;
        private final String archive;

        private final Deferred.Reference<Map<String, List<String>>> headers;
        private final Deferred.Reference<InputStream> inputStream;

        Connection(final URL url) throws IOException {
            super(url);

            // the host part of our root URL itself is an URL
            this.archive = enclosedURL(getURL());

            final URL enclosed = new URL(this.archive);
            assert !PROTOCOL.equals(enclosed.getProtocol()) : getURL();

            this.root = enclosed.openConnection();

            this.headers = Deferred.shared(() -> Exceptions.wrap(() -> {
                final Map<String, List<String>> headers = new HashMap<>();

                final URL _url = getURL();
                final URL _enclosing = Archives.containing(_url);

                final String resource = Archives.resourcePath(_url, _enclosing)[0];

                Archives.read(Archives.open(true, _enclosing), _url, new Archives.Entry() {
                    public boolean matches(final URL url1, final JarEntry entry) throws IOException {
                        return resource.equals(entry.getName());
                    }

                    public boolean read(final URL url1, final JarEntry entry, final InputStream stream) throws IOException {
                        final String type = URLConnection.getFileNameMap().getContentTypeFor(resource);
                        headers.put(CONTENT_TYPE, Collections.singletonList(type == null ? UNKNOWN_TYPE : type));

                        headers.put(LAST_MODIFIED, Collections.singletonList(String.valueOf(entry.getTime())));

                        long size = entry.getSize();

                        if (size != -1) {
                            headers.put(CONTENT_LENGTH, Collections.singletonList(String.valueOf(size)));
                        } else {
                            final byte[] buffer = new byte[4096];

                            size = 0;
                            for (int length; (length = stream.read(buffer)) != -1; ) {
                                size += length;
                            }

                            headers.put(CONTENT_LENGTH, Collections.singletonList(String.valueOf(size)));
                        }

                        return false;
                    }
                });


                return Collections.unmodifiableMap(headers);
            }));

            this.inputStream = Deferred.shared(() -> Exceptions.wrap(() -> {
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
            }));
        }

        @Override
        public void connect() throws IOException {
            root.connect();
        }

        @Override
        public Permission getPermission() throws IOException {
            return new AccessPermission(archive);
        }

        @Override
        public void setUseCaches(final boolean flag) {
            super.setUseCaches(flag);
            root.setUseCaches(flag);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (getDoInput()) {
                try {
                    return inputStream.get();
                } catch (final Exceptions.Wrapper wrapper) {
                    throw wrapper.rethrow(IOException.class);
                }
            } else {
                throw new IllegalStateException(String.format("Input stream disabled on %s", url));
            }
        }

        @Override
        public void setDoOutput(final boolean value) {
            if (value) {
                throw new IllegalStateException("Output not supported on nested URLs");
            }
        }

        @Override
        public String getHeaderField(final String name) {
            final List<String> list = headers.get().get(name);
            return list == null ? null : list.get(0);
        }

        @Override
        public long getHeaderFieldDate(final String name, final long defaultValue) {
            final List<String> list = headers.get().get(name);
            return list == null ? defaultValue : Long.valueOf(list.get(0));
        }

        @Override
        public String getHeaderFieldKey(final int n) {
            return n < 0 ? null : n < FIELDS.length ? FIELDS[n] : null;
        }

        @Override
        public String getHeaderField(final int n) {
            final String header = getHeaderFieldKey(n);
            return header == null ? null : getHeaderField(header);
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            return headers.get();
        }
    }

    /**
     * Caches archive contents.
     *
     * @author Tibor Varga
     */
    static final class Cache {

        private static final String ROOT = "";
        private static final byte[] NO_DATA = new byte[0];

        /**
         * Represents the contents of a cached archive.
         *
         * @author Tibor Varga
         */
        interface Entry {

            /**
             * Returns the content of a named archive entry.
             *
             * @param name the name of the entry.
             *
             * @return a byte array; may be <code>null</code>.
             */
            Entry entry(String name) throws IOException;

            /**
             * Returns the contents of the archive itself.
             *
             * @return the contents of the archive itself.
             */
            byte[] data();

            /**
             * Tells if the archive is cached on demand (<code>true</code>) or all at once (<code>false</code>).
             *
             * @return <code>true</code> if the archive is cached on demand; <code>false</code> otherwise.
             */
            boolean dynamic();
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
         * inherited by threads created by <code>command</code> but it will not be stable outside a call to this method by a new thread made while this call
         * was still in scope.
         *
         * @param command the command that potentially accesses nested archives.
         * @param <T>     the return type of the command.
         * @param <E>     the exception type thrown by the command.
         *
         * @return whatever the command returns.
         */
        static <T, E extends Exception> T access(final Process<T, E> command) throws E {
            return access(null, command);
        }

        /**
         * Cache entries.
         *
         * @author Tibor Varga
         */
        private static class Entries {

            private final Map<String, ArchiveEntry> map = new HashMap<>();
            private final Set<String> active = new HashSet<>();

            Entries() {
                this((Entries) null);
            }

            Entries(final Entries base) {
                if (base != null) {
                    map.putAll(base.all());
                }
            }

            private Entries(final Map<String, ArchiveEntry> map) {
                this((Entries) null);
                this.map.putAll(map);
            }

            boolean contains(final String key) {
                return map.containsKey(key);
            }

            ArchiveEntry get(final String key) {
                final ArchiveEntry entry = map.get(key);

                if (entry != null) {
                    active.add(key);
                }

                return entry;
            }

            void put(final String key, final ArchiveEntry entry) {
                active.add(key);
                map.put(key, entry);
            }

            void remove(final String key) {
                active.remove(key);
                map.remove(key);
            }

            private Map<String, ArchiveEntry> all() {
                synchronized (map) {
                    return map;
                }
            }

            Map<String, ArchiveEntry> capture(final boolean all) {
                return Collections.unmodifiableMap(entries(all).map);
            }

            private Entries entries(final boolean all) {
                if (all) {
                    return this;
                } else {
                    final Map<String, ArchiveEntry> list = new HashMap<>();

                    for (final String key : active) {
                        list.put(key, map.get(key));
                    }

                    return new Entries(list);
                }
            }
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
        static Object capture(final boolean active) {
            return activeCache().capture(!active);
        }

        /**
         * Isolates the effects on the caching of nested archives of the given <code>command</code> from the rest of the application. The isolated cache is
         * inherited by threads created by <code>command</code> but it will not be stable outside a call to this method by a new thread made while this call
         * was still in scope.
         * <p>
         * The initial content of the cache will consist of what has been {@linkplain #capture(boolean) captured} in the given <code>context</code> and
         * whatever else is in the cache at the point of invocation.
         *
         * @param captured the cache contents captured using a previous {@link #capture(boolean)} call.
         * @param command the command that potentially accesses nested archives.
         * @param <T>     the return type of the command.
         * @param <E>     the exception type thrown by the command.
         *
         * @return whatever the command returns.
         */
        @SuppressWarnings("StatementWithEmptyBody")
        static <T, E extends Exception> T access(final Object captured, final Process<T, E> command) throws E {
            if (captured != null && !(captured instanceof Map)) {
                throw new IllegalArgumentException("Invalid captured context; use one returned by the capture(...) method");
            }

            final Context saved = context.get();
            UUID id;

            synchronized (privateCache) {
                for (id = UUID.randomUUID(); privateCache.containsKey(id); id = UUID.randomUUID()) {
                    // empty
                }

                if (captured != null) {
                    @SuppressWarnings("unchecked")
                    final Map<String, ArchiveEntry> data = (Map<String, ArchiveEntry>) captured;
                    privateCache.put(id, new Entries(data));
                } else {
                    privateCache.put(id, new Entries(saved == null ? sharedCache : privateCache.get(saved.id)));
                }
            }

            context.set(new Context(id, saved));

            try {
                return command.run();
            } finally {
                context.set(saved);

                synchronized (privateCache) {
                    privateCache.remove(id);
                }
            }
        }

        static byte[] contents(final URL url) throws IOException {
            final Cache.Entry archive = Cache.archive(url);
            return archive == null ? null : (PROTOCOL.equals(url.getProtocol()) ? archive.entry(Cache.path(url)) : archive).data();
        }

        static Entry archive(final URL url) throws IOException {
            final boolean nested = PROTOCOL.equals(url.getProtocol());

            final URL root = nested ? new URL(enclosedURL(url)) : url;
            final String path = nested ? path(url) : ROOT;

            return load(root).entry(path);
        }

        @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
        static Entry load(final URL root) throws IOException {
            assert root != null;
            final String key = root.toExternalForm();

            final Entries cache = activeCache();
            ArchiveEntry archive = cache.get(key);

            if (archive == null) {
                archive = new ArchiveEntry(root, ROOT);

                synchronized (archive) {
                    synchronized (cache) {
                        if (cache.contains(key)) {
                            archive = cache.get(key);

                            synchronized (archive) {
                                return archive;
                            }
                        } else {
                            cache.put(key, archive);
                        }
                    }

                    final byte[] buffer = new byte[1024 * 1024];
                    return archive.load(Streams.load(Archives.connection(true, root).getInputStream(), buffer, true), buffer);
                }
            } else {
                synchronized (archive) {
                    return archive;
                }
            }
        }

        private static Entries activeCache() {
            final Context local = context.get();
            final UUID id = local == null ? null : local.id;
            assert local == null | id != null;

            Entries cache = null;

            if (id != null) {
                synchronized (privateCache) {
                    cache = privateCache.get(id);
                }

                if (cache == null) {
                    context.set(null);
                }
            }

            return cache == null ? sharedCache : cache;
        }

        /**
         * Unloads the contents of the given URL from the caches.
         *
         * @param url the URL to unload the contents of.
         */
        static void unload(final URL url) {
            if (PROTOCOL.equals(url.getProtocol())) {
                activeCache().remove(enclosedURL(url));
            }
        }

        static boolean loaded(final URL url, final boolean local) {
            return (local ? activeCache() : sharedCache).contains(enclosedURL(url));
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

        private static final Map<UUID, Entries> privateCache = new HashMap<>();
        private static final Entries sharedCache = new Entries();

        /**
         * @author Tibor Varga
         */
        private static class Metadata {

            private final String name;
            private final long size;
            private final long crc;

            Metadata(final String name, final long size, final long crc) {
                assert name != null;
                final String[] components = name.split(DELIMITER);
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

        /**
         * @author Tibor Varga
         */
        private static class ArchiveEntry implements Entry {

            private final URL root;
            private final String base;
            private final Map<Metadata, String> metadata;

            private boolean loaded;
            private byte[] data;
            private Map<String, ArchiveEntry> content;

            ArchiveEntry(final URL root, final String base) {
                this(root, base, new HashMap<>(), new HashMap<>());
            }

            ArchiveEntry(final URL root, final String base, final ArchiveEntry parent, final byte[] data, final boolean loaded) {
                this(root, base, parent);
                this.data = data;
                this.loaded = loaded;
            }

            ArchiveEntry(final URL root, final String base, final ArchiveEntry parent) {
                this(root, base, parent.metadata, parent.content);
            }

            private ArchiveEntry(final URL root, final String base, final Map<Metadata, String> metadata, final Map<String, ArchiveEntry> content) {
                this.root = root;
                this.base = base;

                if (base != null) {
                    this.metadata = metadata;
                    this.content = content;
                } else {
                    this.metadata = null;
                    this.content = null;
                }
            }

            Entry load(final byte[] bytes, final byte[] buffer) throws IOException {
                if (base == null) {
                    throw new FileNotFoundException(root.toExternalForm());
                }

                if (loaded) {
                    return this;
                }

                if (data == null) {
                    assert bytes != null : root;
                    data = bytes;
                }

                final Map<String, ArchiveEntry> map = new HashMap<>();
                loaded = true;

                load(base, data, content, map, buffer, metadata);

                if (map.isEmpty()) {

                    // no archive entries and not a directory: either not an archive or the URL handler hides the content: ignore the garbage
                    if (directory(root.getPath())) {
                        data = NO_DATA;
                    }
                }

                content.putAll(map);

                return this;
            }

            public Entry entry(final String name) throws IOException {
                assert name != null;

                if (content == null) {
                    return null;
                } else if (name.equals(base)) {
                    return this;
                } else {
                    final boolean absolute = name.startsWith(DELIMITER);
                    final String key = absolute ? name : String.format("%s%s%s", base, DELIMITER, name);
                    final String relative = absolute ? name.substring(DELIMITER.length()) : name;

                    ArchiveEntry archive;

                    synchronized (this) {
                        archive = content.get(key);
                    }

                    if (archive == null) {
                        final String[] parts = relative.split(DELIMITER);

                        if (parts.length > 1) {
                            archive = this;
                            for (int i = 0, limit = parts.length; archive != null && i < limit; i++) {
                                archive = (ArchiveEntry) archive.entry(parts[i]);
                            }
                        }
                    }

                    if (archive == null || !archive.loaded) {
                        synchronized (this) {
                            if (archive == null) {
                                archive = content.get(key);
                            }

                            if (archive == null || !archive.loaded) {
                                final byte[] buffer = new byte[16384];

                                byte[] bytes = null;
                                if (archive == null) {
                                    final URL url = relativeURL(root, relative);

                                    if (directory(root.getPath())) {
                                        try {
                                            bytes = Streams.load(Archives.connection(true, url).getInputStream(), buffer, true);
                                            archive = new ArchiveEntry(url, key, this);

                                            content.put(key, archive);
                                        } catch (final FileNotFoundException e) {
                                            content.put(key, archive = new ArchiveEntry(url, null));
                                        }
                                    } else {
                                        content.put(key, archive = new ArchiveEntry(url, null));
                                    }
                                }

                                archive.load(bytes, buffer);
                            }
                        }
                    }

                    return archive;
                }
            }

            public byte[] data() {
                return data;
            }

            public boolean dynamic() {
                return data == NO_DATA;
            }

            private void load(final String base,
                              final byte[] data,
                              final Map<String, ArchiveEntry> global,
                              final Map<String, ArchiveEntry> local,
                              final byte[] buffer,
                              final Map<Metadata, String> meta) throws IOException {
                try (final ZipInputStream stream = new ZipInputStream(new ByteArrayInputStream(data))) {
                    for (ZipEntry next = stream.getNextEntry(); next != null; stream.closeEntry(), next = stream.getNextEntry()) {
                        final String name = next.getName();

                        if (!directory(name)) {
                            final String entry = String.format("%s%s%s", base, DELIMITER, name);
                            final byte[] bytes = Streams.load(stream, buffer, false);
                            final Metadata metadata = new Metadata(name, next.getSize(), next.getCrc());

                            final String reference = meta.get(metadata);

                            final URL url = Handler.formatURL(root, name);

                            if (reference == null) {
                                meta.put(metadata, entry);
                                local.put(entry, new ArchiveEntry(url, entry, this, bytes, false));
                            } else {
                                assert !global.containsKey(entry) : entry;
                                assert global.containsKey(reference) : entry;
                                local.put(entry, copy(url, entry, global.get(reference)));
                                final String prefix = reference.concat(DELIMITER);

                                for (final Map.Entry<String, ArchiveEntry> candidate : global.entrySet()) {
                                    final String key = candidate.getKey();

                                    if (key.startsWith(prefix)) {
                                        final String root = entry.concat(key.substring(reference.length()));
                                        local.put(root, copy(url, root, candidate.getValue()));
                                    }
                                }
                            }
                        }
                    }
                }
            }

            private ArchiveEntry copy(final URL url, final String base, final ArchiveEntry entry) {
                return new ArchiveEntry(url, base, entry, entry.data, entry.loaded);
            }
        }
    }
}

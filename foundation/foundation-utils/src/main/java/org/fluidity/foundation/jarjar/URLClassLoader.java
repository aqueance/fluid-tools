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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketPermission;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLStreamHandlerFactory;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.fluidity.foundation.Archives;
import org.fluidity.foundation.ClassLoaders;
import org.fluidity.foundation.Command;
import org.fluidity.foundation.Deferred;
import org.fluidity.foundation.Exceptions;
import org.fluidity.foundation.Lists;
import org.fluidity.foundation.Proxies;
import org.fluidity.foundation.Security;

import static org.fluidity.foundation.Command.Operation;
import static org.fluidity.foundation.Command.Process;

/**
 * Caching URL class loader that does not keep files or connections open.
 *
 * @author Tibor Varga
 */
public class URLClassLoader extends SecureClassLoader {

    private static final int INITIAL_CAPACITY = 128;
    private static final int CONCURRENCY = Runtime.getRuntime().availableProcessors() << 1;

    private static final String FILE_ACCESS = "read";
    private static final String SOCKET_ACCESS = "connect,accept";

    private final AccessControlContext context = Security.CONTROLLED ? AccessController.getContext() : null;

    private final Deferred.Reference<String[]> keys;
    private final Map<String, Archive> entries;

    private Object cache;

    /**
     * Creates a new class loader based on the given URLs. Calls {@link #URLClassLoader(Collection, ClassLoader, URLStreamHandlerFactory)} with the {@linkplain
     * ClassLoader#getSystemClassLoader() system class loader} as <code>parent</code>.
     *
     * @param urls    the list of URLs; may not be <code>null</code> or empty and may not contain <code>null</code> entries.
     * @param factory the optional {@link URLStreamHandlerFactory} if some URLs are not otherwise understood by the system.
     */
    public URLClassLoader(final Collection<URL> urls, final URLStreamHandlerFactory factory) {
        this(urls, getSystemClassLoader(), factory);
    }

    /**
     * Creates a new class loader based on the given URLs.
     *
     * @param urls    the list of URLs; may not be <code>null</code> or empty and may not contain <code>null</code> entries.
     * @param parent  the parent class loader to use; may not be <code>null</code>.
     * @param factory the optional {@link URLStreamHandlerFactory} if some URLs are not otherwise understood by the system.
     */
    public URLClassLoader(final Collection<URL> urls, final ClassLoader parent, final URLStreamHandlerFactory factory) {
        super(parent);

        if (urls == null || urls.size() == 0) {
            throw new IllegalArgumentException("No URL specified");
        }

        {
            int i = 0;
            for (final URL url : urls) {
                if (url == null) {
                    throw new IllegalArgumentException(String.format("URL at index %d is null", i));
                }

                ++i;
            }
        }

        final Collection<URL> locations = new ArrayList<URL>(urls);

        this.cache = Archives.Cache.capture(false);
        this.entries = new LinkedHashMap<String, Archive>(locations.size(), 1.0f);

        this.keys = Deferred.global(new Deferred.Factory<String[]>() {
            public String[] create() {
                final List<String> collected = new ArrayList<String>();

                // collects all processed URLs, may be recursive when an archive refers to others as its class path
                final Operation<URL, IOException> collect = new Operation<URL, IOException>() {
                    public void run(final URL dependency) throws IOException {
                        final String location = dependency.toExternalForm();

                        if (!entries.containsKey(location)) {
                            final Handler.Cache.Entry archive = Handler.Cache.archive(dependency);

                            if (archive == null) {
                                throw new IllegalArgumentException(String.format("Not an archive: %s", dependency));
                            }

                            collected.add(dependency.toExternalForm());
                            entries.put(location,
                                        archive.dynamic()
                                        ? new LazyLoadedArchive(dependency, archive, factory, this)
                                        : new PackagedArchive(dependency, archive, factory, this));
                        }
                    }
                };

                cache = Exceptions.wrap(new Process<Object, IOException>() {
                    public Object run() throws IOException {
                        for (final URL url : locations) {
                            collect.run(url);
                        }

                        return Archives.Cache.capture(true);
                    }
                });

                return Lists.asArray(String.class, collected);
            }
        });
    }

    private Archive.Entry entry(final String url, final String resource) throws IOException {
        final Archive.Entry entry = entries.get(url).entry(resource);
        return entry == Archive.Entry.NOT_FOUND ? null : entry;
    }

    private <R, T extends Exception, E extends Exception> R access(final Object label, final Class<T> wrapper, final Process<R, E> action) throws T {
        return Exceptions.wrap(label, wrapper, new Process<R, E>() {
            public R run() throws E {
                return Archives.Cache.access(cache, action);
            }
        });
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        final PrivilegedExceptionAction<Class<?>> action = new PrivilegedExceptionAction<Class<?>>() {
            public Class<?> run() throws Exception {
                final String resource = ClassLoaders.classResourceName(name);

                for (final String key : keys.get()) {
                    final Archive.Entry entry = entry(key, resource);

                    if (entry != null) {
                        return entry.define(name);
                    }
                }

                throw new ClassNotFoundException(name);
            }
        };

        return access(name, ClassNotFoundException.class, new Process<Class<?>, Exception>() {
            public Class<?> run() throws Exception {
                return context == null ? action.run() : AccessController.doPrivileged(action, context);
            }
        });
    }

    @Override
    protected URL findResource(final String name) {
        final PrivilegedAction<URL> action = new PrivilegedAction<URL>() {
            public URL run() {
                for (final String key : keys.get()) {
                    try {
                        final Archive.Entry entry = entry(key, name);

                        if (entry != null) {
                            return entry.url();
                        }
                    } catch (final IOException e) {
                        // ignored
                    }
                }

                return null;
            }
        };

        return access(null, Exceptions.Wrapper.class, new Process<URL, RuntimeException>() {
            public URL run() throws RuntimeException {
                return context == null ? action.run() : AccessController.doPrivileged(action, context);
            }
        });
    }

    @Override
    protected Enumeration<URL> findResources(final String name) throws IOException {
        final PrivilegedAction<Enumeration<URL>> action = new PrivilegedAction<Enumeration<URL>>() {
            public Enumeration<URL> run() {
                final List<URL> list = new ArrayList<URL>();

                for (final String key : keys.get()) {
                    try {
                        final Archive.Entry entry = entry(key, name);

                        if (entry != null) {
                            list.add(entry.url());
                        }
                    } catch (final IOException e) {
                        // ignored
                    }
                }

                return Collections.enumeration(list);
            }
        };

        return access(null, Exceptions.Wrapper.class, new Process<Enumeration<URL>, RuntimeException>() {
            public Enumeration<URL> run() throws RuntimeException {
                return context == null ? action.run() : AccessController.doPrivileged(action, context);
            }
        });
    }

    @Override
    protected PermissionCollection getPermissions(final CodeSource source) {
        final PermissionCollection permissions = super.getPermissions(source);

        final URL url = source.getLocation();

        Permission permission;

        try {
            permission = Archives.connection(true, url).getPermission();
        } catch (final IOException e) {
            permission = null;
        }

        if (permission instanceof FilePermission) {
            final String path = permission.getName();

            if (path.endsWith(File.separator)) {
                permission = new FilePermission(path.concat("-"), FILE_ACCESS);
            }
        }  else if (permission == null) {
            if (Archives.FILE.equals(url.getProtocol())) {
                try {
                    final String path = URLDecoder.decode(url.getPath().replace('/', File.separatorChar), "UTF-8");
                    permission = new FilePermission(path.endsWith(File.separator) ? path.concat("-") : path, FILE_ACCESS);
                } catch (final UnsupportedEncodingException e) {
                    assert false : e;
                }
            } else {
                try {
                    final String host = Archives.Nested.rootURL(url).getHost();

                    if (host != null && !host.isEmpty()) {
                        permission = new SocketPermission(host, SOCKET_ACCESS);
                    }
                } catch (final IOException e) {
                    // ignore
                }
            }
        }

        if (permission != null) {
            permissions.add(context == null ? permission : checkPermission(permission));
        }

        return permissions;
    }

    private Permission checkPermission(final Permission permission) {
        assert context != null;

        return AccessController.doPrivileged(new PrivilegedAction<Permission>() {
            public Permission run() throws SecurityException {
                final SecurityManager security = System.getSecurityManager();
                assert security != null;
                security.checkPermission(permission);
                return permission;
            }
        }, context);
    }

    Class defineClass(final String name, final byte[] bytes, final int offset, final int length, final CodeSigner[] signers, final Archive.Entry resource) throws IOException {
        final URL url = resource.url();

        int i = name.lastIndexOf('.');
        if (i > -1) {
            final String packageName = name.substring(0, i);
            final Package pkg = getPackage(packageName);

            final Manifest manifest = resource.manifest();
            final Attributes entry = manifest == null ? null : manifest.getAttributes(packageName.replace('.', '/').concat("/"));
            final Attributes main = manifest == null ? null : manifest.getMainAttributes();

            final boolean sealed = manifest != null && Boolean.parseBoolean(attribute(entry, main, Attributes.Name.SEALED));

            if (pkg != null) {
                if (pkg.isSealed()) {
                    if (!pkg.isSealed(url)) {
                        throw new SecurityException(String.format("sealing violation: package %s is sealed", packageName));
                    }
                } else if (sealed) {
                    throw new SecurityException(String.format("sealing violation: can't seal package %s: already loaded", packageName));
                }
            } else {
                definePackage(packageName,
                              manifest == null ? null : attribute(entry, main, Attributes.Name.SPECIFICATION_TITLE),
                              manifest == null ? null : attribute(entry, main, Attributes.Name.SPECIFICATION_VERSION),
                              manifest == null ? null : attribute(entry, main, Attributes.Name.SPECIFICATION_VENDOR),
                              manifest == null ? null : attribute(entry, main, Attributes.Name.IMPLEMENTATION_TITLE),
                              manifest == null ? null : attribute(entry, main, Attributes.Name.IMPLEMENTATION_VERSION),
                              manifest == null ? null : attribute(entry, main, Attributes.Name.IMPLEMENTATION_VENDOR),
                              sealed ? url : null);
            }
        }

        return defineClass(name, bytes, offset, length, new CodeSource(resource.root(), signers));
    }

    private String attribute(final Attributes primary, final Attributes fallback, final Attributes.Name name) {
        final String value = primary == null ? null : primary.getValue(name);
        return value == null && fallback != null ? fallback.getValue(name) : value;
    }

    private static URL[] classpath(final URL url, final Manifest manifest, final URLStreamHandlerFactory factory) throws IOException {
        final Collection<URL> list = new LinkedHashSet<URL>();

        final String classpath = manifest == null ? null : manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
        if (classpath != null) {
            for (final String relative : classpath.split("[ \t\n\r\f]")) {
                list.add(Archives.relativeURL(url, relative, factory));
            }
        }

        return Lists.asArray(URL.class, list);
    }

    /**
     * Represents an archive.
     *
     * @author Tibor Varga
     */
    private interface Archive {

        Entry entry(String resource) throws IOException;

        /**
         * Represents an entry in an archive.
         *
         * @author Tibor Varga
         */
        interface Entry {

            /**
             * Represents an entry that does not exist.
             */
            Entry NOT_FOUND = Proxies.create(Entry.class, null);

            /**
             * Send the contents of the entry to the given feed.
             *
             * @param resource the name of the resource this entry represents.
             *
             * @return whatever the feed returns.
             *
             * @throws IOException when accessing the archive fails.
             */
            Class<?> define(String resource) throws IOException;

            /**
             * Returns the URL of the entry corresponding to the given resource under the given archive URL.
             *
             * @return the URL corresponding to the given resource in the given archive.
             */
            URL url();

            /**
             * Returns the archive manifest, if any.
             *
             * @return the archive manifest, if any.
             */
            Manifest manifest() throws IOException;

            /**
             * Returns a new input stream for this entry.
             *
             * @return a new input stream for this entry.
             */
            InputStream stream() throws IOException;

            /**
             * Returns the root URL for this entry, suitable for creating a {@link CodeSource} object.
             *
             * @return the root URL for this entry.
             */
            URL root();
        }
    }

    /**
     * Represents a directory or an archive that cannot be browsed.
     *
     * @author Tibor Varga
     */
    private class LazyLoadedArchive implements Archive {

        private final Map<String, Entry> map = new ConcurrentHashMap<String, Entry>(INITIAL_CAPACITY, 0.75f, CONCURRENCY);
        private final Manifest manifest;

        private final URL root;
        private final Handler.Cache.Entry archive;

        public LazyLoadedArchive(final URL url,
                                 final Handler.Cache.Entry archive,
                                 final URLStreamHandlerFactory factory,
                                 final Operation<URL, IOException> collect) throws IOException {
            this.root = url;
            this.archive = archive;

            final Entry entry = entry(JarFile.MANIFEST_NAME);
            this.manifest = entry != null ? new Manifest(entry.stream()) : null;

            for (final URL relative : classpath(url, manifest, factory)) {
                collect.run(relative);
            }
        }

        /**
         * Loads an entry from the archive.
         *
         * @param resource the resource to load the entry for.
         *
         * @return an entry or <code>null</code>.
         *
         * @throws IOException when loading the entry fails.
         */
        public Entry entry(final String resource) throws IOException {
            final Entry found = map.get(resource);

            if (found == null) {
                final URL url = Handler.relativeURL(root, resource);

                try {
                    final Handler.Cache.Entry entry = archive.entry(resource);

                    if (entry != null) {
                        final byte[] data = entry.data();

                        final Entry created = new Entry() {
                            public Class<?> define(final String resource) throws IOException {
                                return defineClass(resource, data, 0, data.length, null, this);
                            }

                            public URL url() {
                                return url;
                            }

                            public Manifest manifest() {
                                return null;
                            }

                            public InputStream stream() throws IOException {
                                return new ByteArrayInputStream(data);
                            }

                            public URL root() {
                                return root;
                            }
                        };

                        map.put(resource, created);

                        return created;
                    }
                } catch (final FileNotFoundException e) {
                    // ignore
                }

                map.put(resource, Archive.Entry.NOT_FOUND);
            }

            return found;
        }
    }

    /**
     * Represents a packaged archive, regardless of where it came from.
     *
     * @author Tibor Varga
     */
    private class PackagedArchive implements Archive {

        private final Map<String, Archive.Entry> map = new HashMap<String, Entry>(INITIAL_CAPACITY);

        PackagedArchive(final URL url,
                        final Handler.Cache.Entry archive,
                        final URLStreamHandlerFactory factory,
                        final Command.Operation<URL, IOException> collect) throws IOException {
            final Manifest manifest[] = { null };

            Archives.read(archive.data(), url, new Archives.Entry() {
                public boolean matches(final URL url, final JarEntry entry) throws IOException {
                    return true;
                }

                public boolean read(final URL url, final JarEntry entry, final InputStream stream) throws IOException {
                    final String resource = entry.getName();
                    final CodeSigner[] signers = entry.getCodeSigners();

                    final byte[] data = archive.entry(resource).data();
                    assert data != null : Handler.formatURL(url, resource);

                    if (JarFile.MANIFEST_NAME.equals(resource)) {
                        manifest[0] = new Manifest(new ByteArrayInputStream(data));
                    }

                    map.put(resource, new Entry() {
                        private final URL entry = Archives.Nested.formatURL(url, resource);

                        public Class<?> define(final String resource) throws IOException {
                            return defineClass(resource, data, 0, data.length, signers, this);
                        }

                        public URL url() {
                            return entry;
                        }

                        public Manifest manifest() throws IOException {
                            return manifest[0];
                        }

                        public InputStream stream() throws IOException {
                            return new ByteArrayInputStream(data);
                        }

                        public URL root() {
                            return url;
                        }
                    });

                    return true;
                }
            });

            for (final URL relative : classpath(url, manifest[0], factory)) {
                collect.run(relative);
            }
        }

        public Entry entry(final String resource) {
            return map.get(resource);
        }
    }
}

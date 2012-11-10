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
import java.io.FileInputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketPermission;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import org.fluidity.foundation.Streams;

import sun.security.util.SecurityConstants;

/**
 * Caching URL class loader that does not keep files or connections open.
 *
 * @author Tibor Varga
 */
public class URLClassLoader extends SecureClassLoader {

    private static final int INITIAL_CAPACITY = 128;
    private static final int CONCURRENCY = Runtime.getRuntime().availableProcessors() << 1;

    private final AccessControlContext context = AccessController.getContext();

    private final Deferred.Reference<String[]> keys;
    private final Map<String, Archive> entries;

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

        this.entries = new LinkedHashMap<String, Archive>(urls.size(), 1.0f);

        this.keys = Deferred.reference(new Deferred.Factory<String[]>() {
            public String[] create() {
                final List<String> collected = new ArrayList<String>();

                // collects all processed URLs, may be recursive when an archive refers to others as its class path
                final Command.Operation<URL, IOException> collect = new Command.Operation<URL, IOException>() {
                    public void run(final URL dependency) throws IOException {
                        final String location = dependency.toExternalForm();

                        if (!entries.containsKey(location)) {
                            collected.add(dependency.toExternalForm());

                            final String protocol = dependency.getProtocol();
                            final File file = Archives.localFile(dependency);

                            entries.put(location,
                                        Archives.FILE.equals(protocol) && file.exists() && file.isDirectory()
                                        ? new LocalDirectoryArchive(file)
                                        : new PackagedArchive(dependency, factory, this));
                        }
                    }
                };

                Exceptions.wrap(new Command.Job<IOException>() {
                    public void run() throws IOException {
                        Archives.Nested.access(new Command.Job<IOException>() {
                            public void run() throws IOException {
                                for (final URL url : urls) {
                                    collect.run(url);
                                }
                            }
                        });
                    }
                });

                return Lists.asArray(String.class, collected);
            }
        });
    }

    private Archive.Entry entry(final String url, final String resource) throws IOException {
        return entries.get(url).entry(resource);
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        return Exceptions.wrap(name, ClassNotFoundException.class, new Command.Process<Class<?>, PrivilegedActionException>() {
            public Class<?> run() throws PrivilegedActionException {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
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
                }, context);
            }
        });
    }

    @Override
    protected URL findResource(final String name) {
        return AccessController.doPrivileged(new PrivilegedAction<URL>() {
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
        }, context);
    }

    @Override
    protected Enumeration<URL> findResources(final String name) throws IOException {
        return AccessController.doPrivileged(new PrivilegedAction<Enumeration<URL>>() {
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
        }, context);
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

        if (permission instanceof AccessPermission) {
            permission = ((AccessPermission) permission).delegate();
        }

        if (permission instanceof FilePermission) {
            final String path = permission.getName();

            if (path.endsWith(File.separator)) {
                permission = new FilePermission(path.concat("-"), SecurityConstants.FILE_READ_ACTION);
            }
        } else if (permission == null && url.getProtocol().equals(Archives.FILE)) {
            try {
                final String path = URLDecoder.decode(url.getPath().replace('/', File.separatorChar), "UTF-8");
                permission = new FilePermission(path.endsWith(File.separator) ? path.concat("-") : path, SecurityConstants.FILE_READ_ACTION);
            } catch (final UnsupportedEncodingException e) {
                assert false : e;
            }
        } else {
            try {
                final String host = Archives.Nested.rootURL(url).getHost();

                if (host != null && !host.isEmpty()) {
                    permission = new SocketPermission(host, SecurityConstants.SOCKET_CONNECT_ACCEPT_ACTION);
                }
            } catch (final IOException e) {
                // ignore
            }
        }

        if (permission != null) {
            permissions.add(checkPermission(permission));
        }

        return permissions;
    }

    private Permission checkPermission(final Permission permission) {
        final SecurityManager security = System.getSecurityManager();

        return security == null ? permission : AccessController.doPrivileged(new PrivilegedAction<Permission>() {
            public Permission run() throws SecurityException {
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

        final URL root = Archives.containing(url);
        return defineClass(name, bytes, offset, length, new CodeSource(root == null ? url : root, signers));
    }

    private String attribute(final Attributes primary, final Attributes fallback, final Attributes.Name name) {
        final String value = primary == null ? null : primary.getValue(name);
        return value == null && fallback != null ? fallback.getValue(name) : value;
    }

    /**
     * Represents an archive.
     *
     * @author Tibor Varga
     */
    private interface Archive {

        /**
         * Loads an entry from the archive.
         *
         * @param resource the resource to load the entry for.
         *
         * @return an entry or <code>null</code>.
         *
         * @throws IOException when loading the entry fails.
         */
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
        }
    }

    /**
     * Represents a directory archive in the file system.
     *
     * @author Tibor Varga
     */
    private class LocalDirectoryArchive implements Archive {

        private final Map<String, Archive.Entry> map = new ConcurrentHashMap<String, Archive.Entry>(INITIAL_CAPACITY, 0.75f, CONCURRENCY);

        private String location;

        LocalDirectoryArchive(final File file) {
            this.location = file.getAbsolutePath();
        }

        public Entry entry(final String resource) throws IOException {
            final Entry found = map.get(resource);

            if (found == null && found != Entry.NOT_FOUND) {
                final File file = new File(relative(location, resource));

                if (file.exists()) {
                    final Entry created = new Entry() {
                        private final URL entry = file.toURI().toURL();

                        public Class<?> define(final String resource) throws IOException {
                            final byte[] bytes = Streams.load(new FileInputStream(file), new byte[16384], true);
                            return defineClass(resource, bytes, 0, bytes.length, null, this);
                        }

                        public URL url() {
                            return entry;
                        }

                        public Manifest manifest() {
                            return null;
                        }

                        public InputStream stream() throws IOException {
                            return new FileInputStream(file);
                        }
                    };

                    map.put(resource, created);

                    return created;
                } else {
                    map.put(resource, Entry.NOT_FOUND);
                }
            }

            return found;
        }

        @SuppressWarnings("StringBufferReplaceableByString")
        private String relative(final String location, final String resource) {
            return new StringBuilder(location.length() + resource.length() + 1).append(location).append(File.separatorChar).append(resource).toString();
        }
    }

    /**
     * Represents a packaged archive, regardless of where it came from.
     *
     * @author Tibor Varga
     */
    private class PackagedArchive implements Archive {

        private final Map<String, Archive.Entry> map = new HashMap<String, Archive.Entry>(INITIAL_CAPACITY);

        PackagedArchive(final URL url, final URLStreamHandlerFactory factory, final Command.Operation<URL, IOException> collect) throws IOException {
            final Manifest manifest[] = { null };
            final Handler.Cache.Archive cache = Handler.Cache.archive(url);

            Archives.read(cache.root(), url, new Archives.Entry() {
                public boolean matches(final URL url, final JarEntry entry) throws IOException {
                    return true;
                }

                public boolean read(final URL url, final JarEntry entry, final InputStream stream) throws IOException {
                    final String resource = entry.getName();
                    final CodeSigner[] signers = entry.getCodeSigners();

                    final byte[] data = cache.entry(resource);
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
                    });

                    return true;
                }
            });

            final String classpath = manifest[0] == null ? null : manifest[0].getMainAttributes().getValue(Attributes.Name.CLASS_PATH);

            if (classpath != null) {
                for (final String relative : classpath.split("[ \t\n\r\f]")) {
                    URL dependency;

                    try {
                        dependency = new URL(url, relative);
                    } catch (final MalformedURLException e) {
                        final int colon = relative.indexOf(':');
                        final URLStreamHandler handler = colon == -1 || factory == null ? null : factory.createURLStreamHandler(relative.substring(colon));

                        if (handler != null) {
                            dependency = new URL(url, relative, handler);
                        } else {
                            throw e;
                        }
                    }

                    collect.run(dependency);
                }
            }
        }

        public Entry entry(final String resource) {
            return map.get(resource);
        }
    }
}

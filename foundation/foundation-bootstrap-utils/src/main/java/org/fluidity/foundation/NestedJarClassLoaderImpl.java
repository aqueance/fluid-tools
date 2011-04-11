/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import org.fluidity.foundation.jarjar.Handler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * A class loader that looks into jar files inside a jar file, one level only. The implementation loads and caches in a memory sensitive cache that is
 * automatically purged by the garbage collector all yet unused byte codes from any nested jar that was used to load a class from. If a nested jar is larger
 * than the available heap, this will cause thrashing. Don't go wild with your nested jars, keep them of reasonable size.
 * <p/>
 * You call methods of this class from an actual {@link ClassLoader}. When instantiating this class, the <code>caller</code> parameter must call the actual
 * class loader instance or its super class, depending on the method as implemented in {@link JarJarClassLoader}.
 *
 * @author Tibor Varga
 */
public final class NestedJarClassLoaderImpl {

    /**
     * Allows this the {@link NestedJarClassLoaderImpl} instance to call actual {@link ClassLoader} methods on the caller. See {@link JarJarClassLoader} for a
     * working example.
     */
    public static interface Caller {

        /**
         * See {@link ClassLoader#findClass(String)}.
         *
         * @param name the name of the class.
         * @return the loaded class, if found.
         * @throws ClassNotFoundException thrown by {@link ClassLoader#findClass(String)}.
         */
        Class<?> findClass(String name) throws ClassNotFoundException;

        /**
         * See {@link ClassLoader#findResource(String)}.
         *
         * @param resource the resource path.
         * @return the resource URL.
         */
        URL findResource(String resource);

        /**
         * See {@link ClassLoader#findResources(String)}.
         *
         * @param resource the resource path.
         * @return a list of URLs, one for each instance of the resource.
         * @throws IOException thrown by {@link ClassLoader#findResources(String)}.
         */
        Enumeration<URL> findResources(String resource) throws IOException;

        /**
         * See {@link ClassLoader#defineClass(String, byte[], int, int)}.
         *
         * @param name   the name of the class to define.
         * @param bytes  the byte code that defines the class.
         * @param offset the offset in the array where the byte code starts.
         * @param length the length of the byte code in bytes.
         * @return the defines class, if successful.
         * @throws ClassFormatError thrown by {@link ClassLoader#defineClass(String, byte[], int, int)}.
         */
        Class<?> defineClass(String name, byte[] bytes, int offset, int length) throws ClassFormatError;

        /**
         * See {@link ClassLoader#getResource(String)}.
         *
         * @param resource the resource path.
         * @return the resource URL.
         */
        URL getResource(String resource);

        /**
         * Returns some human readable identification for the caller.
         *
         * @return some human readable identification for the caller.
         */
        String toString();
    }

    // linked so that it retains order of entries in the root jar file
    private final Map<String, Set<String>> nameMap = new LinkedHashMap<String, Set<String>>();
    private final Map<String, Lock> lockMap = new HashMap<String, Lock>();
    private final Map<String, Map<String, Reference<byte[]>>> bytecodeMap = new ConcurrentHashMap<String, Map<String, Reference<byte[]>>>();

    private final Caller caller;

    public NestedJarClassLoaderImpl(final Caller caller) throws IOException {
        this.caller = caller;
    }

    public void init(final String[] paths) throws IOException {
        for (final String path : paths) {
            final Set<String> names = new HashSet<String>();

            nameMap.put(path, names);
            lockMap.put(path, new ReentrantLock());

            final URL resource = caller.getResource(path);

            if (resource == null) {
                throw new IllegalArgumentException(String.format("Path %s not found in %s", path, caller.toString()));
            }

            JarStreams.readEntries(resource, new JarStreams.JarEntryReader() {
                public boolean matches(final JarEntry entry) {
                    return true;
                }

                public boolean read(final JarEntry entry, final JarInputStream stream) throws IOException {
                    names.add(entry.getName());
                    return true;
                }
            });
        }
    }

    public Class<?> findClass(final String name) throws ClassNotFoundException {
        try {
            return caller.findClass(name);
        } catch (final ClassNotFoundException e) {
            if (e.getCause() != null) {
                throw e;
            } else {
                try {
                    final String resource = ClassLoaders.classResourceName(name);

                    for (final Map.Entry<String, Set<String>> mapping : nameMap.entrySet()) {
                        if (mapping.getValue().contains(resource)) {
                            final String path = mapping.getKey();
                            final Lock lock = lockMap.get(path);

                            byte[] bytes = null;

                            try {

                                // prevent another thread from loading from the same path
                                lock.lockInterruptibly();

                                try {
                                    final Map<String, Reference<byte[]>> bytecodes = bytecodeMap.get(path);

                                    if (bytecodes == null) {

                                        // not loaded yet: load it

                                        bytecodeMap.put(path, new HashMap<String, Reference<byte[]>>());
                                        bytes = loadNestedJar(resource, path, bytecodeMap.get(path), false);
                                    } else {

                                        // loaded already but not yet defined: remove reference from cache to signify that it should not be loaded any more
                                        final Reference<byte[]> reference = bytecodes.remove(resource);

                                        if (reference == null) {

                                            // we don't have such a class
                                            throw new ClassNotFoundException(name);
                                        } else if ((bytes = reference.get()) == null) {

                                            // we have a class but our byte code has been purged: load the whole thing again
                                            bytes = loadNestedJar(resource, path, bytecodes, true);
                                        }
                                    }
                                } finally {
                                    lock.unlock();
                                }
                            } catch (final InterruptedException i) {
                                Thread.currentThread().interrupt();     // set the interrupted flag
                                throw new ClassNotFoundException(name);
                            }

                            if (bytes == null) {
                                throw new ClassNotFoundException(name);
                            }

                            return caller.defineClass(name, bytes, 0, bytes.length);
                        }
                    }

                    throw new ClassNotFoundException(name);
                } catch (final IOException io) {
                    throw new ClassNotFoundException(name, io);
                }
            }
        }
    }

    private byte[] loadNestedJar(final String resource, final String path, final Map<String, Reference<byte[]>> bytecodes, final boolean reloading)
            throws IOException {

        // strong reference prevents collection as garbage while we fill up the cache (i.e., class is found even when thrashing)
        final byte[][] found = new byte[1][];

        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final byte[] buffer = new byte[1024];

        JarStreams.readEntries(caller.getResource(path), new JarStreams.JarEntryReader() {
            public boolean matches(final JarEntry entry) {
                return true;
            }

            public boolean read(final JarEntry entry, final JarInputStream stream) throws IOException {
                if (!entry.isDirectory()) {
                    final String entryName = entry.getName();
                    final Reference<byte[]> reference = bytecodes.get(entryName);
                    final byte[] cached = reference == null ? null : reference.get();

                    if (entryName.endsWith(ClassLoaders.CLASS_SUFFIX) && (!reloading || (reference != null && cached == null))) {
                        bytes.reset();

                        int read;
                        while ((read = stream.read(buffer, 0, buffer.length)) != -1) {
                            bytes.write(buffer, 0, read);
                        }

                        final byte[] code = bytes.toByteArray();

                        if (resource.equals(entryName)) {
                            found[0] = code;
                        } else {

                            // only cached if needed later
                            bytecodes.put(entryName, new SoftReference<byte[]>(code));
                        }
                    }
                }

                return true;
            }
        });

        return found[0];
    }

    public URL findResource(final String name) {
        final URL url = caller.findResource(name);

        if (url == null) {
            try {
                final Enumeration<URL> resources = findResources(name);
                return resources.hasMoreElements() ? resources.nextElement() : null;
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return url;
        }
    }

    // takes advantage of a side effect of calling Handler methods, namely that loading the Handler class registers it as an URL stream handler
    public Enumeration<URL> findResources(final String resource) throws IOException {
        final List<URL> list = new ArrayList<URL>(Collections.list(caller.findResources(resource)));

        for (final Map.Entry<String, Set<String>> mapping : nameMap.entrySet()) {
            if (mapping.getValue().contains(resource)) {
                list.add(Handler.formatURL(caller.getResource(mapping.getKey()), ClassLoaders.absoluteResourceName(resource)));
            }
        }

        return Collections.enumeration(list);
    }
}

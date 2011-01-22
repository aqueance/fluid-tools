/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.fluidity.foundation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.net.URLClassLoader;
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

import org.fluidity.foundation.jarjar.Handler;

/**
 * A class loader that looks into jar files inside a jar file, one level only. The implementation loads and caches in a memory sensitive cache that is
 * automatically purged by the garbage collector all yet unused byte codes from any nested jar that was used to load a class from. If a nested jar is larger
 * than the available heap, this will cause thrashing. Don't go wild with your nested jars, keep them of reasonable size.
 *
 * @author Tibor Varga
 */
public final class JarJarClassLoader extends URLClassLoader {

    private final URL url;

    // linked so that it retains order of entries in the root jar file
    private final Map<String, Set<String>> nameMap = new LinkedHashMap<String, Set<String>>();
    private final Map<String, Lock> lockMap = new HashMap<String, Lock>();
    private final Map<String, Map<String, Reference<byte[]>>> bytecodeMap = new ConcurrentHashMap<String, Map<String, Reference<byte[]>>>();

    public JarJarClassLoader(final URL url, final ClassLoader parent, final String dependencies) throws IOException {
        super(new URL[] { url }, parent);
        this.url = url;

        JarStreams.readEntries(url, new JarStreams.JarEntryReader() {
            public boolean matches(final JarEntry entry) {
                return entry.getName().startsWith(dependencies);
            }

            public boolean read(final JarEntry entry, final JarInputStream stream) throws IOException {
                final Set<String> names = new HashSet<String>();

                JarEntry next;
                while ((next = stream.getNextJarEntry()) != null) {
                    if (!next.isDirectory()) {
                        names.add(next.getName());
                    }
                }

                final String name = entry.getName();
                nameMap.put(name, names);
                lockMap.put(name, new ReentrantLock());

                return true;
            }
        });
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        try {
            return super.findClass(name);
        } catch (final ClassNotFoundException e) {

            if (e.getCause() != null) {
                throw e;
            } else {
                try {
                    final String resource = ClassLoaders.classResourceName(name);

                    for (final Map.Entry<String, Set<String>> mapping : nameMap.entrySet()) {
                        if (mapping.getValue().contains(resource)) {
                            final String dependency = mapping.getKey();
                            final Lock lock = lockMap.get(dependency);

                            byte[] bytes = null;

                            try {

                                // prevent another thread from loading the same dependency
                                lock.lockInterruptibly();

                                try {
                                    final Map<String, Reference<byte[]>> bytecodes = bytecodeMap.get(dependency);

                                    if (bytecodes == null) {

                                        // not loaded yet: load it

                                        bytecodeMap.put(dependency, new HashMap<String, Reference<byte[]>>());
                                        bytes = loadDependency(resource, dependency, bytecodeMap.get(dependency), false);
                                    } else {

                                        // loaded already but not yet defined: remove reference from cache to signify that it should not be loaded any more
                                        final Reference<byte[]> reference = bytecodes.remove(resource);

                                        if (reference == null) {

                                            // we don't have such a class
                                            throw new ClassNotFoundException(name);
                                        } else if ((bytes = reference.get()) == null) {

                                            // we have a class but our byte code has been purged: load the whole thing again
                                            bytes = loadDependency(resource, dependency, bytecodes, true);
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

                            return defineClass(name, bytes, 0, bytes.length);
                        }
                    }

                    throw new ClassNotFoundException(name);
                } catch (final IOException io) {
                    throw new ClassNotFoundException(name, io);
                }
            }
        }
    }

    private byte[] loadDependency(final String resource, final String dependency, final Map<String, Reference<byte[]>> bytecodes, final boolean reloading)
            throws IOException {

        // strong reference prevents collection as garbage while we fill up the cache (i.e., class is found even when thrashing)
        final byte[][] found = new byte[1][];

        JarStreams.readEntries(url, new JarStreams.JarEntryReader() {
            public boolean matches(final JarEntry entry) {
                return dependency.equals(entry.getName());
            }

            public boolean read(final JarEntry entry, final JarInputStream stream) throws IOException {
                final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                final byte[] buffer = new byte[1024];

                JarEntry next;
                while ((next = stream.getNextJarEntry()) != null) {
                    if (!next.isDirectory()) {
                        final String entryName = next.getName();
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
                }

                return false;
            }
        });

        return found[0];
    }

    @Override
    public URL findResource(final String name) {
        final URL url = super.findResource(name);

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

    @Override
    public Enumeration<URL> findResources(final String resource) throws IOException {
        final List<URL> list = new ArrayList<URL>(Collections.list(super.findResources(resource)));

        for (final Map.Entry<String, Set<String>> mapping : nameMap.entrySet()) {
            if (mapping.getValue().contains(resource)) {

                // Handler.formatURL has a side effect of loading Handler as an URL stream handler
                list.add(Handler.formatURL(url, mapping.getKey() + "!/" + ClassLoaders.absoluteResourceName(resource)));
            }
        }

        return Collections.enumeration(list);
    }
}

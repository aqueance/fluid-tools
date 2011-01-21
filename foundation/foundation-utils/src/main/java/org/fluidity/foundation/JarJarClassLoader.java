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
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.fluidity.foundation.jarjar.Handler;

/**
 * A class loader that looks into jar files inside a jar file, one level only.
 *
 * @author Tibor Varga
 */
public final class JarJarClassLoader extends URLClassLoader {

    private final URL url;

    private final Map<String, Set<String>> nameMap = new LinkedHashMap<String, Set<String>>();
    private final Map<String, Map<String, byte[]>> bytecodeMap = new HashMap<String, Map<String, byte[]>>();

    public JarJarClassLoader(final URL url, final ClassLoader parent, final String dependencies) throws IOException {
        super(new URL[] { url }, parent);
        this.url = url;

        JarStreams.readEntry(url, new JarStreams.JarEntryReader() {
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

                nameMap.put(entry.getName(), names);

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
                    final String resource = name.replace('.', '/').concat(".class");

                    for (final Map.Entry<String, Set<String>> mapping : nameMap.entrySet()) {
                        if (mapping.getValue().contains(resource)) {
                            final String dependency = mapping.getKey();

                            if (!bytecodeMap.containsKey(dependency)) {
                                final Map<String, byte[]> bytecodes = new HashMap<String, byte[]>();
                                bytecodeMap.put(dependency, bytecodes);

                                JarStreams.readEntry(url, new JarStreams.JarEntryReader() {
                                    public boolean matches(final JarEntry entry) {
                                        return dependency.equals(entry.getName());
                                    }

                                    public boolean read(JarEntry entry, final JarInputStream stream) throws IOException {
                                        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                                        final byte[] buffer = new byte[1024];

                                        JarEntry next;
                                        while ((next = stream.getNextJarEntry()) != null) {
                                            if (!next.isDirectory()) {
                                                final String entryName = next.getName();

                                                int read;
                                                while ((read = stream.read(buffer, 0, buffer.length)) != -1) {
                                                    bytes.write(buffer, 0, read);
                                                }

                                                bytecodes.put(entryName, bytes.toByteArray());
                                            }
                                        }

                                        return false;
                                    }
                                });
                            }

                            final Map<String, byte[]> bytecodes = bytecodeMap.get(dependency);
                            if (bytecodes == null) {
                                throw new ClassNotFoundException(name);
                            }

                            final byte[] bytes = bytecodes.remove(resource);
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

    @Override
    public Enumeration<URL> findResources(final String resource) throws IOException {
        final List<URL> list = new ArrayList<URL>(Collections.list(super.findResources(resource)));

        for (final Map.Entry<String, Set<String>> mapping : nameMap.entrySet()) {
            if (mapping.getValue().contains(resource)) {
                list.add(Handler.formatURL(url, mapping.getKey() + "!/" + ClassLoaders.absoluteResourceName(resource)));
            }
        }

        return Collections.enumeration(list);
    }
}

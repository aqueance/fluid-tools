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

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

/**
 * Handles jar files inside another jar file with known location.
 */
public class JarJarClassLoader extends URLClassLoader {

    private final NestedJarClassLoader nested;

    public JarJarClassLoader(final URL url, final ClassLoader parent, final String... paths) throws IOException {
        super(new URL[] { url }, parent);

        this.nested = new NestedJarClassLoader(new NestedJarClassLoader.Caller() {
            public Class<?> findClass(final String name) throws ClassNotFoundException {
                return JarJarClassLoader.super.findClass(name);
            }

            public URL findResource(final String resource) {
                return JarJarClassLoader.super.findResource(resource);
            }

            public Enumeration<URL> findResources(final String resource) throws IOException {
                return JarJarClassLoader.super.findResources(resource);
            }

            public Class<?> defineClass(final String name, final byte[] bytes, final int offset, final int length) throws ClassFormatError {
                return JarJarClassLoader.this.defineClass(name, bytes, offset, length);
            }

            public URL getResource(final String resource) {
                return JarJarClassLoader.this.getResource(resource);
            }

            @Override
            public String toString() {
                return getURLs()[0].toExternalForm();
            }
        });

        this.nested.init(paths);
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        return nested.findClass(name);
    }

    @Override
    public URL findResource(final String name) {
        return nested.findResource(name);
    }

    @Override
    public Enumeration<URL> findResources(final String name) throws IOException {
        return nested.findResources(name);
    }
}

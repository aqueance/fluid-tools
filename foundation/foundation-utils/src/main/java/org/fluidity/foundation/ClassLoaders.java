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

import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

import org.apache.xbean.classloader.JarFileClassLoader;

/**
 * Finds a suitable class loader in various application containers (web, ejb, junit, testng, maven).
 * <p/>
 * In test cases this class can be controlled by setting the context class loader to a mock class loader that returns what the test case desires. This is done
 * by calling
 * <pre>
 * Thread.currentThread().setContextClassLoader(...);
 * </pre>
 *
 * @author Tibor Varga
 */
public final class ClassLoaders extends Utilities {

    public static final String CLASS_SUFFIX = ".class";

    public static ClassLoader findClassLoader(final Class sourceClass) {
        ClassLoader result = Thread.currentThread().getContextClassLoader();
        result = result == null ? sourceClass.getClassLoader() : result;
        result = result == null ? ClassLoaders.class.getClassLoader() : result;
        return result == null ? ClassLoader.getSystemClassLoader() : result;
    }

    public static String absoluteResourceName(final String format, final Object... params) {
        final String resourceName = String.format(format, params);
        return resourceName.startsWith("/") ? resourceName.substring(1) : resourceName;
    }

    public static URL findResource(final Class sourceClass, final String format, final Object... params) {
        return findClassLoader(sourceClass).getResource(absoluteResourceName(format, params));
    }

    public static String classResourceName(final Class sourceClass) {
        return classResourceName(sourceClass.getName());
    }

    public static String classResourceName(final String sourceClass) {
        return sourceClass.replace('.', '/').concat(CLASS_SUFFIX);
    }

    public static URL findClassResource(final Class sourceClass) {
        return findClassLoader(sourceClass).getResource(classResourceName(sourceClass));
    }

    public static InputStream readClassResource(final Class sourceClass) {
        return findClassLoader(sourceClass).getResourceAsStream(classResourceName(sourceClass));
    }

    public static JarFileClassLoaders jarFileClassLoaders() {
        return JarFileClassLoaders.INSTANCE;
    }

    /**
     * Isolates JarFileClassLoader, which is in an optional dependency of this Maven project.
     */
    public static class JarFileClassLoaders {
        static JarFileClassLoaders INSTANCE = new JarFileClassLoaders();

        private static final String[] EMPTY_STRING_ARRAY = new String[0];

        public JarFileClassLoader create(final ClassLoader parent, final URL... urls) {
            return new JarFileClassLoader(UUID.randomUUID().toString(),
                                          urls,
                                          parent == null ? ClassLoader.getSystemClassLoader() : parent, // pass null and even java.lang.Object goes missing...
                                          true, // some idiot thought it was a good idea to create a class loader that fails to delegate to parent by default...
                                          EMPTY_STRING_ARRAY,
                                          EMPTY_STRING_ARRAY);
        }
    }
}

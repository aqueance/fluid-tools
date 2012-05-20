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

package org.fluidity.foundation;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.xbean.classloader.JarFileClassLoader;

/**
 * Utility methods related to {@linkplain ClassLoader class loaders}.
 *
 * @author Tibor Varga
 */
@SuppressWarnings("UnusedDeclaration")
public final class ClassLoaders extends Utility {

    private ClassLoaders() { }

    /**
     * The <code>.class</code> suffix of Java class files.
     */
    public static final String CLASS_SUFFIX = ".class";

    /**
     * Finds the class loader appropriate for the given class. Here, the term <em>appropriate</em> means:<ul>
     * <li>the thread context class loader, if given</li>
     * <li>else the class loader that loaded the supplied class, if not <code>null</code></li>
     * <li>else the class loader that loaded this class, if not <code>null</code></li>
     * <li>else the system class loader</li>
     * </ul>
     *
     * @param sourceClass the class to find the appropriate class loader for.
     *
     * @return the class loader appropriate for the given class.
     */
    public static ClassLoader findClassLoader(final Class sourceClass) {
        ClassLoader result = Thread.currentThread().getContextClassLoader();
        result = result == null ? sourceClass.getClassLoader() : result;
        result = result == null ? ClassLoaders.class.getClassLoader() : result;
        return result == null ? ClassLoader.getSystemClassLoader() : result;
    }

    /**
     * Returns the name that can be fed to {@link ClassLoader#findResource(String)} and {@link ClassLoader#findResources(String)}. The resource name is
     * computed from a format string and its parameters.
     *
     * @param format the format string.
     * @param params the parameters of the format string.
     *
     * @return the resource name that can be used with <code>ClassLoader</code> methods.
     */
    public static String absoluteResourceName(final String format, final Object... params) {
        final String resourceName = String.format(format, params);
        return resourceName.startsWith("/") ? resourceName.substring(1) : resourceName;
    }

    /**
     * Finds the named resource in the class loader appropriate for the given class. See {@link #findClassLoader(Class)} for the definition of
     * <em>appropriate</em>. The resource name is computed from a format string and its parameters.
     *
     * @param sourceClass the class to use the appropriate class loader for.
     * @param format      the format string.
     * @param params      the parameters of the format string.
     *
     * @return the URL for the resource, or <code>null</code> if the resource could not be found.
     */
    public static URL findResource(final Class sourceClass, final String format, final Object... params) {
        return findClassLoader(sourceClass).getResource(absoluteResourceName(format, params));
    }

    /**
     * Finds the named resources in the class loader appropriate for the given class. See {@link #findClassLoader(Class)} for the definition of
     * <em>appropriate</em>. The resource name is computed from a format string and its parameters.
     *
     * @param sourceClass the class to use the appropriate class loader for.
     * @param format      the format string.
     * @param params      the parameters of the format string.
     *
     * @return the list of URLs for the resource; possibly empty.
     */
    public static List<URL> findResources(final Class sourceClass, final String format, final Object... params) throws IOException {
        return Collections.list(findClassLoader(sourceClass).getResources(absoluteResourceName(format, params)));
    }

    /**
     * Finds the named resource in the class loader appropriate for the given class and returns an input stream to read its contents. See {@link
     * #findClassLoader(Class)} for the definition of <em>appropriate</em>. The resource name is computed from a format string and its parameters.
     *
     * @param sourceClass the class to use the appropriate class loader for.
     * @param format      the format string.
     * @param params      the parameters of the format string.
     *
     * @return the input stream for the resource, or <code>null</code> if the resource could not be found.
     */
    public static InputStream readResource(final Class sourceClass, final String format, final Object... params) {
        return findClassLoader(sourceClass).getResourceAsStream(absoluteResourceName(format, params));
    }

    /**
     * Returns the resource name for the given class. The resource name is the value to feed to {@link ClassLoader#findResource(String)} to find the URL where
     * the class may be loaded from. The method {@link #findClassResource(Class)} calls that method with the output of this one.
     *
     * @param sourceClass the class to return the resource name of.
     *
     * @return a string, never <code>null</code>.
     */
    public static String classResourceName(final Class sourceClass) {
        return classResourceName(sourceClass.getName());
    }

    /**
     * Returns the resource name for the given class name. The resource name is the value to feed to {@link ClassLoader#findResource(String)} to find the URL
     * where the class may be loaded from.
     *
     * @param sourceClass the name of the class to return the resource name of.
     *
     * @return a string, never <code>null</code>.
     */
    public static String classResourceName(final String sourceClass) {
        return sourceClass.replace('.', '/').concat(CLASS_SUFFIX);
    }

    /**
     * Returns the resource URL where the given class may be loaded from.
     *
     * @param sourceClass the class to return the resource URL for.
     *
     * @return the resource URL for the given class.
     */
    public static URL findClassResource(final Class sourceClass) {
        return findClassLoader(sourceClass).getResource(classResourceName(sourceClass));
    }

    /**
     * Returns the input stream where the given class may be loaded from.
     *
     * @param sourceClass the class to return the resource URL for.
     *
     * @return the input stream for the given class.
     */
    public static InputStream readClassResource(final Class sourceClass) {
        return findClassLoader(sourceClass).getResourceAsStream(classResourceName(sourceClass));
    }

    /**
     * Returns a closeable URL class loader. To close it, cast it to {@link Closeable} and invoke {@link Closeable#close()} on the returned class loader.
     *
     * @param parent the parent class loader for the returned one.
     * @param urls   the URLs to initialize the returned class loader with.
     *
     * @return a closeable URL class loader.
     */
    public static ClassLoader create(final ClassLoader parent, final URL... urls) {
        return Closeable.class.isAssignableFrom(URLClassLoader.class) ? new URLClassLoader(urls, parent) : XBeansClassLoaders.INSTANCE.create(parent, urls);
    }

    /**
     * Sets the given class loader as the {@linkplain Thread#setContextClassLoader(ClassLoader) context class loader} and returns the previous context class
     * loader.
     *
     * @param loader the class loader to set.
     *
     * @return the previous class loader.
     */
    private static ClassLoader set(final ClassLoader loader) {
        final Thread thread = Thread.currentThread();

        try {
            return thread.getContextClassLoader();
        } finally {
            thread.setContextClassLoader(loader);
        }
    }

    /**
     * Sets as the {@linkplain Thread#setContextClassLoader(ClassLoader) context class loader} the one that loaded the given class and returns the previous
     * context class loader.
     *
     * @param type the class loader to set.
     *
     * @return the previous class loader.
     */
    public static ClassLoader set(final Class type) {
        final ClassLoader loader = type.getClassLoader();
        return set(loader == null ? ClassLoader.getSystemClassLoader() : loader);
    }

    /**
     * Establishes the given class loader as the {@linkplain Thread#setContextClassLoader(ClassLoader) context class loader}, executes the given command and
     * the establishes the previous context class loader before returning what the command returned, or throwing what the command threw.
     *
     * @param loader  the class loader to set as the context class loader.
     * @param command the command to execute with the given class loader as the context class loader.
     * @param <R>     the return type of the command.
     * @param <E>     the type of the exception thrown by the command.
     *
     * @return whatever the command returns.
     *
     * @throws E from the command.
     */
    public static <R, E extends Throwable> R context(final ClassLoader loader, final ContextCommand<R, E> command) throws E {
        final ClassLoader saved = set(loader);

        try {
            return command.run(loader);
        } finally {
            set(saved);
        }
    }

    /**
     * A command to execute by {@link ClassLoaders#context(ClassLoader, ClassLoaders.ContextCommand)}.
     *
     * @param <R> the return type of the command.
     * @param <E> the type of the exception thrown by the command.
     */
    public interface ContextCommand<R, E extends Throwable> {

        /**
         * Executes custom code in the {@linkplain ClassLoaders#context(ClassLoader, ClassLoaders.ContextCommand) context} of a class loader.
         *
         * @param loader the context class loader.
         *
         * @return whatever it wants.
         *
         * @throws E whenever it wants to.
         */
        R run(ClassLoader loader) throws E;
    }

    /**
     * This class isolates the xbeans class loader thus prevents it from being loaded if it is not necessary (i.e., when Java 7+ is present)
     */
    private static class XBeansClassLoaders {

        static final XBeansClassLoaders INSTANCE = new XBeansClassLoaders();

        public ClassLoader create(final ClassLoader parent, final URL... urls) {
            final ClassLoader daddy = parent == null ? ClassLoader.getSystemClassLoader() : parent;
            return new CloseableURLClassLoader(urls, daddy);
        }
    }

    private static class CloseableURLClassLoader extends JarFileClassLoader implements Closeable {

        private static final String[] EMPTY_STRING_ARRAY = new String[0];

        public CloseableURLClassLoader(final URL[] urls, final ClassLoader parent) {
            super(UUID.randomUUID().toString(), urls, parent, true, EMPTY_STRING_ARRAY, EMPTY_STRING_ARRAY);
            assert parent != null;
        }

        public void close() throws IOException {
            destroy();
        }
    }
}

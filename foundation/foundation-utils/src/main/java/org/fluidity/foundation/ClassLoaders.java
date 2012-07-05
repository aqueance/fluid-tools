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
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
     * <li>the thread context class loader, if given and <code>fallback</code> is <code>true</code></li>
     * <li>else the class loader that loaded the supplied class, if not <code>null</code></li>
     * <li>else if <code>fallback</code> is <code>true</code>, the class loader that loaded this class, if not <code>null</code></li>
     * <li>else the system class loader.</li>
     * </ul>
     *
     * @param type     the class to find the appropriate class loader for.
     * @param fallback if <code>true</code>, the context class loader and the class loader of this utility class may be returned.
     *
     * @return the class loader appropriate for the given class.
     */
    public static ClassLoader findClassLoader(final Class type, final boolean fallback) {
        ClassLoader result = fallback ? Thread.currentThread().getContextClassLoader() : null;
        result = result == null ? type.getClassLoader() : result;
        result = fallback && result == null ? ClassLoaders.class.getClassLoader() : result;
        return result == null ? ClassLoader.getSystemClassLoader() : result;
    }

    /**
     * Returns the name that can be fed to {@link ClassLoader#findResource(String) ClassLoader.findResource()} and {@link ClassLoader#findResources(String)
     * ClassLoader.findResources()}. The resource name is computed from a format string and its parameters.
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
     * Finds the named resource in the class loader appropriate for the given class. See {@link #findClassLoader(Class, boolean)
     * ClassLoaders.findClassLoader(type, true)} for the definition of <em>appropriate</em>. The resource name is computed from a format string and its
     * parameters.
     *
     * @param type   the class to use the appropriate class loader for.
     * @param format the format string.
     * @param params the parameters of the format string.
     *
     * @return the URL for the resource, or <code>null</code> if the resource could not be found.
     */
    public static URL findResource(final Class type, final String format, final Object... params) {
        return findClassLoader(type, true).getResource(absoluteResourceName(format, params));
    }

    /**
     * Finds the named resources in the class loader appropriate for the given class. See {@link #findClassLoader(Class, boolean)
     * ClassLoaders.findClassLoader(type, true)} for the definition of <em>appropriate</em>. The resource name is computed from a format string and its
     * parameters.
     *
     * @param type   the class to use the appropriate class loader for.
     * @param format the format string.
     * @param params the parameters of the format string.
     *
     * @return the list of URLs for the resource; possibly empty.
     */
    public static List<URL> findResources(final Class type, final String format, final Object... params) throws IOException {
        return Collections.list(findClassLoader(type, true).getResources(absoluteResourceName(format, params)));
    }

    /**
     * Finds the named resource in the class loader appropriate for the given class and returns an input stream to read its contents. See {@link
     * #findClassLoader(Class, boolean) ClassLoaders.findClassLoader(type, true)} for the definition of <em>appropriate</em>. The resource name is computed from
     * a format string and its parameters.
     *
     * @param type   the class to use the appropriate class loader for.
     * @param format the format string.
     * @param params the parameters of the format string.
     *
     * @return the input stream for the resource, or <code>null</code> if the resource could not be found.
     */
    public static InputStream readResource(final Class type, final String format, final Object... params) {
        return findClassLoader(type, true).getResourceAsStream(absoluteResourceName(format, params));
    }

    /**
     * Returns the resource name for the given class. The resource name is the value to feed to {@link ClassLoader#findResource(String)
     * ClassLoader.findResource()} to find the URL where the class may be loaded from. The method {@link #findClassResource(Class)
     * ClassLoader.findClassResource()} calls that method with the output of this one.
     *
     * @param type the class to return the resource name of.
     *
     * @return a string, never <code>null</code>.
     */
    public static String classResourceName(final Class type) {
        return classResourceName(type.getName());
    }

    /**
     * Returns the resource name for the given class name. The resource name is the value to feed to {@link ClassLoader#findResource(String)
     * ClassLoader.findResource()} to find the URL where the class may be loaded from.
     *
     * @param type the name of the class to return the resource name of.
     *
     * @return a string, never <code>null</code>.
     */
    public static String classResourceName(final String type) {
        return type.replace('.', '/').concat(CLASS_SUFFIX);
    }

    /**
     * Returns the resource URL where the given class may be loaded from.
     *
     * @param type the class to return the resource URL for.
     *
     * @return the resource URL for the given class.
     */
    public static URL findClassResource(final Class type) {
        return findClassLoader(type, false).getResource(classResourceName(type));
    }

    /**
     * Returns the input stream where the given class may be loaded from.
     *
     * @param type the class to return the resource URL for.
     *
     * @return the input stream for the given class.
     */
    public static InputStream readClassResource(final Class type) {
        return findClassLoader(type, false).getResourceAsStream(classResourceName(type));
    }

    /**
     * Returns a closeable URL class loader. To close it, cast it to {@link Closeable}, and invoke the {@link Closeable#close()} method.
     *
     * @param parent the parent class loader for the returned one; may be <code>null</code>.
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
        return set(findClassLoader(type, false));
    }

    /**
     * Establishes the given class loader as the {@linkplain Thread#setContextClassLoader(ClassLoader) context class loader}, executes the given command, and
     * then establishes the previous context class loader before returning whatever the command returned, or throwing whatever the command threw.
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
    public static <R, E extends Throwable> R context(final ClassLoader loader, final Command<R, E> command) throws E {
        final ClassLoader saved = set(loader);

        try {
            return command.run(loader);
        } finally {
            set(saved);
        }
    }

    /**
     * Creates an isolated URL class loader for the given list of URLs, loads using the isolated class loader and instantiates the given type and calls the
     * given method on it with the given parameters to return its return value.
     *
     *
     * @param parent the class loader to load the method parameters and the return values; this class loader must not see the given <code>type</code>.
     * @param urls the list of URLs to use for the isolated class loader; make sure the list contains the JARs containing the type to load.
     * @param type the command class to load and invoke the given method on.
     * @param run the method to call; the parameter types and the return type must be loaded by the parent class loader and the declaring class must be either
     *            visible to the <code>parent</code> class loader or listed in the given list of URLs.
     * @param arguments the arguments to pass to the command.
     * @return whatever the command returns.
     * @throws Exception when anything goes wrong.
     */
    @SuppressWarnings("unchecked")
    public static <T> T isolate(final ClassLoader parent, final Set<URL> urls, final Class<?> type, final Method run, final Object... arguments)
            throws Exception {
        final ClassLoader isolated = create(parent, urls.toArray(new URL[urls.size()]));

        try {
            // find the command
            final Object command = isolated.loadClass(type.getName()).newInstance();

            // find the method to call in the other class loader
            final Method method = isolated.loadClass(run.getDeclaringClass().getName()).getDeclaredMethod(run.getName(), run.getParameterTypes());

            // see if the class loader can see the ContainerBoundary and BundleBootstrap classes and if so, set the bundle activator
            method.setAccessible(true);
            return (T) method.invoke(command, arguments);
        } finally {
            try {
                ((Closeable) isolated).close();
            } catch (final IOException e) {
                // ignore
            }
        }
    }

    /**
     * A command to execute by {@link ClassLoaders#context(ClassLoader, ClassLoaders.Command) ClassLoaders.context()} with some class loader as the
     * context class loader.
     * <h3>Usage</h3>
     * <pre>
     * final {@linkplain ClassLoader} loader = &hellip;;
     *
     * final boolean success = {@linkplain ClassLoaders}.<span class="hl1">context</span>(loader, new <span class="hl1">ClassLoaders.Command</span>&lt;Boolean, {@linkplain RuntimeException}>() {
     *   public Boolean run(final {@linkplain ClassLoader} loader) throws {@linkplain RuntimeException} {
     *     &hellip; // call some library that uses the {@linkplain Thread#getContextClassLoader() thread context class loader}
     *     return true;
     *   }
     * });
     * </pre>
     *
     * @param <R> the return type of the command.
     * @param <E> the type of the exception thrown by the command.
     *
     * @author Tibor Varga
     */
    public interface Command<R, E extends Throwable> {

        /**
         * Executes custom code in the {@linkplain ClassLoaders#context(ClassLoader, ClassLoaders.Command) context} of a class loader.
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
     *
     * @author Tibor Varga
     */
    private static class XBeansClassLoaders {

        static final XBeansClassLoaders INSTANCE = new XBeansClassLoaders();

        public ClassLoader create(final ClassLoader parent, final URL... urls) {
            final ClassLoader daddy = parent == null ? ClassLoader.getSystemClassLoader() : parent;
            return new CloseableURLClassLoader(urls, daddy);
        }
    }

    /**
     * Adapts the xbeans URL class loader to the {@link Closeable} interface.
     *
     * @author Tibor Varga
     */
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

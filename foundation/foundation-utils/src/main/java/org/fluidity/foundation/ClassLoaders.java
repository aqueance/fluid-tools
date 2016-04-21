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

package org.fluidity.foundation;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.fluidity.foundation.jarjar.URLClassLoader;

import static org.fluidity.foundation.Command.Function;

/**
 * Utility methods related to {@linkplain ClassLoader class loaders}.
 *
 * @author Tibor Varga
 */
@SuppressWarnings({ "UnusedDeclaration", "WeakerAccess" })
public final class ClassLoaders extends Utility {

    /**
     * The <code>.class</code> suffix of Java class files.
     */
    public static final String CLASS_SUFFIX = ".class";

    private ClassLoaders() { }

    /**
     * Finds the class loader appropriate for the given class. Here, the term <em>appropriate</em> means:<ul>
     * <li>the thread context class loader, if given and <code>fallback</code> is <code>true</code></li>
     * <li>else the class loader that loaded the supplied class, if not <code>null</code></li>
     * <li>else if <code>fallback</code> is <code>true</code>, the class loader that loaded this class, if not <code>null</code></li>
     * <li>else the system class loader.</li>
     * </ul>
     * The caller must have the {@link RuntimePermission} <code>"getClassLoader"</code> permission.
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
     * <p>
     * The caller must have the {@link RuntimePermission} <code>"getClassLoader"</code> permission.
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
     * Finds the named resources in the given class loader. The resource name is computed from a format string and its parameters. The returned list will not
     * contain duplicates.
     *
     * @param loader the class loader.
     * @param format the format string.
     * @param params the parameters of the format string.
     *
     * @return the list of URLs for the resource; possibly empty.
     *
     * @throws IOException when I/O exception occurs.
     */
    public static Collection<URL> findResources(final ClassLoader loader, final String format, final Object... params) throws IOException {

        // Some dumb class loaders load JAR files more than once; hence the linked hash set
        return new LinkedHashSet<>(Collections.list(loader.getResources(absoluteResourceName(format, params))));
    }

    /**
     * Finds the named resource in the class loader appropriate for the given class and returns an input stream to read its contents. See {@link
     * #findClassLoader(Class, boolean) ClassLoaders.findClassLoader(type, true)} for the definition of <em>appropriate</em>. The resource name is computed
     * from a format string and its parameters.
     * <p>
     * The caller must have the {@link RuntimePermission} <code>"getClassLoader"</code> permission.
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
     * <p>
     * The caller must have the {@link RuntimePermission} <code>"getClassLoader"</code> permission.
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
     * <p>
     * The caller must have the {@link RuntimePermission} <code>"getClassLoader"</code> permission.
     *
     * @param type the class to return the resource URL for.
     *
     * @return the input stream for the given class.
     */
    public static InputStream readClassResource(final Class type) {
        return findClassLoader(type, false).getResourceAsStream(classResourceName(type));
    }

    /**
     * Sets the given class loader as the {@linkplain Thread#setContextClassLoader(ClassLoader) context class loader} and returns the previous context class
     * loader.
     * <p>
     * The caller must have the {@link RuntimePermission} <code>"getClassLoader"</code> and {@link RuntimePermission} <code>"setContextClassLoader"</code>
     * permissions.
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
     * <p>
     * The caller must have the {@link RuntimePermission} <code>"getClassLoader"</code> and {@link RuntimePermission} <code>"setContextClassLoader"</code>
     * permissions.
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
     * <p>
     * The caller must have the {@link RuntimePermission} <code>"getClassLoader"</code> and {@link RuntimePermission} <code>"setContextClassLoader"</code>
     * permissions.
     * <h3>Usage</h3>
     * <pre>
     * final {@linkplain ClassLoader} loader = &hellip;;
     *
     * final boolean success = {@linkplain ClassLoaders}.<span class="hl1">context</span>(loader, _loader -&gt; {
     *   &hellip; // call some library that uses the {@linkplain Thread#getContextClassLoader() thread context class loader}
     *   return true;
     * });
     * </pre>
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
    public static <R, E extends Exception> R context(final ClassLoader loader, final Function<R, ClassLoader, E> command) throws E {
        final ClassLoader saved = set(loader);

        try {
            return command.run(loader);
        } finally {
            set(saved);
        }
    }

    /**
     * Creates a class loader with the given URLs.
     *
     * @param urls     the list of URLs; may not be <code>null</code>.
     * @param parent   the parent class loader; may be <code>null</code>.
     * @param handlers the optional URL stream handler factory; may be <code>null</code>.
     *
     * @return a class loader that loads classes from the given URLs.
     */
    public static ClassLoader create(final Collection<URL> urls, final ClassLoader parent, final URLStreamHandlerFactory handlers) {
        return new URLClassLoader(urls, parent, handlers);
    }

    /**
     * Creates a class loader with the given URLs. The parent class loader of the returned class loader with be the {@linkplain
     * ClassLoader#getSystemClassLoader() system class loader}.
     *
     * @param urls     the list of URLs; may not be <code>null</code>.
     * @param handlers the optional URL stream handler factory; may be <code>null</code>.
     *
     * @return a class loader that loads classes from the given URLs.
     */
    public static ClassLoader create(final Collection<URL> urls, final URLStreamHandlerFactory handlers) {
        return new URLClassLoader(urls, handlers);
    }
}

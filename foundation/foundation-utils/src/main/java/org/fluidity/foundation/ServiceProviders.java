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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Implements the Sun JDK service provider discovery. This class is used internally. Use the {@link org.fluidity.composition.ServiceProvider @ServiceProvider}
 * annotation to produce and the {@link org.fluidity.composition.container.ClassDiscovery} component to consume service providers, rather than directly using
 * this low level utility.
 *
 * @author Tibor Varga
 */
@SuppressWarnings("JavadocReference")
public final class ServiceProviders extends Utility {

    /**
     * The name of the subdirectory in JAR files that contains service provider files.
     */
    public static final String TYPE = "services";

    /**
     * The directory in JAR files that contains service provider files.
     */
    public static final String LOCATION = String.format("%s/%s", Archives.META_INF, TYPE);

    private ServiceProviders() { }

    /**
     * Returns the directory in JAR files that may contain Fluid Tools {@link org.fluidity.composition.ServiceProvider @ServiceProvider} files of the given
     * type.
     *
     * @param type the service provider type.
     *
     * @return a directory name.
     */
    @SuppressWarnings("JavadocReference")
    public static String location(final String type) {
        return TYPE.equals(type) ? LOCATION : String.format("%s/%s", LOCATION, type);
    }

    /**
     * Returns the first service provider implementation for the given interface.
     *
     * @param interfaceClass the service provider interface.
     * @param classLoader    the class loader to look for implementations in.
     * @param <T>            the service provider interface type.
     *
     * @return the first implementation of the given interface or <code>null</code> if none found.
     */
    public static <T> T findInstance(final Class<T> interfaceClass, final ClassLoader classLoader) {
        final Iterator<T> providers = providers(interfaceClass, classLoader);
        return providers.hasNext() ? providers.next() : null;
    }

    /**
     * Returns all service provider implementations for the given interface.
     *
     * @param interfaceClass the service provider interface.
     * @param classLoader    the class loader to look for implementations in.
     * @param <T>            the service provider interface type.
     *
     * @return the implementations of the given interface or an empty list if none found.
     */
    public static <T> List<T> findInstances(final Class<T> interfaceClass, final ClassLoader classLoader) {
        final List<T> list = new ArrayList<>();

        for (final Iterator<T> providers = providers(interfaceClass, classLoader); providers != null && providers.hasNext();) {
            list.add(providers.next());
        }

        return list;
    }

    private static <T> Iterator<T> providers(final Class<T> interfaceClass, final ClassLoader classLoader) {

        /*
         * The standard service provider discovery utilities seem to use <code>Class.forName(...)</code>, and that is a no-no.
         */

        final Class<T>[] types = findClasses(TYPE, interfaceClass, classLoader, false, true, true, new Log() {
            public void debug(final String format, final Object... arguments) {
                // ignore
            }

            public void error(final String format, final Object... arguments) {
                System.err.printf(format.concat("%n"), arguments);
            }

            public void error(final Exception error, final String format, final Object... arguments) {
                final StringWriter problem = new StringWriter();
                error.printStackTrace(new PrintWriter(problem));
                System.err.printf("%s: %s%n", String.format(format, arguments), problem);
            }
        });

        return new Iterator<T>() {
            private int index = 0;

            public boolean hasNext() {
                return index < types.length;
            }

            public T next() {
                return Exceptions.wrap(() -> {
                    final PrivilegedExceptionAction<Constructor<T>> action = () -> {
                        final Constructor<T> constructor = types[index++].getDeclaredConstructor();

                        if (!constructor.isAccessible()) {
                            constructor.setAccessible(true);
                        }

                        return constructor;
                    };

                    return (Security.CONTROLLED ? AccessController.doPrivileged(action) : action.run()).newInstance();
                });
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Finds all classes visible to the given class loader that implement or extend the given service provider interface.
     *
     * @param type     the service provider type; Java uses <code>"services"</code>  while Fluid Tools allow any name here using @{@link
     *                 org.fluidity.composition.ServiceProvider#type() ServiceProvider(type = "&hellip;")}.
     * @param api      the interface or class all discovered classes should implement or extend.
     * @param loader   the class loader to use to find the classes.
     * @param strict   specifies whether to find classes directly visible to the given class loader (<code>true</code>) or indirectly via any of its parent
     *                 class loaders (<code>false</code>).
     * @param standard specifies whether only standard service providers are accepted (<code>true</code>) or dependency injected ones also
     *                 (<code>false</code>).
     * @param inherit  specifies whether the discovered classes must be assignable to <code>api</code> (<code>true</code>) or not (<code>false</code>); ignored
     *                 when <code>standard</code> is <code>true</code>.
     * @param log      the logger to emit messages through.
     * @param <T>      the type of the given service provider interface.
     *
     * @return a list of <code>Class</code> objects for the discovered classes.
     */
    public static <T> Class<T>[] findClasses(final String type,
                                             final Class<T> api,
                                             final ClassLoader loader,
                                             final boolean strict,
                                             final boolean standard,
                                             final boolean inherit,
                                             final Log log) {
        return Exceptions.wrap(() -> {
            final ClassLoader classLoader = loader != null
                ? loader
                : !Security.CONTROLLED
                    ? ClassLoaders.findClassLoader(api, true)
                    : AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> ClassLoaders.findClassLoader(api, true));

            log.debug("Loading %s service provider files for %s using class loader %s", standard ? "standard" : String.format("'%s' type", type), api, classLoader);

            final Collection<Class<T>> componentList = new LinkedHashSet<>();

            for (final URL url : ClassLoaders.findResources(classLoader, "%s/%s", ServiceProviders.location(type), api.getName())) {
                log.debug("Loading %s", url);

                final Collection<Class<T>> localList = new LinkedHashSet<>();

                try (final BufferedReader reader = new BufferedReader(new InputStreamReader(Archives.open(true, url), "UTF-8"))) {
                    String content;

                    while ((content = reader.readLine()) != null) {
                        final int hash = content.indexOf('#');
                        final String line = (hash < 0 ? content : content.substring(0, hash)).trim();

                        if (!line.isEmpty()) {
                            final Class<?> rawClass;

                            try {
                                rawClass = classLoader.loadClass(line);
                            } catch (final ClassNotFoundException e) {
                                log.error(e, "Invalid class name: %s", line);
                                continue;
                            }

                            final boolean loadable = !strict || classLoader == (!Security.CONTROLLED ? rawClass.getClassLoader() : AccessController.doPrivileged((PrivilegedAction<ClassLoader>) rawClass::getClassLoader));
                            final boolean visible = !standard || null != (!Security.CONTROLLED ? rawClass.getDeclaredConstructor() : AccessController.doPrivileged((PrivilegedExceptionAction<Object>) rawClass::getDeclaredConstructor));

                            if (loadable && visible) {
                                final boolean compatible = api.isAssignableFrom(rawClass);

                                if (compatible || (!standard && !inherit)) {

                                    @SuppressWarnings("unchecked")
                                    final Class<T> componentClass = (Class<T>) rawClass;

                                    if (Modifier.isAbstract(componentClass.getModifiers())) {
                                        log.debug("Ignoring abstract service provider %s", componentClass);
                                    } else {
                                        if (componentList.contains(componentClass)) {
                                            log.error("Multiple export of %s", componentClass);
                                        } else {
                                            if (localList.contains(componentClass)) {
                                                log.error("Duplicate %s", componentClass);
                                            } else {
                                                log.debug("Found %s", componentClass);
                                                localList.add(componentClass);
                                            }
                                        }
                                    }
                                } else {
                                    log.error("%s does not implement %s", rawClass, api);
                                }
                            }
                        }
                    }
                }

                componentList.addAll(localList);
            }

            if (componentList.isEmpty()) log.debug("None found");
            return Lists.asArray(Class.class, componentList);
        });
    }

    /**
     * Provides means to emit log messages without having {@link org.fluidity.foundation.Log} available.
     *
     * @author Tibor Varga
     */
    public interface Log {

        /**
         * Emits a DEBUG level message, provided that DEBUG level message emission is {@link org.fluidity.foundation.Log#isDebugEnabled() permitted}.
         *
         * @param format    the format parameter of a {@link String#format(String, Object...)} call.
         * @param arguments the arguments parameter of a {@link String#format(String, Object...)} call.
         */
        void debug(String format, Object... arguments);

        /**
         * Emits an ERROR level message.
         *
         * @param format    the format parameter of a {@link String#format(String, Object...)} call.
         * @param arguments the arguments parameter of a {@link String#format(String, Object...)} call.
         */
        void error(String format, Object... arguments);

        /**
         * Emits an ERROR level message and an exception stack trace.
         *
         * @param error     the exception to log the stack trace of.
         * @param format    the format parameter of a {@link String#format(String, Object...)} call.
         * @param arguments the arguments parameter of a {@link String#format(String, Object...)} call.
         */
        void error(Exception error, String format, Object... arguments);
    }
}

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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import sun.misc.Service;
import sun.misc.ServiceConfigurationError;

/**
 * Wraps the Sun JDK service provider discovery implementation that was private API prior to Java 6. This class is used internally to maintain compatibility
 * with Java 5. Use the {@link org.fluidity.composition.ServiceProvider @ServiceProvider} annotation and the {@link org.fluidity.foundation.ClassDiscovery}
 * component instead of this low level utility to make your tasks in dealing with service providers much, much simpler.
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
     * @param <T>            the service provider interface
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
     * @param <T>            the service provider interface
     *
     * @return the implementations of the given interface or an empty list if none found.
     */
    public static <T> List<T> findInstances(final Class<T> interfaceClass, final ClassLoader classLoader) {
        final List<T> list = new ArrayList<T>();

        for (final Iterator<T> providers = providers(interfaceClass, classLoader); providers != null && providers.hasNext();) {
            list.add(providers.next());
        }

        return list;
    }

    // TODO: need to find a way to call ClassDiscoveryImpl... Class.forName() is invoked internally and that is a no-no
    private static <T> Iterator<T> providers(final Class<T> interfaceClass, final ClassLoader classLoader) {
        try {

            // Java 6+
            return ServiceLoader.load(interfaceClass, classLoader).iterator();
        } catch (final NoClassDefFoundError e) {

            // Java 6-
            return Java5ServiceLocator.providers(interfaceClass, classLoader);
        }
    }

    // isolates dependence on sun.misc package
    private static class Java5ServiceLocator {

        @SuppressWarnings("unchecked")
        public static <T> Iterator<T> providers(final Class<T> interfaceClass, final ClassLoader classLoader) throws ServiceConfigurationError {
            return (Iterator<T>) Service.providers(interfaceClass, classLoader);
        }
    }
}

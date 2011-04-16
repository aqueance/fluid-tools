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

import sun.misc.Service;
import sun.misc.ServiceConfigurationError;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Wraps the Sun JDK service provider discovery implementation, which was private API prior to Java 6. This class is used internally before the more
 * convenient service provider mechanism is available. Use the <code>@ServiceProvider</code> annotation instead of this low level utility and the
 * <code>ClassDiscovery<code> component to make your tasks in dealing with service providers much, much simpler.
 *
 * @author Tibor Varga
 */
public final class ServiceProviders {

    private ServiceProviders() {
        throw new UnsupportedOperationException("No instance allowed");
    }

    /**
     * Returns the first service provider implementation for the given interface.
     *
     * @param interfaceClass the service provider interface.
     * @param classLoader    the class loader to look for implementations in.
     * @param <T>            the service provider interface
     * @return the first implementation of the given interface or <code>null</code> if none found.
     */
    public static <T> T findInstance(final Class<T> interfaceClass, final ClassLoader classLoader) {
        final Iterator<T> providers = providers(interfaceClass, classLoader);
        return providers.hasNext() ? providers.next() : null;
    }

    private static <T> Iterator<T> providers(final Class<T> interfaceClass, final ClassLoader classLoader) {
        try {

            // Java 6+
            return ServiceLoader.load(interfaceClass, classLoader).iterator();
        } catch (final NoClassDefFoundError e) {

            // Java 5-
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

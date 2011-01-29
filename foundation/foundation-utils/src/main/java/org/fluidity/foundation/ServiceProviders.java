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

import java.util.Iterator;
import java.util.ServiceLoader;

import sun.misc.Service;
import sun.misc.ServiceConfigurationError;

/**
 * Wraps the Sun JDK service provider discovery implementation, which was private API prior to Java 6. This class is used internally before the more convenient
 * service provider mechanism is available. Use the <code>@ServiceProvider</code> annotation instead of this low level utility to make your tasks in dealing
 * with service providers much, much simpler.
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
     *
     * @return the first implementation of the given interface or <code>null</code> if none found.
     */
    public static <T> T findInstance(final Class<T> interfaceClass, final ClassLoader classLoader) {
        for (final Iterator<T> providers = providers(interfaceClass, classLoader); providers.hasNext(); ) {
            try {
                return providers.next();
            } catch (final ServiceConfigurationError e) {
                System.err.printf("Finding service providers for %s using %s", interfaceClass, classLoader);
                e.printStackTrace(System.err);
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> Iterator<T> providers(final Class<T> interfaceClass, final ClassLoader classLoader) {
        try {

            // Java 6+
            return ServiceLoader.load(interfaceClass, classLoader).iterator();
        } catch (final NoClassDefFoundError e) {

            // Java 5-
            return (Iterator<T>) Service.providers(interfaceClass, classLoader);
        }
    }
}

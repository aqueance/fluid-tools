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

package org.fluidity.composition.spi;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.OpenComponentContainer;

/**
 * Implements basic method relationships and functionality useful for container and registry implementations.
 *
 * @author Tibor Varga
 */
public abstract class EmptyComponentContainer implements ComponentContainer {

    /**
     * Implements this convenience method using the primary methods of the container.
     * <p/>
     * {@inheritDoc}
     */
    public <T> T getComponent(final Class<T> componentInterface, final Bindings bindings) throws ResolutionException {
        final OpenComponentContainer child = makeChildContainer();
        bindings.bindComponents(child.getRegistry());
        return child.getComponent(componentInterface);
    }

    /**
     * Implements basic method relationships and useful functionality to container and registry implementations.
     */
    public static abstract class EmptyRegistry implements Registry {

        /**
         * Finds the possible single API that the given component class should be bound against.
         *
         * @param implementation the component class.
         *
         * @return a class object, never <code>null</code>. When no suitable interface is found, the implementation class itself is returned.
         */
        protected final Class<?> componentInterface(final Class<?> implementation) {
            final Component component = implementation.getAnnotation(Component.class);
            final boolean fallback = component != null && !component.primary();

            if (fallback) {
                return implementation;
            }

            final Class<?> api = component == null ? Object.class : component.api();

            assert api != null;
            if (api != Object.class) {
                return api;
            }

            final Class<?> single = findSingleInterface(implementation);
            return single == null ? implementation : single;
        }

        private Class<?> findSingleInterface(final Class<?> implementation) {
            final Class[] interfaces = implementation.getInterfaces();

            if (interfaces.length == 1) {
                return interfaces[0];
            } else if (interfaces.length == 0) {
                final Class<?> ancestor = implementation.getSuperclass();

                if (ancestor != Object.class) {
                    return findSingleInterface(ancestor);
                }
            }

            return null;
        }
    }
}

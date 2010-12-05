/*
 * Copyright (c) 2006-2010 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.composition;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.fluidity.composition.spi.ComponentCache;
import org.fluidity.composition.spi.ReferenceChain;
import org.fluidity.foundation.Reflection;

/**
 * Common functionality for component producers.
 *
 * @author Tibor Varga
 */
abstract class AbstractProducer implements ComponentProducer {

    protected final ComponentCache cache;

    protected AbstractProducer(final ComponentCache cache) {
        this.cache = cache;
    }

    /**
     * Returns the command to use to create an instance. Called only when no circular references were detected.
     *
     * @param container the container containing this producer.
     *
     * @return a command or <code>null</code> if no instance should be created.
     */
    protected ComponentCache.Command createCommand(final SimpleContainer container) {
        return null;
    }

    /**
     * Checks for circular references and, if possible, wraps the first component that is referenced by interface back the reference chain with a proxy that
     * defers the instantiation of that component. Uses the {@link #createCommand(SimpleContainer)} method to perform actual component creation along with the
     * producer's {@link org.fluidity.composition.spi.ComponentCache} to cache instances.
     */
    public Object create(final SimpleContainer container, final boolean circular) {
        final Class<?> componentClass = componentClass();
        final ComponentCache.Command create = createCommand(container);
        final ReferenceChain resolutions = container.referenceChain();

        if (create == null) {
            return null;
        } else if (circular) {
            return deferredCreate(container, componentClass, create, resolutions, null);
        } else {
            try {
                return cache.lookup(container, componentInterface(), componentClass, create);
            } catch (final ComponentContainer.CircularReferencesException e) {

                // handle circular reference that was noticed later in the reference chain that could not be handled at that point
                return deferredCreate(container, componentClass, create, resolutions, e);
            }
        }
    }

    private Object deferredCreate(final SimpleContainer container,
                                  final Class<?> componentClass,
                                  final ComponentCache.Command create,
                                  ReferenceChain resolutions,
                                  ComponentContainer.CircularReferencesException error) {
        final Class<?> reference = resolutions.lastReference();

        if (reference.isInterface()) {
            return Proxy.newProxyInstance(componentClass.getClassLoader(), new Class<?>[] { reference }, new InvocationHandler() {
                private Object delegate;

                public Object invoke(final Object proxy, final Method method, final Object[] arguments) throws Throwable {
                    synchronized (this) {
                        if (delegate == null) {
                            delegate = cache.lookup(container, componentInterface(), componentClass, create);
                        }
                    }

                    final boolean accessible = Reflection.isAccessible(method);

                    if (!accessible) {
                        method.setAccessible(true);
                    }

                    try {
                        return method.invoke(delegate, arguments);
                    } finally {
                        if (!accessible) {
                            method.setAccessible(accessible);
                        }
                    }
                }
            });
        } else {
            throw error == null ? new ComponentContainer.CircularReferencesException(componentClass, resolutions.print()) : error;
        }
    }

    public boolean isVariantMapping() {
        return false;
    }

    public boolean isInstanceMapping() {
        return false;
    }

    public Class<?> factoryClass() {
        return null;
    }

    @Override
    public String toString() {
        return String.format(" %s (%s)", componentClass(), componentInterface());
    }
}

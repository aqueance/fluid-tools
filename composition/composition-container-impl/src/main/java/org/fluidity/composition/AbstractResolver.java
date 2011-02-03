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

package org.fluidity.composition;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.fluidity.foundation.logging.Log;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Common functionality for component resolvers.
 *
 * @author Tibor Varga
 */
abstract class AbstractResolver implements ComponentResolver {

    private final int priority;
    protected final Class<?> api;
    protected final ReferenceChain references;
    protected final ComponentCache cache;
    protected final Log log;

    protected AbstractResolver(final int priority, final Class<?> api, final ReferenceChain references, final ComponentCache cache, final LogFactory logs) {
        this.priority = priority;
        this.api = api;
        this.references = references;
        this.cache = cache;
        this.log = logs.createLog(getClass());
    }

    /**
     * Returns the command to use to create an instance. Called only when no circular references were detected.
     *
     * @param container the container containing this resolver.
     * @param api       the API the component is requested for.
     *
     * @return a command or <code>null</code> if no instance should be created.
     */
    protected ComponentCache.Command createCommand(final SimpleContainer container, final Class<?> api) {
        return null;
    }

    /**
     * Checks for circular references and, if possible, wraps the first component up the reference chain that is referenced by interface with a proxy that
     * defers the instantiation of that component. Uses the {@link #createCommand(SimpleContainer, Class)} method to perform actual component creation along
     * with the resolver's {@link ComponentCache} to cache instances.
     */
    public Object create(final SimpleContainer container, final Class<?> api, final boolean circular) {
        final ComponentCache.Command create = createCommand(container, api);

        if (create == null) {
            return null;
        } else if (circular) {
            return deferredCreate(container, api, create, null);
        } else {
            try {
                return cache.lookup(container, api, this, create);
            } catch (final ComponentContainer.CircularReferencesException e) {

                // handle circular reference that was noticed later in the reference chain that could not be handled at that point
                return deferredCreate(container, api, create, e);
            }
        }
    }

    private Object deferredCreate(final SimpleContainer container,
                                  final Class<?> api,
                                  final ComponentCache.Command create,
                                  final ComponentContainer.CircularReferencesException error) {
        final Class<?> reference = references.lastLink().reference();

        if (reference.isInterface()) {
            log.info("%s: deferred creation of %s component", container, reference.getName());
            return Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[] { reference }, new InvocationHandler() {
                private Object delegate;

                public Object invoke(final Object proxy, final Method method, final Object[] arguments) throws Throwable {
                    synchronized (this) {
                        if (delegate == null) {
                            delegate = cache.lookup(container, api, AbstractResolver.this, create);
                        }
                    }

                    method.setAccessible(true);
                    return method.invoke(delegate, arguments);
                }
            });
        } else {
            throw error == null ? new ComponentContainer.CircularReferencesException(reference, references.print()) : error;
        }
    }

    public int priority() {
        return priority;
    }

    public boolean replaces(final ComponentResolver resolver) {
        final int check = resolver.priority();

        if (check == priority) {
            throw new ComponentContainer.BindingException("Component %s already bound", api);
        } else {
            return check < priority;
        }
    }

    public void resolverReplaced(final Class<?> api, final ComponentResolver previous, final ComponentResolver replacement) {
        // empty
    }

    public boolean isFactoryMapping() {
        return false;
    }

    public boolean isVariantMapping() {
        return false;
    }

    public boolean isInstanceMapping() {
        return false;
    }

    @Override
    public String toString() {
        return String.format(" %s (%s)", getClass().getSimpleName(), api);
    }
}

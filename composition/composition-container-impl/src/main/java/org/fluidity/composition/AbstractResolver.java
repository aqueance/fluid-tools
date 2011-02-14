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
    protected final Log log;
    protected final Class<?> api;
    protected final ComponentCache cache;

    protected AbstractResolver(final int priority, final Class<?> api, final ComponentCache cache, final LogFactory logs) {
        this.priority = priority;
        this.api = api;
        this.cache = cache;
        this.log = logs.createLog(getClass());
    }

    /**
     * Returns the command to use to get a component instance. Called only when no circular references were detected.
     *
     * @param container the container containing this resolver.
     * @param api       the API the component is requested for.
     *
     * @return a command or <code>null</code> if no instance should be created.
     */
    protected ComponentCache.Instantiation createCommand(final SimpleContainer container, final Class<?> api) {
        return null;
    }

    /**
     * Checks for circular references and, if possible, wraps the first component up the reference chain that is referenced by interface with a proxy that
     * defers the instantiation of that component. Uses the {@link #createCommand(SimpleContainer, Class)} method to perform actual component creation along
     * with the resolver's {@link ComponentCache} to cache instances.
     */
    public Object getComponent(final DependencyChain.Lineage lineage, final ContextDefinition context, final SimpleContainer container, final Class<?> api) {
        final ComponentCache.Instantiation create = createCommand(container, api);

        if (create == null) {
            return null;
        } else if (lineage.isCircular()) {
            return deferredCreate(lineage, context.copy(), container, api, create, null);
        } else {
            try {
                return cache.lookup(container, context, api, create);
            } catch (final ComponentContainer.CircularReferencesException e) {

                // handle circular lineage that was noticed later in the reference chain that could not be handled at that point
                return deferredCreate(lineage, context.copy(), container, api, create, e);
            }
        }
    }

    private Object deferredCreate(final DependencyChain.Lineage lineage,
                                  final ContextDefinition context,
                                  final SimpleContainer container,
                                  final Class<?> api,
                                  final ComponentCache.Instantiation command,
                                  final ComponentContainer.CircularReferencesException error) {
        if (api.isInterface()) {
            log.info("%s: deferred creation of %s component", container, api.getName());
            return Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[] { api }, new InvocationHandler() {
                private Object delegate;

                public Object invoke(final Object proxy, final Method method, final Object[] arguments) throws Throwable {
                    synchronized (this) {
                        if (delegate == null) {
                            final DependencyChain dependencyChain = container.services().dependencyChain();

                            delegate = dependencyChain.follow(context, AbstractResolver.this, new DependencyChain.Command<Object>() {
                                public Object run(final DependencyChain.Lineage newLineage, final ContextDefinition context) {
                                    if (newLineage.isCircular()) {
                                        throw error == null ? new ComponentContainer.CircularReferencesException(api, lineage.toString()) : error;
                                    } else {
                                        return cache.lookup(container, context, api, command);
                                    }
                                }
                            });
                        }
                    }

                    method.setAccessible(true);
                    return method.invoke(delegate, arguments);
                }
            });
        } else {
            throw error == null ? new ComponentContainer.CircularReferencesException(api, lineage.toString()) : error;
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

    public boolean isGroupMapping() {
        return false;
    }
}

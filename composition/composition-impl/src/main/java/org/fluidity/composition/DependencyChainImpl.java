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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.fluidity.composition.spi.ComponentMapping;

/**
 * @author Tibor Varga
 */
final class DependencyChainImpl implements DependencyChain {

    /*
     * When component instantiation is deferred in order to break circular dependence, the deferred resolver must be able to use the reference chain prevalent
     * at its invocation. Without a thread local variable to hold that prevalent chain, the deferred resolver could either use the chain prevalent at its
     * creation or a new empty chain, both of which are wrong in one case or the other.
     *
     * Thus, we need a thread local variable to hold the prevalent reference chain.
     */
    private final ThreadLocal<Chain> prevalent = new InheritableThreadLocal<Chain>() {
        @Override
        protected Chain initialValue() {
            return new Chain();
        }
    };

    public Object follow(final Class<?> api, final ContextDefinition context, final ComponentMapping mapping, final Command command) {
        final Chain lastChain = prevalent.get();
        final Chain newChain = lastChain.descend(mapping);
        final ContextDefinition newContext = context == null ? new ContextDefinitionImpl() : context;

        prevalent.set(newChain);
        try {
            final boolean deferred = command instanceof DeferredCommand;

            if (newChain.isCircular() && !deferred) {
                return deferredCreate(api, newChain, newContext, mapping, command, null);
            } else {
                try {
                    return command.run(newChain, newContext);
                } catch (final ComponentContainer.CircularReferencesException error) {
                    if (deferred) {
                        throw error;
                    } else {
                        return deferredCreate(api, newChain, newContext, mapping, command, error);
                    }
                }
            }
        } finally {
            prevalent.set(lastChain);
        }
    }

    @SuppressWarnings("unchecked")
    private Object deferredCreate(final Class<?> api,
                                 final Lineage lineage,
                                 final ContextDefinition context,
                                 final ComponentMapping mapping,
                                 final Command command,
                                 final ComponentContainer.CircularReferencesException error) {
        if (api.isInterface()) {
            return Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[] { api }, new InvocationHandler() {
                private volatile Object delegate;

                public Object invoke(final Object proxy, final Method method, final Object[] arguments) throws Throwable {
                    Object cache = delegate;

                    if (delegate == null) {
                        synchronized (this) {
                            cache = delegate;

                            if (cache == null) {
                                delegate = cache = follow(api, context, mapping, new DeferredCommand(api, command, error));
                            }
                        }
                    }

                    return method.invoke(cache, arguments);
                }
            });
        } else {
            throw error == null ? new ComponentContainer.CircularReferencesException(api, lineage.toString()) : error;
        }
    }

    private static class DeferredCommand implements Command {

        private final Class<?> api;
        private final Command command;
        private final ComponentContainer.CircularReferencesException error;

        public DeferredCommand(final Class<?> api, final Command command, final ComponentContainer.CircularReferencesException error) {
            this.api = api;
            this.error = error;
            this.command = command;
        }

        public Object run(final Lineage lineage, final ContextDefinition context) {
            if (lineage.isCircular()) {
                throw error == null ? new ComponentContainer.CircularReferencesException(api, lineage.toString()) : error;
            } else {
                return command.run(lineage, context);
            }
        }
    }

    private class Chain implements Lineage {

        private final Set<ComponentMapping> loop = new LinkedHashSet<ComponentMapping>();
        private final ComponentMapping mapping;
        private final boolean circular;

        private Chain() {
            this.circular = false;
            this.mapping = null;
        }

        public Chain(final Set<ComponentMapping> loop, final ComponentMapping mapping) {
            this.loop.addAll(loop);
            this.circular = !this.loop.add(mapping);
            this.mapping = mapping;
        }

        public Chain descend(final ComponentMapping mapping) {
            return new Chain(loop, mapping);
        }

        public boolean isCircular() {
            return circular;
        }

        @Override
        public String toString() {
            if (circular) {
                final List<ComponentMapping> list = new ArrayList<ComponentMapping>(loop.size() + 1);
                list.addAll(loop);
                list.add(mapping);
                return list.toString();
            } else {
                return loop.toString();
            }
        }
    }
}

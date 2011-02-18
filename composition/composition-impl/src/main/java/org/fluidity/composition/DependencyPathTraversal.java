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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.network.ContextDefinition;
import org.fluidity.composition.network.Graph;

/**
* TODO: documentation
*/
final class DependencyPathTraversal implements Graph.Traversal {

    private final AtomicReference<Loop> path = new AtomicReference<Loop>(new Loop());
    private final Strategy strategy;
    private final Observer observer;

    public DependencyPathTraversal(final Strategy strategy, final Observer observer) {
        this.strategy = strategy;
        this.observer = observer;
    }

    public Graph.Node follow(final Graph graph, final ContextDefinition context, final boolean explore, final Graph.Reference reference) {
        final Class<?> api = reference.api();

        final ContextDefinition newContext = context == null ? new ContextDefinitionImpl() : context;

        final Loop lastPath = path.get();
        final Loop newPath = lastPath.descend(api, newContext);

        path.set(newPath);
        try {
            final boolean circular = newPath.isCircular();

            final Trail trail = new Trail() {
                public Class<?> head() {
                    return newPath.head();
                }

                public List<Class<?>> path() {
                    return newPath.path();
                }

                public Graph.Node advance() {
                    if (circular) {
                        return deferredCreate(graph, reference, newContext, newPath, null);
                    } else {
                        try {
                            return reference.resolve(DependencyPathTraversal.this, newContext, explore);
                        } catch (final ComponentContainer.CircularReferencesException error) {
                            return deferredCreate(graph, reference, newContext, newPath, error);
                        }
                    }
                }
            };

            final Graph.Node replacement = strategy.resolve(circular, graph, this, trail);
            final Graph.Node node = replacement == null ? trail.advance() : replacement;

            return new Graph.Node() {
                public Class<?> type() {
                    return node.type();
                }

                public Object instance() {
                    final Loop lastPath = path.get();
                    final Loop newPath = lastPath.descend(api, newContext);     // TODO: is context valid when strategy returned different node?

                    path.set(newPath);
                    try {
                        final Object instance = node.instance();

                        if (instance != null && !api.isAssignableFrom(instance.getClass())) {
                            throw new ComponentContainer.ResolutionException("%s is not assignable to %s in path %s", instance.getClass(), api, newPath);
                        }

                        if (observer != null) {
                            observer.resolved(newPath, instance);
                        }

                        return instance;
                    } finally {
                        path.set(lastPath);
                    }
                }
            };
        } finally {
            path.set(lastPath);
        }
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public Graph.Node deferredCreate(final Graph graph,
                                     final Graph.Reference reference,
                                     final ContextDefinition context,
                                     final Loop path,
                                     final ComponentContainer.CircularReferencesException error) {
        final Class<?> api = reference.api();

        if (reference instanceof DeferredReference) {
            throw circularity(api, path, ((DeferredReference) reference).error());
        } else if (api.isInterface()) {
            final Object instance = Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[] { api }, new InvocationHandler() {
                private volatile Object delegate;
                private Set<Method> path = new HashSet<Method>();

                public Object invoke(final Object proxy, final Method method, final Object[] arguments) throws Throwable {
                    Object cache = delegate;

                    if (delegate == null) {
                        synchronized (this) {
                            cache = delegate;

                            if (cache == null) {
                                delegate = cache = follow(graph, context, false, new DeferredReference(reference, error)).instance();
                            }
                        }
                    }

                    if (!path.add(method)) {
                        throw new ComponentContainer.CircularInvocationException(delegate, method);
                    } else {
                        try {
                            return method.invoke(cache, arguments);
                        } finally {
                            path.remove(method);
                        }
                    }
                }
            });

            return new Graph.Node() {
                public Class<?> type() {
                    return instance.getClass();
                }

                public Object instance() {
                    return instance;
                }
            };
        } else {
            throw circularity(api, path, error);
        }
    }

    public static ComponentContainer.CircularReferencesException circularity(final Class<?> api,
                                                                             final Loop path,
                                                                             final ComponentContainer.CircularReferencesException error) {
        return error == null ? new ComponentContainer.CircularReferencesException(api, path.toString()) : error;
    }

    private static class DeferredReference implements Graph.Reference {

        private final Graph.Reference reference;
        private final ComponentContainer.CircularReferencesException error;

        public DeferredReference(final Graph.Reference reference, final ComponentContainer.CircularReferencesException error) {
            this.error = error;
            this.reference = reference;
        }

        public Class<?> api() {
            return reference.api();
        }

        public Graph.Node resolve(final Graph.Traversal traversal, final ContextDefinition context, final boolean explore) {
            return reference.resolve(traversal, context, explore);
        }

        public ComponentContainer.CircularReferencesException error() {
            return error;
        }
    }

    /**
     * Represents the path of references to the particular dependency where the object appears.
     */
    private static class Loop implements Graph.Path {

        private final Set<Loop.Element> path = new LinkedHashSet<Loop.Element>();
        private final Loop.Element last;
        private final boolean circular;

        private Loop() {
            this.circular = false;
            this.last = null;
        }

        public Loop(final Set<Loop.Element> path, final Loop.Element last) {
            this.path.addAll(path);
            this.circular = !this.path.add(last);
            this.last = last;
        }

        public Loop descend(final Class<?> api, final ContextDefinition context) {
            return new Loop(path, new Loop.Element(api, context));
        }

        /**
         * Tells if the path of reference up to and including the dependency resolver receiving this object that it is being invoked the second time during
         * one
         * dependency resolution cycle; i.e., there is circular reference in the dependency path.
         *
         * @return <code>true</code> if the dependency resolver adds circular dependency to the dependency path, <code>false</code> otherwise.
         */
        public boolean isCircular() {
            return circular;
        }

        public Class<?> head() {
            return last.api;
        }

        public List<Class<?>> path() {
            final List<Class<?>> list = new ArrayList<Class<?>>(path.size() + (circular ? 1 : 0));
            for (final Loop.Element element : path) {
                list.add(element.api);
            }

            if (circular) {
                list.add(last.api);
            }

            return list;
        }

        @Override
        public String toString() {
            return path().toString();
        }

        static class Element {

            public final Class<?> api;
            public final ContextDefinition context;

            Element(final Class<?> api, final ContextDefinition context) {
                this.api = api;
                this.context = context.copy().reduce(null);
            }

            @Override
            public boolean equals(final Object o) {
                if (this == o) {
                    return true;
                } else if (o == null || getClass() != o.getClass()) {
                    return false;
                }

                final Loop.Element element = (Loop.Element) o;

                return api.equals(element.api) && context.equals(element.context);
            }

            @Override
            public int hashCode() {
                int result = api.hashCode();
                result = 31 * result + context.hashCode();
                return result;
            }
        }
    }
}

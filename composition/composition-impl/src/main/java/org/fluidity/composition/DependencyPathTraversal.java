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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fluidity.composition.network.ContextDefinition;
import org.fluidity.composition.network.Graph;

/**
 * TODO: documentation
 *
 * This object keeps state, do not use it concurrently or if its API method terminates abnormally (i.e., throws an exception).
 */
final class DependencyPathTraversal implements Graph.Traversal {

    private final Strategy strategy;

    private DependencyPath<ResolutionElement> explorePath = new DependencyPath<ResolutionElement>(false);
    private DependencyPath<InstantiationElement> createPath = new DependencyPath<InstantiationElement>(true);

    public DependencyPathTraversal(final Strategy strategy) {
        this.strategy = strategy;
    }

    public Graph.Node follow(final Graph graph, final ContextDefinition context, final Graph.Reference reference) {
        final Class<?> api = reference.api();

        final ContextDefinition newContext = context == null ? new ContextDefinitionImpl() : context;

        final DependencyPath<ResolutionElement> lastPath = explorePath;
        final DependencyPath<ResolutionElement> newPath = lastPath.descend(new ResolutionElement(api, newContext));

        explorePath = newPath;
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
                        return deferredResolution(graph, reference, newContext, newPath, null);
                    } else {
                        try {
                            final Graph.Node node = reference.resolve(DependencyPathTraversal.this, newContext);
                            newPath.last().context = newContext.create();
                            return node;
                        } catch (final ComponentContainer.CircularReferencesException error) {
                            return deferredResolution(graph, reference, newContext, newPath, error);
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

                public Object instance(final Observer observer) {
                    final DependencyPath<InstantiationElement> lastPath = createPath;
                    final DependencyPath<InstantiationElement> newPath = lastPath.descend(new InstantiationElement(api, node.context()));

                    createPath = newPath;
                    try {
                        Object instance = node.instance(observer);

                        if (instance != null) {
                            if (!api.isAssignableFrom(instance.getClass())) {
                                throw new ComponentContainer.ResolutionException("%s is not assignable to %s in path %s", instance.getClass(), api, newPath);
                            }

                            if (newPath.last().hasInstance()) {

                                // we've been cut short
                                instance = newPath.last().instance();
                            } else {
                                if (newPath.isCircular()) {

                                    // cut short the instantiation chain
                                    newPath.last().setInstance(instance);
                                }

                                if (observer != null) {
                                    observer.resolved(newPath, instance);
                                }
                            }
                        } else if (observer != null) {
                            observer.resolved(newPath, instance);
                        }

                        return instance;
                    } finally {
                        createPath = lastPath;
                    }
                }

                public ComponentContext context() {
                    return node.context();
                }
            };
        } finally {
            explorePath = lastPath;
        }
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public Graph.Node deferredResolution(final Graph graph,
                                         final Graph.Reference reference,
                                         final ContextDefinition context,
                                         final DependencyPath<ResolutionElement> path,
                                         final ComponentContainer.CircularReferencesException error) {
        final Class<?> api = reference.api();

        if (reference instanceof DeferredReference) {
            throw circularity(api, path, ((DeferredReference) reference).error());
        } else if (api.isInterface()) {
            final Class<?>[] interfaces = { api };
            final Object instance = Proxy.newProxyInstance(api.getClassLoader(), interfaces, new InvocationHandler() {
                private volatile Object delegate;
                private Set<Method> path = new HashSet<Method>();

                public Object invoke(final Object proxy, final Method method, final Object[] arguments) throws Throwable {
                    Object cache = delegate;

                    if (delegate == null) {
                        synchronized (this) {
                            cache = delegate;

                            if (cache == null) {
                                delegate = cache = follow(graph, context, new DeferredReference(reference, error)).instance(null);
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

                public Object instance(final Observer observer) {
                    return instance;
                }

                public ComponentContext context() {
                    return path.last().context;
                }
            };
        } else {
            throw circularity(api, path, error);
        }
    }

    public static ComponentContainer.CircularReferencesException circularity(final Class<?> api,
                                                                             final DependencyPath<ResolutionElement> path,
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

        public Graph.Node resolve(final Graph.Traversal traversal, final ContextDefinition context) {
            return reference.resolve(traversal, context);
        }

        public ComponentContainer.CircularReferencesException error() {
            return error;
        }
    }

    /**
     * Maintains a potentially circular dependency path.
     */
    private static class DependencyPath<T extends Element> implements Graph.Path {

        private final Map<T, T> path = new LinkedHashMap<T, T>();
        private final T last;
        private final boolean collapse;
        private final boolean circular;

        private DependencyPath(final boolean collapse) {
            this.collapse = collapse;
            this.circular = false;
            this.last = null;
        }

        public DependencyPath(final Map<T, T> path, final T last, final boolean collapse) {
            this.collapse = collapse;
            this.path.putAll(path);
            this.circular = path.containsKey(last);

            if (!this.circular) {
                this.path.put(last, last);
            }

            this.last = this.path.get(last);
            assert this.last != null : this.path.keySet();
        }

        public DependencyPath<T> descend(final T element) {
            return new DependencyPath<T>(path, element, collapse);
        }

        /**
         * Tells if the path of reference up to and including the dependency resolver receiving this object that it is being invoked the second time during
         * one dependency resolution cycle; i.e., there is circular reference in the dependency path.
         *
         * @return <code>true</code> if the dependency resolver adds circular dependency to the dependency path, <code>false</code> otherwise.
         */
        public boolean isCircular() {
            return circular;
        }

        public Class<?> head() {
            return last.api();
        }

        public List<Class<?>> path() {
            final List<Class<?>> list = new ArrayList<Class<?>>(path.size() + (circular ? 1 : 0));
            for (final T element : path.keySet()) {
                if (collapse && circular && last == element) {
                    break;
                } else {
                    list.add(element.api());
                }
            }

            if (circular) {
                list.add(last.api());
            }

            return list;
        }

        public T last() {
            return last;
        }

        @Override
        public String toString() {
            return path().toString();
        }
    }

    private static abstract class Element {

        private final Class<?> api;

        private Element(final Class<?> api) {
            this.api = api;
        }

        public final Class<?> api() {
            return api;
        }

        protected abstract Object context();

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            } else if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Element element = (Element) o;
            return api.equals(element.api()) && context().equals(element.context());
        }

        @Override
        public int hashCode() {
            int result = api.hashCode();
            result = 31 * result + context().hashCode();
            return result;
        }
    }

    /**
     * Defines circularity in terms of API and provided context.
     */
    private static class ResolutionElement extends Element {

        public final ContextDefinition definition;

        public ComponentContext context;

        ResolutionElement(final Class<?> api, final ContextDefinition definition) {
            super(api);
            this.definition = definition.copy();
        }

        @Override
        protected Object context() {
            return definition;
        }
    }

    /**
     * Defines circularity in terms of API and consumed context, which may be detected sooner than when relying only on provided context.
     */
    private static class InstantiationElement extends Element {

        public final ComponentContext context;

        private boolean cached;
        private Object cache;

        InstantiationElement(final Class<?> api, final ComponentContext context) {
            super(api);
            this.context = context;
        }

        @Override
        protected Object context() {
            return context;
        }

        public Object instance() {
            return cache;
        }

        public boolean hasInstance() {
            return cached;
        }

        public void setInstance(final Object instance) {
            cached = true;
            cache = instance;
        }
    }
}

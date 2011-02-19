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
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.network.ContextDefinition;
import org.fluidity.composition.network.Graph;

/**
 * TODO: documentation
 */
final class DependencyPathTraversal implements Graph.Traversal {

    private final AtomicReference<ExploreLoop> explorePath = new AtomicReference<ExploreLoop>(new ExploreLoop());
    private final AtomicReference<CreateLoop> createPath = new AtomicReference<CreateLoop>(new CreateLoop());
    private final Strategy strategy;

    public DependencyPathTraversal(final Strategy strategy) {
        this.strategy = strategy;
    }

    public Graph.Node follow(final Graph graph, final ContextDefinition context, final Graph.Reference reference) {
        final Class<?> api = reference.api();

        final ContextDefinition newContext = context == null ? new ContextDefinitionImpl() : context;

        final ExploreLoop lastPath = explorePath.get();
        final ExploreLoop newPath = lastPath.descend(api, newContext);

        explorePath.set(newPath);
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
                        return deferredResolve(graph, reference, newContext, newPath, null);
                    } else {
                        try {
                            final Graph.Node node = reference.resolve(DependencyPathTraversal.this, newContext);
                            newPath.setContext(newContext.create());
                            return node;
                        } catch (final ComponentContainer.CircularReferencesException error) {
                            return deferredResolve(graph, reference, newContext, newPath, error);
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
                    final CreateLoop lastPath = createPath.get();
                    final CreateLoop newPath = lastPath.descend(api, node.context());

                    createPath.set(newPath);
                    try {
                        Object instance = node.instance(observer);

                        if (instance != null) {
                            if (!api.isAssignableFrom(instance.getClass())) {
                                throw new ComponentContainer.ResolutionException("%s is not assignable to %s in path %s", instance.getClass(), api, newPath);
                            }

                            if (newPath.hasInstance()) {
                                instance = newPath.instance();
                            } else {
                                if (newPath.isCircular()) {
                                    newPath.setInstance(instance);
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
                        createPath.set(lastPath);
                    }
                }

                public ComponentContext context() {
                    return node.context();
                }
            };
        } finally {
            explorePath.set(lastPath);
        }
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public Graph.Node deferredResolve(final Graph graph,
                                      final Graph.Reference reference,
                                      final ContextDefinition context,
                                      final ExploreLoop path,
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
                    return path.context();
                }
            };
        } else {
            throw circularity(api, path, error);
        }
    }

    public static ComponentContainer.CircularReferencesException circularity(final Class<?> api,
                                                                             final ExploreLoop path,
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
     * Represents the path of references to the particular dependency where the object appears.
     */
    private static class ExploreLoop implements Graph.Path {

        private final Map<Element, Element> path = new LinkedHashMap<Element, Element>();
        private final ExploreLoop.Element last;
        private final boolean circular;

        private ExploreLoop() {
            this.circular = false;
            this.last = null;
        }

        public ExploreLoop(final Map<Element, Element> path, final ExploreLoop.Element last) {
            this.path.putAll(path);
            this.circular = path.containsKey(last);

            if (!this.circular) {
                this.path.put(last, last);
            }

            this.last = this.path.get(last);
            assert this.last != null : this.path.keySet();
        }

        public ExploreLoop descend(final Class<?> api, final ContextDefinition context) {
            return new ExploreLoop(path, new ExploreLoop.Element(api, context));
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
            for (final ExploreLoop.Element element : path.keySet()) {
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

        public void setContext(final ComponentContext context) {
            last.context = context;
        }

        public ComponentContext context() {
            return last.context;
        }

        static class Element {

            public final Class<?> api;
            public final ContextDefinition definition;
            public ComponentContext context;

            Element(final Class<?> api, final ContextDefinition definition) {
                this.api = api;
                this.definition = definition.copy();
            }

            @Override
            public boolean equals(final Object o) {
                if (this == o) {
                    return true;
                } else if (o == null || getClass() != o.getClass()) {
                    return false;
                }

                final ExploreLoop.Element element = (ExploreLoop.Element) o;

                return api.equals(element.api) && definition.equals(element.definition);
            }

            @Override
            public int hashCode() {
                int result = api.hashCode();
                result = 31 * result + definition.hashCode();
                return result;
            }
        }
    }

    /**
     * Represents the path of references to the particular dependency where the object appears.
     */
    private static class CreateLoop implements Graph.Path {

        private final Map<Element, Element> path = new LinkedHashMap<Element, Element>();
        private final CreateLoop.Element last;
        private final boolean circular;

        private CreateLoop() {
            this.circular = false;
            this.last = null;
        }

        public CreateLoop(final Map<Element, Element> path, final Element last) {
            this.path.putAll(path);
            this.circular = path.containsKey(last);

            if (!this.circular) {
                this.path.put(last, last);
            }

            this.last = this.path.get(last);
            assert this.last != null : this.path.keySet();
        }

        public CreateLoop descend(final Class<?> api, final ComponentContext context) {
            return new CreateLoop(path, new Element(api, context));
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
            for (final Element element : path.keySet()) {
                if (circular && last == element) {
                    break;
                } else {
                    list.add(element.api);
                }
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

        public boolean hasInstance() {
            return last.hasInstance();
        }

        public Object instance() {
            return last.instance();
        }

        public void setInstance(final Object instance) {
            last.setInstance(instance);
        }

        static class Element {

            public final Class<?> api;
            public final ComponentContext context;

            private boolean cached;
            private Object cache;

            Element(final Class<?> api, final ComponentContext context) {
                this.api = api;
                this.context = context;
            }

            @Override
            public boolean equals(final Object o) {
                if (this == o) {
                    return true;
                } else if (o == null || getClass() != o.getClass()) {
                    return false;
                }

                final CreateLoop.Element element = (CreateLoop.Element) o;

                return api.equals(element.api) && context.equals(element.context);
            }

            @Override
            public int hashCode() {
                int result = api.hashCode();
                result = 31 * result + context.hashCode();
                return result;
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
}

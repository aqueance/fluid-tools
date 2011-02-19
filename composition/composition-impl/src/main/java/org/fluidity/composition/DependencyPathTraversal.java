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
 * Detects and handles circular reference when possible.
 * <p/>
 * This object keeps state, do not use it concurrently or if its API method terminates abnormally (i.e., throws an exception).
 */
final class DependencyPathTraversal implements Graph.Traversal {

    private final Strategy strategy;

    private DependencyPath<ResolutionElement> resolutionPath = new DependencyPath<ResolutionElement>(false);
    private DependencyPath<InstantiationElement> instantiationPath = new DependencyPath<InstantiationElement>(true);

    public DependencyPathTraversal(final Strategy strategy) {
        this.strategy = strategy;
    }

    public Graph.Node follow(final Graph graph, final ContextDefinition context, final Graph.Reference reference) {
        assert instantiationPath.last == null : instantiationPath;
        final Class<?> api = reference.api();

        assert context != null;

        final DependencyPath<ResolutionElement> savedPath = resolutionPath;
        final DependencyPath<ResolutionElement> currentPath = savedPath.descend(new ResolutionElement(api, context));

        resolutionPath = currentPath;
        try {
            final boolean circular = currentPath.isCircular();

            final Trail trail = new Trail() {
                public Class<?> head() {
                    return currentPath.head();
                }

                public List<Class<?>> path() {
                    return currentPath.path();
                }

                public Graph.Node advance() {
                    if (circular) {
                        return new DeferredNode(api, currentPath.toString(), currentPath.last.context, null);
                    } else {
                        try {
                            final Graph.Node node = reference.resolve(DependencyPathTraversal.this, context);

                            // context now contains actual consumed context annotations
                            currentPath.last.context = context.create();

                            return node;
                        } catch (final ComponentContainer.CircularReferencesException error) {
                            return new DeferredNode(api, currentPath.toString(), currentPath.last.context, error);
                        }
                    }
                }
            };

            final Graph.Node replacement = strategy.resolve(circular, graph, this, trail);
            final Graph.Node node = replacement == null ? trail.advance() : replacement;

            return new ResolvedNode(api, node);
        } finally {
            resolutionPath = savedPath;
        }
    }

    private class DeferredNode implements Graph.Node {

        private final Class<?> api;
        private final Class<?> type;
        private final ComponentContext context;

        private Object instance;

        public DeferredNode(final Class<?> api, final String path, final ComponentContext context, final ComponentContainer.CircularReferencesException error) {
            if (!api.isInterface()) {
                throw circularity(api, path, error);
            }

            this.api = api;
            this.type = Proxy.getProxyClass(api.getClassLoader(), api);
            this.context = context;
        }

        public Class<?> type() {
            return type;
        }

        public Object instance(final Observer observer) {
            final Class<?>[] interfaces = { api };
            return instance = Proxy.newProxyInstance(api.getClassLoader(), interfaces, new InvocationHandler() {
                private volatile Object delegate;
                private Set<Method> path = new HashSet<Method>();

                public Object invoke(final Object proxy, final Method method, final Object[] arguments) throws Throwable {
                    Object cache = delegate;

                    if (delegate == null) {
                        synchronized (this) {
                            cache = delegate;

                            if (cache == null) {
                                assert instantiationPath.last != null;
                                final Graph.Node node = instantiationPath.last.node;

                                if (node == null) {
                                    throw circularity(api, path.toString(), null);
                                }

                                delegate = cache = node.instance(observer);
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
        }

        public Object replay(final Observer observer) {
            assert instance != null;
            return instance;
        }

        public ComponentContext context() {
            return context;
        }
    }

    Object instantiate(final Class<?> api, final Graph.Node node, final Observer observer, final Instance reference) {
        final DependencyPath<InstantiationElement> savedPath = instantiationPath;
        final DependencyPath<InstantiationElement> currentPath = savedPath.descend(new InstantiationElement(api, node.context()));

        instantiationPath = currentPath;
        try {
            Object instance = reference.get();

            // set node after instantiation
            currentPath.last.node = node;

            if (instance != null) {
                if (currentPath.last.hasInstance()) {

                    // chain has been cut short
                    instance = currentPath.last.instance();
                } else {
                    if (currentPath.isCircular() && !currentPath.isEdge()) {

                        // cut short the instantiation chain
                        currentPath.last.setInstance(instance);
                    }

                    if (observer != null) {
                        observer.resolved(currentPath, instance);
                    }
                }
            } else if (observer != null) {
                observer.resolved(currentPath, instance);
            }

            return instance;
        } finally {
            instantiationPath = savedPath;
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
            throw circularity(api, path.toString(), ((DeferredReference) reference).error());
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

            return new ProxyNode(path, instance);
        } else {
            throw circularity(api, path.toString(), error);
        }
    }

    public static ComponentContainer.CircularReferencesException circularity(final Class<?> api,
                                                                             final String path,
                                                                             final ComponentContainer.CircularReferencesException error) {
        return error == null ? new ComponentContainer.CircularReferencesException(api, path) : error;
    }

    private class ResolvedNode implements Graph.Node {

        private final Class<?> api;
        private final Graph.Node node;

        public ResolvedNode(final Class<?> api, final Graph.Node node) {
            this.api = api;
            this.node = node;
        }

        public Class<?> type() {
            return node.type();
        }

        public Object instance(final Observer observer) {
            return instantiate(api, node, observer, new Instance() {
                public Object get() {
                    return node.instance(observer);
                }
            });
        }

        public Object replay(final Observer observer) {
            return instantiate(api, node, observer, new Instance() {
                public Object get() {
                    return node.replay(observer);
                }
            });
        }

        public ComponentContext context() {
            return node.context();
        }
    }

    private static class ProxyNode implements Graph.Node {

        private final DependencyPath<ResolutionElement> path;
        private final Object instance;

        public ProxyNode(final DependencyPath<ResolutionElement> path, final Object instance) {
            this.path = path;
            this.instance = instance;
        }

        public Class<?> type() {
            return instance.getClass();
        }

        public Object instance(final Observer observer) {
            return instance;
        }

        public Object replay(final Observer observer) {
            return instance;
        }

        public ComponentContext context() {
            return path.last.context;
        }
    }

    private static interface Instance {

        Object get();
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

        public final T last;

        private final Map<T, T> path = new LinkedHashMap<T, T>();
        private final boolean collapse;
        private final boolean circular;
        private final boolean edge;

        private DependencyPath(final boolean collapse) {
            this.collapse = collapse;
            this.circular = false;
            this.edge = false;
            this.last = null;
        }

        public DependencyPath(final Map<T, T> path, final T last, final boolean collapse, final boolean circular) {
            this.collapse = collapse;
            this.path.putAll(path);
            this.circular = path.containsKey(last);

            if (!this.circular) {
                this.path.put(last, last);
            }

            this.last = this.path.get(last);
            assert this.last != null : this.path.keySet();
            this.edge = !circular;
        }

        public DependencyPath<T> descend(final T element) {
            return new DependencyPath<T>(path, element, collapse, circular);
        }

        public boolean isCircular() {
            return circular;
        }

        public boolean isEdge() {
            return edge;
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
            final Object myContext = context();
            final Object otherContext = element.context();
            return api.equals(element.api()) && (myContext == null ? otherContext == null : myContext.equals(otherContext));
        }

        @Override
        public int hashCode() {
            return context() == null ? api.hashCode() : (31 * api.hashCode() + context().hashCode());
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
        public Graph.Node node;

        private boolean cached;
        private Object instance;

        InstantiationElement(final Class<?> api, final ComponentContext context) {
            super(api);
            this.context = context;
        }

        @Override
        protected Object context() {
            return context;
        }

        public Object instance() {
            return instance;
        }

        public boolean hasInstance() {
            return cached;
        }

        public void setInstance(final Object instance) {
            cached = true;
            this.instance = instance;
        }
    }
}

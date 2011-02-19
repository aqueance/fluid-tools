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
            final boolean circular = currentPath.circular;

            final Trail trail = new Trail() {
                public Class<?> head() {
                    return currentPath.head();
                }

                public List<Class<?>> path() {
                    return currentPath.path();
                }

                public Graph.Node advance() {
                    if (circular) {
                        return new DeferredNode(api, currentPath.toString(), currentPath.last, null);
                    } else {
                        try {
                            final Graph.Node node = reference.resolve(DependencyPathTraversal.this, context);

                            // context now contains actual consumed context annotations
                            currentPath.last.context = context.create();

                            return node;
                        } catch (final ComponentContainer.CircularReferencesException error) {
                            return new DeferredNode(api, currentPath.toString(), currentPath.last, error);
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
        private final Element last;

        public DeferredNode(final Class<?> api,
                            final String path,
                            final Element last,
                            final ComponentContainer.CircularReferencesException error) {
            if (!api.isInterface()) {
                throw circularity(api, path, error);
            }

            this.api = api;
            this.type = Proxy.getProxyClass(api.getClassLoader(), api);
            this.last = last;
        }

        public Class<?> type() {
            return type;
        }

        public Object instance(final Observer observer) {
            final Class<?>[] interfaces = { api };
            return Proxy.newProxyInstance(api.getClassLoader(), interfaces, new InvocationHandler() {
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

        public ComponentContext context() {
            return last.context;
        }
    }

    Object instantiate(final Class<?> api, final Graph.Node node, final Observer observer) {
        final DependencyPath<InstantiationElement> savedPath = instantiationPath;
        final DependencyPath<InstantiationElement> currentPath = savedPath.descend(new InstantiationElement(api, node.context()));

        instantiationPath = currentPath;
        try {
            Object instance = node.instance(observer);

            // set node after instantiation
            currentPath.last.node = node;

            if (instance != null) {
                if (currentPath.last.hasInstance()) {

                    // chain has been cut short
                    instance = currentPath.last.instance();
                } else {
                    if (currentPath.circular && !currentPath.edge) {

                        // cut short the instantiation chain
                        currentPath.last.setInstance(instance);
                    }

                    if (observer != null) {
                        observer.resolved(currentPath, node.type());
                    }
                }
            } else if (observer != null) {
                observer.resolved(currentPath, node.type());
            }

            return instance;
        } finally {
            instantiationPath = savedPath;
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
            return instantiate(api, node, observer);
        }

        public ComponentContext context() {
            return node.context();
        }
    }

    /**
     * Maintains a potentially circular dependency path.
     */
    private static class DependencyPath<T extends Element> implements Graph.Path {

        public final T last;
        public final boolean circular;
        public final boolean edge;

        private final List<T> list = new ArrayList<T>();
        private final Map<T, T> map = new LinkedHashMap<T, T>();
        private final boolean collapse;

        private DependencyPath(final boolean collapse) {
            this.collapse = collapse;
            this.circular = false;
            this.edge = false;
            this.last = null;
        }

        public DependencyPath(final List<T> list, final Map<T, T> map, final T last, final boolean collapse) {
            this.collapse = collapse;
            this.list.addAll(list);
            this.map.putAll(map);
            this.circular = map.containsKey(last);

            if (!this.circular) {
                this.map.put(last, last);
            }

            this.last = this.map.get(last);
            assert this.last != null : this.map.keySet();

            this.edge = !this.list.isEmpty() && this.list.lastIndexOf(this.last) == this.list.size() - 1;
            this.list.add(last);
        }

        public DependencyPath<T> descend(final T element) {
            return new DependencyPath<T>(list, map, element, collapse);
        }

        public Class<?> head() {
            return last.api();
        }

        public List<Class<?>> path() {
            final List<Class<?>> path = new ArrayList<Class<?>>();

            for (final T element : list) {
                if (collapse && circular && last == element) {
                    break;
                } else {
                    path.add(element.api());
                }
            }

            if (circular) {
                path.add(last.api());
            }

            return path;
        }

        @Override
        public String toString() {
            return path().toString();
        }
    }

    private static abstract class Element {

        public final Class<?> api;
        public ComponentContext context;

        private Element(final Class<?> api) {
            this.api = api;
        }

        public final Class<?> api() {
            return api;
        }

        protected abstract Object supplement();

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            } else if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Element element = (Element) o;
            final Object mine = supplement();
            final Object theirs = element.supplement();
            return api.equals(element.api) && (mine == null ? theirs == null : mine.equals(theirs));
        }

        @Override
        public int hashCode() {
            return supplement() == null ? api.hashCode() : (31 * api.hashCode() + supplement().hashCode());
        }
    }

    /**
     * Defines circularity in terms of API and provided context.
     */
    private static class ResolutionElement extends Element {

        public final ContextDefinition definition;

        ResolutionElement(final Class<?> api, final ContextDefinition definition) {
            super(api);
            this.definition = definition.copy();
        }

        @Override
        protected Object supplement() {
            return definition;
        }
    }

    /**
     * Defines circularity in terms of API and consumed context, which may be detected sooner than when relying only on provided context.
     */
    private static class InstantiationElement extends Element {

        public Graph.Node node;

        private boolean cached;
        private Object instance;

        InstantiationElement(final Class<?> api, final ComponentContext context) {
            super(api);
            this.context = context;
        }

        @Override
        protected Object supplement() {
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

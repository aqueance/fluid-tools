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
import java.lang.reflect.InvocationTargetException;
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
import org.fluidity.foundation.Exceptions;

/**
 * Detects and handles circular reference when possible.
 * <p/>
 * This object keeps state, do not use it concurrently or if its API method terminates abnormally (i.e., throws an exception).
 */
final class DependencyPathTraversal implements Graph.Traversal {

    private final Strategy strategy;

    private DependencyPath resolutionPath = new DependencyPath();

    public DependencyPathTraversal(final Strategy strategy) {
        this.strategy = strategy;
    }

    public Graph.Node follow(final Graph graph, final ContextDefinition context, final Graph.Reference reference) {
        final Class<?> api = reference.api();

        assert context != null;

        final DependencyPath savedPath = resolutionPath;
        final DependencyPath currentPath = savedPath.descend(new Element(api, context));

        resolutionPath = currentPath;
        try {
            final boolean circular = currentPath.circular;

            if (circular) {
                return new ProxyNode(api, currentPath, null);
            } else {
                final Trail trail = new ResolutionTrail(currentPath, reference, context);
                final Graph.Node node = strategy.resolve(circular, graph, this, trail);
                return new ResolvedNode(api, currentPath.head, node == null ? trail.advance() : node);
            }
        } finally {
            resolutionPath = savedPath;
        }
    }

    Object instantiate(final Class<?> api, final Graph.Node node, final Element element, final Observer observer) {
        final DependencyPath savedPath = resolutionPath;
        final DependencyPath currentPath = savedPath.descend(element.redefine(node));

        resolutionPath = currentPath;
        try {
            final Graph.Node resolved = currentPath.head.node = resolve(api, savedPath, node, observer);
            Object instance = resolved.instance(observer);

            if (instance != null) {
                if (currentPath.head.cache != null) {

                    // chain has been cut short
                    instance = currentPath.head.cache.instance;
                } else {
                    if (currentPath.circular && !currentPath.tip) {

                        // cut short the instantiation chain
                        currentPath.head.cache = new Cache(instance);
                    }

                    if (observer != null) {
                        observer.resolved(currentPath, resolved.type(), resolved.context());
                    }
                }
            } else if (observer != null) {
                observer.resolved(currentPath, resolved.type(), resolved.context());
            }

            return instance;
        } finally {
            resolutionPath = savedPath;
        }
    }

    private Graph.Node resolve(final Class<?> api, final DependencyPath path, final Graph.Node node, final Observer observer) throws CircularReferencesException {
        try {
            return new Graph.Node.Constant(node.type(), node.instance(observer), node.context());
        } catch (final CircularReferencesException error) {
            if (error.node == node) {
                throw error;
            } else {
                return new ProxyNode(api, path, error.node.error == null ? error : error.node.error);
            }
        }
    }

    private class ProxyNode implements Graph.Node {

        public final Element repeat;
        public final Class<?> api;
        public final String path;
        public final CircularReferencesException error;

        public ProxyNode(final Class<?> api, final DependencyPath path, final CircularReferencesException error) {
            this.api = api;
            this.path = path.toString();
            this.error = error;
            this.repeat = path.head;
        }

        public Class<?> type() {
            return Proxy.class;
        }

        public Object instance(final Observer observer) {
            if (api.isInterface()) {
                return Exceptions.wrap(new Exceptions.Command<Object>() {
                    public Object run() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
                        return Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[] {api}, new InvocationHandler() {
                            private volatile Object delegate;
                            private Set<Method> methods = new HashSet<Method>();

                            public Object invoke(final Object proxy, final Method method, final Object[] arguments) throws Throwable {
                                if (!methods.add(method)) {
                                    throw new ComponentContainer.CircularInvocationException(delegate, methods);
                                } else {
                                    try {
                                        return Exceptions.wrap(new Exceptions.Command<Object>() {
                                            public Object run() throws Exception {
                                                return method.invoke(delegate(), arguments);
                                            }
                                        });
                                    } finally {
                                        methods.remove(method);
                                    }
                                }
                            }

                            Object delegate() {
                                Object cache = delegate;

                                if (delegate == null) {
                                    synchronized (this) {
                                        cache = delegate;

                                        if (cache == null) {
                                            if (repeat == null || repeat.node == null) {
                                                throw circularity(ProxyNode.this);
                                            } else {
                                                assert repeat.node != ProxyNode.this : api;
                                                delegate = cache = repeat.node.instance(observer);
                                            }
                                        }
                                    }
                                }

                                return cache;
                            }
                        });
                    }
                });
            } else {
                throw circularity(this);
            }
        }

        public ComponentContext context() {
            assert repeat != null && repeat.node != null : path;
            return repeat.node.context();
        }
    }

    public static CircularReferencesException circularity(final ProxyNode node) {
        return node.error == null ? new CircularReferencesException(node) : node.error;
    }

    private class ResolvedNode implements Graph.Node {

        private final Class<?> api;
        private final Graph.Node node;
        private final Element element;

        public ResolvedNode(final Class<?> api, final Element element, final Graph.Node node) {
            this.api = api;
            this.element = element;
            this.node = node;
        }

        public Class<?> type() {
            return node.type();
        }

        public Object instance(final Observer observer) {
            return instantiate(api, node, element, observer);
        }

        public ComponentContext context() {
            return node.context();
        }
    }

    /**
     * Maintains a potentially circular dependency path.
     */
    private static class DependencyPath implements Graph.Path {

        public final Element head;
        public final boolean circular;
        public final boolean tip;

        private final List<Element> list = new ArrayList<Element>();
        private final Map<Element, Element> map = new LinkedHashMap<Element, Element>();

        private DependencyPath() {
            this.circular = false;
            this.tip = false;
            this.head = null;
        }

        public DependencyPath(final List<Element> list, final Map<Element, Element> map, final Element head) {
            this.list.addAll(list);
            this.map.putAll(map);
            this.circular = map.containsKey(head);

            if (!this.circular) {
                this.map.put(head, head);
            }

            this.head = this.map.get(head);
            assert this.head != null : this.map.keySet();

            this.tip = !this.list.isEmpty() && this.list.lastIndexOf(this.head) == this.list.size() - 1;
            this.list.add(head);
        }

        public DependencyPath descend(final Element element) {
            return new DependencyPath(list, map, element);
        }

        public Class<?> head() {
            return head.api;
        }

        public List<Class<?>> path() {
            final List<Class<?>> path = new ArrayList<Class<?>>();

            for (final Element element : list) {
                path.add(element.api);
            }

            return path;
        }

        @Override
        public String toString() {
            return path().toString();
        }
    }

    private static class Cache {

        public final Object instance;

        public Cache(final Object instance) {
            this.instance = instance;
        }
    }

    private static final class Element {

        public final Class<?> api;
        public ContextDefinition definition;
        public ComponentContext context;
        public Graph.Node node;
        public Cache cache;

        private Element(final Class<?> api) {
            this.api = api;
        }

        public Element(final Class<?> api, final ContextDefinition definition) {
            this(api);
            this.definition = definition.copy();
        }

        private Object context() {
            return definition == null ? context : definition;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            } else if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Element element = (Element) o;
            final Object mine = context();
            final Object theirs = element.context();
            return api.equals(element.api) && (mine == null ? theirs == null : mine.equals(theirs));
        }

        @Override
        public int hashCode() {
            final int hash = api.hashCode();
            final Object context = context();
            return context != null ? context.hashCode() + 31 * hash : hash;
        }

        public Element redefine(final Graph.Node node) {
            this.definition = null;
            this.context = node.context();
            return this;
        }
    }

    private static final class CircularReferencesException extends ComponentContainer.CircularReferencesException {

        public final ProxyNode node;

        public CircularReferencesException(final ProxyNode node) {
            super(node.api, node.path);
            this.node = node;
        }
    }

    private class ResolutionTrail implements Trail {

        private final DependencyPath currentPath;
        private final Graph.Reference reference;
        private final ContextDefinition context;

        public ResolutionTrail(final DependencyPath currentPath, final Graph.Reference reference, final ContextDefinition context) {
            this.currentPath = currentPath;
            this.reference = reference;
            this.context = context;
        }

        public Class<?> head() {
            return currentPath.head();
        }

        public List<Class<?>> path() {
            return currentPath.path();
        }

        public Graph.Node advance() {
            return reference.resolve(DependencyPathTraversal.this, context);
        }
    }
}

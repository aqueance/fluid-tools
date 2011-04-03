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
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.spi.ComponentResolutionObserver;
import org.fluidity.foundation.Exceptions;
import org.fluidity.foundation.Strings;

/**
 * Detects and handles circular reference when possible.
 * <p/>
 * This object keeps state, do not use it concurrently or if its API method terminates abnormally (i.e., throws an exception).
 */
final class DependencyPathTraversal implements DependencyGraph.Traversal {

    private final Strategy strategy;
    private final ComponentResolutionObserver observer;

    private final AtomicReference<DependencyPath> resolutionPath;

    public DependencyPathTraversal(final Strategy strategy, final ComponentResolutionObserver observer) {
        this(new AtomicReference<DependencyPath>(new DependencyPath()), strategy, observer);
    }

    public DependencyPathTraversal(final AtomicReference<DependencyPath> path, final Strategy strategy, final ComponentResolutionObserver observer) {
        assert path != null;
        assert strategy != null;
        this.resolutionPath = path;
        this.strategy = strategy;
        this.observer = observer;
    }

    public DependencyGraph.Node follow(final DependencyGraph graph, final ContextDefinition context, final DependencyGraph.Node.Reference reference) {
        final Class<?> api = reference.api();

        assert context != null;

        final DependencyPath savedPath = resolutionPath.get();
        final DependencyPath currentPath = savedPath.descend(new Element(api, context));

        resolutionPath.set(currentPath);
        try {
            final boolean repeating = currentPath.repeating;

            if (repeating) {
                final DependencyGraph.Node node = strategy.advance(graph, context, this, repeating, new TerminationTrail(currentPath));

                if (node == null) {
                    return new ProxyNode(api, currentPath, null);
                } else {
                    if (observer != null) {
                        observer.resolved(currentPath, node.type());
                    }

                    return node;
                }
            } else {
                final Trail trail = new ResolutionTrail(this, currentPath, reference, context);
                final DependencyGraph.Node node = strategy.advance(graph, context, this, repeating, trail);
                final DependencyGraph.Node resolved = node == null ? trail.advance() : node;

                if (observer != null) {
                    observer.resolved(currentPath, resolved.type());
                }

                return new ResolvedNode(api, currentPath.head, resolved);
            }
        } finally {
            resolutionPath.set(savedPath);
        }
    }

    public DependencyGraph.Traversal observed(final ComponentResolutionObserver observer) {
        return new DependencyPathTraversal(resolutionPath, strategy, CompositeObserver.combine(this.observer, observer));
    }

    public void instantiating(final Class<?> type) {
        resolutionPath.get().head.type = type;
    }

    public void instantiated(final Class<?> type) {
        if (observer != null) {
            observer.instantiated(resolutionPath.get());
        }
    }

    Object instantiate(final Class<?> api, final DependencyGraph.Node node, final Element element, final DependencyGraph.Traversal traversal) {
        final DependencyPath savedPath = resolutionPath.get();
        final DependencyPath currentPath = savedPath.descend(element.redefine(node));

        resolutionPath.set(currentPath);
        try {
            final DependencyGraph.Node resolved = currentPath.head.node = resolve(api, savedPath, node, traversal);
            Object instance = resolved.instance(traversal);

            if (instance != null) {
                if (currentPath.head.cache != null) {

                    // chain has been cut short
                    instance = currentPath.head.cache.instance;
                } else {
                    if (currentPath.repeating && !currentPath.tip) {

                        // cut short the instantiation chain
                        currentPath.head.cache = new Cache(instance);
                    }
                }
            }

            return instance;
        } finally {
            resolutionPath.set(savedPath);
        }
    }

    private DependencyGraph.Node resolve(final Class<?> api,
                                         final DependencyPath path,
                                         final DependencyGraph.Node node,
                                         final DependencyGraph.Traversal traversal) throws CircularReferencesException {
        try {
            final Object instance = node.instance(traversal);
            return instance == null ? null : new DependencyGraph.Node.Constant(node.type(), instance, node.context());
        } catch (final CircularReferencesException error) {
            if (error.node == node) {
                throw error;
            } else {
                return new ProxyNode(api, path, error.node.error == null ? error : error.node.error);
            }
        }
    }

    private class ProxyNode implements DependencyGraph.Node {

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

        public Object instance(final DependencyGraph.Traversal traversal) {
            if (api.isInterface()) {
                return Exceptions.wrap(new Exceptions.Command<Object>() {
                    public Object run() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
                        return Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[] { api }, new InvocationHandler() {
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
                                                delegate = cache = repeat.node.instance(traversal);
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

    private class ResolvedNode implements DependencyGraph.Node {

        private final Class<?> api;
        private final DependencyGraph.Node node;
        private final Element element;

        public ResolvedNode(final Class<?> api, final Element element, final DependencyGraph.Node node) {
            this.api = api;
            this.element = element;
            this.node = node;
        }

        public Class<?> type() {
            return node.type();
        }

        public Object instance(final DependencyGraph.Traversal traversal) {
            return instantiate(api, node, element, traversal);
        }

        public ComponentContext context() {
            return node.context();
        }
    }

    /**
     * Maintains a potentially circular dependency path.
     */
    private static class DependencyPath implements org.fluidity.composition.spi.DependencyPath {

        public final Element head;
        public final boolean repeating;
        public final boolean tip;

        private final List<Element> list = new ArrayList<Element>();
        private final Map<Element, Element> map = new LinkedHashMap<Element, Element>();

        private DependencyPath() {
            this.repeating = false;
            this.tip = false;
            this.head = null;
        }

        public DependencyPath(final List<Element> list, final Map<Element, Element> map, final Element head) {
            this.list.addAll(list);
            this.map.putAll(map);
            this.repeating = map.containsKey(head);

            if (!this.repeating) {
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

        public Class<?> head(final boolean api) {
            return api || head.type == null ? head.api : head.type;
        }

        public List<Class<?>> path(final boolean api) {
            final List<Class<?>> path = new ArrayList<Class<?>>();

            for (final Element element : list) {
                path.add(api || element.type == null ? element.api : element.type);
            }

            return path;
        }

        @Override
        public String toString() {
            return toString(true);
        }

        public String toString(final boolean api) {
            final StringBuilder builder = new StringBuilder();
            for (final Class<?> type : path(api)) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }

                builder.append(Strings.arrayNotation(type));
            }

            return builder.insert(0, '[').append(']').toString();
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
        public Class<?> type;
        public ContextDefinition definition;
        public ComponentContext context;
        public DependencyGraph.Node node;
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

        public Element redefine(final DependencyGraph.Node node) {
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

    private static class TerminationTrail implements Trail {

        private final DependencyPath currentPath;

        public TerminationTrail(final DependencyPath currentPath) {
            this.currentPath = currentPath;
        }

        public Class<?> head(final boolean api) {
            return currentPath.head(api);
        }

        public List<Class<?>> path(final boolean api) {
            return currentPath.path(api);
        }

        @Override
        public String toString() {
            return currentPath.toString();
        }

        public String toString(final boolean api) {
            return currentPath.toString(api);
        }

        public DependencyGraph.Node advance() {
            return null;
        }
    }

    private static class ResolutionTrail implements Trail {

        private final DependencyGraph.Traversal traversal;
        private final DependencyPath currentPath;
        private final DependencyGraph.Node.Reference reference;
        private final ContextDefinition context;

        public ResolutionTrail(final DependencyGraph.Traversal traversal,
                               final DependencyPath currentPath,
                               final DependencyGraph.Node.Reference reference,
                               final ContextDefinition context) {
            this.traversal = traversal;
            this.currentPath = currentPath;
            this.reference = reference;
            this.context = context;
        }

        public Class<?> head(final boolean api) {
            return currentPath.head(api);
        }

        public List<Class<?>> path(final boolean api) {
            return currentPath.path(api);
        }

        @Override
        public String toString() {
            return currentPath.toString();
        }

        public String toString(final boolean api) {
            return currentPath.toString(api);
        }

        public DependencyGraph.Node advance() {
            return reference.resolve(traversal, context);
        }
    }
}
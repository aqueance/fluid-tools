/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fluidity.composition;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.spi.ComponentResolutionObserver;
import org.fluidity.composition.spi.DependencyPath;
import org.fluidity.foundation.Deferred;
import org.fluidity.foundation.Proxies;
import org.fluidity.foundation.Strings;

/**
 * Detects and handles circular reference when possible.
 * <p/>
 * This object keeps state, do not use it concurrently or if its API method terminates abnormally (i.e., throws an exception).
 */
final class DependencyPathTraversal implements DependencyGraph.Traversal {

    private final Strategy strategy;
    private final ComponentResolutionObserver observer;

    private final AtomicReference<ActualPath> resolutionPath;

    private final DependencyInjector injector;

    public DependencyPathTraversal(final DependencyInjector injector, final Strategy strategy, final ComponentResolutionObserver observer) {
        this(new AtomicReference<ActualPath>(new ActualPath()), injector, strategy, observer);
    }

    public DependencyPathTraversal(final AtomicReference<ActualPath> path, final DependencyInjector injector, final Strategy strategy, final ComponentResolutionObserver observer) {
        assert path != null;
        assert strategy != null;
        this.resolutionPath = path;
        this.injector = injector;
        this.strategy = strategy;
        this.observer = observer;
    }

    public DependencyGraph.Node follow(final DependencyGraph graph, final ContextDefinition context, final DependencyGraph.Node.Reference reference) {
        final Class<?> api = reference.api();

        assert context != null;

        final ActualPath savedPath = resolutionPath.get();
        final ActualPath currentPath = savedPath.descend(new ElementImpl(api, context, null));

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
                final Trail trail = new ResolutionTrail(injector, this, currentPath, reference, context);
                final DependencyGraph.Node node = strategy.advance(graph, context, this, repeating, trail);
                final DependencyGraph.Node resolved = node == null ? trail.advance() : node;
                assert resolved != null : api;

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
        return new DependencyPathTraversal(resolutionPath, injector, strategy, CompositeObserver.combine(this.observer, observer));
    }

    public void instantiating(final Class<?> type) {
        resolutionPath.get().head.type = type;
    }

    public void instantiated(final Class<?> type, final Object component) {
        if (observer != null) {
            final AtomicReference<Object> reference = new AtomicReference<Object>();
            observer.instantiated(resolutionPath.get(), reference);
            reference.set(component);
        }
    }

    public DependencyPath path() {
        return resolutionPath.get();
    }

    public void resolving(final Class<?> declaringType,
                          final Class<?> dependencyType,
                          final Annotation[] typeAnnotations,
                          final Annotation[] referenceAnnotations) {
        if (observer != null) {
            observer.resolving(resolutionPath.get().head().api(), declaringType, dependencyType, typeAnnotations, referenceAnnotations);
        }
    }

    Object instantiate(final Class<?> api, final DependencyGraph.Node node, final ElementImpl element, final DependencyGraph.Traversal traversal) {
        final ActualPath savedPath = resolutionPath.get();
        final ActualPath currentPath = savedPath.descend(element.redefine(node));

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
                                         final ActualPath path,
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
        } catch (final ComponentContainer.InstantiationException e) {
            throw e;
        } catch (final Exception e) {
            throw new ComponentContainer.InstantiationException(resolutionPath.get(), e);
        }
    }

    private class ProxyNode implements DependencyGraph.Node {

        public final ElementImpl repeat;
        public final Class<?> api;
        public final String path;
        public final CircularReferencesException error;

        public ProxyNode(final Class<?> api, final ActualPath path, final CircularReferencesException error) {
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
                final Deferred.Reference<Object> delegate = Deferred.defer(new Deferred.Factory<Object>() {
                    public Object create() {
                        if (repeat == null || repeat.node == null) {
                            throw circularity(ProxyNode.this);
                        } else {
                            assert repeat.node != ProxyNode.this : api;
                            return repeat.node.instance(traversal);
                        }
                    }
                });

                return Proxies.create(api, new InvocationHandler() {
                    private Set<Method> methods = new HashSet<Method>();

                    public Object invoke(final Object proxy, final Method method, final Object[] arguments) throws Throwable {
                        if (!methods.add(method)) {
                            throw new ComponentContainer.CircularInvocationException(delegate.get(), methods);
                        } else {
                            try {
                                return method.invoke(delegate.get(), arguments);
                            } finally {
                                methods.remove(method);
                            }
                        }
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
        private final ElementImpl element;

        public ResolvedNode(final Class<?> api, final ElementImpl element, final DependencyGraph.Node node) {
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
    private static class ActualPath implements DependencyPath {

        public final ElementImpl head;
        public final boolean repeating;
        public final boolean tip;

        private final List<ElementImpl> list = new ArrayList<ElementImpl>();
        private final Map<ElementImpl, ElementImpl> map = new LinkedHashMap<ElementImpl, ElementImpl>();

        private ActualPath() {
            this.repeating = false;
            this.tip = false;
            this.head = null;
        }

        public ActualPath(final List<ElementImpl> list, final Map<ElementImpl, ElementImpl> map, final ElementImpl head) {
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

        public ActualPath descend(final ElementImpl element) {
            return new ActualPath(list, map, element);
        }

        public Element head() {
            return head;
        }

        public List<Element> path() {
            return new ArrayList<Element>(list);
        }

        @Override
        public String toString() {
            return toString(true);
        }

        public String toString(final boolean api) {
            final StringBuilder builder = new StringBuilder();
            for (final Element type : path()) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }

                builder.append(Strings.arrayNotation(api ? type.api() : type.type()));
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

    private static final class ElementImpl implements DependencyPath.Element {

        public final Class<?> api;
        public Class<?> type;
        public ContextDefinition definition;
        public ComponentContext context;
        public DependencyGraph.Node node;
        public Cache cache;
        private Set<Annotation> annotations = new HashSet<Annotation>();

        public ElementImpl(final Class<?> api, final ContextDefinition definition, final Annotation[] annotations) {
            this.api = api;
            this.definition = definition.copy();
            if (annotations != null) {
                this.annotations.addAll(Arrays.asList(annotations));
            }
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

            final ElementImpl element = (ElementImpl) o;
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

        public ElementImpl redefine(final DependencyGraph.Node node) {
            this.definition = null;
            this.context = node.context();
            return this;
        }

        public Class<?> api() {
            return api;
        }

        public Class<?> type() {
            return type == null ? api : type;
        }

        public Set<Annotation> annotations() {
            return Collections.unmodifiableSet(annotations);
        }

        @SuppressWarnings("unchecked")
        public <T extends Annotation> T annotation(final Class<T> type) {
            for (final Annotation annotation : annotations()) {
                if (type.isAssignableFrom(annotation.getClass())) {
                    return (T) annotation;
                }
            }

            return null;
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

        private final ActualPath currentPath;

        public TerminationTrail(final ActualPath currentPath) {
            this.currentPath = currentPath;
        }

        public Element head() {
            return currentPath.head();
        }

        public List<Element> path() {
            return currentPath.path();
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
        private final ActualPath currentPath;
        private final DependencyGraph.Node.Reference reference;
        private final ContextDefinition context;
        private final DependencyInjector injector;

        public ResolutionTrail(final DependencyInjector injector,
                               final DependencyGraph.Traversal traversal,
                               final ActualPath currentPath,
                               final DependencyGraph.Node.Reference reference,
                               final ContextDefinition context) {
            this.injector = injector;
            this.traversal = traversal;
            this.currentPath = currentPath;
            this.reference = reference;
            this.context = context;
        }

        public Element head() {
            return currentPath.head();
        }

        public List<Element> path() {
            return currentPath.path();
        }

        @Override
        public String toString() {
            return currentPath.toString();
        }

        public String toString(final boolean api) {
            return currentPath.toString(api);
        }

        public DependencyGraph.Node advance() {
            return injector.resolve(reference.api(), new DependencyInjector.Resolution() {
                public ComponentContext context() {
                    return null;
                }

                public ComponentContainer container() {
                    return null;
                }

                public DependencyGraph.Node regular() {
                    return reference.resolve(traversal, context);
                }

                public void handle(final RestrictedContainer container) {
                    // empty
                }
            });
        }
    }
}

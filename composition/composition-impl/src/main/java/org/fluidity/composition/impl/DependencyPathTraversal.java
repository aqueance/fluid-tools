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

package org.fluidity.composition.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.spi.ComponentResolutionObserver;
import org.fluidity.composition.spi.ContextDefinition;
import org.fluidity.composition.spi.DependencyGraph;
import org.fluidity.composition.spi.DependencyInjector;
import org.fluidity.composition.spi.DependencyPath;
import org.fluidity.composition.spi.RestrictedContainer;
import org.fluidity.foundation.Deferred;
import org.fluidity.foundation.Proxies;
import org.fluidity.foundation.Strings;

/**
 * Detects and handles circular reference when possible.
 * <p/>
 * This object keeps state, do not use it concurrently or if its API method terminates abnormally (i.e., throws an exception).
 */
final class DependencyPathTraversal implements DependencyGraph.Traversal {

    private final ComponentResolutionObserver observer;
    private final AtomicReference<ActualPath> resolutionPath;
    private final DependencyInjector injector;

    final AtomicReference<CircularReferencesException> deferring = new AtomicReference<CircularReferencesException>();

    public DependencyPathTraversal(final DependencyInjector injector, final ComponentResolutionObserver observer) {
        this(new AtomicReference<ActualPath>(new ActualPath()), injector, observer);
    }

    public DependencyPathTraversal(final AtomicReference<ActualPath> path, final DependencyInjector injector, final ComponentResolutionObserver observer) {
        assert path != null;
        this.resolutionPath = path;
        this.injector = injector;
        this.observer = observer;
    }

    private interface Resolution {

        DependencyGraph.Node perform();
    }

    public DependencyGraph.Node follow(final DependencyGraph graph, final ContextDefinition context, final DependencyGraph.Node.Reference reference) {
        assert context != null;

        final Class<?> api = reference.type();

        final ActualPath savedPath = resolutionPath.get();
        final ActualPath currentPath = savedPath.descend(new ElementImpl(api, null));

        final Resolution resolution = new Resolution() {
            public DependencyGraph.Node perform() {
                final CircularReferencesException error = deferring.get();

                if (error != null && currentPath.repeating) {
                    if (currentPath.head.node == null) {
                        throw error;
                    } else {
                        return currentPath.head.node;
                    }
                }

                resolutionPath.set(currentPath);

                try {
                    final DependencyGraph.Node resolved = injector.resolve(reference.type(), new DependencyInjector.Resolution() {
                        public ComponentContext context() {
                            assert false;
                            return null;
                        }

                        public ComponentContainer container() {
                            assert false;
                            return null;
                        }

                        public DependencyGraph.Node regular() {
                            return reference.resolve(DependencyPathTraversal.this, context);
                        }

                        public void handle(final RestrictedContainer container) {
                            assert false;
                        }
                    });

                    assert resolved != null : api;

                    if (observer != null) {
                        observer.resolved(currentPath, resolved.type());
                    }

                    return new ResolvedNode(api, currentPath.head, resolved);
                } finally {
                    resolutionPath.set(savedPath);
                }
            }
        };

        if (currentPath.repeating) {
            if (currentPath.head.node == null) {
                @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
                final CircularReferencesException original = deferring.get();
                final CircularReferencesException error = original == null ? new CircularReferencesException(api, currentPath) : original;

                if (api.isInterface() && original == null) {
                    return new ProxyNode(api, error, resolution);
                } else {
                    throw error;
                }
            } else {
                return currentPath.head.node;
            }
        } else {
            try {
                return resolution.perform();
            } catch (final CircularReferencesException error) {
                if (error.unrolled) {
                    throw error;
                } else {
                    if (error.api == api) {
                        error.unrolled = true;
                    }

                    if (api.isInterface()) {
                        return new ProxyNode(api, error, resolution);
                    } else {
                        throw error;
                    }
                }
            }
        }
    }

    public DependencyGraph.Traversal observed(final ComponentResolutionObserver observer) {
        return new DependencyPathTraversal(resolutionPath, injector, CompositeObserver.combine(this.observer, observer));
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
            observer.resolving(declaringType, dependencyType, typeAnnotations, referenceAnnotations);
        }
    }

    Object instantiate(final Class<?> api, final DependencyGraph.Node node, final ElementImpl element, final DependencyGraph.Traversal traversal) {
        final ActualPath savedPath = resolutionPath.get();
        final ActualPath currentPath = savedPath.descend(element, node.context());

        resolutionPath.set(currentPath);
        try {
            final DependencyGraph.Node resolved = currentPath.head.node = resolve(api, currentPath, node, traversal);
            Object instance = resolved.instance(traversal);

            if (instance != null) {
                if (currentPath.head.cache.get() != null) {

                    // chain has been cut short
                    instance = currentPath.head.cache.get();
                } else {
                    if (currentPath.repeating && !currentPath.tip) {

                        // cut short the instantiation chain
                        currentPath.head.cache.set(instance);
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
        final Resolution resolution = new Resolution() {
            public DependencyGraph.Node perform() {
                final ActualPath saved = resolutionPath.getAndSet(path);

                try {
                    final Object instance = node.instance(traversal);
                    return instance == null ? null : new DependencyGraph.Node.Constant(node.type(), instance, node.context());
                } finally {
                    resolutionPath.set(saved);
                }
            }
        };

        try {
            return resolution.perform();
        } catch (final CircularReferencesException error) {
            if (error.unrolled) {
                throw error;
            } else {
                if (error.api == api) {
                    error.unrolled = true;
                }

                if (api.isInterface()) {
                    return new ProxyNode(api, error, resolution);
                } else {
                    throw error;
                }
            }
        } catch (final ComponentContainer.InstantiationException e) {
            throw e;
        } catch (final Exception e) {
            throw new ComponentContainer.InstantiationException(resolutionPath.get(), e);
        }
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
        private final Map<ElementImpl, ElementImpl> map = new HashMap<ElementImpl, ElementImpl>();

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

        public ActualPath descend(final ElementImpl element, final ComponentContext context) {
            final ElementImpl found = map.remove(element);

            element.receive(context);

            if (found != null) {
                map.put(element, element);
            }

            return descend(element);
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

                builder.append(Strings.arrayNotation(true, api ? type.api() : type.type()));
            }

            return builder.insert(0, '[').append(']').toString();
        }
    }

    private static final class ElementImpl implements DependencyPath.Element {

        public final Class<?> api;
        public Class<?> type;
        public ComponentContext context;
        public DependencyGraph.Node node;
        public AtomicReference<Object> cache = new AtomicReference<Object>();
        private Set<Annotation> annotations = new HashSet<Annotation>();

        public ElementImpl(final Class<?> api, final Annotation[] annotations) {
            this.api = api;
            if (annotations != null) {
                this.annotations.addAll(Arrays.asList(annotations));
            }
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            } else if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final ElementImpl that = (ElementImpl) o;
            return api.equals(that.api);
        }

        @Override
        public int hashCode() {
            return api.hashCode();
        }

        /*
         * Note: this changes the object's identity
         */
        public ElementImpl receive(final ComponentContext context) {
            this.context = context;
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

        private final Class<?> api;
        public final ActualPath path;
        public boolean unrolled;

        public CircularReferencesException(final Class<?> api, final ActualPath path) {
            super(api, path.toString(true));
            this.api = api;
            this.path = path;
        }
    }

    private class ProxyNode implements DependencyGraph.Node {

        private final Class<?> api;
        private final Deferred.Reference<DependencyGraph.Node> node;

        public ProxyNode(final Class<?> api, final CircularReferencesException error, final Resolution resolution) {
            this.api = api;

            this.node = Deferred.reference(new Deferred.Factory<DependencyGraph.Node>() {
                public DependencyGraph.Node create() {
                    if (deferring.compareAndSet(null, error)) {
                        try {
                            return resolution.perform();
                        } finally {
                            deferring.set(null);
                        }
                    } else {
                        throw error;
                    }
                }
            });
        }

        public Class<?> type() {
            return api;
        }

        public Object instance(final DependencyGraph.Traversal traversal) {
            assert traversal == DependencyPathTraversal.this : traversal;

            final Deferred.Reference<Object> delegate = Deferred.reference(new Deferred.Factory<Object>() {
                public Object create() {
                    return node.get().instance(traversal);
                }
            });

            return Proxies.create(api, new InvocationHandler() {
                public Object invoke(final Object proxy, final Method method, final Object[] arguments) throws Throwable {
                    return method.invoke(delegate.get(), arguments);
                }
            });
        }

        public ComponentContext context() {
            return node.get().context();
        }
    }
}

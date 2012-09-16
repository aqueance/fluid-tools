/*
 * Copyright (c) 2006-2012 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.composition.container.impl;

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
import org.fluidity.composition.DependencyPath;
import org.fluidity.composition.container.ContextDefinition;
import org.fluidity.composition.container.spi.DependencyGraph;
import org.fluidity.foundation.Deferred;
import org.fluidity.foundation.Proxies;
import org.fluidity.foundation.Strings;

/**
 * Detects and handles circular reference when possible.
 * <p/>
 * This object keeps state, do not use it concurrently or if its API method terminates abnormally (i.e., throws an exception).
 *
 * @author Tibor Varga
 */
final class DependencyPathTraversal implements DependencyGraph.Traversal {

    private final ComponentContainer.Observer observer;

    private final AtomicReference<ActualPath> resolutionPath;

    final AtomicReference<CircularReferencesException> deferring = new AtomicReference<CircularReferencesException>();

    DependencyPathTraversal(final ComponentContainer.Observer observer) {
        this(new AtomicReference<ActualPath>(new ActualPath()), observer);
    }

    private DependencyPathTraversal(final AtomicReference<ActualPath> path, final ComponentContainer.Observer observer) {
        assert path != null;
        this.resolutionPath = path;
        this.observer = observer;
    }

    /**
     * Command to invoke while descending a resolution path.
     *
     * @param <T> the return type of the descent.
     *
     * @author Tibor Varga
     */
    private interface Descent<T> {

        /**
         * Performs the descent.
         *
         * @return whatever the caller of {@link DependencyPathTraversal#descend(DependencyPathTraversal.ActualPath, DependencyPathTraversal.Descent)} expects.
         */
        T perform();
    }

    private <T> T descend(final ActualPath path, final Descent<T> command) {
        final ActualPath saved = resolutionPath.getAndSet(path);

        try {
            return command.perform();
        } finally {
            resolutionPath.set(saved);
        }
    }

    public DependencyGraph.Node follow(final Object identity,
                                       final Class<?> api,
                                       final Class<?> type,
                                       final ContextDefinition context,
                                       final DependencyGraph.Node.Reference reference) {
        assert context != null;

        final ActualPath path = resolutionPath.get().descend(new ActualElement(type, identity, context, null));

        if (path.repeating && observer != null) {
            observer.circular(path);
        }

        final Descent<DependencyGraph.Node> descent = new Descent<DependencyGraph.Node>() {
            public DependencyGraph.Node perform() {
                final CircularReferencesException error = deferring.get();

                if (error != null && path.repeating) {
                    if (path.head.node == null) {
                        throw error;
                    } else if (context.equals(path.head.definition)) {
                        return path.head.node;
                    }
                }

                final DependencyGraph.Node resolved = reference.resolve();
                assert resolved != null : api;

                if (observer != null) {
                    observer.resolved(path, resolved.type());
                }

                return new ResolvedNode(api, path.head, resolved);
            }
        };

        if (path.repeating) {
            if (path.head.node == null) {
                final CircularReferencesException error = deferring.get();

                if (api.isInterface() && error == null) {
                    return new ProxyNode(api, path.head.definition, new CircularReferencesException(api, path), descent);
                } else {
                    throw new CircularReferencesException(api, path, error);
                }
            } else {
                return path.head.node;
            }
        } else {
            try {
                return descend(path, descent);
            } catch (final CircularReferencesException error) {
                return resolve(error, api, path.head, descent);
            }
        }
    }

    private DependencyGraph.Node resolve(final CircularReferencesException error, final Class<?> api, final ActualElement head, final Descent<DependencyGraph.Node> descent) {
        if (!error.descriptor.unrolled(head) && api.isInterface()) {
            return new ProxyNode(api, head.definition, error, descent);
        } else {
            throw error;
        }
    }

    public DependencyGraph.Traversal observed(final ComponentContainer.Observer... observers) {
        if (observers.length > 0) {
            final ComponentContainer.Observer[] list = new ComponentContainer.Observer[observers.length + 1];
            list[0] = this.observer;
            System.arraycopy(observers, 0, list, 1, observers.length);
            return new DependencyPathTraversal(resolutionPath, CompositeObserver.combine(list));
        } else {
            return this;
        }
    }

    public void instantiating(final Class<?> type) {
        resolutionPath.get().head.type = type;
    }

    public Object instantiated(final Class<?> type, final Object component) {
        if (observer != null) {
            final AtomicReference<Object> reference = new AtomicReference<Object>();

            try {
                observer.instantiated(resolutionPath.get(), reference);
            } finally {
                reference.set(component);
            }
        }

        return component;
    }

    public DependencyPath path() {
        return resolutionPath.get();
    }

    public void descend(final Class<?> declaringType,
                        final Class<?> dependencyType,
                        final Annotation[] typeAnnotations,
                        final Annotation[] referenceAnnotations) {
        if (observer != null) {
            observer.descending(declaringType, dependencyType, typeAnnotations, referenceAnnotations);
        }
    }

    public void ascend(final Class<?> declaringType, final Class<?> dependencyType) {
        if (observer != null) {
            observer.ascending(declaringType, dependencyType);
        }
    }

    private Object instantiate(final Class<?> api, final DependencyGraph.Node node, final ActualElement element, final DependencyGraph.Traversal traversal) {
        final ActualPath path = resolutionPath.get().descend(element, node.context());

        return descend(path, new Descent<Object>() {
            public Object perform() {
                final DependencyGraph.Node resolved = path.head.node = resolve(api, path, node, traversal);
                Object instance = resolved.instance(traversal);

                if (instance != null) {
                    if (path.head.cache.get() != null) {

                        // chain has been cut short
                        instance = path.head.cache.get();
                    } else {
                        if (path.repeating && !path.tip) {

                            // cut short the instantiation chain
                            path.head.cache.set(instance);
                        }
                    }
                }

                return instance;
            }
        });
    }

    private DependencyGraph.Node resolve(final Class<?> api,
                                         final ActualPath path,
                                         final DependencyGraph.Node node,
                                         final DependencyGraph.Traversal traversal) throws CircularReferencesException {
        final ComponentContext context = node.context();

        final Descent<DependencyGraph.Node> descent = new Descent<DependencyGraph.Node>() {
            public DependencyGraph.Node perform() {
                final Object instance = node.instance(traversal);
                return instance == null ? null : new DependencyGraph.Node.Constant(node.type(), instance, context);
            }
        };

        try {
            return descend(path, descent);
        } catch (final CircularReferencesException error) {
            if (error.failed(api, path.head.definition)) {
                throw error;
            } else {
                return resolve(error, api, path.head, descent);
            }
        } catch (final ComponentContainer.InstantiationException e) {
            throw e;
        } catch (final Exception e) {
            throw new ComponentContainer.InstantiationException(resolutionPath.get(), e);
        }
    }

    /**
     * @author Tibor Varga
     */
    private class ResolvedNode implements DependencyGraph.Node {

        private final Class<?> api;
        private final DependencyGraph.Node node;
        private final ActualElement element;

        public ResolvedNode(final Class<?> api, final ActualElement element, final DependencyGraph.Node node) {
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
     *
     * @author Tibor Varga
     */
    private static class ActualPath implements DependencyPath {

        public final ActualElement head;
        public final boolean repeating;
        public final boolean tip;

        private final List<ActualElement> list = new ArrayList<ActualElement>();
        private final Map<ActualElement, ActualElement> map = new HashMap<ActualElement, ActualElement>();

        private ActualPath() {
            this.repeating = false;
            this.tip = false;
            this.head = null;
        }

        ActualPath(final List<ActualElement> list, final Map<ActualElement, ActualElement> map, final ActualElement head) {
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

        public ActualPath descend(final ActualElement element) {
            return new ActualPath(list, map, element);
        }

        public ActualPath descend(final ActualElement element, final ComponentContext context) {
            final ActualElement found = map.remove(element);

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
            final Strings.Listing text = Strings.delimited();

            for (final ActualElement type : list) {
                @SuppressWarnings("MismatchedQueryAndUpdateOfStringBuilder")
                final StringBuilder builder = text.next();

                if (!type.definition.isEmpty()) {
                    builder.append(type.definition).append(' ');
                }

                builder.append(Strings.printClass(true, api ? type.api() : type.type()));
            }

            return text.surround("[]").toString();
        }
    }

    /**
     * @author Tibor Varga
     */
    private static final class ActualElement implements DependencyPath.Element {

        public final Class<?> api;
        public final Object identity;
        public final ContextDefinition definition;

        public Class<?> type;
        public ComponentContext context;
        public DependencyGraph.Node node;
        public AtomicReference<Object> cache = new AtomicReference<Object>();

        private Set<Annotation> annotations = new HashSet<Annotation>();

        ActualElement(final Class<?> api, final Object identity, final ContextDefinition definition, final Annotation[] annotations) {
            this.api = api;
            this.identity = identity;
            this.definition = definition;

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

            final ActualElement that = (ActualElement) o;
            return identity == that.identity && definition.equals(that.definition);
        }

        @Override
        public int hashCode() {
            return identity.hashCode() + 31 * definition.hashCode();
        }

        /*
         * Note: this changes the object's identity
         */
        public ActualElement receive(final ComponentContext context) {
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

    /**
     * @author Tibor Varga
     */
    private static class CircularityDescriptor {

        public final Class<?> api;
        public final ActualPath path;

        private CircularityDescriptor(final Class<?> api, final ActualPath path) {
            this.api = api;
            this.path = path;
        }

        public boolean unrolled(final ActualElement head) {
            for (final ActualElement element : path.list) {
                if (element.identity == head.identity && element.definition.equals(head.definition)) {
                    return true;
                } else if (element == path.head) {
                    return false;
                }
            }

            return false;
        }
    }

    /**
     * @author Tibor Varga
     */
    private static final class CircularReferencesException extends ComponentContainer.CircularReferencesException {

        private final CircularityDescriptor descriptor;
        private final Set<ProxyContext> failed = new HashSet<ProxyContext>();

        CircularReferencesException(final Class<?> api, final ActualPath path) {
            super(api, path.toString(true));
            this.descriptor = new CircularityDescriptor(api, path);
        }

        CircularReferencesException(final Class<?> api, final ActualPath path, final CircularReferencesException original) {
            super(original == null ? api : original.descriptor.api, (original == null ? path : original.descriptor.path).toString(true));
            this.descriptor = original == null ? new CircularityDescriptor(api, path) : original.descriptor;
        }

        public void failing(final ProxyContext context) {
            failed.add(context);
        }

        public boolean failed(final Class<?> api, final ContextDefinition context) {
            return failed.contains(new ProxyContext(api, context));
        }
    }

    /**
     * @author Tibor Varga
     */
    private static class ProxyContext {
        private final Class<?> api;
        private final ContextDefinition context;

        private ProxyContext(final Class<?> api, final ContextDefinition context) {
            this.api = api;
            this.context = context;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final ProxyContext that = (ProxyContext) o;
            return api.equals(that.api) && context.equals(that.context);

        }

        @Override
        public int hashCode() {
            return 31 * api.hashCode() + context.hashCode();
        }
    }

    /**
     * @author Tibor Varga
     */
    private class ProxyNode implements DependencyGraph.Node {

        private final ProxyContext context;
        private final Deferred.Reference<DependencyGraph.Node> node;
        private final CircularReferencesException error;

        public ProxyNode(final Class<?> api, final ContextDefinition context, final CircularReferencesException error, final Descent<DependencyGraph.Node> descent) {
            this.context = new ProxyContext(api, context);
            this.error = error;

            this.node = Deferred.reference(new Deferred.Factory<DependencyGraph.Node>() {
                public DependencyGraph.Node create() {
                    try {
                        return descent.perform();
                    } catch (final CircularReferencesException e) {
                        e.failing(ProxyNode.this.context);
                        throw e;
                    }
                }
            });
        }

        public Class<?> type() {
            return context.api;
        }

        public Object instance(final DependencyGraph.Traversal traversal) {
            final Deferred.Reference<Object> delegate = Deferred.reference(new Deferred.Factory<Object>() {
                public Object create() {
                    if (deferring.compareAndSet(null, error)) {
                        try {
                            try {
                                return node.get().instance(traversal);
                            } catch (final CircularReferencesException e) {
                                e.failing(context);
                                throw e;
                            }
                        } finally {
                            deferring.set(null);
                        }
                    } else {
                        error.failing(context);
                        throw error;
                    }
                }
            });

            return Proxies.create(context.api, new InvocationHandler() {
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

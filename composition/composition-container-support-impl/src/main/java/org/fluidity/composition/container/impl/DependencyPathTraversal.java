/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.DependencyPath;
import org.fluidity.composition.container.ContextDefinition;
import org.fluidity.composition.container.ResolvedNode;
import org.fluidity.composition.container.spi.DependencyGraph;
import org.fluidity.foundation.Lists;
import org.fluidity.foundation.Strings;

/**
 * Detects and handles circular reference when possible.
 * <p>
 * This object keeps state, do not use it concurrently or if its API method terminates abnormally (i.e., throws an exception).
 *
 * @author Tibor Varga
 */
final class DependencyPathTraversal implements DependencyGraph.Traversal {

    private final ComponentContainer.Observer observer;
    private final AtomicReference<ActualPath> resolutionPath;

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
     * @author Tibor Varga
     */
    private interface Descent {

        /**
         * Performs the descent.
         *
         * @return whatever the caller of {@link DependencyPathTraversal#descend(DependencyPathTraversal.ActualPath, DependencyPathTraversal.Descent)} expects.
         */
        DependencyGraph.Node perform();
    }

    private DependencyGraph.Node descend(final ActualPath path, final Descent command) {
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

        final ActualPath path = resolutionPath.get().descend(new ActualElement(type, identity, context), false);

        if (path.repeating) {
            if (observer != null) {
                observer.circular(path);
            }

            throw new ComponentContainer.CircularReferencesException(api, path);
        }

        return descend(path, new Descent() {
            public DependencyGraph.Node perform() {
                final DependencyGraph.Node resolved = reference.resolve();
                assert resolved != null : api;

                if (observer != null) {
                    observer.resolved(path, resolved.type());
                }

                return new DelegatingNode(path.tail, resolved);
            }
        });
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
        resolutionPath.get().tail.type = type;
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

    @Override
    public ComponentContainer.Observer observer() {
        return observer;
    }

    Object instantiate(final DependencyGraph.Node node, final ActualElement element, final DependencyGraph.Traversal traversal) {
        final ActualPath path = resolutionPath.get().descend(element, true);

        final DependencyGraph.Node resolved = descend(path, new Descent() {
            public DependencyGraph.Node perform() {
                try {
                    return new ResolvedNode(node.type(), node.instance(traversal), node.context());
                } catch (final ComponentContainer.InjectionException e) {
                    throw e;
                } catch (final Exception e) {
                    throw new ComponentContainer.InstantiationException(path, e);
                }
            }
        });

        return resolved.instance(traversal);
    }

    /**
     * Maintains a potentially circular dependency path.
     *
     * @author Tibor Varga
     */
    private static class ActualPath implements DependencyPath {

        public final ActualElement tail;
        public final boolean repeating;

        private final List<ActualElement> list = new ArrayList<ActualElement>();
        private final Map<ActualElement, ActualElement> map = new HashMap<ActualElement, ActualElement>();

        private ActualPath() {
            this.repeating = false;
            this.tail = null;
        }

        ActualPath(final List<ActualElement> list, final Map<ActualElement, ActualElement> map, final ActualElement tail) {
            this.list.addAll(list);
            this.map.putAll(map);
            this.repeating = map.containsKey(tail);

            if (!this.repeating) {
                this.map.put(tail, tail);
            }

            this.tail = this.map.get(tail);
            assert this.tail != null : this.map.keySet();

            this.list.add(tail);
        }

        public ActualPath descend(final ActualElement element, final boolean replace) {
            if (replace && map.remove(element) != null) {
                map.put(element, element);
            }

            return new ActualPath(list, map, element);
        }

        public Element tail() {
            return tail;
        }

        public List<Element> path() {
            return new ArrayList<Element>(list);
        }

        @Override
        public String toString() {
            return toString(true);
        }

        public String toString(final boolean api) {
            final Lists.Delimited text = Lists.delimited();

            for (final ActualElement type : list) {
                @SuppressWarnings("MismatchedQueryAndUpdateOfStringBuilder")
                final StringBuilder builder = text.next();

                if (!type.definition.isEmpty()) {
                    builder.append(type.definition).append(' ');
                }

                builder.append(Strings.formatClass(true, true, api ? type.api() : type.type()));
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

        ActualElement(final Class<?> api, final Object identity, final ContextDefinition definition) {
            this.api = api;
            this.identity = identity;
            this.definition = definition;
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

        public Class<?> api() {
            return api;
        }

        public Class<?> type() {
            return type == null ? api : type;
        }
    }

    /**
     * @author Tibor Varga
     */
    private class DelegatingNode implements DependencyGraph.Node {

        private final DependencyGraph.Node node;
        private final ActualElement element;

        public DelegatingNode(final ActualElement element, final DependencyGraph.Node node) {
            this.element = element;
            this.node = node;
        }

        public Class<?> type() {
            return node.type();
        }

        public Object instance(final DependencyGraph.Traversal traversal) {
            return instantiate(node, element, traversal);
        }

        public ComponentContext context() {
            return node.context();
        }
    }
}

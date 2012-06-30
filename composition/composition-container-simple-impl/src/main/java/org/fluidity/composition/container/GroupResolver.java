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

package org.fluidity.composition.container;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.DependencyPath;
import org.fluidity.composition.container.spi.DependencyGraph;

/**
 * Component mapping for a component group.
 * <p/>
 * TODO: mention that the instantiation ordering of self referencing group members cannot be established before injecting the group array
 *
 * @author Tibor Varga
 */
final class GroupResolver {

    private final Class<?> api;
    private final Set<Class<?>> members = new LinkedHashSet<Class<?>>();

    private volatile int index;     // volatility only guarantees visibility; synchronization is used to guarantee atomicity of mutation
    private final Map<Class<?>, Integer> instantiated = new ConcurrentHashMap<Class<?>, Integer>(64);

    private final ComponentContainer.Observer observer = new ComponentContainer.Observer() {
        public void descending(final Class<?> declaringType,
                               final Class<?> dependencyType,
                               final Annotation[] typeAnnotations,
                               final Annotation[] referenceAnnotations) {
            // ignore
        }

        public void ascending(final Class<?> declaringType, final Class<?> dependencyType) {
            // ignore
        }

        public void circular(final DependencyPath path) {
            // ignore
        }

        public void resolved(final DependencyPath path, final Class<?> type) {
            // ignore
        }

        public void instantiated(final DependencyPath path, final AtomicReference<?> ignored) {
            final Class<?> type = path.head().type();

            if (api.isAssignableFrom(type)) {
                synchronized (instantiated) {       // makes sure index is incremented only once for each instantiated group member class
                    if (!instantiated.containsKey(type)) {
                        instantiated.put(type, index++);
                    }
                }
            }
        }
    };

    private final Comparator<Object> order = new Comparator<Object>() {
        public int compare(final Object o1, final Object o2) {
            final Integer order1 = instantiated.get(o1.getClass());
            final Integer order2 = instantiated.get(o2.getClass());

            if (order1 == null && order2 == null) {
                return 0;
            } else if (order1 == null) {
                return 1;
            } else if (order2 == null) {
                return -1;
            } else {
                return order1.compareTo(order2);
            }
        }
    };

    public GroupResolver(final Class<?> api) {
        this.api = api;
    }

    public ComponentContainer.Observer observer() {
        return observer;
    }

    public interface Node {

        Collection<?> instance(DependencyGraph.Traversal traversal);
    }

    public Node resolve(final ParentContainer domain,
                        final DependencyGraph.Traversal traversal,
                        final SimpleContainer container,
                        final ContextDefinition context,
                        final Type reference) {
        final List<DependencyGraph.Node> nodes = new ArrayList<DependencyGraph.Node>();

        final List<ContextDefinition> consumed = new ArrayList<ContextDefinition>();
        for (final Class<?> member : members) {
            final ContextDefinition copy = context.advance(member);

            final ComponentResolver resolver = container.resolver(member, false);
            nodes.add(container.resolveComponent(domain, false, member, copy.accept(resolver.contextConsumer()), traversal, reference));

            consumed.add(copy);
        }

        context.collect(consumed);

        return new Node() {
            public Collection<?> instance(final DependencyGraph.Traversal traversal) {
                final List<Object> instances = new ArrayList<Object>();
                final DependencyGraph.Traversal observed = traversal.observed(observer);

                for (final DependencyGraph.Node node : nodes) {
                    instances.add(node.instance(observed));
                }

                Collections.sort(instances, order);

                return instances;
            }
        };
    }

    public void addResolver(final Class<?> api) {
        members.add(api);
    }
}

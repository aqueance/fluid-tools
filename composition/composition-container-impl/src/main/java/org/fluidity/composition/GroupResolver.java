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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.spi.ComponentResolutionObserver;
import org.fluidity.composition.spi.DependencyPath;

/**
 * Component mapping for a component group.
 *
 * @author Tibor Varga
 */
final class GroupResolver {

    private final Class<?> api;
    private final Set<Class<?>> members = new LinkedHashSet<Class<?>>();

    private final AtomicInteger index = new AtomicInteger(0);
    private final Map<Class<?>, Integer> instantiated = new HashMap<Class<?>, Integer>();

    private final ComponentResolutionObserver observer = new ComponentResolutionObserver() {
        public void resolving(final Class<?> api, final Class<?> declaringType,
                              final Class<?> dependencyType,
                              final Annotation[] typeAnnotations,
                              final Annotation[] referenceAnnotations) {
            // ignore
        }

        public void resolved(final DependencyPath path, final Class<?> type) {
            // ignore
        }

        public void instantiated(final DependencyPath path, final AtomicReference<?> ignored) {
            synchronized (instantiated) {
                final Class<?> type = path.head().type();
                if (api.isAssignableFrom(type) && !instantiated.containsKey(type)) {
                    instantiated.put(type, index.getAndIncrement());
                }
            }
        }
    };

    private final Comparator<Object> order = new Comparator<Object>() {
        public int compare(final Object o1, final Object o2) {
            return instantiated.get(o1.getClass()).compareTo(instantiated.get(o2.getClass()));
        }
    };

    public GroupResolver(final Class<?> api) {
        this.api = api;
    }

    public ComponentResolutionObserver observer() {
        return observer;
    }

    public interface Node {

        Collection<?> instance(DependencyGraph.Traversal traversal);
    }

    public Node resolve(final ParentContainer domain, final DependencyGraph.Traversal traversal, final SimpleContainer container, final ContextDefinition context) {
        final List<DependencyGraph.Node> nodes = new ArrayList<DependencyGraph.Node>();

        final List<ContextDefinition> consumed = new ArrayList<ContextDefinition>();
        for (final Class<?> member : members) {
            final ContextDefinition copy = context.copy();

            final ComponentResolver resolver = container.resolver(member, false);
            nodes.add(container.resolveComponent(domain, false, member, copy.reduce(resolver.acceptedContext()), traversal));

            consumed.add(copy);
        }

        context.collect(consumed);

        return new Node() {
            public Collection<?> instance(final DependencyGraph.Traversal traversal) {
                final List<Object> instances = new ArrayList<Object>();

                for (final DependencyGraph.Node node : nodes) {
                    instances.add(node.instance(traversal.observed(observer)));
                }

                assert instantiated.size() >= instances.size() : String.format("%s: %d != #%s", api, instances.size(), instantiated);
                Collections.sort(instances, order);

                return instances;
            }
        };
    }

    public void addResolver(final Class<?> api) {
        members.add(api);
    }
}

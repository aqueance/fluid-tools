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

package org.fluidity.composition.container;

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
import java.util.function.Function;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.DependencyPath;
import org.fluidity.composition.container.spi.DependencyGraph;

/**
 * Component mapping for a component group.
 *
 * @author Tibor Varga
 */
final class GroupResolver {

    private final Class<?> api;
    private final Set<Class<?>> members = new LinkedHashSet<>();

    private volatile int index;     // volatility only guarantees visibility; synchronization is used to guarantee atomicity of mutation
    private final Map<Class<?>, Integer> instantiated = new ConcurrentHashMap<>(64);

    private final ComponentContainer.Observer observer = new ComponentContainer.ObserverSupport() {
        public void instantiated(final DependencyPath path, final AtomicReference<?> ignored) {
            final Class<?> type = path.tail().type();

            if (api.isAssignableFrom(type)) {
                synchronized (instantiated) {       // makes sure index is incremented only once for each instantiated group member class
                    if (!instantiated.containsKey(type)) {
                        instantiated.put(type, index++);
                    }
                }
            }
        }
    };

    private final Comparator<Object> order = (o1, o2) -> {
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
    };

    GroupResolver(final Class<?> api) {
        this.api = api;
    }

    ComponentContainer.Observer observer() {
        return observer;
    }

    public Function<DependencyGraph.Traversal, Collection> resolve(final ParentContainer domain,
                                                                   final DependencyGraph.Traversal traversal,
                                                                   final SimpleContainer container,
                                                                   final ContextDefinition context,
                                                                   final Type reference) {
        final List<DependencyGraph.Node> nodes = new ArrayList<>();

        final List<ContextDefinition> consumed = new ArrayList<>();
        for (final Class<?> member : members) {
            final ContextDefinition copy = context.advance(member, true);

            final ComponentResolver resolver = container.resolver(member, false);
            nodes.add(container.resolveComponent(domain, false, member, copy.accept(resolver.contextConsumer()), traversal, reference));

            consumed.add(copy);
        }

        context.collect(consumed);

        return _traversal -> {
            final List<Object> instances = new ArrayList<>();
            final DependencyGraph.Traversal observed = _traversal.observed(observer);

            for (final DependencyGraph.Node node : nodes) {
                instances.add(node.instance(observed));
            }

            Collections.sort(instances, order);

            return instances;
        };
    }

    void addResolver(final Class<?> api) {
        members.add(api);
    }
}

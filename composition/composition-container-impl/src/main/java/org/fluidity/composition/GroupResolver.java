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
        public void resolved(final DependencyPath path, final Class<?> type) {
            // ignore
        }

        public void instantiated(final DependencyPath path) {
            synchronized (instantiated) {
                final Class<?> type = path.head(false);
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

    public static interface Node {

        Collection<?> instance(DependencyGraph.Traversal traversal);
    }

    public Node resolve(final DependencyGraph.Traversal traversal, final SimpleContainer container, final ContextDefinition context) {
        final List<DependencyGraph.Node> nodes = new ArrayList<DependencyGraph.Node>();

        final List<ContextDefinition> consumed = new ArrayList<ContextDefinition>();
        for (final Class<?> member : members) {
            final ContextDefinition copy = context.copy();

            final ComponentResolver resolver = container.resolver(member, false);
            nodes.add(container.resolveComponent(false, member, copy.reduce(resolver.acceptedContext()), traversal));

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

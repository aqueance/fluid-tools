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
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fluidity.composition.spi.ComponentResolutionObserver;
import org.fluidity.composition.spi.DependencyPath;

/**
 * Component mapping for a component group.
 *
 * @author Tibor Varga
 */
final class GroupResolver {

    private Set<Class<?>> members = new LinkedHashSet<Class<?>>();
    private volatile Set<Class<?>> sorted;

    public static interface Node {

        Collection<?> instance();
    }

    public Node resolve(final DependencyGraph.Traversal traversal, final SimpleContainer container, final ContextDefinition context) {

        // make sure we perform the static part of the component resolution of group members
        final List<DependencyGraph.Node> staticOrder = staticResolution(traversal, container, context);

        return new Node() {
            public Collection<?> instance() {
                Set<Class<?>> cache = sorted;

                if (cache == null) {
                    synchronized (this) {
                        cache = sorted;

                        if (cache == null) {
                            @SuppressWarnings("UnusedAssignment")   // IntelliJ IDEA fails to recognize the use of cache below as use of this assignment value
                            final Set<Class<?>> local = cache = new LinkedHashSet<Class<?>>();

                            // list of types resolved while instantiating a group member, i.e., resolved from within its constructor
                            final List<Class<?>> dynamic = new ArrayList<Class<?>>();

                            final Map<Class<?>, DependencyGraph.Node> map = new HashMap<Class<?>, DependencyGraph.Node>();
                            for (final DependencyGraph.Node node : staticOrder) {
                                map.put(node.type(), node);
                            }

                            // observer to collect dynamically resolved group members
                            final ComponentResolutionObserver observer = new ComponentResolutionObserver() {
                                public void resolved(final DependencyPath path, final Class<?> type) {
                                    if (map.containsKey(type)) {
                                        final Set<Class<?>> set = new HashSet<Class<?>>(path.path());

                                        int index = 0;
                                        for (final Class<?> previous : dynamic) {
                                            if (previous == type) return;
                                            if (set.contains(previous)) break;
                                            ++index;
                                        }

                                        dynamic.add(index, type);
                                    }
                                }
                            };

                            // perform dynamic resolution and adjust the order based on dynamic component dependencies
                            return container.observe(observer, new SimpleContainer.Observed<Collection<?>>() {
                                public Collection<?> run(final DependencyGraph.Traversal traversal) {
                                    final Map<DependencyGraph.Node, Object> instantiated = new LinkedHashMap<DependencyGraph.Node, Object>();

                                    for (final DependencyGraph.Node node : staticOrder) {
                                        if (!instantiated.containsKey(node)) {

                                            // clear the dynamic resolution collection
                                            dynamic.clear();

                                            // fill the collection
                                            final Object instance = node.instance();

                                            // instantiate items in the collection before the one that triggered their resolution
                                            // they have already been instantiated so no further dynamic resolution is expected
                                            for (final Class<?> type : dynamic) {
                                                final DependencyGraph.Node next = map.remove(type);
                                                assert next != null : type;

                                                try {
                                                    instantiated.put(next, next.instance());
                                                } catch (final ConcurrentModificationException e) {
                                                    throw new ComponentContainer.ResolutionException( "Set of dynamic dependencies of %s changed from one instance to the next", type);
                                                }

                                                local.add(type);
                                            }

                                            // process the one that triggered the resolutions
                                            instantiated.put(node, instance);
                                            local.add(node.type());
                                        }
                                    }

                                    sorted = local;

                                    return instantiated.values();
                                }
                            });
                        }
                    }
                }

                final List<Object> dynamicOrder = new ArrayList<Object>();

                for (final Class<?> member : cache) {
                    dynamicOrder.add(container.resolver(member, false).resolve(traversal, container, context).instance());
                }

                return dynamicOrder;
            }
        };
    }

    // returns node ordered according to the static dependencies among the group members
    private List<DependencyGraph.Node> staticResolution(final DependencyGraph.Traversal traversal,
                                                        final SimpleContainer container,
                                                        final ContextDefinition context) {
        final List<DependencyGraph.Node> list = new ArrayList<DependencyGraph.Node>();

        // maps node types to nodes
        final Map<Class<?>, DependencyGraph.Node> map = new HashMap<Class<?>, DependencyGraph.Node>();

        // contains the types in reference order
        final Set<Class<?>> sequence = new LinkedHashSet<Class<?>>();

        // collects the types in reference order
        final DependencyGraph.Traversal observed = traversal.observed(new ComponentResolutionObserver() {
            public void resolved(final DependencyPath path, final Class<?> type) {
                if (members.contains(type)) {
                    sequence.add(type);
                }
            }
        });

        // resolves each member and fills the node type to node mapping
        for (final Class<?> member : members) {
            final DependencyGraph.Node node = container.resolver(member, false).resolve(observed, container, context);
            map.put(node.type(), node);
        }

        // we should have the same element in possibly different order
        assert sequence.equals(members);

        // fill the list of nodes in reference order
        for (final Class<?> type : sequence) {
            assert map.containsKey(type) : type;
            list.add(map.get(type));
        }

        return list;
    }

    public void add(final Class<?> implementation) {
        members.add(implementation);
    }
}

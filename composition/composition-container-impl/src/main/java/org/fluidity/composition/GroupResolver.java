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

import java.util.HashMap;
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

    public void resolve(final DependencyGraph.Traversal traversal, final SimpleContainer container, final ContextDefinition context, final List<DependencyGraph.Node> list) {
        Set<Class<?>> cache = sorted;

        if (cache == null) {
            final Map<Class<?>, DependencyGraph.Node> map = new HashMap<Class<?>, DependencyGraph.Node>();

            synchronized (this) {
                cache = sorted;

                if (cache == null) {
                    final Set<Class<?>> local = cache = new LinkedHashSet<Class<?>>();

                    final DependencyGraph.Traversal observed = traversal.observed(new ComponentResolutionObserver() {
                        public void resolved(final DependencyPath path, final Class<?> type) {
                            if (members.contains(type)) {
                                local.add(type);
                            }
                        }
                    });

                    for (final Class<?> member : members) {
                        final DependencyGraph.Node node = container.resolver(member, false).resolve(observed, container, context);
                        map.put(node.type(), node);
                    }

                    assert !map.isEmpty();
                    assert cache.equals(members);
                    sorted = cache;
                    members = null;
                }
            }

            if (!map.isEmpty()) {
                for (final Class<?> type : cache) {
                    assert map.containsKey(type) : type;
                    list.add(map.get(type));
                }

                return;
            }
        }

        for (final Class<?> member : cache) {
            list.add(container.resolver(member, false).resolve(traversal, container, context));
        }
    }

    public void add(final Class<?> implementation) {
        members.add(implementation);
    }
}

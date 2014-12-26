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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.container.spi.DependencyGraph;

/**
 * Common functionality for component resolvers.
 *
 * @author Tibor Varga
 */
abstract class AbstractResolver implements ComponentResolver {

    private final int priority;
    protected final Class<?> api;
    protected final ComponentCache cache;

    private final Collection<Class<?>> groups = new HashSet<Class<?>>();

    protected AbstractResolver(final int priority, final Class<?> api, final ComponentCache cache) {
        this.priority = priority;
        this.api = api;
        this.cache = cache;
    }

    public int priority() {
        return priority;
    }

    public boolean replaces(final ComponentResolver resolver) {
        final int check = resolver.priority();

        if (check == priority) {
            final boolean thisFactory = this instanceof FactoryResolver;
            final boolean thatFactory = resolver instanceof FactoryResolver;

            if (thisFactory && !thatFactory) {
                return true;
            } else if (!thisFactory && thatFactory) {
                return false;
            } else {
                throw new ComponentContainer.BindingException("Component %s bound twice: %s and %s", api, resolver, this);
            }
        } else {
            return check < priority;
        }
    }

    public void resolverReplaced(final Class<?> api, final ComponentResolver previous, final ComponentResolver replacement) {
        // empty
    }

    public final void addGroups(final Collection<Class<?>> groups) {
        this.groups.addAll(groups);
    }

    public final Collection<Class<?>> groups() {
        return Collections.unmodifiableCollection(groups);
    }

    public Object cached(final ComponentCache.Domain domain, final String source, final ComponentContext context) {
        return cache == null ? null : cache.lookup(domain, source, context, api, null);
    }

    protected final CachingNode cachingNode(final ComponentCache.Domain container, final DependencyGraph.Node node) {
        return new CachingNode(container, node);
    }

    protected final ParentContainer resolver(final ParentContainer domain, final ParentContainer container) {
        return domain == null ? container : domain;
    }

    private class CachingNode implements DependencyGraph.Node {

        private final ComponentCache.Domain container;
        private final DependencyGraph.Node node;

        CachingNode(final ComponentCache.Domain container, final DependencyGraph.Node node) {
            assert cache != null : api;
            assert node != null : api;
            assert container != null : api;

            this.container = container;
            this.node = node;
        }

        public Class<?> type() {
            return node.type();
        }

        public Object instance(final DependencyGraph.Traversal traversal) {
            return cache.lookup(container, container.toString(), node.context(), api, new ComponentCache.Entry() {
                public Object create() {
                    return node.instance(traversal);
                }
            });
        }

        public ComponentContext context() {
            return node.context();
        }
    }
}

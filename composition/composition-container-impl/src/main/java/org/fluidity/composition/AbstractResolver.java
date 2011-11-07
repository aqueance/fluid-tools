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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.fluidity.foundation.Log;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Common functionality for component resolvers.
 *
 * @author Tibor Varga
 */
abstract class AbstractResolver implements ComponentResolver {

    private final int priority;
    protected final Log log;
    protected final Class<?> api;
    protected final ComponentCache cache;

    private final Collection<Class<?>> groups = new HashSet<Class<?>>();

    protected AbstractResolver(final int priority, final Class<?> api, final ComponentCache cache, final LogFactory logs) {
        this.priority = priority;
        this.api = api;
        this.cache = cache;
        this.log = logs.createLog(getClass());
    }

    public int priority() {
        return priority;
    }

    public boolean replaces(final ComponentResolver resolver) {
        final int check = resolver.priority();

        if (check == priority) {
            throw new ComponentContainer.BindingException("Component %s already bound", api);
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

    public boolean isVariantMapping() {
        return false;
    }

    public boolean isInstanceMapping() {
        return false;
    }

    protected final CachingNode cachingNode(final ParentContainer domain, final DependencyGraph.Node node, final SimpleContainer container) {
        return new CachingNode(domain, node, container);
    }

    public static Set<Class<? extends Annotation>> acceptedContext(final Class<?> type) {
        final Component.Context annotation = type.getAnnotation(Component.Context.class);
        return annotation == null ? null : new HashSet<Class<? extends Annotation>>(Arrays.asList(annotation.value()));
    }

    private class CachingNode implements DependencyGraph.Node {

        private final ParentContainer domain;
        private final DependencyGraph.Node node;
        private final SimpleContainer container;

        public CachingNode(final ParentContainer domain, final DependencyGraph.Node node, final SimpleContainer container) {
            this.domain = domain;
            this.node = node;
            this.container = container;
        }

        public Class<?> type() {
            return node.type();
        }

        public Object instance(final DependencyGraph.Traversal traversal) {
            final ComponentContext context = node.context();

            return cache.lookup(domain, container, context, api, new ComponentCache.Instantiation() {
                public Object instantiate() {
                    return node.instance(traversal);
                }

            });
        }

        public ComponentContext context() {
            return node.context();
        }
    }
}

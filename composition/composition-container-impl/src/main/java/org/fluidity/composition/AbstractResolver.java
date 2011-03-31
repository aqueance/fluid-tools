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

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.fluidity.foundation.logging.Log;
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

    public boolean isVariantMapping() {
        return false;
    }

    public boolean isInstanceMapping() {
        return false;
    }

    protected final CachingNode cachingNode(final DependencyGraph.Node node, final SimpleContainer container) {
        return new CachingNode(node, container);
    }

    public static Set<Class<? extends Annotation>> acceptedContext(final Class<?> type) {
        final Context annotation = type.getAnnotation(Context.class);
        return annotation == null ? null : new HashSet<Class<? extends Annotation>>(Arrays.asList(annotation.value()));
    }

    private class CachingNode implements DependencyGraph.Node {

        private final DependencyGraph.Node node;
        private final SimpleContainer container;

        public CachingNode(final DependencyGraph.Node node, final SimpleContainer container) {
            this.node = node;
            this.container = container;
        }

        public Class<?> type() {
            return node.type();
        }

        public Object instance() {
            return cache.lookup(container, node.context(), api, new ComponentCache.Instantiation() {
                public Object perform() {
                    return node.instance();
                }
            });
        }

        public ComponentContext context() {
            return node.context();
        }
    }
}

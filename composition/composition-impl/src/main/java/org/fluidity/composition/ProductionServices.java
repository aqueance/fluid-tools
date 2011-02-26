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

import org.fluidity.composition.spi.ComponentResolutionObserver;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Production services for a container implementation.
 *
 * @author Tibor Varga
 */
final class ProductionServices implements ContainerServices {

    private final LogFactory logs;
    private final ClassDiscovery classDiscovery;
    private final DependencyInjector dependencyInjector;
    private final DependencyGraph.Traversal.Strategy strategy;

    public ProductionServices(final LogFactory logs, final DependencyGraph.Traversal.Strategy strategy) {
        this.logs = logs;
        this.classDiscovery = new ClassDiscoveryImpl(logs);
        this.dependencyInjector = new DependencyInjectorImpl();

        this.strategy = strategy != null ? strategy : new DependencyGraph.Traversal.Strategy() {
            public DependencyGraph.Node advance(final DependencyGraph graph,
                                                final ContextDefinition context,
                                                final DependencyGraph.Traversal traversal,
                                                final boolean repeating,
                                                final DependencyGraph.Traversal.Trail trail) {
                return trail.advance();
            }
        };
    }

    public ContextDefinition emptyContext() {
        return new ContextDefinitionImpl();
    }

    public ClassDiscovery classDiscovery() {
        return classDiscovery;
    }

    public DependencyInjector dependencyInjector() {
        return dependencyInjector;
    }

    public DependencyGraph.Traversal graphTraversal() {
        return graphTraversal(null);
    }

    public DependencyGraph.Traversal graphTraversal(final ComponentResolutionObserver observer) {
        return new DependencyPathTraversal(strategy, observer);
    }

    public LogFactory logs() {
        return logs;
    }

    public ComponentCache newCache(final boolean stateless) {
        return new ComponentCacheImpl(logs, stateless);
    }
}

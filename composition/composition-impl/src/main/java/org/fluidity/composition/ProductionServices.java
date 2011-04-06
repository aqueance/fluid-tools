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

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

package org.fluidity.composition.impl;

import org.fluidity.composition.ClassDiscovery;
import org.fluidity.composition.spi.ComponentCache;
import org.fluidity.composition.spi.ComponentResolutionObserver;
import org.fluidity.composition.spi.ContainerServices;
import org.fluidity.composition.spi.ContextDefinition;
import org.fluidity.composition.spi.DependencyGraph;
import org.fluidity.composition.spi.DependencyInjector;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Production services for a container implementation.
 *
 * @author Tibor Varga
 */
final class ProductionServices implements ContainerServices {

    private final LogFactory logs;
    private final ClassDiscovery discovery;
    private final DependencyInjector injector;

    public ProductionServices(final LogFactory logs) {
        this.logs = logs;
        this.discovery = new ClassDiscoveryImpl(logs);
        this.injector = new DependencyInjectorImpl();
    }

    public ContextDefinition emptyContext() {
        return new ContextDefinitionImpl();
    }

    public ClassDiscovery classDiscovery() {
        return discovery;
    }

    public DependencyInjector dependencyInjector() {
        return injector;
    }

    public DependencyGraph.Traversal graphTraversal() {
        return graphTraversal(null);
    }

    public DependencyGraph.Traversal graphTraversal(final ComponentResolutionObserver observer) {
        return new DependencyPathTraversal(injector, observer);
    }

    public LogFactory logs() {
        return logs;
    }

    public ComponentCache newCache(final boolean stateless) {
        return new ComponentCacheImpl(logs, stateless);
    }
}

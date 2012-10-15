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

package org.fluidity.composition.container.impl;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.container.ComponentCache;
import org.fluidity.composition.container.ContainerServices;
import org.fluidity.composition.container.ContextDefinition;
import org.fluidity.composition.container.DependencyInjector;
import org.fluidity.composition.container.spi.DependencyGraph;
import org.fluidity.foundation.ClassDiscovery;
import org.fluidity.foundation.Log;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Production services for a container implementation. Since there is no dependency injection container available for this class, direction instantiations are
 * unavoidable.
 *
 * @author Tibor Varga
 */
final class ProductionServices implements ContainerServices {

    private final LogFactory logs;
    private final ClassDiscovery discovery;
    private final DependencyInjector injector;
    private final Log<ComponentCacheImpl> cacheLog;

    ProductionServices(final LogFactory logs) {
        this.logs = logs;
        this.cacheLog = logs.createLog(ComponentCacheImpl.class);
        this.discovery = new ClassDiscoveryImpl(logs.createLog(ClassDiscoveryImpl.class));
        this.injector = new DependencyInjectorImpl(new DependencyInterceptorsImpl(new InterceptorFilterImpl(), logs.createLog(DependencyInterceptorsImpl.class)));
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

    public DependencyGraph.Traversal graphTraversal(final ComponentContainer.Observer observer) {
        return new DependencyPathTraversal(observer);
    }

    public ComponentContainer.Observer aggregateObserver(final ComponentContainer.Observer... observers) {
        return CompositeObserver.combine(observers);
    }

    public ComponentCache newCache(final boolean stateless) {
        return new ComponentCacheImpl(cacheLog, stateless);
    }

    public Log createLog(final Log log, final Class<?> source) {
        return log == null ? logs.createLog(source) : log;
    }
}

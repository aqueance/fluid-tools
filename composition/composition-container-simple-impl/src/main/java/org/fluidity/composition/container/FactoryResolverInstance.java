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

package org.fluidity.composition.container;

import java.lang.reflect.Type;

import org.fluidity.composition.container.api.ComponentCache;
import org.fluidity.composition.container.api.ContextDefinition;
import org.fluidity.composition.container.spi.DependencyGraph;
import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.composition.spi.CustomComponentFactory;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Component mapping of a {@link CustomComponentFactory} instance.
 *
 * @author Tibor Varga
 */
final class FactoryResolverInstance extends FactoryResolver {

    private final CustomComponentFactory factory;

    public FactoryResolverInstance(final int priority, final Class<?> api, final CustomComponentFactory factory, final ComponentCache cache, final LogFactory logs) {
        super(priority, api, factory.getClass(), cache, logs);
        this.factory = factory;
    }

    @Override
    protected ComponentFactory factory(final SimpleContainer container,
                                       final DependencyGraph.Traversal traversal,
                                       final ContextDefinition definition,
                                       final Type reference) {
        return factory;
    }
}
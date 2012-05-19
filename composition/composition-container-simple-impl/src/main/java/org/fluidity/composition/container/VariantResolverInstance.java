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

import java.lang.reflect.Type;

import org.fluidity.composition.container.spi.DependencyGraph;
import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.composition.spi.ComponentVariantFactory;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Component mapping for a {@link ComponentVariantFactory} instance.
 *
 * @author Tibor Varga
 */
final class VariantResolverInstance extends VariantResolver {

    private final ComponentVariantFactory factory;

    public VariantResolverInstance(final int priority,
                                   final SimpleContainer container,
                                   final Class<?> api,
                                   final ComponentVariantFactory factory,
                                   final ComponentCache cache,
                                   final LogFactory logs) {
        super(priority, container, api, factory.getClass(), cache, logs);
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

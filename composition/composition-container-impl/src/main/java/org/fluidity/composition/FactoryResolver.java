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
import java.util.Set;

import org.fluidity.composition.spi.ComponentCache;
import org.fluidity.composition.spi.ContextDefinition;
import org.fluidity.composition.spi.CustomComponentFactory;
import org.fluidity.composition.spi.DependencyGraph;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Component resolver for a {@link CustomComponentFactory} component.
 *
 * @author Tibor Varga
 */
abstract class FactoryResolver extends AbstractFactoryResolver {

    public FactoryResolver(final int priority,
                           final Class<?> api,
                           final Class<? extends CustomComponentFactory> factoryClass,
                           final ComponentCache cache,
                           final LogFactory logs) {
        super(factoryClass, priority, api, cache, logs);
    }

    public Set<Class<? extends Annotation>> acceptedContext() {
        return AbstractResolver.acceptedContext(factoryClass());
    }

    public Annotation[] providedContext() {
        return null;
    }

    public DependencyGraph.Node resolve(final ParentContainer domain,
                                        final DependencyGraph.Traversal traversal,
                                        final SimpleContainer container,
                                        final ContextDefinition context) {
        return resolve(domain, traversal, container, context, container.newChildContainer(false));
    }

    @Override
    public String toString() {
        return String.format("%s (via %s)", api.getName(), factoryClass().getName());
    }
}

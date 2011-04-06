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

import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Component resolver for a {@link ComponentFactory} component.
 *
 * @author Tibor Varga
 */
abstract class FactoryResolver extends AbstractResolver {

    private final Class<? extends ComponentFactory> factoryClass;

    /**
     * Returns the {@link ComponentFactory} instance this is a mapping for.

     * @param container the container in which to resolve dependencies of the factory.
     * @param traversal the current graph traversal.
     *
     * @return the {@link ComponentFactory} instance this is a mapping for.
     */
    protected abstract ComponentFactory factory(final SimpleContainer container, final DependencyGraph.Traversal traversal);

    public FactoryResolver(final int priority,
                           final Class<?> api,
                           final Class<? extends ComponentFactory> factoryClass,
                           final ComponentCache cache,
                           final LogFactory logs) {
        super(priority, api, cache, logs);
        this.factoryClass = factoryClass;
    }

    public Set<Class<? extends Annotation>> acceptedContext() {
        return AbstractResolver.acceptedContext(factoryClass);
    }

    public Annotation[] annotations() {
        return null;
    }

    public final Class<? extends ComponentFactory> factoryClass() {
        return factoryClass;
    }

    public DependencyGraph.Node resolve(final DependencyGraph.Traversal traversal, final SimpleContainer container, final ContextDefinition context) {
        final SimpleContainer child = container.newChildContainer();
        factory(container, traversal).newComponent(new ComponentContainerShell(child, context, false), context.create());
        return cachingNode(child.resolveComponent(api, context, traversal), child);
    }

    @Override
    public String toString() {
        return String.format("%s (via %s)", api.getName(), factoryClass.getName());
    }
}

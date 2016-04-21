/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.container.spi.DependencyGraph;

/**
 * Component mapping for a pre-instantiated component.
 *
 * @author Tibor Varga
 */
final class InstanceResolver extends AbstractResolver {

    private final Object instance;
    private final Class<?> componentClass;

    InstanceResolver(final int priority, final Class<?> api, final Object instance) {
        super(priority, api, null);

        this.instance = instance;
        this.componentClass = this.instance.getClass();
    }

    public Annotation[] providedContext() {
        return null;
    }

    public Class<?> contextConsumer() {
        return null;
    }

    public DependencyGraph.Node resolve(final ParentContainer domain,
                                        final ParentContainer container,
                                        final DependencyGraph.Traversal traversal,
                                        final ContextDefinition context,
                                        final Type reference) {
        return new ResolvedNode(componentClass, instance, context.create());
    }

    @Override
    public Object cached(final ComponentCache.Domain domain, final String source, final ComponentContext context) {
        return instance;
    }

    @Override
    public String toString() {
        return String.format("%s (instance)", componentClass.getName());
    }
}

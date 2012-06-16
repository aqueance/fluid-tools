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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;

import org.fluidity.composition.container.spi.DependencyGraph;
import org.fluidity.foundation.Generics;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Component resolver that works by instantiating a class.
 *
 * @author Tibor Varga
 */
final class ConstructingResolver extends AbstractResolver {

    private final DependencyInjector injector;

    private final Class<?> componentClass;

    private final Constructor<?> constructor;
    private final boolean ignoreContext;

    public ConstructingResolver(final int priority,
                                final Class<?> api,
                                final Class<?> componentClass,
                                final boolean ignoreContext,
                                final ComponentCache cache,
                                final DependencyInjector injector,
                                final LogFactory logs) {
        super(priority, api, cache, logs);
        this.ignoreContext = ignoreContext;
        this.injector = injector;
        this.componentClass = componentClass;
        this.constructor = injector.findConstructor(componentClass);
    }

    public Annotation[] providedContext() {
        return ignoreContext ? null : componentClass.getAnnotations();
    }

    public Class<?> contextConsumer() {
        return ignoreContext ? null : componentClass;
    }

    public DependencyGraph.Node resolve(final ParentContainer domain,
                                        final DependencyGraph.Traversal traversal,
                                        final SimpleContainer container,
                                        final ContextDefinition context,
                                        final Type reference) {
        final Class<?> group = Generics.rawType(Generics.arrayComponentType(reference));

        return traversal.follow(ConstructingResolver.this, group != null && group.isAssignableFrom(api) ? group : api, api, context, new DependencyGraph.Node.Reference() {
            public DependencyGraph.Node resolve() {
                return cachingNode(domain,
                                   container,
                                   injector.constructor(api, traversal, container.dependencyResolver(domain), ConstructingResolver.this, context, constructor));
            }
        });
    }

    @Override
    public String toString() {
        return componentClass.getName();
    }
}

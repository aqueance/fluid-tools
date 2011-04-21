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
import java.lang.reflect.Constructor;
import java.util.Set;

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

    public Annotation[] annotations() {
        return ignoreContext ? null : componentClass.getAnnotations();
    }

    public Set<Class<? extends Annotation>> acceptedContext() {
        return ignoreContext ? null : AbstractResolver.acceptedContext(componentClass);
    }

    public DependencyGraph.Node resolve(final DependencyGraph.Traversal traversal, final SimpleContainer container, final ContextDefinition context) {
        return traversal.follow(container, context, new DependencyGraph.Node.Reference() {
            public Class<?> api() {
                return api;
            }

            public Annotation[] annotations() {
                return ConstructingResolver.this.annotations();
            }

            public DependencyGraph.Node resolve(final DependencyGraph.Traversal traversal, final ContextDefinition context) {
                return cachingNode(injector.constructor(traversal, container, ConstructingResolver.this, context, constructor), container);
            }
        });
    }

    @Override
    public String toString() {
        return componentClass.getName();
    }
}

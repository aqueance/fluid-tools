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

import org.fluidity.foundation.spi.LogFactory;

/**
 * Component mapping for a pre-instantiated component.
 *
 * @author Tibor Varga
 */
final class InstanceResolver extends AbstractResolver {

    private final Object instance;

    public InstanceResolver(final int priority, final Class<?> api, final Object instance, final LogFactory logs) {
        super(priority, api, null, logs);
        this.instance = instance;
    }

    public Annotation[] annotations() {
        return null;
    }

    public Set<Class<? extends Annotation>> acceptedContext() {
        return null;
    }

    public boolean isInstanceMapping() {
        return true;
    }

    public DependencyGraph.Node resolve(final DependencyGraph.Traversal traversal, final SimpleContainer container, final ContextDefinition context) {
        return traversal.follow(container, context, new DependencyGraph.Node.Reference() {
            public Class<?> api() {
                return api;
            }

            public DependencyGraph.Node resolve(final DependencyGraph.Traversal traversal, final ContextDefinition context) {
                return new DependencyGraph.Node.Constant(instance.getClass(), instance, context.create());
            }
        });
    }

    @Override
    public String toString() {
        return String.format("%s (instance)", instance.getClass().getName());
    }
}

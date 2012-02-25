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

package org.fluidity.composition.container.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.container.api.ContextDefinition;

/**
 * Capable of resolving component references. A dependency injection container implements this interface for a {@link org.fluidity.composition.container.api.DependencyInjector} to be able to use the
 * implementation.
 */
public interface DependencyResolver extends DependencyGraph {

    /**
     * Returns the context node for the given component interface.
     *
     * @param type    the component interface to return a context node for.
     * @param context the context prevalent at the reference.
     *
     * @return the context node for the given component API or <code>null</code> if not found.
     */
    ContextNode contexts(Class<?> type, ContextDefinition context);

    /**
     * Returns a new child container with its base context set to the given definition.
     *
     * @param context the context for the new container to use as base context.
     *
     * @return a new component container.
     */
    ComponentContainer container(ContextDefinition context);

    /**
     * Resolves a component group with the given traversal in the given context.
     *
     * @param api         the group interface.
     * @param context     the component context at the point of resolution.
     * @param traversal   the graph traversal to use.
     * @param annotations the annotations defined at the reference to the group interface being resolved.
     * @param reference   the parameterized type of the dependency reference.
     *
     * @return the resolved component group or <code>null</code> if none could be resolved.
     */
    Node resolveGroup(Class<?> api, ContextDefinition context, Traversal traversal, Annotation[] annotations, Type reference);

    /**
     * Checks if an instance has been cached for the given component interface and component context, and returns the instance of found. This method never
     * instantiates classes.
     *
     * @param api     the component interface to check.
     * @param context the component context to check.
     *
     * @return the instance if found cached, <code>null</code> otherwise.
     */
    Object cached(Class<?> api, ComponentContext context);
}

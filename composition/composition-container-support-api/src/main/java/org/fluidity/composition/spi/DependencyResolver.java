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

package org.fluidity.composition.spi;

import java.lang.annotation.Annotation;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ContextDefinition;
import org.fluidity.composition.DependencyGraph;

/**
 * Capable of resolving component references.
 */
public interface DependencyResolver extends DependencyGraph {

    /**
     * Returns the component mapping for the given component API.
     *
     * @param type    the component API to return a mapping for.
     * @param context the context prevalent at the reference.
     *
     * @return the component mapping for the given component API or <code>null</code> if not found.
     */
    ComponentMapping mapping(Class<?> type, ContextDefinition context);

    /**
     * Returns a new child container with its base context set to the given properties.
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
     *
     * @return the resolved component group or <code>null</code> if none could be resolved.
     */
    Node resolveGroup(Class<?> api, ContextDefinition context, Traversal traversal, Annotation[] annotations);
}

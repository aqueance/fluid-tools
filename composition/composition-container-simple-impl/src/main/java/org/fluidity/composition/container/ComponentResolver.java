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
import java.util.Collection;

import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.container.spi.ContextNode;
import org.fluidity.composition.container.spi.DependencyGraph;

/**
 * Extends the {@link ContextNode} interface with methods required by the actual container implementation.
 *
 * @author Tibor Varga
 */
interface ComponentResolver extends ContextNode {

    /**
     * Resolves the represented component.
     *
     * @param domain    the domain container.
     * @param container the container calling the resolver.
     * @param traversal the graph traversal to use.
     * @param context   the context in which the resolution takes place.
     * @param reference the parameterized type of the dependency reference.
     *
     * @return a node representing the component.
     */
    DependencyGraph.Node resolve(ParentContainer domain,
                                 ParentContainer container,
                                 DependencyGraph.Traversal traversal,
                                 ContextDefinition context,
                                 Type reference);

    /**
     * Returns the relative priority of this mapping compared to another.
     *
     * @return the relative priority of this mapping compared to another.
     */
    int priority();

    /**
     * Tells if the receiver replaces the supplied <code>resolver</code>.
     *
     * @param resolver the resolver that the receiver may need to replace.
     *
     * @return <code>true</code> if the supplied <code>resolver</code> is replaced by the receiver.
     */
    boolean replaces(ComponentResolver resolver);

    /**
     * Notifies the receiver that a previously bound resolver has been replaced by another one.
     *
     * @param api         the api for which the resolver is being replaced
     * @param previous    the old resolver.
     * @param replacement the new resolver.
     */
    void resolverReplaced(Class<?> api, ComponentResolver previous, ComponentResolver replacement);

    /**
     * Adds a list of group interfaces that the resolved component belongs to.
     *
     * @param groups list of group interfaces that the resolved component belongs to.
     */
    void addGroups(Collection<Class<?>> groups);

    /**
     * Returns the list of group interfaces that the resolved component belongs to.
     *
     * @return the list of group interfaces that the resolved component belongs to.
     */
    Collection<Class<?>> groups();

    /**
     * Checks if an instance has been cached for the given component context, and returns the instance of found. This method never instantiates classes.
     *
     * @param domain  the domain container.
     * @param source  identifies the container calling the resolver.
     * @param context the component context to check.
     *
     * @return the instance if found cached, <code>null</code> otherwise.
     */
    Object cached(ComponentCache.Domain domain, String source, ComponentContext context);
}

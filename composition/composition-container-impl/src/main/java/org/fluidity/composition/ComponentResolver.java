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

import java.lang.reflect.Type;
import java.util.Collection;

import org.fluidity.composition.spi.ContextDefinition;
import org.fluidity.composition.spi.ContextNode;
import org.fluidity.composition.spi.DependencyGraph;

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
     * @param traversal the graph traversal to use.
     * @param container the container calling the resolver.
     * @param context   the context in which the resolution takes place.
     * @param reference the parameterized type of the dependency reference.
     *
     * @return a node representing the component.
     */
    DependencyGraph.Node resolve(ParentContainer domain,
                                 DependencyGraph.Traversal traversal,
                                 SimpleContainer container,
                                 ContextDefinition context,
                                 Type reference);

    /**
     * Returns the relative priority of this mapping compared to another.
     *
     * @return the relative priority of this mapping compared to another.
     */
    int priority();

    /**
     * Tells whether the mapping is that of a {@link org.fluidity.composition.spi.ComponentVariantFactory}.
     *
     * @return <code>true</code> if the mapping is that of a {@link org.fluidity.composition.spi.ComponentVariantFactory}, <code>false</code> otherwise.
     */
    boolean isVariantMapping();

    /**
     * Tells whether this mapping has been created for an already instantiated component.
     *
     * @return <code>true</code> if this mapping represents an already instantiated component, <code>false</code> otherwise.
     */
    boolean isInstanceMapping();

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
     * If the resolver maintains a reference to the parent container of its containing one, this method notifies it that it should use the skip one level and
     * use the parent's parent instead.
     */
    void skipParent();

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
     * @param domain    the domain container.
     * @param container the container calling the resolver.
     * @param context   the component context to check.
     *
     * @return the instance if found cached, <code>null</code> otherwise.
     */
    Object cached(Object domain, Object container, ComponentContext context);
}

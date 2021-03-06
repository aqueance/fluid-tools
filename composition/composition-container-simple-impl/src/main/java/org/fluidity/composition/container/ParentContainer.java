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
import java.util.List;
import java.util.function.Function;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.container.spi.ContextNode;

/**
 * Interface to separate internal container methods from the higher level container interface.
 *
 * @author Tibor Varga
 */
interface ParentContainer extends SimpleContainer, ComponentCache.Domain {

    /**
     * Returns a context node either from the receiver or from its parent.
     *
     * @param domain  the domain container, or <code>null</code>.
     * @param type    the component interface.
     * @param context the component context.
     *
     * @return a resolver or <code>null</code> if not found.
     */
    ContextNode contexts(ParentContainer domain, Class<?> type, ContextDefinition context);

    /**
     * Resolves the group API to a list of implementations.
     *
     * @param domain    the domain container, or <code>null</code>.
     * @param api       the group API.
     * @param traversal the current graph traversal.
     * @param context   the current context.
     * @param reference the parameterized type of the dependency reference.
     *
     * @return a list of objects representing the group members in this container and its parent(s), if any, starting with those in the top level container
     *         and ending with those in this one.
     */
    List<Function<Traversal, Collection>> resolveGroup(ParentContainer domain, Class<?> api, Traversal traversal, ContextDefinition context, Type reference);

    /**
     * Resolves a component group with the given traversal in the given context.
     *
     * @param domain    the domain container, or <code>null</code>.
     * @param api       the group interface.
     * @param context   the component context at the point of resolution.
     * @param traversal the graph traversal to use.
     * @param reference the parameterized type of the dependency reference.
     *
     * @return the resolved component group or <code>null</code> if none could be resolved.
     */
    Node resolveGroup(ParentContainer domain, Class<?> api, ContextDefinition context, Traversal traversal, Type reference);

    /**
     * Returns the group resolver for the given interface, consulting the parent, if any, if not found in the container.
     *
     * @param api the group interface.
     *
     * @return the group resolver for the given interface; never <code>null</code>.
     */
    List<GroupResolver> groupResolvers(Class<?> api);

    /**
     * Checks if an instance has been cached for the given component interface and component context, and returns the instance if found. This method never
     * instantiates classes.
     *
     * @param domain  the domain container, or <code>null</code>.
     * @param api     the component interface to check.
     * @param context the component context to check.
     *
     * @return the instance if found cached, <code>null</code> otherwise.
     */
    Object cached(ParentContainer domain, Class<?> api, ComponentContext context);

    /**
     * Wraps this container around a new context.
     *
     * @param context the new context to wrap.
     *
     * @return this container with the new context.
     */
    ComponentContainer container(ContextDefinition context);
}

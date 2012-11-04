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

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.container.spi.DependencyGraph;
import org.fluidity.foundation.ClassDiscovery;
import org.fluidity.foundation.Log;

/**
 * Common services for container implementations. This is an internal interface to be used by dependency injection container implementations.
 * <h3>Usage</h3>
 * <pre>
 * final class MyContainerImpl extends {@linkplain org.fluidity.composition.container.spi.EmptyComponentContainer} {
 *
 *   MyContainerImpl(final <span class="hl1">ContainerServices</span> services, final {@linkplain SuperContainer} bridge) {
 *     &hellip;
 *   }
 * }
 * </pre>
 *
 * @see org.fluidity.composition.container.spi.ContainerProvider
 *
 * @author Tibor Varga
 */
@SuppressWarnings("JavadocReference")
public interface ContainerServices {

    /**
     * Creates an empty context definition.
     *
     * @return an empty context definition; never <code>null</code>.
     */
    ContextDefinition emptyContext();

    /**
     * Returns the service to use to find classes implementing some interface.
     *
     * @return the class discovery service; never <code>null</code>.
     */
    ClassDiscovery classDiscovery();

    /**
     * Returns the component that can perform dependency injection to constructors, fields, and methods.
     *
     * @return the dependency injector; never <code>null</code>.
     */
    DependencyInjector dependencyInjector();

    /**
     * Returns a new graph traversal.
     *
     * @return a new graph traversal; never <code>null</code>.
     */
    DependencyGraph.Traversal graphTraversal();

    /**
     * Returns a new graph traversal initialized with the given resolution observer.
     *
     * @param observer the object to notify when a component interface gets resolved to a class.
     *
     * @return a new graph traversal; never <code>null</code>.
     */
    DependencyGraph.Traversal graphTraversal(ComponentContainer.Observer observer);

    /**
     * Returns an observer that delegates to all of the given observers.
     *
     * @param observers the observers to aggregate.
     *
     * @return an aggregate observer; never <code>null</code>.
     */
    ComponentContainer.Observer aggregateObserver(ComponentContainer.Observer... observers);

    /**
     * Creates and returns a new component cache instance.
     *
     * @param stateless specifies whether the component can actually be cached or not. Value <code>true</code> means the component can be cached.
     *
     * @return a new component cache instance; never <code>null</code>.
     */
    ComponentCache newCache(boolean stateless);

    /**
     * Create an {@link Log} instance, once per source class.
     *
     * @param log    the {@link Log} instance the source class currently has.
     * @param source the source class.
     *
     * @return the instance the source class currently has, unless it is <code>null</code>, in which case a new instance is returned.
     */
    Log createLog(Log log, Class<?> source);
}

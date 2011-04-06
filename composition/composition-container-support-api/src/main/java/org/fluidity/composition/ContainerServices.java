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

import org.fluidity.composition.spi.ComponentResolutionObserver;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Common services for container implementations.
 *
 * @author Tibor Varga
 */
public interface ContainerServices {

    /**
     * Creates an empty context definition.
     *
     * @return an empty context definition.
     */
    ContextDefinition emptyContext();

    /**
     * Returns the service to find classes implementing some interface.
     *
     * @return the service to find classes implementing some interface.
     */
    ClassDiscovery classDiscovery();

    /**
     * Returns the component that can perform dependency injection to constructors or fields.
     *
     * @return the component that can perform dependency injection to constructors or fields.
     */
    DependencyInjector dependencyInjector();

    /**
     * Returns the default graph traversal.
     *
     * @return the default graph traversal.
     */
    DependencyGraph.Traversal graphTraversal();

    /**
     * Returns the default graph traversal with the given strategy and observer.
     *
     *
     * @param observer the object to notify when a component class gets resolved.
     *
     * @return the default graph traversal with the given strategy and observer.
     */
    DependencyGraph.Traversal graphTraversal(ComponentResolutionObserver observer);

    /**
     * Returns the logger factory to use by the container.
     *
     * @return the logger factory to use by the container.
     */
    LogFactory logs();

    /**
     * Creates and returns a new component cache instance.
     *
     * @param stateless specifies whether the component can actually be cached or not. Value <code>true</code> means the component can be cached.
     *
     * @return a new component cache instance.
     */
    ComponentCache newCache(boolean stateless);
}

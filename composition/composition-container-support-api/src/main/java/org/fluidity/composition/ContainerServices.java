/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.fluidity.composition;

import org.fluidity.composition.network.ContextDefinition;
import org.fluidity.composition.network.DependencyGraph;
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
     * @param strategy the traversal strategy to use.
     * @param observer the object to notify when a component class gets resolved.
     *
     * @return the default graph traversal with the given strategy and observer.
     */
    DependencyGraph.Traversal graphTraversal(DependencyGraph.Traversal.Strategy strategy, DependencyGraph.Traversal.Observer observer);

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

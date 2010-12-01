/*
 * Copyright (c) 2006-2010 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.composition.spi;

import org.fluidity.composition.ClassDiscovery;
import org.fluidity.foundation.LogFactory;

/**
 * Common services for container implementations.
 *
 * @author Tibor Varga
 */
public interface ContainerServices {

    /**
     * Returns the service to find classes implementing some interface.
     *
     * @return the service to find classes implementing some interface.
     */
    ClassDiscovery classDiscovery();

    /**
     * Returns the service that can create {@link org.fluidity.composition.ComponentContext} instances.
     *
     * @return the service that can create {@link org.fluidity.composition.ComponentContext} instances.
     */
    ContextFactory contextFactory();

    /**
     * Returns the component that can track a chain of context creations and consumptions by components.
     *
     * @return the component that can track a chain of context creations and consumptions by components.
     */
    ContextChain contextChain();

    /**
     * Returns the component that can track references from one component to the other.
     *
     * @return the component that can track references from one component to the other.
     */
    ReferenceChain referenceChain();

    /**
     * Returns the component that can perform dependency injection to constructors or fields.
     *
     * @return the component that can perform dependency injection to constructors or fields.
     */
    DependencyInjector dependencyInjector();

    /**
     * Returns the logger to use by the container.
     *
     * @return the logger to use by the container.
     */
    LogFactory log();

    /**
     * Creates and returns a new component cache instance.
     *
     * @return a new component cache instance.
     */
    ComponentCache newCache();
}

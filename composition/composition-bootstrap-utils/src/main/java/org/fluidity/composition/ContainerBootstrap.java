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

import java.util.Map;

import org.fluidity.composition.spi.ContainerProvider;

/**
 * Bootstraps the component container provided by a {@link ContainerProvider}.
 *
 * @author Tibor Varga
 */
@ServiceProvider
interface ContainerBootstrap {

    /**
     * Finds all package component bindings and invokes them to add their bindings and initializes components after all components have been bound.
     * <p/>
     * The idea is to find all {@link org.fluidity.composition.spi.PackageBindings} objects and invoke their {@link
     * org.fluidity.composition.spi.PackageBindings#bindComponents(org.fluidity.composition.ComponentContainer.Registry)} method.
     *
     * @param services    provides basic services for containers
     * @param provider    is the provider of actual dependency injection containers and related functionality.
     * @param properties  is the properties to bind to the container as a means to configure binding instances at run-time.
     * @param parent      is the container to use as the parent of the one returned; can be <code>null</code>, in which case a standalone container is
     *                    returned.
     * @param classLoader is the class loader to use to discover package bindings. Package bindings found in the class loader ancestry are ignored when the
     *
     * @return the container with the bindings registered.
     */
    OpenComponentContainer populateContainer(ContainerServices services,
                                             ContainerProvider provider,
                                             Map properties,
                                             OpenComponentContainer parent,
                                             ClassLoader classLoader);

    /**
     * Calls the {@link org.fluidity.composition.spi.PackageBindings#initializeComponents(ComponentContainer)} method on all bindings and adds shutdown tasks to
     * call the {@link org.fluidity.composition.spi.PackageBindings#shutdownComponents(ComponentContainer)} method on the bindings, in reverse order.
     *
     * @param container the container, returned by the {@link #populateContainer(ContainerServices, org.fluidity.composition.spi.ContainerProvider,
     *                  java.util.Map, OpenComponentContainer, ClassLoader)} method, to initialize.
     * @param services  provides basic services for containers
     */
    void initializeContainer(OpenComponentContainer container, ContainerServices services);
}

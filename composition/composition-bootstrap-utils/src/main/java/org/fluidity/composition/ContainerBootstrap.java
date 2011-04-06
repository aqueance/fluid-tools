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

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

package org.fluidity.composition;

import java.util.Map;

import org.fluidity.composition.container.ContainerServices;
import org.fluidity.composition.container.PlatformContainer;
import org.fluidity.composition.container.spi.ContainerProvider;

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
     * org.fluidity.composition.spi.PackageBindings#bindComponents(ComponentContainer.Registry)} method.
     *
     * @param services    provides basic services for containers
     * @param provider    is the provider of actual dependency injection containers and related functionality.
     * @param properties  is the properties to bind to the container as a means to configure binding instances at run time.
     * @param parent      is the container to use as the parent of the one returned; can be <code>null</code>, in which case a standalone container is
     *                    returned.
     * @param classLoader is the class loader to use to discover package bindings. Package bindings found in the class loader ancestry are ignored when the
     * @param platform    the container that turns the host platform's component container into a super container for Fluid Tools.
     * @param callback    object to notify when a container is initialized or shut down.
     *
     * @return the container with the bindings registered.
     */
    ExposedComponentContainer populateContainer(ContainerServices services,
                                             ContainerProvider provider,
                                             Map properties,
                                             ExposedComponentContainer parent,
                                             ClassLoader classLoader,
                                             PlatformContainer platform,
                                             Callback callback);

    /**
     * Calls the {@link org.fluidity.composition.spi.PackageBindings#initializeComponents(OpenComponentContainer)} method on all bindings and adds shutdown tasks
     * to call the {@link org.fluidity.composition.spi.PackageBindings#shutdownComponents()} method on the bindings, in reverse order.
     *
     * @param container the container, returned by the {@link #populateContainer(ContainerServices, ContainerProvider, Map, ExposedComponentContainer,
     *                  ClassLoader, PlatformContainer, ContainerBootstrap.Callback)} method, to initialize.
     * @param services  provides basic services for containers
     */
    void initializeContainer(OpenComponentContainer container, ContainerServices services);

    /**
     * Notification receiver concerning container initialization / shutdown.
     *
     * @author Tibor Varga
     */
    interface Callback {

        /**
         * Notifies the receiver that the given container has been initialized.
         *
         */
        void containerInitialized();

        /**
         * Notifies the receiver that the given container has been shut down.
         *
         */
        void containerShutdown();
    }
}

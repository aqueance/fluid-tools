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

package org.fluidity.composition.spi;

import java.util.List;
import java.util.Map;

import org.fluidity.composition.ContainerServices;
import org.fluidity.composition.OpenComponentContainer;
import org.fluidity.composition.ServiceProvider;

/**
 * Provides actual dependency injection container instances and related functionality. This is an internal interface to be implemented by dependency injection
 * container implementations.
 *
 * @author Tibor Varga
 */
@ServiceProvider
public interface ContainerProvider {

    /**
     * Creates and returns and empty standalone dependency injection container.
     *
     * @param services provides service components for the container.
     * @param platform the container that turns the host platform's component container into a super container for Fluid Tools.
     *
     * @return and empty standalone dependency injection container.
     */
    OpenComponentContainer newContainer(ContainerServices services, PlatformContainer platform);

    /**
     * Instantiates all {@link PackageBindings} classes in the given set of bindings and returns the instances in instantiation order.
     *
     * @param services   provides service components for containers, in case needed.
     * @param properties properties to be made available to any {@link PackageBindings} object that may depend on it.
     * @param bindings   the collection of classes to instantiate. Some may depend on others in the set and that influences the instantiation order.
     *
     * @return the list of {@link PackageBindings} instances in instantiation order.
     */
    List<PackageBindings> instantiateBindings(ContainerServices services, Map properties, Class<PackageBindings>[] bindings);
}

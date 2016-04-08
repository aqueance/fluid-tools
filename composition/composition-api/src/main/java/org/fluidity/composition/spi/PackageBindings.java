/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.OpenContainer;
import org.fluidity.composition.ServiceProvider;

/**
 * An implementation of this interface is placed, either automatically by the <code>org.fluidity.maven:composition-maven-plugin</code> Maven plugin or manually
 * by the component developer, in each package that contains component implementations. The goal of these classes is to add, to the supplied registry,
 * component bindings for component implementations in the respective package. The components thus bound may depend on other components in the container and
 * vice versa.
 * <p>
 * Placing the implementation of this interface in the same package as the components it binds allows those component classes to be package protected and thus
 * makes it possible to hide them from view to prevent careless use.
 * <h3>Usage</h3>
 * See {@link EmptyPackageBindings}.
 *
 * @author Tibor Varga
 */
@ServiceProvider(api = PackageBindings.class, type = PackageBindings.SERVICE_TYPE)
public interface PackageBindings extends ComponentContainer.Bindings {

    /**
     * Distinguishes package bindings from {@linkplain org.fluidity.foundation.ServiceProviders#TYPE ordinary} service providers.
     */
    String SERVICE_TYPE = "bindings";

    /**
     * Perform package initialization if necessary. This method is invoked once after the {@link
     * org.fluidity.composition.ComponentContainer.Bindings#bindComponents(org.fluidity.composition.ComponentContainer.Registry)
     * ComponentContainer.Bindings.bindComponents()} method of all {@link PackageBindings} objects have been invoked and before any component is externally
     * accessed in the provided container.
     *
     * @param container the container that was populated by the {@link
     *                  org.fluidity.composition.ComponentContainer.Bindings#bindComponents(org.fluidity.composition.ComponentContainer.Registry)} method.
     * @param shutdown  allows registration of actions to be performed when the container shuts down.
     *
     * @throws Exception when an error occurs.
     */
    void initialize(OpenContainer container, ContainerTermination shutdown) throws Exception;
}

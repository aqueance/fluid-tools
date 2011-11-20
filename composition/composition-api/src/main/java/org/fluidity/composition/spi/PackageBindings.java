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

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.OpenComponentContainer;
import org.fluidity.composition.ServiceProvider;

/**
 * An implementation of this interface is placed, either automatically by the <code>org.fluidity.maven:maven-composition-plugin</code> Maven plugin or manually
 * by the component developer, in each package that contains component implementations. The goal of these classes is to add to the supplied registry component
 * bindings for component implementations in the respective package. The components thus bound may depend on other components in the container and vice versa.
 * <p/>
 * Placing the implementation of this interface in the same package as the components it binds allows those component classes to be package protected and thus
 * makes it possible to hide them from inadvertent use.
 *
 * @author Tibor Varga
 */
@ServiceProvider(api = PackageBindings.class, type = PackageBindings.SERVICE_TYPE)
public interface PackageBindings extends OpenComponentContainer.Bindings {

    /**
     * Distinguishes package bindings from the usual JAR service providers.
     */
    String SERVICE_TYPE = "bindings";

    /**
     * Perform component specific initialization if necessary. This method is invoked once after the {@link
     * org.fluidity.composition.OpenComponentContainer.Bindings#bindComponents(org.fluidity.composition.OpenComponentContainer.Registry)
     * OpenComponentContainer.Bindings.bindComponents()} method of all {@link PackageBindings} objects have been invoked and
     * before any component is accessed in the provided container from outside the container.
     *
     * @param container is the container that was populated by the <code>OpenComponentContainer.Bindings.bindComponents()</code>
     *                  method.
     */
    void initializeComponents(ComponentContainer container);

    /**
     * Perform component specific shutdown if necessary. This method is invoked once when the application is being shut down.
     *
     * @param container is the container that was populated by the <code>OpenComponentContainer.Bindings.bindComponents()</code>
     *                  method.
     */
    void shutdownComponents(ComponentContainer container);
}

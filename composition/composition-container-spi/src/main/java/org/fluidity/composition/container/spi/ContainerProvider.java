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

package org.fluidity.composition.container.spi;

import org.fluidity.composition.MutableContainer;
import org.fluidity.composition.ServiceProvider;
import org.fluidity.composition.container.ContainerServices;

/**
 * Creates {@linkplain org.fluidity.composition.MutableContainer dependency injection container} instances. This is the entry point to a container
 * implementation that hooks it up to the rest of Fluid Tools.
 * <h3>Usage</h3>
 * You don't interact with an internal interface.
 *
 * @author Tibor Varga
 */
@ServiceProvider
public interface ContainerProvider {

    /**
     * Creates and returns and empty standalone dependency injection container.
     *
     * @param services provides service components for the container; never <code>null</code>.
     *
     * @return and empty standalone dependency injection container.
     */
    MutableContainer newContainer(ContainerServices services);
}

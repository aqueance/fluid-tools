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

package org.fluidity.composition.container.spi;

import org.fluidity.composition.ServiceProvider;
import org.fluidity.composition.container.ContainerServices;

/**
 * Provides actual dependency injection container instances and related functionality. This is an internal interface to be implemented by dependency injection
 * container implementations.
 * <h3>Usage</h3>
 * The only way you might ever interact with this interface is by implementing it:
 * <pre>
 * final class MyContainerProviderImpl implements <span class="hl1">ContainerProvider</span> {
 *   public {@linkplain OpenComponentContainer} newContainer(final {@linkplain ContainerServices} services, final {@linkplain PlatformContainer} platform) {
 *     return new MyContainerImpl(services, platform);
 *   }
 * }
 * </pre>
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
}

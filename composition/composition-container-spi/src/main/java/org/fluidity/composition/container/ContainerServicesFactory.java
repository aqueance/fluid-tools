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

package org.fluidity.composition.container;

import org.fluidity.composition.ServiceProvider;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Creates a {@link ContainerServices} object.
 * <h3>Usage</h3>
 * The only way you might ever interact with this interface is by implementing it:
 * <pre>
 * final MyServicesFactoryImpl implements <span class="hl1">ContainerServicesFactory</span> {
 *   public {@linkplain ContainerServices} containerServices(final {@linkplain LogFactory} logs) {
 *     return new ...
 *   }
 * }
 * </pre>
 */
@ServiceProvider
public interface ContainerServicesFactory {

    /**
     * Creates a {@link ContainerServices} object.
     *
     * @param logs the logger factory to use.
     *
     * @return a <code>ContainerServices</code> object.
     */
    ContainerServices containerServices(LogFactory logs);
}
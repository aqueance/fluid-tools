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

package org.fluidity.composition;

import org.fluidity.composition.container.ContainerServices;
import org.fluidity.foundation.spi.LogFactory;

/**
 * An internal component not intended for any sort of interaction with application or service provider logic. This factory allows testing the
 * <code>ContainerBoundary</code> class without having an actual container in place.
 */
@ServiceProvider
public interface ContainerServicesFactory {

    /**
     * Creates a {@link org.fluidity.composition.container.ContainerServices} object.
     *
     * @param logs the logger factory to use.
     *
     * @return a <code>ContainerServices</code> object.
     */
    ContainerServices containerServices(LogFactory logs);
}

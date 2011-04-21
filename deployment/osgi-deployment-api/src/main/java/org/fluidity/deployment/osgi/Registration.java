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

package org.fluidity.deployment.osgi;

import java.util.Properties;

import org.fluidity.composition.ComponentGroup;

/**
 * Denotes an OSGi service that will be registered when the host bundle is started.
 */
@ComponentGroup
public interface Registration {

    /**
     * Returns the dictionary to pass to OSGi when registering this service.
     *
     * @return the dictionary to pass to OSGi when registering this service; may be <code>null</code>.
     */
    Properties properties();

    /**
     * Returns the service API.
     *
     * @return the service API.
     */
    Class<?>[] api();

    /**
     * Returns the service implementation.
     *
     * @return the service implementation.
     */
    Object service();
}

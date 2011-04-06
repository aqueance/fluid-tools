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

package org.fluidity.deployment;

import org.fluidity.composition.ComponentGroup;

/**
 * Components implementing this interface get notified when all {@link DeployedComponent} objects have been started/stopped. Methods of this interface are invoked from the main thread
 * and block that thread until they return.
 * <p/>
 * All subclasses of this interface will be marked as a service provider for this interface and will be automatically found and controlled by a suitable {@link
 * DeploymentBootstrap} implementation.
 *
 * @author Tibor Varga
 */
@ComponentGroup
public interface DeploymentObserver {

    /**
     * Notifies the component that all {@link DeployedComponent} objects have been started.
     *
     * @throws Exception when thrown is logged.
     */
    void started() throws Exception;

    /**
     * Notifies the component that all {@link DeployedComponent} objects have been stopped.
     *
     * @throws Exception when thrown is logged.
     */
    void stopped() throws Exception;
}

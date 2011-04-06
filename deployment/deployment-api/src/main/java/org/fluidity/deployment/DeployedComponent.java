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
 * This is a component that is started/stopped as the application container starts/stops. There is no deterministic order in which deployed components are
 * started/stopped other than the following guarantee: a deployed component that depends, directly or indirectly, on other deployed components will be started
 * later and stopped sooner than those it depends on.
 * <p/>
 * The component is started in the main thread and halts the application startup until its {@link #start(DeployedComponent.Context)} method returns. The
 * component may start its own thread if it wants to but that thread must stop when the component receives a {@link #stop()} method call.
 * <p/>
 * A component should notify the system that it is no longer active by calling {@link DeployedComponent.Context#complete()} on the observer passed in the {@link
 * DeployedComponent#start(DeployedComponent.Context)} method. If the component stops and notifies the observer, the component's {@link
 * DeployedComponent#stop()} method will not be invoked.
 * <p/>
 * If the component fails to call {@link DeployedComponent.Context#complete()} when its {@link #stop()} method is called, the system will wait indefinitely for
 * the component to stop. This allows the component to asynchronously invoke the {@link DeployedComponent.Context#complete()} method once the component has been
 * asked to stop.
 * <p/>
 * All subclasses of this interface will be marked as a service provider for this interface and will be automatically found and controlled by a suitable {@link
 * DeploymentBootstrap} implementation.
 *
 * @author Tibor Varga
 */
@ComponentGroup
public interface DeployedComponent {

    /**
     * The component has an ID that is returned by this method.
     *
     * @return a short String identifying this component among other {@link DeployedComponent}s.
     */
    String id();

    /**
     * The component has a name that is returned by this method.
     *
     * @return a String, never <code>null</code>.
     */
    String name();

    /**
     * Starts the component.
     *
     * @param observer is notified when this component is done.
     *
     * @throws Exception when thrown is logged and will cause the application start-up to be aborted if possible.
     */
    void start(Context observer) throws Exception;

    /**
     * Stops the component, unless the component notified the observer passed in {@link DeployedComponent#start(DeployedComponent.Context)} that it had stopped.
     * The component must, synchronously or asynchronously, call {@link DeployedComponent.Context#complete()} to allow the host application to terminate.
     *
     * @throws Exception when thrown is logged.
     */
    void stop() throws Exception;

    /**
     * Callback interface to notify the recipient that the caller has completed.
     */
    interface Context {

        /**
         * Notifies the recipient that the caller has completed.
         */
        void complete();
    }
}

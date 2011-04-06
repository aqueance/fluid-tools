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

/**
 * Receives notifications concerning the deployment of {@link DeployedComponent} and {@link DeploymentObserver}
 * components.
 *
 * @author Tibor Varga
 */
public interface DeploymentControl extends RuntimeControl {

    /**
     * Notification about all deployed components having been started.
     */
    void completed();

    /**
     * Tells if the deployment is designed to function without any {@link DeployedComponent} object running. Returning
     * <code>false</code> when no such deployed component is running results in a call to {@link RuntimeControl#stop()}.
     *
     * @return <code>true</code> if the system should run even when no deployed components are running, <code>false</code> otherwise.
     */
    boolean isStandalone();
}

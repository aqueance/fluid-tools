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
 * Bootstraps the application.
 *
 * @author Tibor Varga
 */
public interface DeploymentBootstrap {

    /**
     * Loads all {@link DeployedComponent} and {@link DeploymentObserver} objects and calls their {@link DeployedComponent#start(DeployedComponent.Context)} and
     * {@link DeploymentObserver#started()} methods, respectively.
     *
     * @throws Exception will cause the bootstrap to be aborted.
     */
    void load() throws Exception;

    /**
     * Locates all loaded {@link DeployedComponent} and {@link DeploymentObserver} objects and calls their {@link DeployedComponent#stop()} and {@link
     * DeploymentObserver#stopped()} methods, respectively, and in a reverse order than in the {@link #load()} method.
     */
    void unload();
}

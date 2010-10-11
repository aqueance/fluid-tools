/*
 * Copyright (c) 2006-2010 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.fluidity.deployment;

import org.fluidity.composition.ServiceProvider;
import org.fluidity.foundation.KeyedNamed;

/**
 * This is a component that is started/stopped as the application container starts/stops. There is no deterministic
 * order in which deployed components are started/stopped and so deployed components should be independent of one
 * another. However, they may of course depend on components that happens to be deployed components, too.
 * <p/>
 * A component can notify the system that it stopped running by calling {@link org.fluidity.deployment.DeployedComponent.Context#complete()}
 * on the observer passed in the {@link org.fluidity.deployment.DeployedComponent#start(org.fluidity.deployment.DeployedComponent.Context)}
 * method. If the component stops and notifies the observer, the component's {@link DeployedComponent#stop()} method
 * will not be invoked.
 * <p/>
 * All subclasses of this interface will be marked as a service provider for this interface and will be automatically
 * found and controlled by a suitable {@link DeploymentBootstrap} implementation.
 */
@ServiceProvider(api = DeployedComponent.class)
public interface DeployedComponent extends KeyedNamed {

    /**
     * The component has an ID that is returned by this method.
     *
     * @return a short String idenfifying this component among other {@link DeployedComponent}s.
     */
    String key();

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
     * @throws Exception when thrown is logged and will cause the application start-up to be aborted if possible.
     */
    void start(Context observer) throws Exception;

    /**
     * Stops the component, unless the component notified the observer passed in {@link
     * org.fluidity.deployment.DeployedComponent#start(org.fluidity.deployment.DeployedComponent.Context)} that it had
     * stopped.
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

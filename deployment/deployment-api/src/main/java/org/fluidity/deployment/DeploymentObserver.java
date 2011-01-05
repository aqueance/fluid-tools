/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
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

/**
 * This component gets notified when all {@link org.fluidity.deployment.DeployedComponent} objects have been started/stopped. Methods of this interface are
 * invoked from the main thread and block that thread until they return.
 * <p/>
 * All subclasses of this interface will be marked as a service provider for this interface and will be automatically found and controlled by a suitable {@link
 * DeploymentBootstrap} implementation.
 */
@ServiceProvider
public interface DeploymentObserver {

    /**
     * Notifies the component that all {@link org.fluidity.deployment.DeployedComponent} objects have been started.
     *
     * @throws Exception when thrown is logged.
     */
    void started() throws Exception;

    /**
     * Notifies the component that all {@link org.fluidity.deployment.DeployedComponent} objects have been stopped.
     *
     * @throws Exception when thrown is logged.
     */
    void stopped() throws Exception;
}

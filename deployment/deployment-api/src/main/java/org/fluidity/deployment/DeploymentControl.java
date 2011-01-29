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

/*
 * Copyright (c) 2006-2008 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Softweare"), to deal
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.fluidity.deployment;

/**
 * Bootstraps the application. The singleton implementing instance is not thread safe.
 */
public interface DeploymentBootstrap {

    /**
     * Loads all {@link org.fluidity.deployment.DeployedComponent} and {@link org.fluidity.deployment.DeploymentObserver} objects and calls their {@link
     * org.fluidity.deployment.DeployedComponent#start()} and {@link org.fluidity.deployment.DeploymentObserver#started()} methods, respectively.
     *
     * @throws Exception will cause the bootstrap to be aborted.
     */
    void load() throws Exception;

    /**
     * Locates all loaded {@link org.fluidity.deployment.DeployedComponent} and {@link org.fluidity.deployment.DeploymentObserver} objects and calls their
     * {@link org.fluidity.deployment.DeployedComponent#stop()} and {@link org.fluidity.deployment.DeploymentObserver#stopped()} methods, respectively, and in a
     * reverse order than in the {@link #load()} method.
     */
    void unload();

    /**
     * Returns the number of deployed components and observers found.
     *
     * @return the number of deployed components and observers found.
     */
    int deploymentCount();
}

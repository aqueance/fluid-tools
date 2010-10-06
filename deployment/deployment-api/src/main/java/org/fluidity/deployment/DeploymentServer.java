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

import org.fluidity.foundation.ApplicationInfo;

/**
 * Allows the caller to control the web server by means of starting and stopping known web applications.
 */
public interface DeploymentServer extends RuntimeControl {

    /**
     * Returns the names of the application in the order they have been specified to the deployer.
     *
     * @return a list of applications names, may be empty.
     */
    ApplicationInfo[] applications();

    /**
     * Tells whether the application with the given name is deployed or not. The list of deployable applications are returned by the {@link #applications()}
     * method.
     *
     * @param key identifies the application as returned by {@link org.fluidity.foundation.ApplicationInfo#key()}.
     *
     * @return <code>true</code> if the application is deployed, <code>false</code> otherwise.
     */
    boolean isApplicationDeployed(String key);

    /**
     * Deploys the application with the given names. The list of deployable applications are returned by the {@link #applications()} method.
     *
     * @param key identifies the application as returned by {@link org.fluidity.foundation.ApplicationInfo#key()}.
     *
     * @throws Exception when something goes wrong with the deployment.
     */
    void deployApplication(String key) throws Exception;

    /**
     * Undeploys the application with the given names. The list of deployable applications are returned by the {@link #applications()} method.
     *
     * @param key identifies the application as returned by {@link org.fluidity.foundation.ApplicationInfo#key()}.
     *
     * @throws Exception when something goes wrong with the undeployment.
     */
    void undeployApplication(String key) throws Exception;
}

package org.fluidity.deployment;

import org.fluidity.foundation.ApplicationInfo;

/**
 * Allows the caller to control the web server by means of starting and stopping known web applications.
 */
public interface DeploymentServer extends ServerControl {

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
    boolean isApplicationDeployed(final String key);

    /**
     * Deploys the application with the given names. The list of deployable applications are returned by the {@link #applications()} method.
     *
     * @param key identifies the application as returned by {@link org.fluidity.foundation.ApplicationInfo#key()}.
     *
     * @throws Exception when something goes wrong with the deployment.
     */
    void deployApplication(final String key) throws Exception;

    /**
     * Undeploys the application with the given names. The list of deployable applications are returned by the {@link #applications()} method.
     *
     * @param key identifies the application as returned by {@link org.fluidity.foundation.ApplicationInfo#key()}.
     *
     * @throws Exception when something goes wrong with the undeployment.
     */
    void undeployApplication(final String key) throws Exception;
}

package org.fluidity.deployment;

/**
 * Allows the caller to control the web server by means of starting and stopping known web applications.
 */
public interface DeploymentServer extends ServerControl {

    /**
     * Returns the names of the application in the order they have been specified to the deployer.
     *
     * @return a list of applications names, may be empty.
     */
    String[] applicationKeys();

    /**
     * Tells whether the application with the given name is deployed or not. The list of deployable applications are returned by the {@link #applicationKeys()}
     * method.
     *
     * @param key identifies the application
     * @return <code>true</code> if the application is deployed, <code>false</code> otherwise.
     */
    boolean isApplicationDeployed(final String key);

    /**
     * Deploys the application with the given names. The list of deployable applications are returned by the {@link #applicationKeys()} method.
     *
     * @param key identifies the application
     * @throws Exception when something goes wrong with the deployment.
     */
    void deployApplication(final String key) throws Exception;

    /**
     * Undeploys the application with the given names. The list of deployable applications are returned by the {@link #applicationKeys()} method.
     *
     * @param key identifies the application
     *
     * @throws Exception when something goes wrong with the undeployment.
     */
    void undeployApplication(final String key) throws Exception;
}

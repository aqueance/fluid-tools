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
    String[] applicationNames();

    /**
     * Tells whether the application with the given name is deployed or not. The list of deployable applications are returned by the {@link #applicationNames()}
     * method.
     *
     * @param name the name of the application.
     *
     * @return <code>true</code> if the application is deployed, <code>false</code> otherwise.
     */
    boolean isApplicationDeployed(final String name);

    /**
     * Deploys the application with the given names. The list of deployable applications are returned by the {@link #applicationNames()} method.
     *
     * @param name the name of the application.
     *
     * @throws Exception when something goes wrong with the deployment.
     */
    void deployApplication(final String name) throws Exception;

    /**
     * Undeploys the application with the given names. The list of deployable applications are returned by the {@link #applicationNames()} method.
     *
     * @param name the name of the application.
     *
     * @throws Exception when something goes wrong with the undeployment.
     */
    void undeployApplication(final String name) throws Exception;
}

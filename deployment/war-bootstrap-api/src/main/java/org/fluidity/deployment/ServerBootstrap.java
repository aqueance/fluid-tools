package org.fluidity.deployment;

import java.io.File;
import java.util.List;

import org.fluidity.composition.ServiceProvider;

/**
 * Bootstraps an HTTP server and deploys one or more .war files.
 */
@ServiceProvider
public interface ServerBootstrap {

    /**
     * Bootstraps a web server and deploys a list of .war files, either automatically or by providing a component through which another entity can manage the
     * deployment.
     *
     * @param bootApp       The web application .war file used to establish the boot class path. The list of .jar files to be added to the boot class path will
     *                      be found under <code>WEB-INF/boot</code> of this .war file. This .war file shall be deployed under the root (<code>/</code>)
     *                      context.
     * @param managedApps   The web application .war files to deploy. These .war files shall be deployed under a context that is derived from the .war name
     *                      exlucing the version number and the extension.
     * @param workDirectory The working directory for use by the web server.
     * @param args          The list of command line arguments left unprocessed by the invoker.
     */
    void bootstrap(final File bootApp, final List<File> managedApps, final File workDirectory, final String args[]);
}

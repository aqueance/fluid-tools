package org.fluidity.web;

import java.io.File;
import java.util.List;

/**
 * Bootstraps an HTTP server and deploys one or more .war files.
 */
public interface ServerBootstrap {

    /**
     * Bootstraps a web server. A {@link org.fluidity.web.ServerBootstrap.Settings} implementation may be provided as a component but since the invokee will
     * have to trigger population of the dependency injection container, that <code>Settings</code> component will have to be found manually.
     *
     * @param bootWar       The .war file to be used to establish the boot class path. The list of .jar files to be added to the boot class path will be found
     *                      under <code>WEB-INF/boot</code> of this .war file. This .war file shall be deployed under the root (<code>/</code>) context.
     * @param otherWars     The other .war files to deploy. These .war files shall be deployed under a context that is derived from the .war name exlucing the
     *                      version number and the extension.
     * @param workDirectory The working directory for use by the web server.
     */
    void bootstrap(final File bootWar, final List<File> otherWars, final File workDirectory);

    /**
     * Web server settings. The implementing class should be a @{@link org.fluidity.composition.Component}.
     */
    interface Settings {

        /**
         * The HTTP port to listen on. Returning 0 causes no listener to be set up.
         *
         * @return a non-negative number.
         */
        int httpPort();
    }
}

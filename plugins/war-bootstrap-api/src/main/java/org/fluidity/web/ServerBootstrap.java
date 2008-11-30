package org.fluidity.web;

import java.io.File;

/**
 * Bootstraps an HTTP server and deploys a .war file.
 */
public interface ServerBootstrap {

    void bootstrap(final File warFile, final File workDirectory);

    interface Settings {

        int httpPort();
    }
}

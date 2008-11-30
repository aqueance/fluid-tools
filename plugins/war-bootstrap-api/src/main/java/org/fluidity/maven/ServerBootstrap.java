package org.fluidity.maven;

import java.io.File;

/**
 * Bootstraps an HTTP server.
 */
public interface ServerBootstrap {

    void bootstrap(final File warFile, final File workDirectory);
}

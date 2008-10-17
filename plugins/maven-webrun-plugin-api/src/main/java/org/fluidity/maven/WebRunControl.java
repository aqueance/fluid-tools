package org.fluidity.maven;

/**
 * Allows stopping by the deployed application the web server started by the org.fluidity.shared:maven-webrun-plugin
 * Maven plugin.
 */
public interface WebRunControl {

    void stopServer() throws Exception;
}

package org.fluidity.deployment;

import java.io.File;

import org.fluidity.foundation.ApplicationInfo;

/**
 * Web application descriptor.
 */
public interface WebApplicationInfo extends ApplicationInfo {

    String HTTP_ATTRIBUTE = "Http-Support";

    /**
     * Returns the .war file.
     *
     * @return the .war file.
     */
    File archive();

    /**
     * Tells whether this application handles HTTP requests.
     *
     * @return <code>true</code> if this application needs an HTTP server, <code>false</code> otherwise.
     */
    boolean needsHttp();
}

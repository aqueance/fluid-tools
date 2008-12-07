package org.fluidity.deployment;

import java.io.File;

/**
 * Web application descriptor.
 */
public interface WebApplicationInfo {

    File archive();

    /**
     * Short name for the application. Can be used to find properties file on the class path, etc.
     *
     * @return a short String; never <code>null</code>.
     */
    String key();

    /**
     * Human readable name of the application. Can be used to display user messages, etc.
     *
     * @return a short String; never <code>null</code>.
     */
    String name();
}

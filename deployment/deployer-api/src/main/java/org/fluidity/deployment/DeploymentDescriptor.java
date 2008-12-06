package org.fluidity.deployment;

import org.fluidity.foundation.Named;

/**
 * Describes a .war file.
*/
public interface DeploymentDescriptor extends Named {

    ServerBootstrap.Settings settings();
}

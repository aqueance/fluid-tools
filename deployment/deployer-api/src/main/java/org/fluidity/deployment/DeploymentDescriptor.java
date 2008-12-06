package org.fluidity.deployment;

import org.fluidity.foundation.Named;
import org.fluidity.web.ServerBootstrap;

/**
 * Describes a .war file.
*/
public interface DeploymentDescriptor extends Named {

    ServerBootstrap.Settings settings();
}

package org.fluidity.foundation.logging;

import org.fluidity.composition.EmptyPackageBindings;
import org.fluidity.composition.ComponentContainer;

/**
 * Post-initializes the Log class. We use this mechanism to catch the point when we can initialize it using a just populated continer.
 */
public class LogInitialization extends EmptyPackageBindings {

    @Override
    public void initializeComponents(final ComponentContainer container) {

        // we initialize the Log class at this point as this is the point when we know it has been loaded
        // and the container populated and the application had little chance to access it yet.
        container.initialize(new Log());
    }
}

package org.fluidity.foundation.logging;

import org.fluidity.composition.EmptyPackageBindings;
import org.fluidity.composition.ComponentContainer;

/**
 * TODO: documentation...
 */
public class LogResetBindings extends EmptyPackageBindings {

    @Override
    public void initializeComponents(final ComponentContainer container) {
        Log.reset(container);
    }
}

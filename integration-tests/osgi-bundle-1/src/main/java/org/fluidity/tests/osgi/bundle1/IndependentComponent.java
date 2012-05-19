package org.fluidity.tests.osgi.bundle1;

import org.fluidity.deployment.osgi.BundleComponentContainer;

/**
 * Independent component that must be recognized and instantiated by the bundle component container.
 *
 * @author Tibor Varga
 */
final class IndependentComponent implements BundleComponentContainer.Managed {

    public void start() throws Exception {
        // empty
    }

    public void stop() throws Exception {
        // empty
    }
}

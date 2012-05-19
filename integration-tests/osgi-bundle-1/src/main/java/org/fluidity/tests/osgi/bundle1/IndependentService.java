package org.fluidity.tests.osgi.bundle1;

import java.util.Properties;

import org.fluidity.deployment.osgi.BundleComponentContainer;

/**
 * Independent service that must be recognized and instantiated by the bundle component container.
 *
 * @author Tibor Varga
 */
final class IndependentService implements BundleComponentContainer.Registration {

    public Class<?>[] types() {
        return new Class<?>[] { IndependentService.class };
    }

    public Properties properties() {
        return null;
    }

    public void start() throws Exception {
        // empty
    }

    public void stop() throws Exception {
        // empty
    }
}

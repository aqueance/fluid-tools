package org.fluidity.tests.osgi.bundle1;

import java.util.Properties;

import org.fluidity.deployment.osgi.BundleComponentContainer;
import org.fluidity.deployment.osgi.Service;

/**
 * Service that depends on a local service and thus must be recognized and instantiated by the bundle component container.
 *
 * @author Tibor Varga
 */
final class LocalServiceDependency implements BundleComponentContainer.Registration {

    private final IndependentService dependency;

    public LocalServiceDependency(final @Service IndependentService dependency) {
        this.dependency = dependency;
    }

    public Class<?>[] types() {
        return new Class<?>[] { LocalServiceDependency.class };
    }

    public Properties properties() {
        return null;
    }

    public void start() throws Exception {
        assert dependency != null;
    }

    public void stop() throws Exception {
        // empty
    }
}

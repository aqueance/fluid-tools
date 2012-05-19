package org.fluidity.tests.osgi.bundle1;

import org.fluidity.deployment.osgi.BundleComponentContainer;

/**
 * Component that depends on a local component and thus must be recognized and instantiated by the bundle component container.
 *
 * @author Tibor Varga
 */
final class ComponentDependency implements BundleComponentContainer.Managed {

    private final IndependentComponent dependency;

    public ComponentDependency(final IndependentComponent dependency) {
        this.dependency = dependency;
    }

    public void start() throws Exception {
        assert dependency != null;
    }

    public void stop() throws Exception {
        // empty
    }
}

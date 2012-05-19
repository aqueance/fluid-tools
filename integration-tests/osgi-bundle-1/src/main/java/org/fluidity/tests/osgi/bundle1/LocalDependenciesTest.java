package org.fluidity.tests.osgi.bundle1;

import java.util.Properties;

import org.fluidity.deployment.osgi.Service;
import org.fluidity.tests.osgi.BundleTest;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public final class LocalDependenciesTest implements BundleTest {

    private final ComponentDependency component;
    private final LocalServiceDependency service;

    public LocalDependenciesTest(final ComponentDependency component, final @Service LocalServiceDependency service) {
        this.component = component;
        this.service = service;
    }

    @Test
    public void test() throws Exception {
        assert component != null;
        assert service != null;
    }

    public void start() throws Exception {
        // empty
    }

    public void stop() throws Exception {
        // empty
    }

    public Class<?>[] types() {
        return new Class<?>[] { BundleTest.class };
    }

    public Properties properties() {
        return null;
    }
}

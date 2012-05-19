package org.fluidity.tests.osgi;

import org.fluidity.deployment.osgi.BundleComponentContainer;

/**
 * Individual integration tests executed within an OSGi bundle. The implementation is an ordinary TestNG unit test with appropriate annotations, etc.
 *
 * @author Tibor Varga
 */
public interface BundleTest extends BundleComponentContainer.Registration { }

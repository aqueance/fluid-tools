package org.fluidity.deployment.osgi;

/**
 * Internal interface used to connect the {@link BundleComponentContainer.Status} implementation with the internals of the {@link BundleComponentContainer}
 * implementations without breaking object encapsulation.
 *
 * @author Tibor Varga
 */
interface ComponentStatus extends BundleComponentContainer.Status { }

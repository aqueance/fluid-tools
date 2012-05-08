package org.fluidity.deployment.osgi;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.osgi.framework.BundleContext;

/**
 * This is the OSGi service implementation for the {@link BundleComponentContainer.Status} service API. It requires a delegate to connect to the internal state
 * of the host bundle's {@link BundleComponentContainer}, which will be provided by that same container.
 * <p/>
 * This is a {@linkplain BundleComponentContainer.Managed managed} {@linkplain BundleComponentContainer.Registration service registration} component, hence no
 * {@link org.fluidity.composition.Component @Component} annotation.
 *
 * @author Tibor Varga
 */
final class ComponentStatusImpl implements BundleComponentContainer.Status, BundleComponentContainer.Registration {

    private final Properties registration = new Properties();
    private final ComponentStatus delegate;

    public ComponentStatusImpl(final BundleContext context, final ComponentStatus delegate) {
        this.delegate = delegate;
        this.registration.setProperty(ID, context.getBundle().getSymbolicName());
    }

    public Class<?>[] types() {
        return new Class<?>[] { BundleComponentContainer.Status.class };
    }

    public Properties properties() {
        return registration;
    }

    public Collection<Class<?>> active() {
        return delegate.active();
    }

    public Map<Class<?>, Collection<Service>> inactive() {
        return delegate.inactive();
    }

    public Collection<Class<?>> failed() {
        return delegate.failed();
    }

    public void start() throws Exception {
        // empty
    }

    public void stop() throws Exception {
        // empty
    }
}

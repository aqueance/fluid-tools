package org.fluidity.deployment.osgi;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Tibor Varga
 */
final class ComponentDescriptor extends Descriptor {

    private boolean failed = false;

    private final Set<ServiceDescriptor> dependencies = new HashSet<ServiceDescriptor>();
    private final Class<? super BundleComponentContainer.Managed>[] api;

    @SuppressWarnings("unchecked")
    public ComponentDescriptor(final Class<BundleComponentContainer.Managed> type, final Collection<Class<? super BundleComponentContainer.Managed>> api) {
        super(type);
        this.api = api.toArray(new Class[api.size()]);
    }

    public Set<ServiceDescriptor> dependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

    public void dependencies(final Set<ServiceDescriptor> services) {
        assert dependencies().isEmpty() : type;
        dependencies.addAll(services);
    }

    public Class<?>[] interfaces() {
        return api;
    }

    public void failed(final boolean flag) {
        failed = flag;
    }

    public boolean failed() {
        return failed;
    }
}

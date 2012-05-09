package org.fluidity.deployment.osgi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.spi.CustomComponentFactory;
import org.fluidity.foundation.Generics;

/**
 * Binds registered service instances.
 *
 * @author Tibor Varga
 */
@Component(automatic = false)
@Component.Context({ Service.class, Component.Reference.class })
final class ServiceComponentFactory implements CustomComponentFactory {

    private final Map<String, ServiceDescriptor> services;
    private final Class<?>[] api;

    public ServiceComponentFactory(final ServiceDescriptor[] services) {
        final Map<String, ServiceDescriptor> map = new HashMap<String, ServiceDescriptor>();
        final Set<Class<?>> types = new HashSet<Class<?>>();

        for (final ServiceDescriptor descriptor : services) {
            final String filter = descriptor.filter;
            map.put(String.format("%s:%s", descriptor.type, filter == null ? "" : filter), descriptor);
            types.add(descriptor.type);
        }

        this.services = map;
        this.api = types.toArray(new Class[types.size()]);
    }

    public Instance resolve(final ComponentContext context, final Resolver dependencies) throws ComponentContainer.ResolutionException {
        final Service annotation = context.annotation(Service.class, null);
        final Component.Reference reference = context.annotation(Component.Reference.class, null);
        final Class<?> type = annotation.api();
        final ServiceDescriptor descriptor = services.get(String.format("%s:%s",
                                                                        type == Object.class ? Generics.rawType(reference.type()) : type,
                                                                        annotation.filter()));

        if (descriptor.instance() == null) {
            throw new ComponentContainer.ResolutionException(descriptor.toString());
        } else {
            return new Instance() {
                @SuppressWarnings("unchecked")
                public void bind(final Registry registry) throws ComponentContainer.BindingException {
                    registry.bindInstance(descriptor.instance(), (Class<Object>) descriptor.type);
                }
            };
        }
    }

    public Class<?>[] api() {
        return api;
    }
}

/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fluidity.deployment.osgi;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.Components;
import org.fluidity.composition.DependencyPath;
import org.fluidity.composition.ObservedComponentContainer;
import org.fluidity.composition.ServiceProvider;
import org.fluidity.composition.spi.CustomComponentFactory;
import org.fluidity.foundation.ClassDiscovery;
import org.fluidity.foundation.Generics;
import org.fluidity.foundation.Log;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Tibor Varga
 */
@Component
final class BundleComponentContainerImpl implements BundleComponentContainer {

    private final List<Stoppable> cleanup = new ArrayList<Stoppable>();

    private final Log log;
    private final BundleContext context;
    private final ComponentContainer container;
    private final BundleBoundary border;

    private final ServiceDescriptor[] services;
    private final ComponentDescriptor[] components;

    private final ServiceComponentFactory serviceFactory;

    @SuppressWarnings("unchecked")
    private final Status status = new Status() {

        public Collection<Class<?>> active() {
            final Collection<Class<?>> list = new ArrayList<Class<?>>();

            for (final ComponentDescriptor component : components) {
                if (component.instance() != null) {
                    list.addAll(Arrays.asList(component.interfaces()));
                }
            }

            return list;
        }

        public Map<Class<?>, Collection<Service>> inactive() {
            final Map<Class<?>, Collection<Service>> map = new HashMap<Class<?>, Collection<Service>>();
            final Set<ServiceDescriptor> services = activeServices();

            for (final ComponentDescriptor component : components) {
                if (component.instance() == null && !component.failed()) {
                    final Set<ServiceDescriptor> dependencies = new HashSet<ServiceDescriptor>(component.dependencies());
                    dependencies.removeAll(services);

                    final Collection<Service> list = new ArrayList<Service>();

                    for (final ServiceDescriptor dependency : dependencies) {
                        list.add(dependency.annotation);
                    }

                    for (final Class<?> type : component.interfaces()) {
                        map.put(type, list);
                    }
                }
            }

            return map;
        }

        public Collection<Class<?>> failed() {
            final Collection<Class<?>> list = new ArrayList<Class<?>>();

            for (final ComponentDescriptor component : components) {
                if (component.failed()) {
                    list.addAll(Arrays.asList(component.interfaces()));
                }
            }

            return list;
        }
    };

    public BundleComponentContainerImpl(final BundleContext context,
                                        final ComponentContainer container,
                                        final Log<BundleComponentContainerImpl> log,
                                        final BundleBoundary border,
                                        final ClassDiscovery discovery) {
        this.context = context;
        this.container = container;
        this.border = border;
        this.log = log;

        // find all managed component classes
        final Class<Managed>[] items = discovery.findComponentClasses(Managed.class, getClass().getClassLoader(), false);

        final Map<Class<Managed>, ComponentDescriptor> components = new HashMap<Class<Managed>, ComponentDescriptor>();

        final ComponentContainer pool = container.makeChildContainer(new ComponentContainer.Bindings() {
            @SuppressWarnings("unchecked")
            public void bindComponents(final ComponentContainer.Registry registry) {
                for (final Class<Managed> type : items) {
                    final List<Class<? super BundleComponentContainer.Managed>> interfaces = new ArrayList<Class<? super BundleComponentContainer.Managed>>();

                    for (final Components.Specification specification : inspect(type).api) {
                        final Class<? super BundleComponentContainer.Managed> api = (Class<? super BundleComponentContainer.Managed>) specification.api;

                        // we can't bind to service provider interfaces (group interfaces are not present in the iterated list)
                        if (api == type || !isServiceProvider(api)) {
                            interfaces.add(api);
                        }
                    }

                    final ComponentDescriptor descriptor = new ComponentDescriptor(type, interfaces);
                    components.put(type, descriptor);

                    registry.bindComponent(type, descriptor.interfaces());
                }
            }
        });

        final AtomicReference<Set<ServiceDescriptor>> dependencies = new AtomicReference<Set<ServiceDescriptor>>();

        // collects the OSGi service dependencies encountered during dependency resolution
        final ObservedComponentContainer observed = pool.observed(new ComponentContainer.Observer() {
            public void descend(final Class<?> declaringType,
                                final Class<?> dependencyType,
                                final Annotation[] typeAnnotations,
                                final Annotation[] referenceAnnotations) {
                for (final Annotation annotation : referenceAnnotations) {
                    if (annotation.annotationType() == Service.class) {
                        final ServiceDescriptor descriptor = new ServiceDescriptor(dependencyType, (Service) annotation);

                        if (dependencies.get().add(descriptor)) {
                            final String filter = descriptor.filter;

                            if (filter != null) {

                                // fail fast: verify the OSGi service filter

                                try {
                                    context.createFilter(filter);
                                } catch (final InvalidSyntaxException e) {
                                    throw new ComponentContainer.BindingException(e, "Invalid OSGi service filter at dependency %s of %s: %s", dependencyType, declaringType, filter);
                                }
                            }
                        }

                        break;     // it was an OSGi service: we're done
                    }
                }
            }

            public void ascend(final Class<?> declaringType, final Class<?> dependencyType) {
                // empty
            }

            public void circular(final DependencyPath path) {
                // empty
            }

            public void resolved(final DependencyPath path, final Class<?> type) {
                // empty
            }

            public void instantiated(final DependencyPath path, final AtomicReference<?> reference) {
                // empty
            }
        });

        final Set<ServiceDescriptor> services = new HashSet<ServiceDescriptor>();

        // get the observer methods invoked
        for (final Class<Managed> type : items) {
            final ComponentDescriptor descriptor = components.get(type);
            final Set<ServiceDescriptor> collected = new HashSet<ServiceDescriptor>();

            dependencies.set(collected);
            for (final Class<?> api : descriptor.interfaces()) {
                observed.resolveComponent(api);
            }

            descriptor.dependencies(collected);

            services.addAll(collected);
        }

        this.components = components.values().toArray(new ComponentDescriptor[components.size()]);
        this.services = services.toArray(new ServiceDescriptor[services.size()]);

        this.serviceFactory = new ServiceComponentFactory(this.services);
    }

    private boolean isServiceProvider(final Class<?> type) {
        if (type.isAnnotationPresent(ServiceProvider.class)) {
            return true;
        }

        for (final Class<?> api : type.getInterfaces()) {
            if (isServiceProvider(api)) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private Components.Interfaces inspect(final Class<Managed> type) {
        final Component componentAnnotation = type.getAnnotation(Component.class);
        if (componentAnnotation != null && componentAnnotation.automatic()) {
            throw new IllegalStateException(String.format("Managed component %s may not have @%s(automatic = true)", type, Component.class));
        } else if (componentAnnotation == null && type.isAnnotationPresent(ComponentGroup.class)) {
            throw new IllegalStateException(String.format("Managed component %s may not have @%s without @%s(automatic = false)", type, ComponentGroup.class, Component.class));
        } else {
            return Components.inspect(type);
        }
    }

    public void start() {
        startResolved();

        for (final ServiceDescriptor service : services) {
            final ServiceChangeListener listener = new ServiceChangeListener(service);
            final String filter = serviceFilter(service);

            try {
                context.addServiceListener(listener, filter);
            } catch (final InvalidSyntaxException e) {
                throw new IllegalStateException(filter, e);     // filter has already been verified when the listener was created
            }

            cleanup(String.format("service listener for %s %s", service.type, filter), listener);

            listener.servicesChanged(ServiceEvent.MODIFIED);
        }
    }

    public void stop() {
        for (final Iterator<Stoppable> iterator = cleanup.iterator(); iterator.hasNext(); ) {
            final Stoppable stoppable = iterator.next();

            try {
                stoppable.stop();
            } catch (final Exception e) {
                assert false : e;
            } finally {
                iterator.remove();
            }
        }
    }

    private void cleanup(final String name, final Stoppable stoppable) {
        cleanup.add(new Stoppable() {
            public void stop() {
                try {
                    stoppable.stop();
                } catch (final Exception e) {
                    log.error(e, "Stopping %s", name);
                }
            }
        });
    }

    private <T> void register(final Registration.Listener<T> source) {
        final String name = context.getBundle().getSymbolicName();
        final Class<T> type = source.clientType();

        final ServiceListener listener = new ServiceListener() {
            public void serviceChanged(final ServiceEvent event) {
                final ServiceReference reference = event.getServiceReference();

                @SuppressWarnings("unchecked")
                final T service = (T) context.getService(reference);

                switch (event.getType()) {
                case ServiceEvent.REGISTERED:
                    final Properties properties = new Properties();

                    for (final String key : reference.getPropertyKeys()) {
                        final Object property = reference.getProperty(key);
                        properties.setProperty(key, property.getClass().isArray() ? Arrays.toString((Object[]) property) : String.valueOf(property));
                    }

                    source.clientAdded(service, properties);
                    log.info("%s (%s) added to %s", service.getClass(), properties, source.getClass());

                    break;

                case ServiceEvent.UNREGISTERING:
                    source.clientRemoved(service);
                    log.info("%s removed from %s", service.getClass(), source.getClass());
                    break;

                default:
                    context.ungetService(reference);
                    break;
                }
            }
        };

        try {
            context.addServiceListener(listener, String.format("(%s=%s)", Constants.OBJECTCLASS, type.getName()));

            log.info("%s accepting %s components", name, source.getClass());

            final ServiceReference[] references = context.getServiceReferences(type.getName(), null);
            if (references != null) {
                for (final ServiceReference reference : references) {
                    listener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference));
                }
            }
        } catch (final InvalidSyntaxException e) {
            assert false : e;
        }

        cleanup(String.format("service listener for %s", source.getClass().getName()), new Stoppable() {
            public void stop() {
                context.removeServiceListener(listener);
                log.info("%s dropping %s components", name, source.getClass());
            }
        });
    }

    private void register(final Registration service) {
        final Properties properties = service.properties();
        final String[] classes = serviceApi(service.types());

        final String propertyMessage = properties == null ? "no properties" : String.format("properties %s", properties);
        final String serviceMessage = String.format("service for API %s with %s", Arrays.toString(classes), propertyMessage);

        log.info("Registering %s", serviceMessage);

        @SuppressWarnings({ "unchecked", "RedundantCast" })
        final ServiceRegistration registration = context.registerService(classes, service, (Dictionary) properties);

        cleanup(service.getClass().getName(), new Stoppable() {
            public void stop() {
                registration.unregister();
                log.info("Unregistered %s", serviceMessage);
            }
        });
    }

    private void start(final String name, final Managed component) throws Exception {
        component.start();

        log.info("%s started", name);

        if (component instanceof Registration.Listener) {
            register((Registration.Listener<?>) component);
        }

        if (component instanceof Registration) {
            register((Registration) component);
        }
    }

    private synchronized void stopped(final ServiceDescriptor service) {
        service.stopped();

        final Set<ServiceDescriptor> servicesUp = activeServices();
        final Set<ComponentDescriptor> unresolved = new HashSet<ComponentDescriptor>();

        for (final ComponentDescriptor descriptor : components) {
            if (descriptor.instance() != null && !resolved(servicesUp, descriptor)) {
                unresolved.add(descriptor);
            }
        }

        for (final Descriptor descriptor : unresolved) {
            final Managed component = (Managed) descriptor.stopped();
            assert component != null;   // we just checked above

            try {
                component.stop();
            } catch (final Exception e) {
                log.error(e, "Stopping %s", descriptor.toString());
            }
        }
    }

    private synchronized void started(final ServiceDescriptor service, final Object component) {
        service.started(component);
        startResolved();
    }

    private void startResolved() {
        final Set<ServiceDescriptor> servicesUp = activeServices();
        final Set<ComponentDescriptor> resolved = new HashSet<ComponentDescriptor>();
        final Set<ComponentDescriptor> running = new HashSet<ComponentDescriptor>();

        for (final ComponentDescriptor descriptor : components) {
            final Object instance = descriptor.instance();

            if (instance != null) {
                running.add(descriptor);
            } else if (resolved(servicesUp, descriptor)) {
                resolved.add(descriptor);
            }
        }

        if (!resolved.isEmpty()) {
            final ComponentContainer child = container.makeChildContainer(new ComponentContainer.Bindings() {
                @SuppressWarnings("unchecked")
                public void bindComponents(final ComponentContainer.Registry registry) {
                    registry.bindInstance(status);

                    final Class<?>[] services = serviceFactory.api();
                    if (services.length > 0) {
                        registry.bindFactory(serviceFactory, services); // will ONLY be valid when services.length > 0
                    }

                    for (final ComponentDescriptor descriptor : running) {
                        for (final Class<? super Managed> api : descriptor.interfaces()) {
                            registry.bindInstance(descriptor.instance(), (Class) api);
                        }
                    }

                    for (final ComponentDescriptor descriptor : resolved) {
                        final Class<? extends Managed> type = (Class<? extends Managed>) descriptor.type;

                        for (final Class<? super Managed> api : descriptor.interfaces()) {
                            registry.bindComponent(type, api);
                        }
                    }
                }
            });

            for (final ComponentDescriptor descriptor : resolved) {
                final String name = descriptor.toString();
                final Managed instance = (Managed) child.getComponent(descriptor.interfaces()[0]);

                try {
                    start(name, instance);

                    cleanup(name, new Stoppable() {
                        public void stop() throws Exception {
                            final Managed component = (Managed) descriptor.stopped();

                            if (component != null) {
                                component.stop();
                            }
                        }
                    });

                    descriptor.started(instance);
                } catch (final Exception e) {
                    log.error(e, "Failed to start %s", name);
                    descriptor.started(false);
                }
            }
        }
    }

    private Set<ServiceDescriptor> activeServices() {
        final Set<ServiceDescriptor> services = new HashSet<ServiceDescriptor>();

        for (final ServiceDescriptor descriptor : this.services) {
            if (descriptor.instance() != null) {
                services.add(descriptor);
            }
        }

        return services;
    }

    private boolean resolved(final Set<ServiceDescriptor> servicesUp, final ComponentDescriptor descriptor) {
        final Set<ServiceDescriptor> dependenciesUp = new HashSet<ServiceDescriptor>(descriptor.dependencies());

        dependenciesUp.retainAll(servicesUp);

        return dependenciesUp.containsAll(descriptor.dependencies());
    }

    private String serviceFilter(final ServiceDescriptor service) {
        final StringBuilder parsed = new StringBuilder();
        final String prefix = String.format("(%s=", Constants.OBJECTCLASS);

        final Class<?> dependency = service.type;
        final String filter = service.filter;

        final boolean filtered = filter != null && filter.length() > 0;

        if (filtered) {
            parsed.append("(&");
        }

        if (!filtered || !filter.contains(prefix)) {
            parsed.append(prefix).append(dependency.getName()).append(')');
        }

        if (filtered) {
            parsed.append(filter).append(")");
        }

        return parsed.toString();
    }

    private String[] serviceApi(final Class<?>[] types) {
        final String[] names = new String[types.length];

        for (int i = 0, limit = types.length; i < limit; i++) {
            names[i] = types[i].getName();
        }

        return names;
    }

    @Component(automatic = false)
    @Component.Context({ Service.class, Component.Reference.class})
    private static final class ServiceComponentFactory implements CustomComponentFactory {

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
            final ServiceDescriptor descriptor = services.get(String.format("%s:%s", type == Object.class ? Generics.rawType(reference.type()) : type, annotation.filter()));

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

    private class ServiceChangeListener implements ServiceListener, Stoppable {

        private final String name = context.getBundle().getSymbolicName();
        private final ServiceDescriptor descriptor;

        private ServiceReference reference;

        public ServiceChangeListener(final ServiceDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        @SuppressWarnings("unchecked")
        public void servicesChanged(final int eventType) {
            final boolean registered = eventType == ServiceEvent.REGISTERED;
            final boolean modified = eventType == ServiceEvent.MODIFIED;
            final boolean unregistering = eventType == ServiceEvent.UNREGISTERING;

            final ServiceReference[] references = references(descriptor);

            if (modified || unregistering) {
                boolean found = reference == null;      // reference == null: nothing to stop

                for (int i = 0, limit = references.length; !found && i < limit; i++) {
                    found = reference == references[i];     // TODO: we assume that if the reference is unregistering it will not be returned by the context
                }

                if (!found) {
                    stopped(descriptor);
                    context.ungetService(reference);
                    reference = null;
                }
            }

            final boolean stopped = reference == null;

            if (stopped && (modified || registered)) {
                if (references != null && references.length > 0) {
                    reference = references[0];
                    final Object instance = context.getService(reference);
                    started(descriptor, descriptor.type.isInterface() ? border.imported((Class) descriptor.type, instance) : instance);
                }
            }

            if (log.isInfoEnabled() && reference == null) {
                log.info("%s waiting for: %s", name, descriptor);
            }
        }

        public void serviceChanged(final ServiceEvent event) {
            servicesChanged(event.getType());
        }

        private ServiceReference[] references(final ServiceDescriptor service) {
            final String filter = service.filter;

            try {
                final ServiceReference[] references = context.getServiceReferences(service.type.getName(), filter);
                return references == null ? new ServiceReference[0] : references;
            } catch (final InvalidSyntaxException e) {
                throw new IllegalStateException(filter, e);   // filter has already been used when the listener was created
            }
        }

        public void stop() {
            context.removeServiceListener(this);
        }
    }
}

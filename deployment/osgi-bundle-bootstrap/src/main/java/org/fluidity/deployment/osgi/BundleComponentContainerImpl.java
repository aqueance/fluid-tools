/*
 * Copyright (c) 2006-2012 Tibor Adam Varga (tibor.adam.varga on gmail)
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
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.Components;
import org.fluidity.composition.DependencyPath;
import org.fluidity.composition.ObservedComponentContainer;
import org.fluidity.composition.ServiceProvider;
import org.fluidity.foundation.ClassDiscovery;
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

    private final BundleContext context;
    private final ComponentContainer container;
    private final Log<BundleComponentContainerImpl> log;
    private final ClassDiscovery discovery;

    BundleComponentContainerImpl(final BundleContext context,
                                 final ComponentContainer container,
                                 final Log<BundleComponentContainerImpl> log,
                                 final ClassDiscovery discovery) {
        this.context = context;
        this.container = container;
        this.log = log;
        this.discovery = discovery;
    }

    // postponed "construction" allows use of the container in the constructor
    private final AtomicReference<BundleComponentContainer> logic = new AtomicReference<BundleComponentContainer>();

    public void start() {
        logic.set(new Logic(context, container, log, discovery));
        logic.get().start();
    }

    public void stop() {
        assert logic.get() != null;
        logic.getAndSet(null).stop();
    }

    private static class Logic implements BundleComponentContainer {
        private final List<Stoppable> cleanup = new ArrayList<Stoppable>();

        private final Log log;
        private final BundleContext context;
        private final ComponentContainer container;

        private final String bundleName;

        private final ServiceDescriptor[] services;
        private final ComponentDescriptor[] components;

        private final ServiceComponentFactory serviceFactory;

        private final Status status = new ComponentStatus() {

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

        Logic(final BundleContext context, final ComponentContainer container, final Log<BundleComponentContainerImpl> log, final ClassDiscovery discovery) {
            this.context = context;
            this.container = container;
            this.log = log;

            this.bundleName = context.getBundle().getSymbolicName();

            // find all managed component classes
            final Class<Managed>[] items = discovery.findComponentClasses(Managed.class, getClass().getClassLoader(), false);

            final Map<Class<Managed>, ComponentDescriptor> components = new HashMap<Class<Managed>, ComponentDescriptor>();

            log.debug("[%s] Discovering service dependencies", bundleName);

            final ComponentContainer pool = container.makeChildContainer(new ComponentContainer.Bindings() {
                @SuppressWarnings("unchecked")
                public void bindComponents(final ComponentContainer.Registry registry) {
                    for (final Class<Managed> type : items) {
                        if (type.isAnnotationPresent(Component.Context.class)) {
                            log.warning("[%s] Managed component %s accepts context annotations; the received context will always be empty", bundleName, type);
                        }

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

                        registry.bindComponent((Class) type, descriptor.interfaces());
                    }
                }
            });

            final AtomicReference<Set<ServiceDescriptor>> dependencies = new AtomicReference<Set<ServiceDescriptor>>();

            // collects the OSGi service dependencies encountered during dependency resolution
            final ObservedComponentContainer observed = pool.observed(new ComponentContainer.Observer() {
                public void descending(final Class<?> declaringType,
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

                            break;     // it was an OSGi service dependency: we're done
                        }
                    }
                }

                public void ascending(final Class<?> declaringType, final Class<?> dependencyType) {
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

            // only remote services will need a dynamic service factory
            this.serviceFactory = new ServiceComponentFactory(remoteServices(services, components));
        }

        private ServiceDescriptor[] remoteServices(final Set<ServiceDescriptor> services, final Map components) {
            final List<ServiceDescriptor> remote = new ArrayList<ServiceDescriptor>();

            for (final ServiceDescriptor service : services) {
                if (!components.containsKey(service.type)) {
                    remote.add(service);
                }
            }

            return remote.toArray(new ServiceDescriptor[remote.size()]);
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
                        log.error(e, "[%s] Stopping %s", bundleName, name);
                    }
                }
            });
        }

        private <T> void register(final Registration.Listener<T> source) {
            final Class<T> type = source.type();

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

                        source.serviceAdded(service, properties);
                        log.debug("[%s] %s (%s) added to %s", bundleName, service.getClass(), properties, source.getClass());

                        break;

                    case ServiceEvent.UNREGISTERING:
                        source.serviceRemoved(service);
                        log.debug("[%s] %s removed from %s", bundleName, service.getClass(), source.getClass());
                        break;

                    default:
                        context.ungetService(reference);
                        break;
                    }
                }
            };

            final String sourceName = source.getClass().getName();

            try {
                context.addServiceListener(listener, String.format("(%s=%s)", Constants.OBJECTCLASS, type.getName()));

                log.debug("[%s] Accepting %s components", bundleName, sourceName);

                for (final ServiceReference<T> reference : context.getServiceReferences(type, null)) {
                    listener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference));
                }
            } catch (final InvalidSyntaxException e) {
                assert false : e;
            }

            cleanup(String.format("service listener for %s", sourceName), new Stoppable() {
                public void stop() {
                    context.removeServiceListener(listener);
                    log.debug("[%s] Ignoring %s components", bundleName, sourceName);
                }
            });
        }

        private void register(final Registration service) {
            final Properties properties = service.properties();
            final String[] classes = serviceApi(service.types());

            final String propertyMessage = properties == null ? "no properties" : String.format("properties %s", properties);
            final String serviceMessage = String.format("service for %s with %s", Arrays.toString(classes), propertyMessage);

            log.debug("[%s] Registering %s", bundleName, serviceMessage);

            @SuppressWarnings("unchecked")
            final ServiceRegistration registration = context.registerService(classes, service, (Dictionary) properties);

            cleanup(service.getClass().getName(), new Stoppable() {
                public void stop() {
                    registration.unregister();
                    log.debug("[%s] Unregistered %s", bundleName, serviceMessage);
                }
            });
        }

        private void start(final String name, final Managed component) throws Exception {
            component.start();

            log.debug("[%s] Started %s", bundleName, name);

            if (component instanceof Registration.Listener) {
                register((Registration.Listener<?>) component);
            }

            if (component instanceof Registration) {
                register((Registration) component);
            }
        }

        private synchronized void stopped(final ServiceDescriptor service) {
            service.stopped(false);

            final Set<ServiceDescriptor> servicesUp = activeServices();

            for (final ComponentDescriptor descriptor : components) {
                if ((descriptor.instance() != null || descriptor.failed()) && !resolved(servicesUp, descriptor)) {
                    final Managed component = (Managed) descriptor.stopped(false);

                    if (component != null) {
                        try {
                            component.stop();
                        } catch (final Exception e) {
                            log.error(e, "[%s] Stopping %s", bundleName, descriptor.toString());
                        }
                    }
                }
            }
        }

        private void started(final ServiceDescriptor service, final Object component) {
            service.started(component);

            startResolved();
        }

        private synchronized void startResolved() {
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
                log.debug("[%s] Starting components: %s", bundleName, resolved);

                final ComponentContainer child = container.makeChildContainer(new ComponentContainer.Bindings() {
                    @SuppressWarnings("unchecked")
                    public void bindComponents(final ComponentContainer.Registry registry) {
                        registry.bindInstance(status);

                        final Class<?>[] services = serviceFactory.api();
                        if (services.length > 0) {
                            registry.bindFactory(serviceFactory, services); // call is ONLY valid if services.length > 0
                        }

                        for (final ComponentDescriptor descriptor : running) {
                            for (final Class<?> api : descriptor.interfaces()) {
                                registry.bindInstance(descriptor.instance(), (Class) api);
                            }
                        }

                        for (final ComponentDescriptor descriptor : resolved) {
                            final Class<? extends Managed> type = (Class<? extends Managed>) descriptor.type;

                            for (final Class<?> api : descriptor.interfaces()) {
                                registry.bindComponent((Class) type, api);
                            }
                        }
                    }
                });

                for (final ComponentDescriptor descriptor : resolved) {
                    final String name = descriptor.toString();
                    final Managed instance = (Managed) child.getComponent(descriptor.interfaces()[0]);

                    // if instance is a service, it must be recorded in the descriptor before service registration takes place in the start() method
                    descriptor.started(instance);

                    try {
                        start(name, instance);

                        cleanup(name, new Stoppable() {
                            public void stop() throws Exception {
                                final Managed component = (Managed) descriptor.stopped(false);

                                if (component != null) {
                                    component.stop();
                                }
                            }
                        });
                    } catch (final Exception e) {
                        log.error(e, "[%s] Failed to start %s", bundleName, name);
                        descriptor.stopped(true);
                    }
                }
            }
        }

        private Set<ServiceDescriptor> activeServices() {
            final Set<ServiceDescriptor> active = new HashSet<ServiceDescriptor>();

            for (final ServiceDescriptor descriptor : services) {
                if (descriptor.instance() != null) {
                    active.add(descriptor);
                }
            }

            return active;
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

        private class ServiceChangeListener implements ServiceListener, Stoppable {

            private final ServiceDescriptor descriptor;

            private ServiceReference reference;

            ServiceChangeListener(final ServiceDescriptor descriptor) {
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
                        found = reference == references[i];     // we assume that if the service is unregistering, its reference will NOT be returned by the context
                    }

                    if (!found) {
                        log.debug("[%s] Learned of %s stopping", bundleName, descriptor);
                        stopped(descriptor);
                        context.ungetService(reference);
                        reference = null;
                    }
                }

                final boolean stopped = reference == null;

                if (stopped && (modified || registered)) {
                    if (references != null && references.length > 0) {
                        reference = references[0];
                        log.debug("[%s] Learned of %s having started", bundleName, descriptor);
                        started(descriptor, context.getService(reference));
                    }
                }

                if (reference == null) {
                    log.debug("[%s] Waiting for %s to start", bundleName, descriptor);
                }
            }

            public void serviceChanged(final ServiceEvent event) {
                servicesChanged(event.getType());
            }

            private ServiceReference[] references(final ServiceDescriptor service) {
                final String filter = service.filter;

                try {
                    @SuppressWarnings("unchecked")
                    final Collection<ServiceReference> references = context.getServiceReferences((Class) service.type, filter);
                    return references.toArray(new ServiceReference[references.size()]);
                } catch (final InvalidSyntaxException e) {
                    throw new IllegalStateException(filter, e);   // filter has already been used when the listener was created
                }
            }

            public void stop() {
                context.removeServiceListener(this);
            }
        }
    }
}

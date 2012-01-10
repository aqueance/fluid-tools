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
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.fluidity.composition.ClassDiscovery;
import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.Components;
import org.fluidity.composition.spi.DependencyInjector;
import org.fluidity.foundation.Log;
import org.fluidity.foundation.spi.LogFactory;

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

    private final Set<Stoppable> cleanup = new HashSet<Stoppable>();
    private final Map<Managed, Set<Class<?>>> clusters = new HashMap<Managed, Set<Class<?>>>();

    private final Log log;
    private final BundleContext context;
    private final ComponentContainer container;
    private final DependencyInjector injector;
    private final BundleBoundary border;

    private final Class<Managed>[] items;
    private final Observer[] listeners;

    private final Log listenerLog;

    public BundleComponentContainerImpl(final BundleContext context,
                                        final ComponentContainer container,
                                        final LogFactory logs,
                                        final DependencyInjector injector,
                                        final BundleBoundary border,
                                        final ClassDiscovery discovery,
                                        final @ComponentGroup Observer... listeners) {
        this.context = context;
        this.container = container;
        this.injector = injector;
        this.border = border;
        this.log = logs.createLog(BundleComponentContainerImpl.class);

        this.items = discovery.findComponentClasses(Managed.class, getClass().getClassLoader(), false);
        this.listeners = listeners;

        this.listenerLog = logs.createLog(ServiceChangeListener.class);
    }

    private static class Dependencies {
        public final Collection<ServiceSpecification> services = new HashSet<ServiceSpecification>();
        public final Collection<Class<Managed>> components = new HashSet<Class<Managed>>();
    }

    @SuppressWarnings({ "unchecked", "SuspiciousMethodCalls" })
    public void start() {
        final Map<Class<Managed>, Collection<Components.Interfaces>> clusters = new HashMap<Class<Managed>, Collection<Components.Interfaces>>();
        final Collection<Components.Interfaces> interfaces = new HashSet<Components.Interfaces>();

        for (final Class<Managed> type : items) {
            final Components.Interfaces inspection = inspect(type);
            assert inspection.implementation == type : type;
            interfaces.add(inspection);
            clusters.put(type, new HashSet<Components.Interfaces>(Collections.singleton(inspection)));
        }

        final Map<Class<?>, Dependencies> dependenciesMap = new HashMap<Class<?>, Dependencies>();

        final Collection<Class<Managed>> classes = new HashSet<Class<Managed>>(Arrays.asList(items));
        for (final Class<Managed> type : items) {
            final Dependencies dependencies = findDependencies(type, classes);

            dependenciesMap.put(type, dependencies);

            for (final Class<Managed> dependency : dependencies.components) {
                for (final Components.Interfaces inspection : interfaces) {
                    for (final Components.Specification specification : inspection.api) {
                        if (specification.api == dependency) {
                            final Collection<Components.Interfaces> combined = clusters.get(inspection.implementation);
                            combined.addAll(clusters.get(type));
                            clusters.put(type, combined);
                            break;      // the innermost loop
                        }
                    }
                }
            }
        }

        // wrapping in a hash set removes duplicates
        for (final Collection<Components.Interfaces> cluster : new HashSet<Collection<Components.Interfaces>>(clusters.values())) {
            final Collection<ServiceSpecification> services = new HashSet<ServiceSpecification>();

            for (final Components.Interfaces type : cluster) {
                final Dependencies dependencies = dependenciesMap.get(type.implementation);
                services.addAll(dependencies.services);
            }

            resolveDependencies(cluster, services, listenerLog);
        }
    }

    @SuppressWarnings("unchecked")
    private Components.Interfaces inspect(final Class<Managed> type) {
        final Component componentAnnotation = type.getAnnotation(Component.class);
        if (componentAnnotation != null && componentAnnotation.automatic()) {
            throw new IllegalStateException(String.format("Managed component %s may not have @%s(automatic = true)", type, Component.class));
        } else if (componentAnnotation == null && type.isAnnotationPresent(ComponentGroup.class)) {
            throw new IllegalStateException(String.format("Managed component %s may not have @%s without @%s(automatic = false)",
                                                          type,
                                                          ComponentGroup.class,
                                                          Component.class));
        } else {
            return Components.inspect(type);
        }
    }

    @SuppressWarnings("unchecked")
    private Dependencies findDependencies(final Class<Managed> type, final Collection<?> components) {
        final Dependencies dependencies = new Dependencies();

        final Constructor<?> constructor = injector.findConstructor(type);

        final Class<?>[] parameterTypes = constructor.getParameterTypes();
        final Annotation[][] parameterAnnotations = constructor.getParameterAnnotations();

        for (int i = 0, limit = parameterAnnotations.length; i < limit; i++) {
            final Class<?> parameterType = parameterTypes[i];
            final Annotation[] annotations = parameterAnnotations[i];

            boolean isService = false;
            for (final Annotation annotation : annotations) {
                if (annotation instanceof Service) {
                    final Service service = (Service) annotation;
                    dependencies.services.add(new ServiceSpecification(parameterType, service));
                    isService = true;
                    break;
                }
            }

            if (!isService && components.contains(parameterType)) {
                dependencies.components.add((Class<Managed>) parameterType);
            }
        }

        return dependencies;
    }

    private void cleanup(final String name, final Stoppable stoppable) {
        cleanup.add(new Stoppable() {
            public void stop() {
                if (cleanup.remove(this)) {
                    try {
                        stoppable.stop();
                    } catch (final Exception e) {
                        log.error(e, "Stopping %s", name);
                    }
                }
            }
        });
    }

    public void stop() {
        for (final Iterator<Map.Entry<Managed, Set<Class<?>>>> iterator = clusters.entrySet().iterator(); iterator.hasNext(); ) {
            final Map.Entry<Managed, Set<Class<?>>> entry = iterator.next();
            final Managed component = entry.getKey();
            final Set<Class<?>> types = entry.getValue();

            for (final Observer listener : listeners) {
                for (final Class<?> type : listener.types()) {
                    if (types.contains(type)) {
                        listener.stopping(type, component);
                    }
                }
            }

            iterator.remove();
        }

        for (final Stoppable stoppable : new HashSet<Stoppable>(cleanup)) {
            try {
                stoppable.stop();
            } catch (final Exception e) {
                log.error(e, "Stopping a managed component");
            }
        }
    }

    private <T> void register(final Registration.Listener<T> source) {
        final Class<T> type = source.clientType();

        final ServiceListener listener = new ServiceListener() {
            @SuppressWarnings("unchecked")
            public void serviceChanged(final ServiceEvent event) {
                final ServiceReference reference = event.getServiceReference();
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

            final ServiceReference[] references = context.getServiceReferences(type.getName(), null);
            if (references != null) {
                for (final ServiceReference reference : references) {
                    listener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference));
                }
            }
        } catch (final InvalidSyntaxException e) {
            assert false : e;
        }

        cleanup(source.getClass().getName(), new Stoppable() {
            public void stop() {
                context.removeServiceListener(listener);
            }
        });
    }

    private void register(final Registration service) {
        final Properties properties = service.properties();
        final String[] classes = serviceApi(service.types());

        final String propertyMessage = properties == null ? "no properties" : String.format("properties %s", properties);
        final String serviceMessage = String.format("service for API %s with %s", Arrays.toString(classes), propertyMessage);

        log.info("Registering %s", serviceMessage);

        final ServiceRegistration registration = context.registerService(classes, service, properties);

        cleanup(service.getClass().getName(), new Stoppable() {
            public void stop() {
                registration.unregister();
                log.info("Unregistered %s", serviceMessage);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void resolveDependencies(final Collection<Components.Interfaces> cluster, final Collection<ServiceSpecification> services, final Log listenerLog) {
        if (services.isEmpty()) {
            clusters.putAll(start(cluster));

            for (final Managed managed : clusters.keySet()) {
                cleanup(managed.getClass().getName(), managed);
            }
        } else {
            final ServiceSpecification[] dependencies = services.toArray(new ServiceSpecification[services.size()]);
            final ServiceChangeListener listener = new ServiceChangeListener(cluster, dependencies, listenerLog);

            final String filter = serviceFilter(dependencies);

            try {
                context.addServiceListener(listener, filter);
            } catch (final InvalidSyntaxException e) {
                throw new IllegalArgumentException(filter, e);
            }

            listener.servicesChanged(ServiceEvent.MODIFIED);

            cleanup("service listener", listener);
        }
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

    @SuppressWarnings({ "unchecked", "MismatchedQueryAndUpdateOfCollection" })
    Map<Managed, Set<Class<?>>> start(final Collection<Components.Interfaces> cluster, final ComponentContainer.Bindings... list) {
        final ComponentContainer child = container.makeChildContainer(new ComponentContainer.Bindings() {
            public void bindComponents(final ComponentContainer.Registry registry) {
                for (final ComponentContainer.Bindings bindings : list) {
                    bindings.bindComponents(registry);
                }

                for (final Components.Interfaces interfaces : cluster) {
                    registry.bindComponent(interfaces.implementation);
                }
            }
        });

        final Map<Managed, Set<Class<?>>> components = new HashMap<Managed, Set<Class<?>>>();

        for (final Components.Interfaces interfaces : cluster) {
            for (final Components.Specification specification : interfaces.api) {
                final Managed component = (Managed) child.getComponent(specification.api);

                Set<Class<?>> api = components.get(component);

                if (api == null) {
                    components.put(component, api = new HashSet<Class<?>>());
                }

                api.add(specification.api);
            }
        }

        for (final Iterator<Managed> iterator = components.keySet().iterator(); iterator.hasNext(); ) {
            final Managed component = iterator.next();
            final String name = component.getClass().getName();

            try {
                start(component.getClass().getName(), component);
            } catch (final Exception e) {
                iterator.remove();
                log.error(e, "starting %s", name);
            }
        }

        for (final Map.Entry<Managed, Set<Class<?>>> entry : components.entrySet()) {
            final Managed component = entry.getKey();
            final Set<Class<?>> types = entry.getValue();

            for (final Observer listener : listeners) {
                for (final Class<?> type : listener.types()) {
                    if (types.contains(type)) {
                        listener.started(type, component);
                    }
                }
            }
        }

        return components;
    }

    private String serviceFilter(final ServiceSpecification[] dependencies) {
        final StringBuilder parsed = new StringBuilder();
        final boolean multiple = dependencies.length > 1;
        final String prefix = String.format("(%s=", Constants.OBJECTCLASS);

        if (multiple) {
            parsed.append("(|");
        }

        for (final ServiceSpecification service : dependencies) {
            final Class<?> dependency = service.api;
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
        }

        if (multiple) {
            parsed.append(')');
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

        private final Map<ServiceSpecification, ServiceReference> referenceMap = new HashMap<ServiceSpecification, ServiceReference>();
        private final Map<ServiceSpecification, Object> dependencyMap = new HashMap<ServiceSpecification, Object>();

        private final Collection<Components.Interfaces> cluster;
        private final Log log;

        private final String name;
        private final Map<Managed, Set<Class<?>>> components = new IdentityHashMap<Managed, Set<Class<?>>>();

        public ServiceChangeListener(final Collection<Components.Interfaces> cluster, final ServiceSpecification[] dependencies, final Log log) {
            this.cluster = cluster;
            this.log = log;

            final StringBuilder builder = new StringBuilder();
            for (final Components.Interfaces interfaces : this.cluster) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }

                builder.append(interfaces.implementation.getName());
            }

            this.name = builder.toString();

            for (final ServiceSpecification dependency : dependencies) {
                dependencyMap.put(dependency, null);
            }
        }

        @SuppressWarnings("unchecked")
        public void servicesChanged(final int eventType) {
            final boolean registered = eventType == ServiceEvent.REGISTERED;
            final boolean modified = eventType == ServiceEvent.MODIFIED;
            final boolean unregistering = eventType == ServiceEvent.UNREGISTERING;

            if (modified || unregistering) {
                for (final ServiceSpecification service : new HashSet<ServiceSpecification>(dependencyMap.keySet())) {
                    final ServiceReference[] references = references(service);
                    final ServiceReference check = referenceMap.get(service);

                    boolean found = check == null;      // no reference: we have found it...

                    for (int i = 0, limit = references.length; !found && i < limit; i++) {
                        found = check == references[i];     // TODO: we assume that if the reference is unregistered it will not be returned by the context
                    }

                    if (!found) {
                        suspend();
                        dependencyMap.put(service, null);
                        context.ungetService(referenceMap.remove(service));
                    }
                }
            }

            final boolean stopped = dependencyMap.size() != referenceMap.size();

            if (stopped && (modified || registered)) {
                for (final ServiceSpecification service : new HashSet<ServiceSpecification>(dependencyMap.keySet())) {
                    final ServiceReference[] references = references(service);

                    for (int i = 0, limit = references.length; !referenceMap.containsKey(service) && i < limit; i++) {
                        final ServiceReference reference = references[i];
                        referenceMap.put(service, reference);
                        dependencyMap.put(service, border.imported((Class) service.api, context.getService(reference)));
                    }
                }
            }

            if (stopped && dependencyMap.size() == referenceMap.size()) {
                assert components.isEmpty();
                components.putAll(start(cluster, new ComponentContainer.Bindings() {
                    public void bindComponents(final ComponentContainer.Registry registry) {
                        for (final Map.Entry<ServiceSpecification, Object> entry : dependencyMap.entrySet()) {
                            registry.bindInstance(entry.getValue(), (Class<Object>) entry.getKey().api);
                        }
                    }
                }));
            }

            if (log.isInfoEnabled() && dependencyMap.size() != referenceMap.size()) {
                final List<ServiceSpecification> services = new ArrayList<ServiceSpecification>();

                for (final ServiceSpecification service : dependencyMap.keySet()) {
                    if (!referenceMap.containsKey(service)) {
                        services.add(service);
                    }
                }

                log.info("%s %s waiting for services: %s", name, components.size() > 1 ? "are" : "is", services);
            }
        }

        private void suspend() {
            for (final Map.Entry<Managed, Set<Class<?>>> entry : components.entrySet()) {
                final Managed component = entry.getKey();
                final Set<Class<?>> types = entry.getValue();

                for (final Observer listener : listeners) {
                    for (final Class<?> type : listener.types()) {
                        if (types.contains(type)) {
                            listener.stopping(type, component);
                        }
                    }
                }
            }

            for (final Iterator<Managed> iterator = components.keySet().iterator(); iterator.hasNext(); ) {
                final Managed component = iterator.next();
                try {
                    component.stop();
                    log.info("%s stopped", name);
                } catch (final Exception e) {
                    log.error(e, "stopping %s", name);
                } finally {
                    iterator.remove();
                }
            }
        }

        public void serviceChanged(final ServiceEvent event) {
            servicesChanged(event.getType());
        }

        private ServiceReference[] references(final ServiceSpecification service) {
            final String filter = service.filter;

            try {
                final ServiceReference[] references = context.getServiceReferences(service.api.getName(), filter == null || filter.length() == 0 ? null : filter);
                return references == null ? new ServiceReference[0] : references;
            } catch (final InvalidSyntaxException e) {
                throw new IllegalStateException(filter, e);   // filter has already been used when the listener was created
            }
        }

        public void stop() {
            context.removeServiceListener(this);
            suspend();
        }
    }
}

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
import org.fluidity.composition.Components;
import org.fluidity.composition.DependencyInjector;
import org.fluidity.composition.OpenComponentContainer;
import org.fluidity.foundation.logging.Log;
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
public class WhiteboardImpl implements Whiteboard {

    private Set<Stoppable> cleanup = new HashSet<Stoppable>();

    private final Log log;
    private final BundleContext context;
    private final ComponentContainer container;
    private final DependencyInjector injector;

    private final Class<Item>[] items;

    private Log listenerLog;

    public WhiteboardImpl(final BundleContext context,
                          final ComponentContainer container,
                          final LogFactory logs,
                          final DependencyInjector injector,
                          final ClassDiscovery discovery) {
        this.context = context;
        this.container = container;
        this.injector = injector;
        this.log = logs.createLog(WhiteboardImpl.class);

        this.items = discovery.findComponentClasses(Item.class, getClass().getClassLoader(), false);

        this.listenerLog = logs.createLog(ServiceChangeListener.class);
    }

    private static class Dependencies {
        public final Collection<ServiceSpecification> services = new HashSet<ServiceSpecification>();
        public final Collection<Class<Item>> components = new HashSet<Class<Item>>();
    }

    @SuppressWarnings( { "unchecked", "SuspiciousMethodCalls" })
    public void start() {
        final Map<Class<Item>, Collection<Components.Interfaces>> clusters = new HashMap<Class<Item>, Collection<Components.Interfaces>>();
        final Collection<Components.Interfaces> interfaces = new HashSet<Components.Interfaces>();

        for (final Class<Item> type : items) {
            final Components.Interfaces inspection = Components.inspect(type);
            assert inspection.implementation == type : type;
            interfaces.add(inspection);
            clusters.put(type, new HashSet<Components.Interfaces>(Collections.singleton(inspection)));
        }

        final Map<Class<?>, Dependencies> dependenciesMap = new HashMap<Class<?>, Dependencies>();

        final Collection<Class<Item>> classes = new HashSet<Class<Item>>(Arrays.asList(items));
        for (final Class<Item> type : items) {
            final Dependencies dependencies = findDependencies(type, classes);

            dependenciesMap.put(type, dependencies);

            for (final Class<Item> dependency : dependencies.components) {
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
    private Dependencies findDependencies(final Class<Item> type, final Collection<?> components) {
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
                dependencies.components.add((Class<Item>) parameterType);
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
        for (final Stoppable stoppable : new HashSet<Stoppable>(cleanup)) {
            try {
                stoppable.stop();
            } catch (final Exception e) {
                log.error(e, "Stopping a whiteboard item");
            }
        }
    }

    private <T> void register(final EventSource<T> source) {
        final Class<T> type = source.clientType();

        final ServiceListener listener = new ServiceListener() {
            @SuppressWarnings("unchecked")
            public void serviceChanged(final ServiceEvent event) {
                final ServiceReference reference = event.getServiceReference();

                switch (event.getType()) {
                case ServiceEvent.REGISTERED:
                    final Properties properties = new Properties();

                    for (final String key : reference.getPropertyKeys()) {
                        properties.setProperty(key, (String) reference.getProperty(key));
                    }

                    source.clientAdded((T) context.getService(reference), properties);

                    break;

                case ServiceEvent.UNREGISTERING:
                    source.clientRemoved((T) context.getService(reference));
                    break;

                default:
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

    private void register(final Registration service, final Properties properties, final Class<?>... types) {
        final ServiceRegistration registration = context.registerService(serviceApi(types), service, properties);

        cleanup(service.getClass().getName(), new Stoppable() {
            public void stop() {
                registration.unregister();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void resolveDependencies(final Collection<Components.Interfaces> cluster, final Collection<ServiceSpecification> services, final Log listenerLog) {
        if (services.isEmpty()) {
            final OpenComponentContainer child = container.makeChildContainer();
            final ComponentContainer.Registry registry = child.getRegistry();

            for (final Components.Interfaces interfaces : cluster) {
                final Class<?> type = interfaces.implementation;
                registry.bindComponent(type);

                final Item component = (Item) child.getComponent(type);
                final String name = component.getClass().getName();

                try {
                    start(name, component);
                } catch (final Exception e) {
                    log.error(e, "starting %s", name);
                }

                cleanup(type.getName(), component);
            }
        } else {
            final ServiceSpecification[] dependencies = services.toArray(new ServiceSpecification[services.size()]);
            final ServiceChangeListener listener = new ServiceChangeListener(container, cluster, dependencies, listenerLog);

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

    private void start(final String name, final Item component) throws Exception {
        component.start();

        if (component instanceof EventSource) {
            register((EventSource<?>) component);
        }

        if (component instanceof Registration) {
            final Registration registration = (Registration) component;
            register(registration, registration.properties(), registration.types());
        }

        log.info("%s started", name);
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

    @SuppressWarnings("unchecked")
    private class ServiceChangeListener implements ServiceListener, Stoppable {

        private final Map<ServiceSpecification, ServiceReference> referenceMap = new HashMap<ServiceSpecification, ServiceReference>();
        private final Map<ServiceSpecification, Object> dependencyMap = new HashMap<ServiceSpecification, Object>();

        private final ComponentContainer container;
        private final Collection<Components.Interfaces> cluster;
        private final Log log;

        private final String name;
        private final Map<Item, Object> components = new IdentityHashMap<Item, Object>();

        public ServiceChangeListener(final ComponentContainer container,
                                     final Collection<Components.Interfaces> cluster,
                                     final ServiceSpecification[] dependencies,
                                     final Log log) {
            this.container = container;
            this.cluster = cluster;
            this.log = log;

            final StringBuilder builder = new StringBuilder();
            for (final Components.Interfaces interfaces : cluster) {
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
                        found = check == references[i];     // TODO: we only assume that if the reference is unregistered it will not be returned by the context
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
                        dependencyMap.put(service, context.getService(reference));
                    }
                }
            }

            if (stopped && dependencyMap.size() == referenceMap.size()) {
                assert components.isEmpty();

                final OpenComponentContainer child = container.makeChildContainer();
                final ComponentContainer.Registry registry = child.getRegistry();

                for (final Map.Entry<ServiceSpecification, Object> entry : dependencyMap.entrySet()) {
                    registry.bindInstance(entry.getValue(), (Class<Object>) entry.getKey().api);
                }

                for (final Components.Interfaces interfaces : cluster) {
                    final Class<?> type = interfaces.implementation;
                    registry.bindComponent(type);
                }

                for (final Components.Interfaces interfaces : cluster) {
                    for (final Components.Specification specification : interfaces.api) {
                        components.put((Item) child.getComponent(specification.api), null);
                    }
                }

                for (final Iterator<Item> iterator = components.keySet().iterator(); iterator.hasNext(); ) {
                    final Item component = iterator.next();
                    final String name = component.getClass().getName();

                    try {
                        start(component.getClass().getName(), component);
                    } catch (final Exception e) {
                        iterator.remove();
                        log.error(e, "starting %s", name);
                    }
                }
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
            for (final Iterator<Item> iterator = components.keySet().iterator(); iterator.hasNext(); ) {
                final Item component = iterator.next();
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
                final ServiceReference[] references = context.getServiceReferences(service.api.getName(),
                                                                                   filter == null || filter.length() == 0 ? null : filter);
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

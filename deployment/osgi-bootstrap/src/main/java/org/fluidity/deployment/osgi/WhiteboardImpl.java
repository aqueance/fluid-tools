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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.OpenComponentContainer;
import org.fluidity.composition.Optional;
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

    final Whiteboard.EventSource<?>[] sources;
    final Whiteboard.Component[] components;
    final Whiteboard.Registration[] registrations;
    private Log listenerLog;

    public WhiteboardImpl(final BundleContext context,
                          final ComponentContainer container,
                          final LogFactory logs,
                          final @Optional @ComponentGroup Whiteboard.EventSource<?>[] sources,
                          final @Optional @ComponentGroup Whiteboard.Component[] components,
                          final @Optional @ComponentGroup Whiteboard.Registration[] registrations) {
        this.context = context;
        this.container = container;
        this.log = logs.createLog(WhiteboardImpl.class);

        this.sources = sources == null ? new EventSource<?>[0] : sources;
        this.components = components == null ? new Component[0] : components;
        this.registrations = registrations == null ? new Registration[0] : registrations;

        this.listenerLog = logs.createLog(ServiceChangeListener.class);
    }

    public void start() {
        if (sources != null) {
            for (final EventSource<?> source : sources) {
                final Stoppable stoppable = register(source);
                cleanup(stoppable);
                resolveDependencies(source, listenerLog);
            }
        }

        if (registrations != null) {
            for (final Registration registration : registrations) {
                cleanup(register(registration, registration.properties(), registration.types()));
                resolveDependencies(registration, listenerLog);
            }
        }

        if (components != null) {
            for (final Whiteboard.Component component : components) {
                resolveDependencies(component, listenerLog);
            }
        }
    }

    private Stoppable cleanup(final Stoppable stoppable) {
        final Stoppable wrapper = new Stoppable() {
            public void stop() {
                if (cleanup.remove(this)) {
                    try {
                        stoppable.stop();
                    } catch (final Exception e) {
                        log.error(e, "Stopping %s", stoppable);
                    }
                }
            }
        };

        cleanup.add(wrapper);

        return wrapper;
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

    private <T> Stoppable register(final EventSource<T> source) {
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

        return cleanup(new Stoppable() {
            public void stop() {
                context.removeServiceListener(listener);
            }
        });
    }

    private Stoppable register(final Registration service, final Properties properties, final Class<?>... types) {
        final ServiceRegistration registration = context.registerService(serviceApi(types), service, properties);

        return new Stoppable() {
            public void stop() {
                registration.unregister();
            }
        };
    }

    private void resolveDependencies(final Object component, final Log listenerLog) {
        final Class<?> type = component.getClass();

        Method start = null;
        for (final Method method : type.getMethods()) {
            final Start annotation = method.getAnnotation(Start.class);

            if (annotation != null) {
                if (start != null) {
                    throw new IllegalArgumentException(String.format("Whiteboard component %s has more than one @%s annotated methods", type,
                                                                     Start.class));
                }

                if (method.getReturnType() != Stoppable.class) {
                    throw new IllegalArgumentException(String.format("Whiteboard component %s does not return %s from its @%s annotated method", type,
                                                                     Stoppable.class,
                                                                     Start.class));
                }

                start = method;
            }
        }

        assert start != null;
        final Class<?>[] parameterTypes = start.getParameterTypes();
        final Map<Class<?>, ServiceSpecification> serviceTypes = new HashMap<Class<?>, ServiceSpecification>();
        final Annotation[][] parameterAnnotations = start.getParameterAnnotations();

        for (int i = 0, limit = parameterAnnotations.length; i < limit; i++) {
            final Annotation[] annotations = parameterAnnotations[i];

            for (final Annotation annotation : annotations) {
                if (annotation instanceof Service) {
                    final Service service = (Service) annotation;
                    serviceTypes.put(parameterTypes[i], new ServiceSpecification(service.api() == Object.class ? parameterTypes[i] : service.api(), service.filter()));
                    break;
                }
            }
        }

        if (serviceTypes.isEmpty()) {
            final Stoppable stop = (Stoppable) container.invoke(component, start);

            if (stop != null) {
                cleanup(stop);
            }
        } else {
            final ServiceSpecification[] dependencies = serviceTypes.values().toArray(new ServiceSpecification[serviceTypes.size()]);
            final ServiceChangeListener listener = new ServiceChangeListener(type.getName(), container, dependencies, context, listenerLog, component, start);

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

              if (!filtered || filter.indexOf(prefix) < 0) {
                parsed.append(prefix).append(dependency.getName()).append(')');
              }

              if (filtered) {
                parsed.append(filter).append(")");
              }
            }

            if (multiple) {
              parsed.append(')');
            }

            final String filter = parsed.toString();

            try {
              context.addServiceListener(listener, filter);
            } catch (final InvalidSyntaxException e) {
              throw new IllegalArgumentException(filter, e);
            }

            listener.servicesChanged(ServiceEvent.MODIFIED);

            cleanup(listener);
        }
    }

    private String[] serviceApi(final Class<?>[] types) {
        final String[] names = new String[types.length];

        for (int i = 0, limit = types.length; i < limit; i++) {
            names[i] = types[i].getName();
        }

        return names;
    }

    private static class ServiceSpecification {
        public final Class<?> api;
        public final String filter;

        private ServiceSpecification(final Class<?> api, final String filter) {
            this.api = api;
            this.filter = filter;
        }
    }

    @SuppressWarnings("unchecked")
    private static class ServiceChangeListener implements ServiceListener, Stoppable {

        private final Map<ServiceSpecification, ServiceReference> referenceMap = new HashMap<ServiceSpecification, ServiceReference>();
        private final Map<ServiceSpecification, Object> dependencyMap = new HashMap<ServiceSpecification, Object>();

        private final ComponentContainer container;
        private final BundleContext context;
        private final Object component;
        private final Log log;
        private final String name;

        private final Method start;
        private Stoppable stoppable;

        public ServiceChangeListener(final String name,
                                     final ComponentContainer container,
                                     final ServiceSpecification[] dependencies,
                                     final BundleContext context,
                                     final Log log,
                                     final Object component,
                                     final Method start) {
            this.name = name;
            this.container = container;
            this.context = context;
            this.component = component;
            this.start = start;
            this.log = log;

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
                assert stoppable == null;

                final OpenComponentContainer child = container.makeChildContainer();
                final ComponentContainer.Registry registry = child.getRegistry();

                for (final Map.Entry<ServiceSpecification, Object> entry : dependencyMap.entrySet()) {
                    registry.bindInstance(entry.getValue(), (Class<Object>) entry.getKey().api);
                }

                try {
                    stoppable = (Stoppable) child.invoke(component, start);
                    log.info("%s started", name);
                } catch (final Exception e) {
                    log.error(e, "starting %s", name);
                }
            }

            if (log.isInfoEnabled() && dependencyMap.size() != referenceMap.size()) {
                final List<ServiceSpecification> services = new ArrayList<ServiceSpecification>();

                for (final ServiceSpecification service : dependencyMap.keySet()) {
                    if (!referenceMap.containsKey(service)) {
                        services.add(service);
                    }
                }

                log.info("%s is waiting for services: %s", name, services);
            }
        }

        private void suspend() {
            if (stoppable != null) {
                try {
                    stoppable.stop();
                    log.info("%s stopped", name);
                } catch (final Exception e) {
                    log.error(e, "stopping %s", name);
                } finally {
                    stoppable = null;
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

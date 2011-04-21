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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.OpenComponentContainer;
import org.fluidity.composition.spi.ComponentResolutionObserver;
import org.fluidity.composition.spi.DependencyPath;
import org.fluidity.composition.spi.ShutdownTasks;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * @author Tibor Varga
 */
@Component
final class ServiceListenerFactoryImpl implements ServiceListenerFactory {

    private final ShutdownTasks shutdown;

    public ServiceListenerFactoryImpl(final ShutdownTasks shutdown) {
        this.shutdown = shutdown;
    }

    @SuppressWarnings("unchecked")
    public <T> void create(final Class<? super T> api,
                           final Class<T> type,
                           final ComponentContainer container,
                           final BundleContext context,
                           final Callback callback) {
        final OpenComponentContainer child = container.makeChildContainer();
        child.getRegistry().bindComponent(type, api);

        final DirectDependencies observer = new DirectDependencies(api);
        child.observed(observer).resolveComponent(api);

        final Map<Class<?>, String> dependencies = observer.dependencies();
        if (!dependencies.isEmpty()) {
            final StringBuilder parsed = new StringBuilder();

            final boolean multiple = dependencies.size() > 1;
            final String prefix = String.format("(%s=", Constants.OBJECTCLASS);

            if (multiple) {
                parsed.append("(|");
            }

            for (final Map.Entry<Class<?>, String> entry : dependencies.entrySet()) {
                final Class<?> dependency = entry.getKey();
                final String filter = entry.getValue();

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

            final Listener listener = new Listener(api, type, context, dependencies, container, callback);

            try {
                context.addServiceListener(listener, filter);

                shutdown.add(type.getName(), new Runnable() {
                    public void run() {
                        try {
                            context.removeServiceListener(listener);
                        } finally {
                            if (callback.started()) {
                                callback.stop();
                            }
                        }
                    }
                });

                // InvalidSyntaxException would have already been thrown if filter syntax were wrong
                final ServiceReference[] references = context.getServiceReferences(null, filter);
                if (references != null) {
                    for (final ServiceReference reference : references) {
                        listener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference));
                    }
                }
            } catch (final InvalidSyntaxException e) {
                throw new IllegalArgumentException(filter, e);
            }
        } else {

            // component has no service dependencies
            callback.start(container.makeChildContainer().getComponent(type));

            shutdown.add(type.getName(), new Runnable() {
                public void run() {
                    if (callback.started()) {
                        callback.stop();
                    }
                }
            });
        }
    }

    /**
     * @author Tibor Varga
     */
    private static class DirectDependencies implements ComponentResolutionObserver {
        private final Map<Class<?>, String> dependencies = new HashMap<Class<?>, String>();
        private final Class<?> api;

        private DirectDependencies(final Class<?> api) {
            this.api = api;
        }

        public void resolving(final Class<?> api,
                              final Class<?> declaringType,
                              final Class<?> dependencyType,
                              final Annotation[] typeAnnotations,
                              final Annotation[] referenceAnnotations) {
            final Service service = find(Service.class, referenceAnnotations);
            if (api == this.api && service != null) {
                dependencies.put(dependencyType, service.filter());
            }
        }

        public void resolved(final DependencyPath path, final Class<?> type) {
            // empty
        }

        public void instantiated(final DependencyPath path, final AtomicReference<?> ignored) {
            // empty
        }

        public Map<Class<?>, String> dependencies() {
            return dependencies;
        }

        @SuppressWarnings("unchecked")
        private <T extends Annotation> T find(final Class<T> type, final Annotation[] annotations) {
            for (final Annotation annotation : annotations) {
                if (type.isAssignableFrom(annotation.getClass())) {
                    return (T) annotation;
                }
            }

            return null;
        }

    }

    /**
     * @author Tibor Varga
     */
    @SuppressWarnings("unchecked")
    private static class Listener implements ServiceListener {
        private final Map<String, Long> identifiers = new HashMap<String, Long>();
        private final Map<Long, Object> services = new HashMap<Long, Object>();
        private final Class<?> type;
        private final Class<?> api;
        private final BundleContext context;
        private final Map<Class<?>, String> dependencies;
        private final ComponentContainer boundary;
        private final Callback callback;

        public Listener(final Class<?> api,
                        final Class<?> type,
                        final BundleContext context,
                        final Map<Class<?>, String> dependencies,
                        final ComponentContainer boundary,
                        final Callback callback) {
            this.type = type;
            this.context = context;
            this.dependencies = dependencies;
            this.boundary = boundary;
            this.callback = callback;
            this.api = api;
        }

        public void serviceChanged(final ServiceEvent event) {
            final ServiceReference reference = event.getServiceReference();
            final String[] names = (String[]) reference.getProperty(Constants.OBJECTCLASS);
            final Long id = (Long) reference.getProperty(Constants.SERVICE_ID);

            final boolean started = callback.started();

            switch (event.getType()) {
            case ServiceEvent.REGISTERED:
                if (!started) {
                    final Object service = context.getService(reference);

                    for (final String name : names) {
                        if (!identifiers.containsKey(name)) {
                            identifiers.put(name, id);
                            services.put(id, service);
                        }
                    }

                    // got all dependencies?
                    if (services.size() == dependencies.size()) {
                        final OpenComponentContainer container = boundary.makeChildContainer();

                        final ComponentContainer.Registry registry = container.getRegistry();
                        registry.bindComponent(type);
                        for (final Class<?> dependency : dependencies.keySet()) {
                            registry.bindInstance(services.get(identifiers.get(dependency.getName())), (Class<Object>) dependency);
                        }

                        callback.start(container.getComponent(api));
                    }
                }

                break;

            case ServiceEvent.UNREGISTERING:
                final Collection<Long> ids = identifiers.values();
                if (ids.contains(id)) {
                    ids.remove(id);
                    services.remove(id);

                    // any of the dependencies disappeared?
                    if (started && services.size() != dependencies.size()) {
                        callback.stop();
                    }
                }

                break;

            default:
                break;
            }
        }

    }
}

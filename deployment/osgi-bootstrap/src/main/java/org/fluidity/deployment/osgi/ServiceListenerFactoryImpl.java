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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fluidity.composition.Component;
import org.fluidity.composition.spi.ShutdownTasks;
import org.fluidity.foundation.logging.Log;
import org.fluidity.foundation.logging.Marker;
import org.fluidity.foundation.spi.LogFactory;

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
    private final LogFactory logFactory;
    private final Log log;

    public ServiceListenerFactoryImpl(final ShutdownTasks shutdown, final LogFactory logFactory, final @Marker(ServiceListenerFactoryImpl.class) Log log) {
        this.shutdown = shutdown;
        this.logFactory = logFactory;
        this.log = log;
    }

    @SuppressWarnings("unchecked")
    public Handle create(final String name,
                         final BundleContext context,
                         final Map<Service, ServiceDependencyFactory.MutableReference> services,
                         final Callback callback) {
        final Set<Service> dependencies = services.keySet();

        if (!dependencies.isEmpty()) {
            final Listener listener = new Listener(name, context, services, logFactory.createLog(Listener.class), callback);

            final StringBuilder parsed = new StringBuilder();

            final boolean multiple = dependencies.size() > 1;
            final String prefix = String.format("(%s=", Constants.OBJECTCLASS);

            if (multiple) {
                parsed.append("(|");
            }

            for (final Service service : dependencies) {
                final Class<?> dependency = service.api();
                final String filter = service.filter();

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

            // tricky workflow to make sure the listener's handle is set before we call its servicesChanged() method, which refers to the handle
            try {
                return handle(context, callback, name, listener, filter);
            } finally {
                listener.servicesChanged(ServiceEvent.MODIFIED);
            }
        } else {

            // component has no service dependencies
            callback.resume();
            log.info("%s resumed", name);

            return handle(context, callback, name, null, null);
        }
    }

    private Handle handle(final BundleContext context, final Callback callback, final String name, final Listener listener, final String filter) {
        if (listener != null) {
            log.info("Registering service listener with filter '%s' for %s", filter, name);
        }

        final Runnable command = new Runnable() {
            final AtomicBoolean removed = new AtomicBoolean(false);

            public void run() {
                if (!removed.get()) {
                    try {
                        if (listener != null) {
                            context.removeServiceListener(listener);
                            log.info("Unregistered service listener with filter '%s' for %s", filter, name);
                        }

                        removed.set(true);
                    } finally {
                        if (callback.valid() && callback.running()) {
                            callback.suspend();
                            log.info("%s suspended", name);
                        }
                    }
                }
            }
        };

        shutdown.add(name, command);

        final Handle handle = new Handle() {
            public void remove() {
                command.run();
            }
        };

        if (listener != null) {
            listener.setHandle(handle);     // TODO: setters are ugly...
        }

        return handle;
    }

    /**
     * @author Tibor Varga
     */
    @SuppressWarnings("unchecked")
    private static class Listener implements ServiceListener {

        private final Map<Service, ServiceReference> referenceMap = new HashMap<Service, ServiceReference>();

        private final Map<Service, ServiceDependencyFactory.MutableReference> dependencyMap;
        private final BundleContext context;
        private final Callback callback;
        private final Log log;
        private final String name;

        private Handle handle;

        public Listener(final String name,
                        final BundleContext context,
                        final Map<Service, ServiceDependencyFactory.MutableReference> dependencyMap,
                        final Log log,
                        final Callback callback) {
            this.name = name;
            this.context = context;
            this.dependencyMap = dependencyMap;
            this.callback = callback;
            this.log = log;
        }

        public void setHandle(final Handle handle) {
            assert handle != null;
            assert this.handle == null;
            this.handle = handle;
        }

        public void servicesChanged(final int eventType) {
            if (!callback.valid()) {
                if (handle != null) {
                    handle.remove();
                }
            } else {
                final boolean registered = eventType == ServiceEvent.REGISTERED;
                final boolean modified = eventType == ServiceEvent.MODIFIED;
                final boolean unregistering = eventType == ServiceEvent.UNREGISTERING;

                if (modified || unregistering) {
                    for (Map.Entry<Service, ServiceDependencyFactory.MutableReference> entry : dependencyMap.entrySet()) {
                        final Service service = entry.getKey();
                        final ServiceReference[] references = references(service);
                        final ServiceReference check = referenceMap.get(service);

                        boolean found = check == null;      // no reference: we have found it...

                        for (int i = 0, limit = references.length; !found && i < limit; i++) {
                            found = check == references[i];     // we assume that if the reference is unregistered it will not be returned by the context
                        }

                        if (!found) {
                            if (callback.running()) {
                                callback.suspend();
                                assert !callback.running();
                                log.info("%s suspended", name);
                            }

                            entry.getValue().remove();
                            context.ungetService(referenceMap.remove(service));
                        }
                    }
                }

                if (!callback.running() && (modified || registered)) {
                    for (Map.Entry<Service, ServiceDependencyFactory.MutableReference> entry : dependencyMap.entrySet()) {
                        final Service service = entry.getKey();
                        final ServiceReference[] references = references(service);

                        for (int i = 0, limit = references.length; !referenceMap.containsKey(service) && i < limit; i++) {
                            final ServiceReference reference = references[i];
                            referenceMap.put(service, reference);
                            entry.getValue().set(context.getService(reference));
                        }

                        if (!callback.running() && dependencyMap.size() == referenceMap.size()) {
                            callback.resume();
                            assert callback.running();
                            log.info("%s resumed", name);
                        }
                    }
                }
            }
        }

        public void serviceChanged(final ServiceEvent event) {
            servicesChanged(event.getType());
        }

        private ServiceReference[] references(final Service service) {
            final String filter = service.filter();

            try {
                final ServiceReference[] references = context.getServiceReferences(service.api().getName(), filter.length() == 0 ? null : filter);
                return references == null ? new ServiceReference[0] : references;
            } catch (final InvalidSyntaxException e) {
                throw new IllegalStateException(filter, e);   // filter has already been used when the listener was created
            }
        }
    }
}

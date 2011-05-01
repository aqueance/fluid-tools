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

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.Optional;

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

    private Set<Handle> listeners = new HashSet<Handle>();

    private final BundleContext context;

    public WhiteboardImpl(final BundleContext context,
                          final @Optional @ComponentGroup EventSource<Object>[] sources,
                          final @Optional @ComponentGroup Service.Registration[] registrations) {
        this.context = context;

        if (sources != null) {
            for (final EventSource<Object> source : sources) {
                register(source);
            }
        }

        if (registrations != null) {
            for (final Service.Registration registration : registrations) {
                register(registration, registration.properties(), registration.types());
            }
        }
    }

    public <T> Handle register(final EventSource<T> source) {
        final Class<T> type = source.consumerType();

        final ServiceListener listener = new ServiceListener() {
            @SuppressWarnings("unchecked")
            public void serviceChanged(final ServiceEvent event) {
                final ServiceReference reference = event.getServiceReference();

                switch (event.getType()) {
                case ServiceEvent.REGISTERED:
                    source.consumerAdded((T) context.getService(reference), new RegistrationProperties() {
                        public String[] keys() {
                            return reference.getPropertyKeys();
                        }

                        @SuppressWarnings("unchecked")
                        public <T> T get(final String key, final Class<T> type) {
                            return (T) reference.getProperty(key);
                        }
                    });

                    break;

                case ServiceEvent.UNREGISTERING:
                    source.consumerRemoved((T) context.getService(reference));
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

        final Handle handle = new Handle() {
            public void remove() {
                if (listeners.contains(this)) {
                    context.removeServiceListener(listener);
                    source.stop();
                    listeners.remove(this);
                }
            }
        };

        listeners.add(handle);

        return handle;
    }

    public Handle register(final Service.Registration service, final Properties properties, final Class<?>... types) {
        final ServiceRegistration registration = context.registerService(serviceApi(types), service, properties);

        return new Handle() {
            public void remove() {
                registration.unregister();
            }
        };
    }

    public void stop() {
        for (final Handle listener : new HashSet<Handle>(listeners)) {
            listener.remove();
        }
    }

    private String[] serviceApi(final Class<?>[] types) {
        final String[] names = new String[types.length];

        for (int i = 0, limit = types.length; i < limit; i++) {
            names[i] = types[i].getName();
        }

        return names;
    }
}

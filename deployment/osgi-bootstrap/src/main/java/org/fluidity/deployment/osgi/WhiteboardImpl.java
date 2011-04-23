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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

/**
 * @author Tibor Varga
 */
@Component
public class WhiteboardImpl implements Whiteboard {

    private Set<ServiceListener> listeners = new HashSet<ServiceListener>();
    private List<EventSource<?>> sources = new ArrayList<EventSource<?>>();
    private final BundleContext context;

    public WhiteboardImpl(final BundleContext context,
                          final @Optional @ComponentGroup EventSource<Object>[] sources,
                          final @Optional @ComponentGroup Service.Registration[] registrations) {
        this.context = context;

        if (sources != null) {
            for (final EventSource<Object> source : sources) {
                this.sources.add(source);

                final Class<Object> type = source.consumerType();

                final ServiceListener listener = new ServiceListener() {
                    public void serviceChanged(final ServiceEvent event) {
                        final ServiceReference reference = event.getServiceReference();

                        switch (event.getType()) {
                        case ServiceEvent.REGISTERED:
                            source.consumerAdded(context.getService(reference), new RegistrationProperties() {
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
                            source.consumerRemoved(context.getService(reference));
                            break;

                        default:
                            break;
                        }
                    }
                };

                try {
                    context.addServiceListener(listener, String.format("(%s=%s)", Constants.OBJECTCLASS, type.getName()));

                    for (final ServiceReference reference : context.getServiceReferences(type.getName(), null)) {
                        listener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference));
                    }
                } catch (final InvalidSyntaxException e) {
                    assert false : e;
                }


                listeners.add(listener);
            }
        }

        if (registrations != null) {
            for (final Service.Registration registration : registrations) {
                registration.perform(context);
            }
        }
    }

    public Handle register(final EventConsumer consumer, final Properties properties, final Class<?>... types) {
        final org.osgi.framework.ServiceRegistration registration = context.registerService(serviceApi(types), consumer, properties);

        return new Handle() {
            public void remove() {
                registration.unregister();
            }
        };
    }

    public void stop() {
        for (final ServiceListener listener : listeners) {
            context.removeServiceListener(listener);
        }

        for (final EventSource<?> source : sources) {
            source.stop();
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

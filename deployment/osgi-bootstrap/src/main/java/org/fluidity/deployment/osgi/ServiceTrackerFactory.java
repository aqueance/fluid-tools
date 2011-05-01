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

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.OpenComponentContainer;
import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.foundation.logging.Log;
import org.fluidity.foundation.logging.Marker;

import org.osgi.framework.BundleContext;

/**
 * @author Tibor Varga
 */
@Component(api = ServiceTracker.class)
final class ServiceTrackerFactory implements ComponentFactory {

    public Instance resolve(final ComponentContext context, final Resolver dependencies) throws ComponentContainer.ResolutionException {
        dependencies.discover(ServiceTrackerImpl.class);

        return new Instance() {

            @SuppressWarnings("unchecked")
            public void bind(final Registry registry) throws ComponentContainer.BindingException {
                registry.bindComponent(ServiceTrackerImpl.class);
            }
        };
    }

    /**
     * @author Tibor Varga
     */
    @Component(automatic = false)
    private static class ServiceTrackerImpl implements ServiceTracker {

        private final BundleContext context;
        private final ServiceListenerFactory factory;
        private final ComponentContainer container;
        private final ServiceDependencyFactory dependencies;
        private final Log log;

        /**
         * Creates a new service tracker.
         *
         * @param context   the context for the calling bundle.
         * @param factory   the listener factory to use.
         * @param container the container that captures the component context for the component to manage by {@link ServiceTracker#manage(Class)}.
         */
        private ServiceTrackerImpl(final BundleContext context,
                                   final ServiceListenerFactory factory,
                                   final ComponentContainer container,
                                   final ServiceDependencyFactory dependencies,
                                   final @Marker(ServiceTrackerImpl.class) Log log) {
            this.context = context;
            this.factory = factory;
            this.container = container;
            this.dependencies = dependencies;
            this.log = log;
        }

        public <T extends ServiceTracker.Managed> Reference<T> manage(final Class<T> type) {
            return manage(type, container);
        }

        public <T extends ServiceTracker.Managed> Reference<T> manage(final Class<T> type, final ComponentContainer container) {
            final Component annotation = type.getAnnotation(Component.class);

            if (annotation == null) {
                throw new ComponentContainer.ResolutionException("Class %s does not have @%s", type, Component.class);
            }

            if (annotation.automatic()) {
                throw new ComponentContainer.ResolutionException("Class %s does not have @%s(automatic = false)", type, Component.class);
            }

            final ComponentContainer contextual = container == this.container ? container : container.inheritContext(this.container);
            final OpenComponentContainer child = contextual.makeChildContainer();
            child.getRegistry().bindComponent(type, ServiceTracker.Managed.class);

            final Map<Service, ServiceDependencyFactory.MutableReference> services = new HashMap<Service, ServiceDependencyFactory.MutableReference>();

            @SuppressWarnings("unchecked")
            final T component = (T) dependencies.instantiate(ServiceTracker.Managed.class, child, services);

            // we keep a weak link to the component
            final WeakReference<T> link = new WeakReference<T>(component);

            // and maintain a reference that we control
            final AtomicReference<WeakReference<T>> reference = new AtomicReference<WeakReference<T>>();
            final AtomicBoolean failed = new AtomicBoolean(false);

            factory.create(type.getName(), context, services, new ServiceListenerFactory.Callback() {
                public boolean valid() {
                    return link.get() != null;
                }

                public boolean running() {
                    return reference.get() != null;
                }

                public void resume() {
                    reference.set(link);

                    try {
                        final T component = link.get();

                        if (component != null) {
                            component.resume();
                        }
                    } catch (final Exception e) {
                        failed.set(true);
                        log.error(e, "Failed to resume %s", type);
                    }
                }

                public void suspend() {
                    if (!failed.get()) {
                        try {
                            final T component = link.get();

                            if (component != null) {
                                component.suspend();
                            }
                        } catch (final Exception e) {
                            log.error(e, "Error suspending %s", type);
                        } finally {
                            reference.set(null);
                        }
                    } else {
                        failed.set(false);
                    }
                }
            });

            return new Reference<T>() {
                public T get() throws IllegalStateException {
                    final T instance = reference.get().get();

                    if (instance == null) {
                        throw new IllegalStateException(String.format("Component %s not available", type));
                    }

                    return instance;
                }
            };
        }
    }
}

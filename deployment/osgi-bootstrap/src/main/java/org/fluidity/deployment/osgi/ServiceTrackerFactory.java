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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.foundation.logging.Log;
import org.fluidity.foundation.logging.Marker;

import org.osgi.framework.BundleContext;

/**
 * TODO: remove the factory
 * TODO: pass the container in #manage(...) [is it the actual vehicle to pass on the context?]
 * TODO: pass original container around when resolving dependencies?
 * TODO: get rid of the trackers set, use a singleton to maintain the list of listeners to terminate at bundle stop
 *
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
                                   final @Marker(ServiceTrackerImpl.class) Log log) {
            this.context = context;
            this.factory = factory;
            this.container = container;
            this.log = log;
        }

        public <T extends ServiceTracker.Managed> Dependency<T> manage(final Class<T> type) {
            return manage(type, container);
        }

        public <T extends ServiceTracker.Managed> Dependency<T> manage(final Class<T> type, final ComponentContainer container) {
            final Component annotation = type.getAnnotation(Component.class);

            if (annotation == null) {
                throw new ComponentContainer.ResolutionException("Class %s does not have @%s", type, Component.class);
            }

            if (annotation.automatic()) {
                throw new ComponentContainer.ResolutionException("Class %s does not have @%s(automatic = false)", type, Component.class);
            }

            if (!annotation.stateful()) {
                throw new ComponentContainer.ResolutionException("Class %s does not have @%s(stateful = true)", type, Component.class);
            }

            final AtomicReference<T> component = new AtomicReference<T>();
            final AtomicBoolean failed = new AtomicBoolean(false);

            final ComponentContainer contextual = container == this.container ? container : container.inheritContext(this.container);
            factory.create(Managed.class, type, contextual, context, new ServiceListenerFactory.Callback<T>() {
                public boolean started() {
                    return component.get() != null;
                }

                public void start(final T object) {
                    assert object != null : type;
                    component.set(object);

                    try {
                        object.start();
                    } catch (final Exception e) {
                        failed.set(true);
                        log.error(e, "Failed to start %s", type);
                    }
                }

                public void stop() {
                    if (!failed.get()) {
                        try {
                            component.get().stop();
                        } catch (final Exception e) {
                            log.error(e, "Error stopping %s", type);
                        } finally {
                            component.set(null);
                        }
                    } else {
                        failed.set(false);
                    }
                }
            });

            return new Dependency<T>() {
                public T get() throws IllegalStateException {
                    final T instance = component.get();

                    if (instance == null) {
                        throw new IllegalStateException(String.format("Component %s not available", type));
                    }

                    return instance;
                }
            };
        }
    }
}

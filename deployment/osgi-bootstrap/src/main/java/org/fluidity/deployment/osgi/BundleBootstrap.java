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

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ContainerBoundary;
import org.fluidity.composition.ContextDefinition;
import org.fluidity.composition.Inject;
import org.fluidity.composition.spi.PlatformContainer;
import org.fluidity.composition.spi.ShutdownTasks;
import org.fluidity.deployment.DeployedComponent;
import org.fluidity.deployment.DeploymentBootstrap;
import org.fluidity.deployment.DeploymentControl;
import org.fluidity.deployment.DeploymentObserver;
import org.fluidity.foundation.logging.Log;
import org.fluidity.foundation.logging.Marker;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bootstraps the dependency injection container in an OSGi bundle. Must be public with a zero-arg constructor for the OSGi container to be able to call.
 *
 * @author Tibor Varga
 */
public final class BundleBootstrap implements BundleActivator {

    private final Map<Runnable, String> shutdown = new LinkedHashMap<Runnable, String>();
    private final List<BundleActivator> activators = new ArrayList<BundleActivator>();

    @Inject
    @Marker(BundleBootstrap.class)
    private Log log;

    @SuppressWarnings("unchecked")
    public void start(final BundleContext context) throws Exception {
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        final ContainerBoundary boundary = new ContainerBoundary();

        // interfaces that are queried here and are certainly not OSGi services
        final Set<Class<?>> ignored = new HashSet<Class<?>>(Arrays.asList(Service.class,
                                                                          DeploymentBootstrap.class,
                                                                          DeployedComponent.class,
                                                                          DeploymentObserver.class,
                                                                          BundleActivator.class));
        final PlatformContainer platform = new ServiceContainer(context);

        boundary.setPlatformContainer(new PlatformContainer() {
            public boolean containsComponent(final Class<?> api, final ContextDefinition context) {
                return !ignored.contains(api) && platform.containsComponent(api, context);
            }

            public <T> T getComponent(final Class<T> api, final ContextDefinition context) throws ComponentContainer.ResolutionException {
                return ignored.contains(api) ? null : platform.getComponent(api, context);
            }

            public boolean containsComponentGroup(final Class<?> api, final ContextDefinition context) {
                return !ignored.contains(api) && platform.containsComponentGroup(api, context);
            }

            public <T> T[] getComponentGroup(final Class<T> api, final ContextDefinition context) {
                return ignored.contains(api) ? null : platform.getComponentGroup(api, context);
            }

            public String id() {
                return platform.id();
            }

            public void stop() {
                platform.stop();
            }
        });
        boundary.bindBootComponent(new ShutdownTasks() {
            public void add(final String name, final Runnable command) {
                shutdown.put(command, name);
            }
        });
        boundary.bindBootComponent(new DeploymentControl() {
            public void completed() {
                // empty
            }

            public boolean isStandalone() {
                return false;
            }

            public void stop() {
                // empty
            }
        });
        boundary.bindBootComponent(context, BundleContext.class);

        boundary.initialize(this);
        boundary.initialize(platform);

        final Service[] services = boundary.getComponentGroup(Service.class);
        final ServiceRegistration[] registrations = new ServiceRegistration[services == null ? 0 : services.length];
        if (services != null) {
            for (int i = 0, limit = services.length; i < limit; i++) {
                final Service service = services[i];
                registrations[i] = context.registerService(serviceApi(service), service.service(), service.properties());
            }
        }

        final DeploymentBootstrap deployments = boundary.getComponent(DeploymentBootstrap.class);

        deployments.load();

        shutdown.put(new Runnable() {
            public void run() {
                deployments.unload();

                for (final ServiceRegistration registration : registrations) {
                    registration.unregister();
                }

                platform.stop();
            }
        }, "platform");

        addActivators(activators, boundary.getComponent(BundleActivator.class));
        addActivators(activators, boundary.getComponentGroup(BundleActivator.class));

        for (final BundleActivator activator : activators) {
            activator.start(context);
        }
    }

    private void addActivators(final List<BundleActivator> list, final BundleActivator... found) {
        if (found != null) {
            for (final BundleActivator activator : found) {
                if (activator != null) {
                    list.add(activator);
                }
            }
        }
    }

    private String[] serviceApi(final Service service) {
        final Class<?>[] api = service.api();
        final String[] names = new String[api.length];

        for (int i = 0, limit = api.length; i < limit; i++) {
            names[i] = api[i].getName();
        }

        return names;
    }

    public void stop(final BundleContext context) throws Exception {
        try {
            for (final BundleActivator activator : activators) {
                try {
                    activator.stop(context);
                } catch (final Exception e) {
                    log.warning(e, "Stopping %s", activator.getClass().getName());
                }
            }
        } finally {
            for (final Map.Entry<Runnable, String> entry : shutdown.entrySet()) {
                try {
                    entry.getKey().run();
                } catch (final Exception e) {
                    log.warning(e, "Shutting down %s", entry.getValue());
                }
            }
        }
    }
}

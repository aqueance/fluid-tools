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

package org.fluidity.composition;

import java.util.List;
import java.util.Map;

import org.fluidity.composition.spi.ContainerProvider;
import org.fluidity.composition.spi.PackageBindings;
import org.fluidity.composition.spi.ShutdownTasks;
import org.fluidity.foundation.logging.Log;

/**
 * Bootstraps the component container. This class is exported via the standard service provider discovery mechanism described in the <a
 * href="http://download.oracle.com/javase/1.5.0/docs/guide/jar/jar.html#Service Provider">JAR File Specification</a>.
 *
 * @author Tibor Varga
 */
final class ContainerBootstrapImpl implements ContainerBootstrap {

    @SuppressWarnings("unchecked")
    public OpenComponentContainer populateContainer(final ContainerServices services,
                                                    final ContainerProvider provider,
                                                    final Map properties,
                                                    final OpenComponentContainer parent,
                                                    final ClassLoader classLoader) {
        final Log log = services.logs().createLog(getClass());
        final ClassDiscovery discovery = services.classDiscovery();

        final OpenComponentContainer container = parent == null ? provider.newContainer(services) : parent.makeChildContainer();

        log.info("Created new %s%s", container, (classLoader == null ? "" : String.format(" for %s", classLoader)));

        /*
         * Find instances of classes implementing the PackageBindings interface.
         */
        final Class<PackageBindings>[] classes = discovery.findComponentClasses(PackageBindings.class, classLoader, parent != null);

        log.info("Found %s binding set(s) for %s", classes.length, container);

        /*
         * Let the container provider instantiate them using an actual container to resolve inter-binding dependencies.
         */
        final List<PackageBindings> assemblies = provider.instantiateBindings(services, properties, classes);
        assert assemblies != null;

        final ComponentContainer.Registry registry = container.getRegistry();

        if (parent == null) {
            registry.bindInstance(discovery);
        }

        /*
         * Process each package component set.
         */
        for (final PackageBindings bindings : assemblies) {
            log.info("Processing %s in %s", bindings.getClass().getName(), container);
            bindings.bindComponents(registry);
        }

        final ContainerLifecycle state = new ContainerLifecycle(container, assemblies);
        registry.bindInstance(state);

        final ContainerLifecycle parentState = parent != null ? parent.getComponent(ContainerLifecycle.class) : null;
        if (parentState != null) {
            parentState.addChild(state);
        }

        return container;
    }

    public void initializeContainer(final OpenComponentContainer container, final ContainerServices services) {
        final Log log = services.logs().createLog(getClass());
        final ContainerLifecycle lifecycle = container.getComponent(ContainerLifecycle.class);

        if (lifecycle == null) {
            throw new IllegalStateException(String.format("Container %s has not been populated", container));
        }

        lifecycle.initialize(log);

        final ShutdownTasks shutdown = container.getComponent(ShutdownTasks.class);
        if (shutdown == null) {
            throw new RuntimeException(String.format("%s requires a %s component to function", container, ShutdownTasks.class.getName()));
        }

        shutdown.add("container-shutdown", new Runnable() {
            public void run() {
                lifecycle.shutdown(log);
            }
        });
    }
}


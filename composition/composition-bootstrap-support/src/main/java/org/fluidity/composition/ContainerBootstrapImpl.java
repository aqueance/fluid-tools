/*
 * Copyright (c) 2006-2012 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.fluidity.composition.container.ContainerServices;
import org.fluidity.composition.container.PlatformContainer;
import org.fluidity.composition.container.spi.ContainerProvider;
import org.fluidity.composition.container.spi.OpenComponentContainer;
import org.fluidity.composition.spi.PackageBindings;
import org.fluidity.foundation.ClassDiscovery;
import org.fluidity.foundation.Log;

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
                                                    final ClassLoader classLoader,
                                                    final PlatformContainer platform,
                                                    final Callback callback) {
        final Log log = services.logs().createLog(getClass());
        final ClassDiscovery discovery = services.classDiscovery();

        final OpenComponentContainer container = parent == null ? provider.newContainer(services, platform) : parent.makeChildContainer();

        log.debug("Created new %s%s", container, (classLoader == null ? "" : String.format(" for class loader %s", classLoader)));

        /*
         * Find instances of classes implementing the PackageBindings interface.
         */
        final Class<PackageBindings>[] classes = discovery.findComponentClasses(PackageBindings.class, classLoader, parent != null);

        log.debug("Found %s binding set(s) for %s", classes.length, container);

        /*
         * Let the container provider instantiate them using an actual container to resolve inter-binding dependencies.
         */
        final List<PackageBindings> assemblies = instantiateBindings(provider, services, properties, classes);
        assert assemblies != null;

        final ComponentContainer.Registry registry = container.getRegistry();

        if (parent == null) {
            registry.bindInstance(discovery);
        }

        /*
         * Process each package component set.
         */
        for (final PackageBindings bindings : assemblies) {
            log.debug("Processing %s in %s", bindings.getClass().getName(), container);
            bindings.bindComponents(registry);
        }

        final ContainerLifecycle state = new ContainerLifecycle(container, assemblies, callback);

        registry.bindInstance(state);

        final ContainerLifecycle parentState = parent != null ? parent.getComponent(ContainerLifecycle.class) : null;
        if (parentState != null) {
            parentState.addChild(state);
        }

        return container;
    }

    @SuppressWarnings("unchecked")
    private List<PackageBindings> instantiateBindings(final ContainerProvider provider,
                                                      final ContainerServices services,
                                                      final Map properties,
                                                      final Class<PackageBindings>[] bindings) {
        final OpenComponentContainer container = provider.newContainer(services, null);
        final ComponentContainer.Registry registry = container.getRegistry();

        if (properties != null) {
            registry.bindInstance(properties, Map.class);
        }

        /*
         * Add each to the container
         */
        registry.bindComponentGroup(PackageBindings.class, bindings);

        /*
         * Get the instances in instantiation order
         */
        final PackageBindings[] instances = container.getComponentGroup(PackageBindings.class);
        assert instances != null : PackageBindings.class;
        return Arrays.asList(instances);
    }

    public void initializeContainer(final OpenComponentContainer container, final ContainerServices services) {
        final Log log = services.logs().createLog(getClass());
        final ContainerLifecycle lifecycle = container.getComponent(ContainerLifecycle.class);

        if (lifecycle == null) {
            throw new IllegalStateException(String.format("Container %s has not been populated", container));
        }

        lifecycle.initialize(log);
    }
}


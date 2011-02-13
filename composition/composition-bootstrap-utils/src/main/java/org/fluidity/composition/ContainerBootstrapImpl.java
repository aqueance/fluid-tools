/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.fluidity.composition;

import java.util.Arrays;
import java.util.HashSet;
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

    public OpenComponentContainer populateContainer(final ContainerServices services,
                                                    final ContainerProvider provider,
                                                    final Map properties,
                                                    final OpenComponentContainer parent,
                                                    final ClassLoader classLoader) {
        final Log log = services.logs().createLog(getClass());
        final OpenComponentContainer container = parent == null ? provider.newContainer(services) : parent.makeChildContainer();

        log.info("Created new %s%s", container, (classLoader == null ? "" : String.format(" for %s", classLoader)));

        /*
         * Find instances of classes implementing the PackageBindings interface.
         */
        final List<Class<PackageBindings>> classes = Arrays.asList(services.classDiscovery()
                                                                           .findComponentClasses(PackageBindings.class, classLoader, parent != null));

        log.info("Found %s binding set(s) for %s", classes.size(), container);

        /*
         * Let the container provider instantiate them using an actual container to resolve inter-binding dependencies.
         */
        final List<PackageBindings> assemblies = provider.instantiateBindings(services, properties, new HashSet<Class<PackageBindings>>(classes));
        assert assemblies != null;

        final ComponentContainer.Registry registry = container.getRegistry();

        if (parent == null) {
            registry.bindInstance(services.classDiscovery());
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


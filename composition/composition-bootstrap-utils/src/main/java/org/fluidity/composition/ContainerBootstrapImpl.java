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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.fluidity.composition.spi.ContainerServices;
import org.fluidity.foundation.logging.Log;

/**
 * Bootstraps the component container. This class is exported via the standard service provider discovery mechanism described in the JAR file specification (for
 * dummies: the fully qualified name of this class can be found in a file whose name is the fully qualified class name of the implemented interface).
 *
 * Class is public for sun.misc.Services to find it.
 *
 * @author Tibor Varga
 */
public final class ContainerBootstrapImpl implements ContainerBootstrap {

    public OpenComponentContainer populateContainer(final ContainerServices services,
                                                    final ContainerProvider provider,
                                                    final Map properties,
                                                    final OpenComponentContainer parent,
                                                    final ClassLoader classLoader) {
        final Log log = services.logs().createLog(getClass());
        final OpenComponentContainer container = parent == null ? provider.newContainer(services) : parent.makeNestedContainer();

        log.info("Created new %s%s", container, (classLoader == null ? "" : " for " + classLoader));

        final ComponentContainer.Registry registry = container.getRegistry();

        /*
         * Find instances of classes implementing the PackageBindings interface.
         */
        final Collection<Class<PackageBindings>> assemblySet = new HashSet<Class<PackageBindings>>(Arrays.asList(services.classDiscovery().findComponentClasses(
                PackageBindings.class,
                classLoader,
                parent != null)));

        log.info("Found %s package(s).", assemblySet.size());

        /*
         * Let the container provider instantiate them using an actual container to resolve inter-binding dependencies.
         */
        final List<PackageBindings> assemblies = provider.instantiateBindings(services, properties, assemblySet);
        assert assemblies != null;

        if (parent == null) {
            registry.bindInstance(ClassDiscovery.class, services.classDiscovery());     // TODO: any automatic way of doing this?
        }

        /*
         * Process each package component set.
         */
        for (final PackageBindings bindings : assemblies) {
            log.info("processing %s", bindings.getClass().getName());
            bindings.bindComponents(registry);
        }

        /*
         * Perform post-registration initialization.
         */
        for (final PackageBindings bindings : assemblies) {
            bindings.initializeComponents(container);
        }

        final ShutdownHook shutdown = container.getComponent(ShutdownHook.class);
        if (shutdown == null) {
            throw new RuntimeException(String.format("%s requires a %s component to function", getClass(), ShutdownHook.class.getName()));
        }

        shutdown.addTask("container-shutdown", new Runnable() {
            public void run() {

                /*
                 * Perform pre-shutdown tasks.
                 */
                for (final ListIterator i = assemblies.listIterator(assemblies.size()); i.hasPrevious();) {
                    ((PackageBindings) i.previous()).shutdownComponents(container);
                }
            }
        });

        return container;
    }
}


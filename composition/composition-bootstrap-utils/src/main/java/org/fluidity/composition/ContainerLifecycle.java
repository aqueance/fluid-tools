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

import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fluidity.composition.spi.PackageBindings;
import org.fluidity.composition.spi.ShutdownTasks;
import org.fluidity.foundation.logging.Log;

/**
 * Invokes bindings at application start-up and shut-down in the order of registration at start-up and in reverse order at shut-down. Both of these operations
 * are performed at most once per container.
 *
 * @author Tibor Varga
 */
final class ContainerLifecycle {
    private ContainerBootstrap.Callback callback;

    private final OpenComponentContainer container;
    private final List<PackageBindings> bindings;

    private final Set<ContainerLifecycle> children = new HashSet<ContainerLifecycle>();

    private final AtomicBoolean shouldInitialize = new AtomicBoolean(true);
    private final AtomicBoolean shouldShutdown = new AtomicBoolean(true);

    public ContainerLifecycle(final OpenComponentContainer container, final List<PackageBindings> bindings, final ContainerBootstrap.Callback callback) {
        this.container = container;
        this.bindings = bindings;
        this.callback = callback;
    }

    public boolean initialize(final Log log) {
        final boolean init = shouldInitialize.compareAndSet(true, false);
        if (init) {

            log.info("Initializing %s", container);

            /*
             * Perform post-registration initialization.
             */
            for (final PackageBindings next : bindings) {
                next.initializeComponents(container);
            }
        }

        // child containers are initialized next
        for (final ContainerLifecycle child : children) {
            child.initialize(log);
        }

        if (init) {
            if (callback != null) {
                callback.containerInitialized(container);
            }

            final ShutdownTasks shutdown = container.getComponent(ShutdownTasks.class);
            if (shutdown == null) {
                throw new RuntimeException(String.format("%s requires a %s component to function", container, ShutdownTasks.class.getName()));
            }

            shutdown.add("container-shutdown", new Runnable() {
                public void run() {
                    shutdown(log);
                }
            });
        }

        return init;
    }

    public void addChild(final ContainerLifecycle child) {
        children.add(child);
    }

    public void shutdown(final Log log) {
        if (shouldShutdown.compareAndSet(true, false)) {

            // child containers are shut down first
            for (final ContainerLifecycle child : children) {
                child.shutdown(log);
            }

            /*
             * Perform pre-shutdown tasks in reverse order.
             */
            for (final ListIterator i = bindings.listIterator(bindings.size()); i.hasPrevious();) {
                ((PackageBindings) i.previous()).shutdownComponents(container);
            }

            if (callback != null) {
                callback.containerShutdown(container);
            }

            log.info("%s shut down", container);
        }
    }
}

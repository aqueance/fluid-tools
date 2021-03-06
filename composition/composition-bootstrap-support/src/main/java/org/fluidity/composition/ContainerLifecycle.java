/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fluidity.composition.spi.ContainerTermination;
import org.fluidity.composition.spi.PackageBindings;
import org.fluidity.foundation.Log;

/**
 * Invokes bindings at application start-up and shut-down in the order of registration at start-up and in reverse order at shut-down. Both of these operations
 * are performed at most once per container.
 *
 * @author Tibor Varga
 */
final class ContainerLifecycle {

    private final ContainerLifecycle parent;
    private ContainerBootstrap.Callback callback;

    private final OpenContainer container;
    private final List<PackageBindings> bindings;

    private final Set<ContainerLifecycle> children = new HashSet<>();

    private final AtomicBoolean shouldInitialize = new AtomicBoolean(true);
    private final AtomicBoolean shouldShutdown = new AtomicBoolean(true);

    ContainerLifecycle(final ContainerLifecycle parent, final OpenContainer container, final List<PackageBindings> bindings, final ContainerBootstrap.Callback callback) {
        this.parent = parent;
        this.container = container;
        this.bindings = bindings;
        this.callback = callback;
    }

    public boolean initialize(final Log log) throws Exception {
        final boolean init = shouldInitialize.compareAndSet(true, false);

        if (init) {
            try {
                final ContainerTermination termination = container.getComponent(ContainerTermination.class);

                if (termination == null) {
                    throw new RuntimeException(String.format("%s requires a %s component to function", container, ContainerTermination.class.getName()));
                }

                log.debug("Initializing %s", container);

                // shutdown actions are registered first: will be run after the tasks added by bindings and child containers
                termination.add(() -> shutdown(log));

                // post-registration initialization next: may add shutdown tasks
                for (final PackageBindings next : bindings) {
                    next.initialize(container, termination);
                }

                // child containers are initialized last: child shutdown tasks are run before any added above
                for (final ContainerLifecycle child : children) {
                    child.initialize(log);
                }
            } finally {
                if (parent != null) {
                    parent.addChild(this);
                }

                if (callback != null) {
                    callback.containerInitialized();
                }
            }
        }

        return init;
    }

    private void shutdown(final Log log) {
        if (shouldShutdown.compareAndSet(true, false)) {
            if (callback != null) {
                callback.containerShutdown();
            }

            if (parent != null) {
                parent.removeChild(this);
            }

            // make sure the children are shut down before this container
            for (final ContainerLifecycle child : new HashSet<>(children)) {
                child.shutdown(log);
            }

            log.debug("%s shut down", container);
        }
    }

    private void addChild(final ContainerLifecycle child) {
        children.add(child);
    }

    private void removeChild(final ContainerLifecycle child) {
        children.remove(child);
    }
}

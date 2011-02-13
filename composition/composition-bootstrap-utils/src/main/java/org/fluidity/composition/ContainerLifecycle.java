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

import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fluidity.composition.spi.PackageBindings;
import org.fluidity.foundation.logging.Log;

/**
 * Invokes bindings at application start-up and shut-down in the order of registration at start-up and in reverse order at shut-down. Both of these operations
 * are performed at most once per container.
 *
 * @author Tibor Varga
 */
final class ContainerLifecycle {

    private final OpenComponentContainer container;
    private final List<PackageBindings> bindings;

    private final Set<ContainerLifecycle> children = new HashSet<ContainerLifecycle>();

    private final AtomicBoolean shouldInitialize = new AtomicBoolean(true);
    private final AtomicBoolean shouldShutdown = new AtomicBoolean(true);

    public ContainerLifecycle(final OpenComponentContainer container, final List<PackageBindings> bindings) {
        this.container = container;
        this.bindings = bindings;
    }

    public void initialize(final Log log) {
        if (shouldInitialize.compareAndSet(true, false)) {

            log.info("Initializing %s", container);

            /*
             * Perform post-registration initialization.
             */
            for (final PackageBindings next : bindings) {
                next.initializeComponents(container);
            }

            // child containers are initialized next
            for (final ContainerLifecycle child : children) {
                child.initialize(log);
            }
        }
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

            log.info("Shutting down %s", container);

            /*
             * Perform pre-shutdown tasks in reverse order.
             */
            for (final ListIterator i = bindings.listIterator(bindings.size()); i.hasPrevious();) {
                ((PackageBindings) i.previous()).shutdownComponents(container);
            }
        }
    }
}

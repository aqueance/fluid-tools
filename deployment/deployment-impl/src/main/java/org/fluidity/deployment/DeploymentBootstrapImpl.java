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

package org.fluidity.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.Optional;
import org.fluidity.foundation.logging.Log;
import org.fluidity.foundation.logging.Marker;

/**
 * @author Tibor Varga
 */
@Component
final class DeploymentBootstrapImpl implements DeploymentBootstrap {

    private final DeploymentControl deployments;

    private final List<DeployedComponent> components = new ArrayList<DeployedComponent>();
    private final List<DeploymentObserver> observers = new ArrayList<DeploymentObserver>();
    private final List<DeployedComponent> active = new ArrayList<DeployedComponent>();

    private final Log log;

    public DeploymentBootstrapImpl(final @Marker(DeploymentBootstrapImpl.class) Log log,
                                   final @Optional @ComponentGroup DeployedComponent[] components,
                                   final @Optional @ComponentGroup DeploymentObserver[] observers,
                                   final DeploymentControl deployments) {
        this.log = log;
        this.deployments = deployments;

        if (components != null) {
            this.components.addAll(Arrays.asList(components));
        }

        if (observers != null) {
            this.observers.addAll(Arrays.asList(observers));
        }
    }

    private void info(final String message, final Object... args) {
        log.info(message, args);
    }

    public synchronized void load() throws Exception {
        active.clear();
        active.addAll(components);

        for (final DeployedComponent component : components) {
            info("Starting %s", component.name());
            component.start(new DeployedComponent.Context() {
                public void complete() {
                    final boolean empty;

                    synchronized (active) {

                        // empty can only be true if the component has actually been removed
                        empty = active.remove(component) && active.isEmpty();
                    }

                    if (empty && !deployments.isStandalone()) {
                        try {
                            deployments.stop();
                        } catch (final Exception e) {
                            log.error(e, "Could not stop runtime");
                        }
                    }
                }
            });
            info("Started %s",component.name());
        }

        for (final DeploymentObserver observer : observers) {
            try {
                observer.started();
            } catch (final Exception e) {
                log.warning(e, observer.getClass().getName());
            }
        }

        deployments.completed();
    }

    public synchronized void unload() {
        Collections.reverse(components);
        Collections.reverse(active);

        for (final DeployedComponent component : active) {
            try {
                info("Stopping %s", component.name());
                component.stop();
                info("Stopped %s", component.name());
            } catch (final Exception e) {
                log.warning(e, component.name());
            }
        }

        Collections.reverse(observers);
        for (final DeploymentObserver observer : observers) {
            try {
                observer.stopped();
            } catch (final Exception e) {
                log.warning(e, observer.getClass().getName());
            }
        }
    }
}

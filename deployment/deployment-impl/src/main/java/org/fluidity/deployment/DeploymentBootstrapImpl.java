/*
 * Copyright (c) 2006-2010 Tibor Adam Varga (tibor.adam.varga on gmail)
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
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentDiscovery;
import org.fluidity.foundation.Logging;
import org.fluidity.foundation.SystemSettings;
import org.fluidity.foundation.logging.BootstrapLog;

/**
 * @author Tibor Varga
 */
@Component
final class DeploymentBootstrapImpl implements DeploymentBootstrap {

    private final boolean verbose = !SystemSettings.isSet(BootstrapLog.SUPPRESS_LOGS, "deployment", BootstrapLog.ALL_LOGS);

    private final ComponentContainer container;
    private final ComponentDiscovery discovery;
    private final RuntimeControl runtime;

    private final List<DeployedComponent> deployedComponents = Collections.synchronizedList(new ArrayList<DeployedComponent>());
    private final List<DeployedComponent> activeComponents = Collections.synchronizedList(new ArrayList<DeployedComponent>());
    private final List<DeploymentObserver> observers = new ArrayList<DeploymentObserver>();

    private final Logging log;

    public DeploymentBootstrapImpl(final Logging log,
                                   final ComponentContainer container,
                                   final ComponentDiscovery discovery,
                                   final RuntimeControl runtime) {
        this.log = log;
        this.container = container;
        this.discovery = discovery;
        this.runtime = runtime;
    }

    private void info(String message) {
        if (verbose) {
            log.info(getClass(), message);
        }
    }

    public void load() throws Exception {
        deployedComponents.clear();
        observers.clear();

        deployedComponents.addAll(Arrays.asList(discovery.findComponentInstances(container, DeployedComponent.class)));
        observers.addAll(Arrays.asList(discovery.findComponentInstances(container, DeploymentObserver.class)));

        activeComponents.clear();
        activeComponents.addAll(deployedComponents);

        for (final DeployedComponent component : deployedComponents) {
            info("Starting " + component.name());
            component.start(new DeployedComponent.Context() {
                public void complete() {
                    final boolean empty;

                    synchronized (activeComponents) {

                        // empty can only be true if the component has actually be removed
                        empty = activeComponents.remove(component) && activeComponents.isEmpty();
                    }

                    if (empty) {
                        try {
                            runtime.deploymentsComplete();
                        } catch (final Exception e) {
                            log.fatal(getClass(), "Could not stop runtime", e);
                        }
                    }
                }
            });
            info("Started " + component.name());
        }

        for (final DeploymentObserver observer : observers) {
            try {
                observer.started();
            } catch (Exception e) {
                log.warning(getClass(), observer.getClass().getName(), e);
            }
        }

        if (activeComponents.isEmpty() && observers.isEmpty()) {
            runtime.deploymentsComplete();
        }
    }

    public void unload() {
        Collections.reverse(deployedComponents);
        Collections.reverse(activeComponents);
        for (final DeployedComponent component : activeComponents) {
            try {
                info("Stopping " + component.name());
                component.stop();
                info("Stopped " + component.name());
            } catch (Exception e) {
                log.warning(getClass(), component.name(), e);
            }
        }

        Collections.reverse(observers);
        for (final DeploymentObserver observer : observers) {
            try {
                observer.stopped();
            } catch (Exception e) {
                log.warning(getClass(), observer.getClass().getName(), e);
            }
        }
    }
}

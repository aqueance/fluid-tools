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

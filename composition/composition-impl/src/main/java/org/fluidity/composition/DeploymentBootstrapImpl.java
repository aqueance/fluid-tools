/*
 * Copyright (c) 2006-2008 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Softweare"), to deal
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.fluidity.composition;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.fluidity.foundation.Logging;

/**
 * @author Tibor Varga
 */
@Component
final class DeploymentBootstrapImpl implements DeploymentBootstrap {

    private final List<DeployedComponent> components;
    private final List<DeploymentObserver> observers;

    private final Logging log;

    public DeploymentBootstrapImpl(final Logging log, final ComponentContainer container, final ComponentDiscovery discovery) {
        this.log = log;

        components = Arrays.asList(discovery.findComponentInstances(container, DeployedComponent.class));
        Collections.reverse(components);

        observers = Arrays.asList(discovery.findComponentInstances(container, DeploymentObserver.class));
        Collections.reverse(observers);
    }

    public int load() throws Exception {
        Collections.reverse(components);
        for (final DeployedComponent component : components) {
            log.info(getClass(), "Starting " + component.name());
            component.start();
            log.info(getClass(), "Started " + component.name());
        }

        Collections.reverse(observers);
        for (final DeploymentObserver observer : observers) {
            try {
                observer.started();
            } catch (Exception e) {
                log.warning(getClass(), observer.getClass().getName(), e);
            }
        }

        return components.size();
    }

    public void unload() {
        Collections.reverse(components);
        for (final DeployedComponent component : components) {
            try {
                log.info(getClass(), "Stopping " + component.name());
                component.stop();
                log.info(getClass(), "Stopped " + component.name());
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

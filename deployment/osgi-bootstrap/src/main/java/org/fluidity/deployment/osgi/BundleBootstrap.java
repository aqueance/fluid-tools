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

package org.fluidity.deployment.osgi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.ContainerBoundary;
import org.fluidity.composition.Inject;
import org.fluidity.composition.Optional;
import org.fluidity.composition.spi.ShutdownTasks;
import org.fluidity.foundation.logging.Log;
import org.fluidity.foundation.logging.Marker;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Bootstraps the dependency injection container in an OSGi bundle. Must be public with a zero-arg constructor for the OSGi container to be able to call.
 *
 * @author Tibor Varga
 */
public final class BundleBootstrap implements BundleActivator {

    @Inject
    @Marker(BundleBootstrap.class)
    private Log log;

    private final BundleShutdownTasks shutdown = new BundleShutdownTasks();
    private Activators activators;
    private Whiteboard whiteboard;

    @SuppressWarnings("unchecked")
    public void start(final BundleContext context) throws Exception {
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        final ContainerBoundary boundary = new ContainerBoundary();

        boundary.bindBootComponent(shutdown);
        boundary.bindBootComponent(context, BundleContext.class);

        boundary.initialize(this);

        whiteboard = boundary.getComponent(Whiteboard.class);

        activators = boundary.getComponent(Activators.class);
        activators.start();
    }

    public void stop(final BundleContext context) throws Exception {
        activators.stop();
        whiteboard.stop();
        shutdown.stop(log);
        log = null;
    }

    @Component(automatic = false)
    private static class BundleShutdownTasks implements ShutdownTasks {
        private final Map<Runnable, String> tasks = new LinkedHashMap<Runnable, String>();

        public void add(final String name, final Runnable command) {
            tasks.put(command, name);
        }

        public void stop(final Log log) {
            for (final Iterator<Map.Entry<Runnable, String>> list = tasks.entrySet().iterator(); list.hasNext();) {
                final Map.Entry<Runnable, String> entry = list.next();

                try {
                    entry.getKey().run();
                } catch (final Exception e) {
                    log.warning(e, "Shutting down %s", entry.getValue());
                } finally {
                    list.remove();
                }
            }
        }
    }

    @Component
    private static class Activators {

        private final BundleContext context;
        private final List<BundleActivator> activators = new ArrayList<BundleActivator>();
        private final Log log;

        private Activators(final BundleContext context,
                           final @Marker(Activators.class) Log log,
                           final @Optional BundleActivator single,
                           final @Optional @ComponentGroup BundleActivator[] multiple) {
            this.context = context;
            this.log = log;
            addActivators(activators, single);
            addActivators(activators, multiple);
        }

        private void addActivators(final List<BundleActivator> list, final BundleActivator... found) {
            if (found != null) {
                for (final BundleActivator activator : found) {
                    if (activator != null) {
                        list.add(activator);
                    }
                }
            }
        }

        public void start() {
            for (final BundleActivator activator : activators) {
                try {
                    activator.start(context);
                } catch (final RuntimeException e) {
                    throw e;
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public void stop() {
            for (final BundleActivator activator : activators) {
                try {
                    activator.stop(context);
                } catch (final Exception e) {
                    log.warning(e, "Stopping %s", activator.getClass().getName());
                }
            }
        }
    }
}

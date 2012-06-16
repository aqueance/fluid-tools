/*
 * Copyright (c) 2006-2012 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.deployment.osgi.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.ContainerBoundary;
import org.fluidity.composition.Inject;
import org.fluidity.composition.Optional;
import org.fluidity.composition.spi.ContainerTermination;
import org.fluidity.deployment.osgi.BundleComponentContainer;
import org.fluidity.foundation.Log;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Bootstraps the dependency injection container in an OSGi bundle. A properly set up OSGi bundle Maven project will automatically set up this class as the
 * bundle activator for the project artifact. Thus, you should never need to directly deal with this class.
 * <p/>
 * <b>NOTE 1</b>: This class <em>must be</em> public with a zero-arg constructor for the OSGi container to be able to instantiate it.
 * <p/>
 * <b>NOTE 2</b>: This class <em>must be</em> loaded by the class loader of the bundle it is expected to bootstrap.
 *
 * @author Tibor Varga
 */
public final class BundleBootstrap implements BundleActivator {

    // this dependence on the bundle class loader requires this class to be loaded by that bundle class loader
    private final ContainerBoundary bootstrap = new ContainerBoundary(BundleBootstrap.class.getClassLoader());
    private final BundleTermination termination = new BundleTermination();

    private BundleComponentContainer components;
    private Activators activators;

    /**
     * Default constructor.
     */
    @SuppressWarnings("unchecked")
    public BundleBootstrap() {
        bootstrap.bindBootComponent(this.termination);
        bootstrap.initialize(this.termination);
    }

    /**
     * Loads a {@link BundleComponentContainer} and calls {@link BundleActivator#start(BundleContext)} on all {@link ComponentGroup @ComponentGroup} annotated
     * <code>BundleActivator</code> implementations in the bundle.
     *
     * @param context the bundle context for the host bundle.
     *
     * @throws Exception when anything goes wrong.
     */
    @SuppressWarnings("unchecked")
    public void start(final BundleContext context) throws Exception {
        final ComponentContainer container = bootstrap.makeDomainContainer(new ComponentContainer.Bindings() {
            public void bindComponents(final ComponentContainer.Registry registry) {
                registry.bindInstance(context, BundleContext.class);
            }
        });

        components = container.getComponent(BundleComponentContainer.class);
        components.start();

        activators = container.getComponent(Activators.class);
        activators.start();
    }

    /**
     * Stops the {@link BundleComponentContainer} loaded, and calls the {@link BundleActivator#stop(BundleContext)}
     * method on all <code>BundleActivator</code> implementations instantiated, in the {@link #start(BundleContext)} method.
     *
     * @param context the bundle context for the host bundle.
     *
     * @throws Exception when anything goes wrong.
     */
    public void stop(final BundleContext context) throws Exception {
        activators.stop();
        components.stop();
        termination.stop();
    }

    @Component(automatic = false)
    private static class BundleTermination implements ContainerTermination {
        private final List<Runnable> tasks = new ArrayList<Runnable>();

        @Inject
        @SuppressWarnings("UnusedDeclaration")
        private Log<BundleTermination> log;

        public void run(final Runnable command) {
            tasks.add(command);
        }

        public void stop() {
            for (final ListIterator<Runnable> iterator = tasks.listIterator(tasks.size()); iterator.hasPrevious(); ) {
                final Runnable task = iterator.previous();
                try {
                    task.run();
                } catch (final Exception e) {
                    log.error(e, task.getClass().getName());
                }
            }
        }
    }

    @Component
    private static class Activators {

        private final BundleContext context;
        private final List<BundleActivator> activators = new ArrayList<BundleActivator>();
        private final Log log;

        @SuppressWarnings("UnusedDeclaration")
        private Activators(final BundleContext context,
                           final Log<Activators> log,
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

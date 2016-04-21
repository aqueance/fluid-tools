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

package org.fluidity.deployment.osgi.impl;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

import org.fluidity.composition.BoundaryComponent;
import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.Optional;
import org.fluidity.composition.spi.ContainerTermination;
import org.fluidity.foundation.Command;
import org.fluidity.foundation.Log;
import org.fluidity.foundation.Security;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Bootstraps the dependency injection container in an OSGi bundle. A properly set up OSGi bundle Maven project will automatically set up this class as the
 * bundle activator for the project artifact. Thus, you should never need to directly deal with this class.
 * <p>
 * <b>NOTE 1</b>: This class <em>must</em> be public with a zero-arg constructor for the OSGi container to be able to instantiate it.
 * <p>
 * <b>NOTE 2</b>: This class <em>must</em> be loaded by the class loader of the bundle it is expected to bootstrap. The superclass, {@link BoundaryComponent},
 * uses the class loader that loaded this class to find the dependency injection container that will be used as a root container when working with bundle
 * components in the loading bundle. Also, along this class comes the per-bundle implementation of {@link ContainerTermination} that will map the bundle
 * specific container's life cycle to that of the loading bundle.
 *
 * @author Tibor Varga
 */
@SuppressWarnings("UnusedDeclaration")
public final class BundleBootstrap extends BoundaryComponent implements BundleActivator {

    // no synchronization necessary: https://osgi.org/javadoc/r4v43/core/org/osgi/framework/BundleActivator.html
    private Activation activation;

    /**
     * Loads a {@link BundleComponentContainer} and calls {@link BundleActivator#start(BundleContext)} on all {@link ComponentGroup @ComponentGroup} or
     * {@link Component @Component} annotated <code>BundleActivator</code> implementations in the bundle.
     *
     * @param context the bundle context for the host bundle.
     *
     * @throws Exception when anything goes wrong.
     */
    @SuppressWarnings("unchecked")
    public void start(final BundleContext context) throws Exception {
        if (activation != null) {
            throw new IllegalStateException("Bundle has already been started");
        }

        final ComponentContainer container = container().makeDomainContainer(registry -> registry.bindInstance(context, BundleContext.class));
        activation = container.instantiate(Activation.class);

        activation.start();
    }

    /**
     * Stops the {@link BundleComponentContainer} that was previously loaded, and calls the {@link BundleActivator#stop(BundleContext)} method on all
     * <code>BundleActivator</code> implementations instantiated, in the {@link #start(BundleContext)} method.
     *
     * @param context the bundle context for the host bundle.
     *
     * @throws Exception when anything goes wrong.
     */
    public void stop(final BundleContext context) throws Exception {
        if (activation == null) {
            throw new IllegalStateException("Bundle has not been started");
        }

        try {
            activation.stop();
        } finally {
            activation = null;
        }
    }

    /**
     * The current activation of the bundle.
     *
     * @author Tibor Varga
     */
    @Component(automatic = false)
    private static class Activation {

        private static final ClassLoader loader = !Security.CONTROLLED
            ? Activation.class.getClassLoader()
            : AccessController.doPrivileged((PrivilegedAction<ClassLoader>) Activation.class::getClassLoader);

        private final BundleComponentContainer components;
        private final Activators activators;
        private final BundleTermination termination;
        private final ComponentContainer container;

        Activation(final BundleComponentContainer<Activation> components,
                   final Activators activators,
                   final BundleTermination termination,
                   final ComponentContainer container) {
            this.components = components;
            this.activators = activators;
            this.termination = termination;
            this.container = container;
        }

        public void start() {
            components.start(container, loader);
            activators.start();
        }

        public void stop() {
            components.stop();
            activators.stop();
            termination.stop();
        }
    }

    /**
     * Shuts down the dependency container associated with the current activation.
     *
     * @author Tibor Varga
     */
    @Component(api = { BundleTermination.class, ContainerTermination.class })
    private static class BundleTermination implements ContainerTermination {

        private final Jobs jobs;

        BundleTermination(final Jobs<BundleTermination> jobs) {
            this.jobs = jobs;
        }

        @SuppressWarnings("unchecked")
        public void add(final Command.Job<Exception> job) {
            jobs.add(job);
        }

        @SuppressWarnings("unchecked")
        public void remove(final Command.Job<Exception> job) {
            jobs.remove(job);
        }

        public void stop() {
            jobs.flush();
        }
    }

    @Component
    private static class Activators {

        private final BundleContext context;
        private final List<BundleActivator> activators = new ArrayList<>();
        private final Log log;

        Activators(final BundleContext context,
                   final Log<Activators> log,
                   final @Optional BundleActivator single,
                   final @Optional @ComponentGroup BundleActivator... multiple) {
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

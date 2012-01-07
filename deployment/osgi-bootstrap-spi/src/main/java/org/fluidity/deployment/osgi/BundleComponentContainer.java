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

import java.util.Properties;

import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.ServiceProvider;

/**
 * Component life cycle management backed by the OSGi service registry. This container allows its managed components to<ul>
 * <li>declare dependencies on {@linkplain Service OSGi services} and
 * <ul>
 * <li>get instantiated and {@linkplain Managed#start() started} when all those services become available</li>
 * <li>get {@linkplain Managed#stop() stopped} and discarded when any of those services become unavailable</li>
 * </ul>
 * </li>
 * <li>get registered as {@linkplain Registration OSGi service}</li>
 * <li>get notified when OSGi services become {@linkplain Registration.Listener#clientAdded(Object, Properties) available} and
 * {@linkplain Registration.Listener#clientRemoved(Object) unavailable}</li>
 * </ul>
 * This container automatically finds, loads and manages all implementations of the {@link Managed Managed} interface visible to its class loader, which is the
 * OSGi bundle class loader. Implementations of the <code>Managed</code> interface include more specific components that implement {@link
 * Registration Registration} and {@link Registration.Listener Registration.Listener}.
 * <p/>
 * By design, you never directly use this interface; rather, you implement your bundle as a collection of components managed by this container.
 * <h3>Depending on OSGi Services</h3>
 * <code>Managed</code> components with direct dependencies - i.e., those without the {@link Service @Service} annotation - to one another are grouped and
 * their combined set of {@link Service @Service} dependencies are consulted to determine when to instantiate or discard all components in the group.
 * Independent component groups are instantiated and discarded independently.
 * <p/>
 * The parameters of constructor of these components that are annotated with <code>@Service</code> are dependencies to OSGi services while parameters not so
 * annotated are ordinary dependencies.
 * <p/>
 * When all of the OSGi services depended on become available, the component is instantiated and its {@link Managed#start() start()} method is invoked to start
 * the component. The {@link Stoppable#stop() stop()} method of the component will be invoked when any of those OSGi services becomes unavailable and then the
 * component instance is discarded.
 * <h3>Getting registered as OSGi Service</h3>
 * {@link Registration} components are <code>Managed</code> components that are registered as OSGi services when all OSGi services they depend on become
 * available, and get unregistered when any of those become unavailable.
 * <h3>Notification about OSGi Service Registration Events</h3>
 * {@link Registration.Listener} components are <code>Managed</code> components that receive notifications about OSGi service registration events via the
 * {@link Registration.Listener#clientAdded(Object, Properties) clientAdded()} method and un-registration events via the {@link
 * Registration.Listener#clientRemoved(Object) clientRemoved()} method.
 * <p/>
 * A managed component may at the same time be a registered OSGi service and may receive notifications about OSGi service registration events by implementing
 * both {@link Registration} and {@link Registration.Listener}.
 * <h3>Observing Component Management</h3>
 * This container will also find and load all implementations of the {@link Observer} interface. <code>Observer</code> components get notified when any of a
 * set of components it {@linkplain Observer#types() declares} to be interested in gets {@linkplain Observer#started(Class, Object) started} or {@linkplain
 * Observer#stopping(Class, Object) stopped}.
 *
 * @author Tibor Varga
 */
public interface BundleComponentContainer {

    /**
     * Starts the container. Invoked automatically when the host bundle starts.
     */
    void start();

    /**
     * Stops the container. Invoked automatically when the host bundle stops.
     */
    void stop();

    /**
     * A component that gets notified by a {@link BundleComponentContainer} when a {@link BundleComponentContainer.Managed} component gets {@linkplain
     * #started(Class, Object) started} or {@linkplain #stopping(Class, Object) stopped}.
     * <p/>
     * TODO: add start failure notification
     */
    @ComponentGroup
    interface Observer {

        /**
         * Returns the component interfaces this object will be notified about.
         *
         * @return the component interfaces this object will be notified about.
         */
        Class<?>[] types();

        /**
         * Notifies the receiver that the given managed component has been started and is ready to be used.
         *
         * @param type      the component type as listed by {@link #types()}, the supplied instance of which has just been started.
         * @param component the component that has just been started.
         */
        void started(Class<?> type, Object component);

        /**
         * Notifies the receiver that the given managed component is about to be stopped and is thus no longer usable.
         *
         * @param type      the component type as listed by {@link #types()}, the supplied instance of which is about to be stopped.
         * @param component the component that is about to be stopped.
         */
        void stopping(Class<?> type, Object component);
    }

    /**
     * A managed component that will be discovered and added to the container.
     * <p/>
     * Managed components may have two kinds of dependencies: OSGi service interfaces annotated with {@link Service @Service} and ordinary types denoting
     * components in the same bundle.
     * </p>
     * When all OSGi services dependencies of a managed component become available, the component is instantiated and its {@link #start() start()} method is
     * invoked. The {@link #stop() stop()} method of the component is invoked to stop the component if any of the services become unavailable and then the
     * instance is discarded.
     */
    @ServiceProvider(type = "bundle-components")
    interface Managed extends Stoppable {

        /**
         * Starts the receiver.
         *
         * @throws Exception if anything goes wrong.
         */
        void start() throws Exception;
    }

    /**
     * Denotes an OSGi service implemented in this bundle that will be registered when it is {@linkplain BundleComponentContainer.Managed#start() started} and
     * un-registered when {@linkplain BundleComponentContainer.Managed#stop() stopped} by its {@linkplain BundleComponentContainer managing container}.
     * <p/>
     * A service registration is a {@link Managed} component thus the start / stop logic described therein applies.
     *
     * @author Tibor Varga
     */
    interface Registration extends Managed {

        /**
         * Returns the list of classes this service is to be registered as.
         *
         * @return the list of classes this service is to be registered as.
         */
        Class<?>[] types();

        /**
         * Returns the registration properties for this service, if any.
         *
         * @return the registration properties for this service; may be <code>null</code>.
         */
        Properties properties();

        /**
         * An event source that wishes to receive notifications about event consumer registration as per the Whiteboard pattern.
         * <p/>
         * An event source is a {@link Managed} component thus the start / stop logic described therein applies.
         */
        interface Listener<T> extends Managed {

            /**
             * Returns the class that event consumers of interest are expected to register as. Service registrations only against the name of the returned class will be
             * recognized as event consumer registration.
             *
             * @return the class that event consumers of interest are expected to register as.
             */
            Class<T> clientType();

            /**
             * Notifies the event source that a new event consumer has been added.
             *
             * @param component  the event consumer.
             * @param properties the registration properties of the event consumer.
             */
            void clientAdded(T component, Properties properties);

            /**
             * Notifies the event source that an event consumer has been removed.
             *
             * @param component the event consumer that has been removed.
             */
            void clientRemoved(T component);
        }
    }

    /**
     * Allows an item managed by the container to be explicitly removed from the container. This is also done automatically when the owning bundle is stopped.
     * <p/>
     * You never use this interface directly but through more specific interfaces such as {@link Managed}, {@link Registration} and
     * {@link Registration.Listener}.
     */
    interface Stoppable {

        /**
         * Stops the receiver.
         *
         * @throws Exception if anything goes wrong.
         */
        void stop() throws Exception;
    }
}

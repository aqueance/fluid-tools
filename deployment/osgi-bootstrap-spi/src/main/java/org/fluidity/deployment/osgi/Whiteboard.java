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
 * Sugar coating over the OSGi service registry: allows whiteboard managed components to
 * <ul>
 * <li>declare dependencies on OSGi services and
 * <ul>
 * <li>get instantiated and started when all those services become available</li>
 * <li>get stopped and discarded when any of those services become unavailable</li>
 * </ul>
 * </li>
 * <li>get registered as OSGi service</li>
 * <li>get notified when OSGi services become available and unavailable</li>
 * </ul>
 * <p/>
 * <b>Depending on OSGi Services</b>
 * <p/>
 * The {@link Managed} component's constructor parameters annotated with @{@link Service} are dependencies to OSGi services while parameters not so annotated
 * are ordinary dependencies. When all of the OSGi services depended on become available, the component is instantiated and its {@link #start()} method is
 * invoked to start the component. The {@link #stop()} method of the component will be invoked when any of those OSGi services becomes unavailable and then
 * the component instance is discarded.
 * <p/>
 * <b>Getting registered as OSGi Service</b>
 * <p/>
 * {@link Registration} components are {@link Managed} components that are registered as OSGi services when all OSGi services they depend on become available
 * and get unregistered when any of those become unavailable.
 * <p/>
 * <b>Notification about OSGi Service Registration Events</b>
 * <p/>
 * {@link EventSource} components are {@link Managed} components that receive notifications about OSGi service registration and unregistration events.
 * <p/>
 * The same whiteboard component may at the same time be a registered OSGi service and may receive notifications about OSGi service registration events by
 * implementing both {@link Registration} and {@link EventSource}.
 * <p/>
 * The whiteboard loads and manages all {@link Whiteboard.Managed} components visible to its class loader, which is the OSGi bundle class loader. Components
 * with direct dependencies - i.e., those without the @{@link Service} annotation - to one another are grouped and their combined set of @{@link Service}
 * dependencies are consulted to determine when to instantiated or discard all components in the group. Independent component groups are instantiated and
 * discarded independently.
 *
 * @author Tibor Varga
 */
public interface Whiteboard {

    /**
     * Starts the whiteboard.
     */
    void start();

    /**
     * Stops the whiteboard.
     */
    void stop();

    @ComponentGroup
    interface Observer {

        Class<?>[] types();

        /**
         * Notifies the receiver that the given whiteboard managed components have been started and are ready to be used. The objects are proxies and the
         * supplied array contains only those components that registered only against interfaces.
         *
         * @param type      the component type as listed by {@link #types()} an instance of which has just been started.
         * @param component the component that has just been started.
         */
        void started(Class<?> type, Object component);

        /**
         * Notifies the receiver that the whiteboard managed components for the given service types have been stopped and are no longer usable.
         *
         * @param type      the component type as listed by {@link #types()} the current instance of which is about to be stopped.
         * @param component the component that is about to be stopped.
         */
        void stopping(Class<?> type, Object component);
    }

    /**
     * A whiteboard managed component that will be discovered and added to the whiteboard. Whiteboard managed components may have two kinds of dependencies:
     * OSGi service interfaces annotated with @{@link Service} and ordinary types denoting dependency injected components. When all OSGi services dependencies
     * become available, the component is instantiated and its {@link #start()} method is invoked. The {@link #stop()} method of the component is invoked to
     * stop the component if any of the services become unavailable and then the instance is discarded.
     */
    @ServiceProvider(type = "whiteboard")
    interface Managed extends Stoppable {

        /**
         * Starts the receiver.
         *
         * @throws Exception if anything goes wrong.
         */
        void start() throws Exception;
    }

    /**
     * Denotes an OSGi service that will be registered when the host bundle is started. A service registration is also a whiteboard {@link Managed} and the
     * start / stop logic described therein equally applies.
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
    }

    /**
     * An event source that wishes to receive notification about event consumer registration as per the Whiteboard pattern. An event source is also a
     * whiteboard {@link Whiteboard.Managed} and the start / stop logic described therein equally applies.
     */
    interface EventSource<T> extends Managed {

        /**
         * Returns the class event consumers are expected to register as. Service registrations only against the name of the returned class will be recognized
         * as event consumer registration.
         *
         * @return the class event consumers are expected to register as.
         */
        Class<T> clientType();

        /**
         * Notifies the event source that a new consumer has been added.
         *
         * @param consumer   the consumer.
         * @param properties the consumer registration properties.
         */
        void clientAdded(T consumer, Properties properties);

        /**
         * Notifies the event source that a consumer has been removed.
         *
         * @param consumer the consumer that has been removed.
         */
        void clientRemoved(T consumer);
    }

    /**
     * Allows an item managed by the whiteboard to be explicitly removed from the whiteboard. This is also done automatically when the owning bundle is
     * stopped.
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

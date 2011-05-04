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

/**
 * Groups entities related to the Whiteboard pattern as well as general OSGi service registration. There are two distinct patterns related to in OSGi service
 * interaction: an event source expecting its event consumers as OSGi services and ordinary OSGi services.
 * <p/>
 * The former is what is known as the Whiteboard pattern with event consumers being ordinary services while the latter is ordinary service registration itself.
 * <p/>
 * Due to these similarities, these two concepts have been merged into this component. There are four ways to use this component:
 * <ul>
 * <li>Implement {@link Whiteboard.EventSource}: all implementations are automatically loaded and registered when the bundle starts.</li>
 * <li>Implement {@link Whiteboard.EventConsumer} or its super interface {@link Service.Registration}: all implementations are automatically loaded and
 * registered when the bundle starts.</li>
 * <li>Depend on {@link Whiteboard} and call its {@link #register(Whiteboard.EventSource)} method to explicitly add an event source and call {@link
 * Whiteboard.Handle#remove()} on the returned object to remove it.</li>
 * <li>Depend on {@link Whiteboard} and call its {@link #register(Service.Registration, Properties, Class[])} method to explicitly add an event consumer
 * or ordinary service and call {@link Whiteboard.Handle#remove()} on the returned object to remove it.</li>
 * </ul>
 *
 * @author Tibor Varga
 */
public interface Whiteboard {

    /**
     * Registers a new event source to the whiteboard.
     *
     * @param source the event source to register.
     *
     * @return a handle that can be used to programmatically remove the event source from the whiteboard. When the owning bundle stops, all whiteboard
     *         registrations will be removed automatically.
     */
    <T> Handle register(EventSource<T> source);

    /**
     * Registers a new event consumer to the whiteboard.
     *
     *
     * @param consumer   the consumer to register.
     * @param properties the registration properties.
     * @param types      the interfaces against which to register the consumer.
     *
     * @return a handle that can be used to programmatically remove the event consumer from the whiteboard. When the owning bundle stops, all whiteboard
     *         registrations will be removed automatically.
     */
    Handle register(Service.Registration consumer, Properties properties, Class<?>... types);

    /**
     * Called by the framework to free resources held by the whiteboard when the owning bundle is stopped.
     */
    void stop();

    /**
     * An event source that wish to receive notification about event consumer registration as per the Whiteboard pattern.
     */
    @ComponentGroup
    interface EventSource<T> {

        /**
         * Returns the class event consumers are expected to register as. Service registrations against the name of the returned class will be only recognized
         * as event consumer registration.
         *
         * @return the class event consumers are expected to register as.
         */
        Class<T> consumerType();

        /**
         * Notifies the event source that a new consumer has been added.
         *
         * @param consumer   the consumer.
         * @param properties the consumer registration properties.
         */
        void consumerAdded(T consumer, RegistrationProperties properties);

        /**
         * Notifies the event source that a consumer has been removed.
         *
         * @param consumer the consumer that has been removed.
         */
        void consumerRemoved(T consumer);

        /**
         * Stops the event source.
         */
        void stop();
    }

    /**
     * Event consumers are ordinary services to be added to a Whiteboard.
     */
    interface EventConsumer extends Service.Registration { }

    /**
     * Provides access to the registration properties when a service has been resolved.
     */
    interface RegistrationProperties {

        /**
         * Returns the registration property keys.
         *
         * @return the registration property keys.
         */
        String[] keys();

        /**
         * Returns the value of the given registration property.
         *
         * @param key  the property key.
         * @param type the type of the property value.
         * @param <T>  the type of the property value.
         *
         * @return the value of the given registration property.
         */
        <T> T get(String key, Class<T> type);
    }

    /**
     * Allows an event consumer or event source to be explicitly removed from the whiteboard. This is also done automatically when the owning bundle is stopped.
     */
    interface Handle {

        /**
         * Remove the event consumer or event source the registration of which returned this handle. See {@link Whiteboard#register(org.fluidity.deployment.o
                  * sgi.Service.Registration, java.util.Properties, Class} or {@link Whiteboard#register(Whiteboard.EventSource)}.
         */
        void remove();
    }
}

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
 * Groups entities related to the Whiteboard pattern.
 *
 * @author Tibor Varga
 */
public interface Whiteboard {

    /**
     * Registers a new event consumer to the whiteboard.
     *
     * @param consumer   the consumer to register.
     * @param properties the registration properties.
     * @param types      the interfaces against which to register the consumer.
     *
     * @return a handle that can be used to programmatically remove the event consumer from the whiteboard. When the owning bundle stops, all whiteboard
     *         registrations will be removed automatically.
     */
    Handle register(EventConsumer consumer, Properties properties, Class<?>... types);

    /**
     * Called by the framework to free resources held by the whiteboard when the owning bundle is stopped.
     */
    void stop();

    /**
     * An event source that wish to receive notification about event consumer registration as per the Whiteboard pattern.
     */
    @ComponentGroup
    interface EventSource<T> {

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
     * Event consumers to be added to a Whiteboard.
     */
    @ComponentGroup
    interface EventConsumer {

        /**
         * Register the consumer with the whiteboard.
         *
         * @param whiteboard the whiteboard to add the consumer to.
         */
        void register(Whiteboard whiteboard);
    }

    /**
     * Provides access to the registration properties when a service has been resolved.
     */
    interface RegistrationProperties {

        /**
         * Returns the registration property keys.
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
     * Allows an event consumer registration to remove itself from the whiteboard. This is done automatically when the owning bundle is stopped.
     */
    interface Handle {

        /**
         * Remove the event consumer the registration of which returned this handle. See {@link Whiteboard#register(Whiteboard.EventConsumer, Properties,
         * Class[]}.
         */
        void remove();
    }
}

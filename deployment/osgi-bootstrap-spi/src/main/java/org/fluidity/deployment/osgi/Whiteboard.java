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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Properties;

import org.fluidity.composition.ComponentGroup;

/**
 * TODO: documentation...
 *
 * @author Tibor Varga
 */
public interface Whiteboard {

    /**
     * Stops the whiteboard.
     */
    void stop();

    /**
     * Annotates a method of a {@link Whiteboard.Component} class that resumes the component, i.e., starts the component when all its dependencies become
     * available. The method's return type must be {@link Stoppable}. The returned {@link Stoppable} object, if any,
     * will be invoked to stop the component when
     * any of its components become unavailable.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public static @interface Start { }

    /**
     * Denotes a whiteboard component. Whiteboard components mark one of their methods @{@link Start}. That method may have two kinds of arguments: OSGi service
     * interfaces annotated with {@link Service} and ordinary types denoting dependency injected components. When all OSGi services become dependencies become
     * available, the method is called to start the receiver. The method's return type must be {@link Stoppable} and the return value of the method is invoked
     * to stop the component if any of the services become unavailable.
     */
    @ComponentGroup
    interface Component { }

    /**
     * Denotes an OSGi service that will be registered when the host bundle is started. A service registration is also a whiteboard {@link Component} and the
     * start / stop logic described therein equally applies.
     *
     * @author Tibor Varga
     */
    @ComponentGroup
    interface Registration<T> {

        /**
         * Returns the list of classes this service is to be registered as.
         *
         * @return the list of classes this service is to be registered as.
         */
        Class<? super T>[] types();

        /**
         * Returns the registration properties for this service, if any.
         *
         * @return the registration properties for this service; may be <code>null</code>.
         */
        Properties properties();
    }

    /**
     * An event source that wishes to receive notification about event client registration as per the Whiteboard pattern. An event source is also a whiteboard
     * {@link Component} and the start / stop logic described therein equally applies.
     */
    @ComponentGroup
    interface EventSource<T> {

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
         */
        void stop();
    }
}

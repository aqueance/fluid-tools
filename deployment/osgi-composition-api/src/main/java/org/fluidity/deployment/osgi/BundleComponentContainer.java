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

package org.fluidity.deployment.osgi;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

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
 * <li>get notified when OSGi services become {@linkplain Registration.Listener#serviceAdded available} and
 * {@linkplain Registration.Listener#serviceRemoved unavailable}</li>
 * </ul>
 * This container automatically finds, loads, and manages all implementations of the {@link Managed Managed} interface visible to its class loader, which is
 * the OSGi bundle class loader. Implementations of the <code>Managed</code> interface include more specific components that implement {@link Registration
 * Registration} and {@link Registration.Listener Registration.Listener}.
 * <p/>
 * By design, you never directly use this interface; rather, you implement your bundle as a collection of components managed by this container.
 * <h3>Depending on OSGi Services</h3>
 * The constructor parameters of <code>Managed</code> components annotated with the {@link Service @Service} are dependencies to OSGi services while parameters
 * not so annotated are ordinary dependencies.
 * <p/>
 * When all of the OSGi services depended on, directly or indirectly through other <code>Managed</code> components, become available, the component is
 * instantiated and its {@link Managed#start() start()} method is invoked to start the component. The {@link Stoppable#stop() stop()} method of the component
 * will be invoked when any of those OSGi services becomes unavailable and then the component instance is discarded.
 * <h4>Example</h4>
 * <pre>
 * final class MyComponent implements <span class="hl1">BundleComponentContainer.Managed</span> {
 *
 *   public MyComponent(final {@linkplain Service @Service} SomeService service, final SomeDependency dependency) {
 *     ...
 *   }
 *
 *   public void start() throws Exception {
 *     ...
 *   }
 *
 *   public void stop() throws Exception {
 *     ...
 *   }
 *   ...
 * }
 * </pre>
 * <h3>Getting registered as OSGi Service</h3>
 * {@link Registration} components are <code>Managed</code> components that are registered as OSGi services when all OSGi services they depend on become
 * available, and get unregistered when any of those become unavailable.
 * <h4>Example</h4>
 * <pre>
 * final class MyComponent implements <span class="hl1">BundleComponentContainer.Registration</span> {
 *
 *   public MyComponent(final {@linkplain Service @Service} SomeService service, final SomeDependency dependency) {
 *     ...
 *   }
 *   ...
 * }
 * </pre>
 * <h3>Notification about OSGi Service Registration Events</h3>
 * {@link Registration.Listener} components are <code>Managed</code> components that receive notifications about OSGi service registration events via the
 * {@link Registration.Listener#serviceAdded serviceAdded()} method and un-registration events via the {@link
 * Registration.Listener#serviceRemoved serviceRemoved()} method.
 * <p/>
 * A <code>Managed</code> component may, at the same time, be a registered OSGi service and may receive notifications about OSGi service registration events by
 * implementing both {@link Registration} and {@link Registration.Listener}.
 * <h4>Example</h4>
 * <pre>
 * final class MyComponent implements <span class="hl1">BundleComponentContainer.Registration.Listener</span> {
 *
 *   public MyComponent(final {@linkplain Service @Service} SomeService service, final SomeDependency dependency) {
 *     ...
 *   }
 *   ...
 * }
 * </pre>
 * <h3><Code>Managed</Code> Component Status</h3>
 * This container exports an implementations of the OSGi service {@link Status}. OSGi service clients can query that service about active, inactive and failed
 * components managed by this container. The exported service uses the <code>bundle-symbolic-name</code> service property to distinguish these service
 * instances from one another.
 * <h4>Example</h4>
 * This example shows how to use the exported service when there's only one bundle exporting it. If there may be multiple such bundles, it is better to
 * implement the {@link BundleComponentContainer.Registration.Listener} interface and get notified about all exported service instances.
 * <pre>
 * final class MyComponent implements <span class="hl2">BundleComponentContainer.Registration</span> {
 *
 *   private final <span class="hl1">BundleComponentContainer.Status</span> status;
 *
 *   public MyComponent(final {@linkplain Service @Service} <span class="hl1">BundleComponentContainer.Status</span> status) {
 *     this.status = status;
 *   }
 *
 *   ...
 *
 *   public void check() {
 *       final Collection&lt;Class&lt;?>> failed = status.<span class="hl1">failedComponents</span>();
 *       ...
 *   }
 * }
 * </pre>
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
     * A managed component that will be discovered and added to the container.
     * <p/>
     * Managed components may have two kinds of dependencies: OSGi service interfaces annotated with {@link Service @Service} and ordinary types denoting
     * components in the same bundle.
     * </p>
     * When all OSGi services dependencies of a managed component become available, the component is instantiated and its {@link #start() start()} method is
     * invoked. The {@link #stop() stop()} method of the component is invoked to stop the component if any of the services become unavailable and then the
     * instance is discarded.
     *
     * @author Tibor Varga
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
         *
         * @author Tibor Varga
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
            void serviceAdded(T component, Properties properties);

            /**
             * Notifies the event source that an event consumer has been removed.
             *
             * @param component the event consumer that has been removed.
             */
            void serviceRemoved(T component);
        }
    }

    /**
     * Allows an item managed by the container to be explicitly removed from the container. This is also done automatically when the owning bundle is stopped.
     * <p/>
     * You never use this interface directly but through more specific interfaces such as {@link Managed}, {@link Registration} and
     * {@link Registration.Listener}.
     *
     * @author Tibor Varga
     */
    interface Stoppable {

        /**
         * Stops the receiver.
         *
         * @throws Exception if anything goes wrong.
         */
        void stop() throws Exception;
    }

    /**
     * OSGi service to query about component states in this bundle. Each bundle that uses the {@link BundleComponentContainer} to manage OSGi related
     * components will register an instance for this service interface, identifying the instance using the {@link #ID} property with the bundle's symbolic
     * name as its value.
     *
     * @author Tibor Varga
     */
    interface Status {

        /**
         * Used as a service registration property to identify the instance of this service for a particular bundle.
         */
        String ID = "bundle-symbolic-name";

        /**
         * Returns the list of active components.
         *
         * @return a list of <code>Class</code> objects.
         */
        Collection<Class<?>> active();

        /**
         * Returns the list of components that are waiting for OSGi services to start, along with the descriptor of the services they each are waiting to
         * start.
         *
         * @return a list of <code>Class</code> objects.
         */
        Map<Class<?>, Collection<Service>> inactive();

        /**
         * Returns the list of components that failed to start.
         *
         * @return a list of <code>Class</code> objects.
         */
        Collection<Class<?>> failed();
    }
}

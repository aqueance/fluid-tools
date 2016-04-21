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

package org.fluidity.deployment.osgi;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.fluidity.composition.ServiceProvider;

/**
 * Bundle components with managed life cycle. Managed components can do any combination of the followings simply by implementing one or more interfaces:<ul>
 * <li>declare dependencies on {@linkplain Service OSGi services} and
 * <ul>
 * <li>get instantiated and {@linkplain org.fluidity.deployment.osgi.BundleComponents.Managed#start() started} when all those services become available</li>
 * <li>get {@linkplain org.fluidity.deployment.osgi.BundleComponents.Managed#stop() stopped} and discarded when any of those services become unavailable</li>
 * </ul>
 * </li>
 * <li>get registered as {@linkplain org.fluidity.deployment.osgi.BundleComponents.Registration OSGi service}</li>
 * <li>get notified when certain OSGi services become {@linkplain org.fluidity.deployment.osgi.BundleComponents.Registration.Listener#serviceAdded available} and
 * {@linkplain org.fluidity.deployment.osgi.BundleComponents.Registration.Listener#serviceRemoved unavailable}</li>
 * </ul>
 * This container automatically finds, loads, and manages all implementations of the {@link org.fluidity.deployment.osgi.BundleComponents.Managed Managed} interface visible to its class loader, which is
 * the OSGi bundle class loader. Implementations of the <code>Managed</code> interface include more specific components that implement {@link org.fluidity.deployment.osgi.BundleComponents.Registration
 * Registration} and {@link org.fluidity.deployment.osgi.BundleComponents.Registration.Listener Registration.Listener}.
 * <p>
 * By design, you never directly use this interface. Rather, you implement your bundle as a collection of components managed by this container.
 * <p>
 * <b>Note</b>: Managed components cannot consume or transmit component context. Each bundle will have exactly zero or one instance of every managed component
 * in the empty context and their dependency references will have an empty base context.
 * <h3>Depending on OSGi Services</h3>
 * The dependencies of <code>Managed</code> components annotated with the {@link Service @Service} are dependencies to OSGi services while parameters not so
 * annotated are ordinary dependencies.
 * <p>
 * When all of the OSGi services depended on, directly or indirectly through other <code>Managed</code> components, become available, the component is
 * instantiated and its {@link org.fluidity.deployment.osgi.BundleComponents.Managed#start() start()} method is invoked to start the component. The {@link org.fluidity.deployment.osgi.BundleComponents.Stoppable#stop() stop()} method of the component
 * will be invoked when any of those OSGi services becomes unavailable and then the component instance is discarded.
 * <p>
 * An OSGi service referenced as a Java interface will be able to see classes in the local OSGi bundle as well as the bundle that exports it.
 * <h4>Example</h4>
 * <pre>
 * final class <span class="hl2">MyComponent</span> implements <span class="hl1">BundleComponents.Managed</span> {
 *
 *   <span class="hl2">MyComponent</span>(final {@linkplain Service @Service} SomeService service, final SomeDependency dependency) {
 *     &hellip;
 *   }
 *
 *   public void <span class="hl1">start</span>() throws Exception {
 *     &hellip;
 *   }
 *
 *   public void <span class="hl1">stop</span>() throws Exception {
 *     &hellip;
 *   }
 *
 *   &hellip;
 * }
 * </pre>
 * <h3>Getting Registered as OSGi Service</h3>
 * {@link org.fluidity.deployment.osgi.BundleComponents.Registration} components are <code>Managed</code> components that are registered as OSGi services when all OSGi services they depend on become
 * available, and get unregistered when any of those become unavailable.
 * <h4>Example</h4>
 * <pre>
 * final class <span class="hl2">MyComponent</span> implements <span class="hl1">BundleComponents.Registration</span> {
 *
 *   <span class="hl2">MyComponent</span>(final {@linkplain Service @Service} SomeService service, final SomeDependency dependency) {
 *     &hellip;
 *   }
 *
 *   &hellip;
 * }
 * </pre>
 * <h3>Notification about OSGi Service Registrations</h3>
 * {@link org.fluidity.deployment.osgi.BundleComponents.Registration.Listener} components are <code>Managed</code> components that receive notifications about OSGi service registration events via the
 * {@link org.fluidity.deployment.osgi.BundleComponents.Registration.Listener#serviceAdded serviceAdded()} method and un-registration events via the {@link
 * org.fluidity.deployment.osgi.BundleComponents.Registration.Listener#serviceRemoved serviceRemoved()} method.
 * <h4>Example</h4>
 * <pre>
 * final class <span class="hl2">MyComponent</span> implements <span class="hl1">BundleComponents.Registration.Listener</span> {
 *
 *   <span class="hl2">MyComponent</span>(final {@linkplain Service @Service} SomeService service, final SomeDependency dependency) {
 *     &hellip;
 *   }
 *
 *   &hellip;
 * }
 * </pre>
 * <h3>Managed Component Status</h3>
 * This container exports an implementations of the OSGi service {@link org.fluidity.deployment.osgi.BundleComponents.Status}. OSGi service clients can query that service about active, inactive and failed
 * components managed by this container. The exported service uses the <code>bundle-symbolic-name</code> service property to distinguish these service
 * instances from one another.
 * <h4>Example</h4>
 * This example shows how to use the exported service when there's only one bundle exporting it. If there may be multiple such bundles, it is better to
 * implement the {@link org.fluidity.deployment.osgi.BundleComponents.Registration.Listener} interface and get notified about all exported service instances.
 * <pre>
 * final class <span class="hl3">MyComponent</span> implements <span class="hl2">BundleComponents.Registration</span> {
 *
 *   private final <span class="hl1">BundleComponents.Status</span> status;
 *
 *   <span class="hl3">MyComponent</span>(final {@linkplain Service @Service} <span class="hl1">BundleComponents.Status</span> status) {
 *     this.status = status;
 *   }
 *
 *   &hellip;
 *
 *   public void check() {
 *       final Collection&lt;Class&lt;?&gt;&gt; failed = status.<span class="hl1">failedComponents</span>();
 *       &hellip;
 *   }
 * }
 * </pre>
 *
 * @author Tibor Varga
 */
public interface BundleComponents {

    /**
     * A managed component that will be discovered and added to the bundle's container.
     * <p>
     * Managed components may have two kinds of dependencies: OSGi service interfaces annotated with {@link org.fluidity.deployment.osgi.Service @Service} and ordinary types denoting
     * components in the same bundle.
     * </p>
     * When all OSGi services dependencies of a managed component become available, the component is instantiated and its {@link #start() start()} method is
     * invoked. The {@link #stop() stop()} method of the component is invoked to stop the component if any of its dependent services become unavailable, and
     * then the instance is discarded.
     *
     * @author Tibor Varga
     */
    @ServiceProvider(type = "bundle-components")
    interface Managed extends BundleComponents.Stoppable {

        /**
         * Starts the receiver.
         *
         * @throws Exception if anything goes wrong.
         */
        void start() throws Exception;
    }

    /**
     * An OSGi service that will be registered when {@linkplain BundleComponents.Managed#start() started} and un-registered when
     * {@linkplain BundleComponents.Managed#stop() stopped} by its managing container. In terms of the <a
     * href="http://www.osgi.org/wiki/uploads/Links/whiteboard.pdf">Whiteboard pattern</a>, this is an event consumer that an event source will discover and
     * send events to.
     * <p>
     * The {@link Service.Type} annotation can be used to explicitly specify the service interfaces, which default to those
     * directly implemented by the service class, less {@link BundleComponents.Registration}. If the service class does not directly implement
     * interface, its super class is consulted. If the service class or its ancestor being checked does not have a super class, the class itself will be the
     * service interface.
     * <p>
     * This is a {@link Managed} component thus the start / stop logic described therein applies.
     *
     * @author Tibor Varga
     */
    interface Registration extends Managed {

        /**
         * Returns the registration properties for this service, if any.
         *
         * @return the registration properties for this service; may be <code>null</code>.
         */
        Properties properties();

        /**
         * An OSGi service registration listener. In terms of the <a href="http://www.osgi.org/wiki/uploads/Links/whiteboard.pdf">Whiteboard pattern</a>, this
         * is an event source that will receive notifications about event consumer registrations.
         * <p>
         * This is a {@link Managed} component thus the start / stop logic described therein applies.
         *
         * @param <T> the type of the component whose registration events this is a listener for.
         *
         * @author Tibor Varga
         */
        interface Listener<T> extends Managed {

            /**
             * Returns the class that event consumers of interest are expected to register as. Service registrations only against the returned class or its
             * name will be recognized as event consumer registration.
             *
             * @return the class that event consumers of interest are expected to register as.
             */
            Class<T> type();

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
     * Super-interface for the various {@linkplain BundleComponents.Managed managed} component interfaces.
     * <p>
     * <b>NOTE</b>: You never use this interface directly but through more specific interfaces such as {@link Managed}, {@link Registration}, and
     * {@link Registration.Listener}.
     *
     * @author Tibor Varga
     */
    @FunctionalInterface
    interface Stoppable {

        /**
         * Stops the receiver.
         *
         * @throws Exception if anything goes wrong.
         */
        void stop() throws Exception;
    }

    /**
     * OSGi service to query about managed component states in this bundle. Each bundle that uses a dependency injection container to manage OSGi related
     * components will register an instance for this service interface, identifying the instance using the {@link #BUNDLE} property with the bundle's symbolic
     * name as its value.
     *
     * @author Tibor Varga
     */
    interface Status {

        /**
         * Used as a service registration property to identify the instance of this service for a particular bundle. The value of the property is the symbolic
         * name of the bundle that exports the status service.
         */
        String BUNDLE = "bundle";

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

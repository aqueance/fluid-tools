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

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.ServiceProvider;

/**
 * Allows an ordinary component to integrate to the OSGi service listener infrastructure. Because services in OSGi come and go, service tracking is implemented
 * to instantiate a new component when all of the services it depends on become available and to discard that component when any of its required dependencies
 * get unregistered.
 * <p/>
 * Ordinary components can participate in this dance one of two ways:
 * <ul>
 * <li>by implementing {@link ServiceTracker.Registration} and depending on this interface:
 * <pre>
 * final class ServiceDependentActivation implements ServiceTracker.Registration {
 *
 *     public ServiceDependentActivation(final &#64;SomeContext ServiceTracker tracker) {
 *         tracker.manage(ServiceDependent.class);
 *     }
 * }
 *
 * &#64;Component(automatic = false)
 * &#64;Context(SomeContext.class)
 * final class ServiceDependent implements ServiceTracker.Managed {
 *
 *     public ServiceDependent(final &#64;Service(api = ServiceApi.class, filter = "...") ServiceApi service, ...) {
 *         ...
 *     }
 * }
 * </pre>
 * This method allows context to be provided to the {@link Managed} component: the context present at the reference on the {@link ServiceTracker} dependency
 * will be passed on to the {@link Managed} component when it is instantiated.
 * </li>
 * <li>by implementing {@link ServiceTracker.Managed}:</li>
 * <pre>
 * final class ServiceDependent implements ServiceTracker.Managed {
 *
 *     public ServiceDependent(final &#64;Service(api = ServiceApi.class, filter = "...") ServiceApi service, ...) {
 *         ...
 *     }
 * }
 * </pre>
 * </ul>
 *
 * Note the presense of the <code>@Component(automatic = false)</code> annotation in the first case and the absence thereof in the second.
 *
 * @author Tibor Varga
 */
public interface ServiceTracker {

    /**
     * Marks a component that wishes to establish component context to the component managed by a {@link ServiceTracker}.
     *
     * @author Tibor Varga
     */
    @ComponentGroup
    interface Registration { }

    /**
     * Asks the tracker to instantiate the given component type when its {@link Service} dependencies are all available. The component will be discarded when
     * one of its {@link Service} dependencies become unavailable.
     * <p/>
     * The component must implement {@link Managed} and must be a component with {@link org.fluidity.composition.Component#automatic()} set to false.
     * <p/>
     * The callback is invoked about the availability of the service tracked. No tracking will take place until this method is invoked. The method returns in a
     * timely manner.
     *
     * @param type the component class the tracker should instantiate when all its service dependencies become available.
     *
     * @return a reference to the component that will either return the currently available instance of the component or throw a
     *         {@link ComponentContainer.ResolutionException} when the component is not available.
     */
    <T extends ServiceTracker.Managed> Dependency<T> manage(Class<T> type);

    /**
     * Allows the caller to pass the container it wishes to resolve the dependencies of the {@link Managed} component. See {@link #manage(Class)} for other
     * details.
     *
     * @param type      see {@link #manage(Class)}.
     * @param container the container to use to instantiate the managed component.
     *
     * @return see {@link #manage(Class)}.
     */
    <T extends ServiceTracker.Managed> Dependency<T> manage(Class<T> type, ComponentContainer container);

    /**
     * Interface to be implemented by the components intended to be managed by a {@link ServiceTracker}.
     *
     * @author Tibor Varga
     */
    @ServiceProvider(type = "tracker")
    interface Managed {

        /**
         * Starts the component once it has been instantiated.
         * @throws Exception thrown at will
         */
        void start() throws Exception;

        /**
         * Stops the component before it is discarded.
         * @throws Exception thrown at will
         */
        void stop() throws Exception;
    }

    /**
     * A reference to a {@link Managed} component.
     *
     * @param <T> the type of the component.
     *
     * @author Tibor Varga
     */
    interface Dependency<T> {

        /**
         * Returns the current instance of the {@link Managed} component.
         *
         * @return the current instance of the {@link Managed} component.
         *
         * @throws ComponentContainer.ResolutionException
         *          when the method is called before without a component instance being available to return.
         */
        T get() throws ComponentContainer.ResolutionException;
    }

}

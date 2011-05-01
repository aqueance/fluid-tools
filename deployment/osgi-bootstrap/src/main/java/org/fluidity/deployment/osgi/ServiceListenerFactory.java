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

import java.util.Map;

import org.osgi.framework.BundleContext;

/**
 * Manages OSGi service tracking for components that depend on those services. This component creates OSGi service listeners that invoke callback methods at
 * appropriate events.
 *
 * @author Tibor Varga
 */
public interface ServiceListenerFactory {

    /**
     * Creates a new service listener for the given services. When all specified services of the component become available, the callback's {@link
     * ServiceListenerFactory.Callback#resume()} method will be invoked. When any one of the services become unavailable, the callback's {@link
     * ServiceListenerFactory.Callback#suspend()} method will be invoked.
     * discarded.
     *
     * @param name     the name of the listener for diagnostic purposes.
     * @param context  the containing OSGi bundle context to register service listeners to.
     * @param services the list of service specifications mapped to a list of mutable service references.
     * @param callback the callback to invoke.
     *
     * @return a handle that can be used to explicitly remove the listener. The listener will automatically removed when the containing bundle is stopped.
     */
    Handle create(String name, BundleContext context, Map<Service, ServiceDependencyFactory.MutableReference> services, Callback callback);

    /**
     * Allows explicit removal of the service listener.
     */
    interface Handle {

        /**
         * Removes the service listener.
         */
        void remove();
    }

    /**
     * Callback interface to invoke at appropriate events.
     *
     * @author Tibor Varga
     */
    interface Callback {

        /**
         * Invokes to check that the reference to a possibly transient component is still valid.
         *
         * @return <code>true</code> if callback methods are still expected, <code>false</code> otherwise.
         */
        boolean valid();

        /**
         * Returns whether the component has been resumed and not suspended since.
         *
         * @return <code>true</code> if the component has been resumed and not suspended since, <code>false</code> otherwise.
         */
        boolean running();

        /**
         * Notifies the receiver that all dependent services have been registered, the component may be resumed.
         */
        void resume();

        /**
         * Notifies the receiver that some dependent services have been unregistered, the component must be suspended.
         */
        void suspend();
    }
}

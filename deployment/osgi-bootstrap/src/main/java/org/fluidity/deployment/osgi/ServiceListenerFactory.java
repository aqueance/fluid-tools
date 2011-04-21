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

import org.osgi.framework.BundleContext;

/**
 * TODO: documentation
 *
 * @author Tibor Varga
 */
public interface ServiceListenerFactory {

    /**
     * Creates a new service listener for the given type. The type parameter must be an actual component class, with
     * @{@link org.fluidity.composition.Component#automatic()} set to <code>false</code>. When all service dependencies of the component become available,
     * it is instantiated and the callback is notified. When any one of the services become unavailable, the callback is notified and the component is
     * discarded.
     *
     * @param api
     * @param type is the component class to bind.
     * @param container
     * @param context
     * @param callback
     * @return
     */
    <T> void create(Class<? super T> api, Class<T> type, ComponentContainer container, BundleContext context, Callback callback);

    /**
     * TODO: documentation
     *
     * @author Tibor Varga
     */
    interface Callback<T> {

        /**
         * TODO: documentation
         * @return
         */
        boolean started();

        /**
         * TODO: documentation
         * @param component the service dependent component instantiated or <code>null</code> if not bound.
         */
        void start(T component);

        /**
         * TODO: documentation
         */
        void stop();
    }
}

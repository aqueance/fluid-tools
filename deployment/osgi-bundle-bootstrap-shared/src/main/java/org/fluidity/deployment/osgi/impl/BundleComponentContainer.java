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

package org.fluidity.deployment.osgi.impl;

import org.fluidity.composition.ComponentContainer;

/**
 * Component life cycle management backed by the OSGi service registry. This container provides the functionality described for {@link
 * org.fluidity.deployment.osgi.BundleComponents}.
 * <p>
 * *Note*: this class is <code>only</code> public to allow access to it from an OSGi  bundle by the bundle's class loader.
 *
 * @param <T> identifies the class the class loader of which to use to populate the bundle's dependency injection container.
 *
 * @author Tibor Varga
 * @see org.fluidity.deployment.osgi.BundleComponents
 */
@SuppressWarnings("UnusedDeclaration")
public interface BundleComponentContainer<T> {

    /**
     * Starts the container. Invoked automatically when the host bundle starts.
     *
     * @param container the dependency injection container of the calling bundle to use.
     * @param loader    the class loader of the calling bundle.
     */
    void start(ComponentContainer container, ClassLoader loader);

    /**
     * Stops the container. Invoked automatically when the host bundle stops.
     */
    void stop();
}

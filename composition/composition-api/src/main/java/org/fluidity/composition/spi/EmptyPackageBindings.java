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

package org.fluidity.composition.spi;

import org.fluidity.composition.ComponentContainer;

/**
 * Empty package bindings to simplify creation of actual implementations.
 *
 * @author Tibor Varga
 */
public abstract class EmptyPackageBindings implements PackageBindings {

    /**
     * {@inheritDoc}
     * <p/>
     * This implementation does nothing, allows subclasses to do nothing by not overriding this method.
     */
    public void bindComponents(final ComponentContainer.Registry registry) {
        // empty
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This implementation does nothing, allows subclasses to do nothing by not overriding this method.
     */
    public void initializeComponents(final ComponentContainer container) {
        // empty
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This implementation does nothing, allows subclasses to do nothing by not overriding this method.
     */
    public void shutdownComponents(final ComponentContainer container) {
        // empty
    }
}

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

package org.fluidity.composition.container.spi;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.container.ContextDefinition;

/**
 * Implemented by a {@linkplain org.fluidity.composition.MutableContainer component container} to allow a {@link
 * org.fluidity.composition.container.DependencyInjector} to interact with that container.
 * <h3>Usage</h3>
 * You don't interact with an internal interface.
 *
 * @author Tibor Varga
 */
public interface DependencyResolver extends DependencyGraph {

    /**
     * Returns a new child container with its base context set to the given definition.
     *
     * @param context the context for the new container to use as base context.
     *
     * @return a new component container.
     */
    ComponentContainer container(ContextDefinition context);

    /**
     * Checks if an instance has been cached for the given component interface and component context, and returns the instance of found. This method never
     * instantiates classes.
     *
     * @param api     the component interface to check.
     * @param context the component context to check.
     *
     * @return the instance if found cached, <code>null</code> otherwise.
     */
    Object cached(Class<?> api, ComponentContext context);
}

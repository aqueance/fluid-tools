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

package org.fluidity.composition.container.spi;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.container.ContextDefinition;

/**
 * Parent container of the root dependency injection container of an application. When available, the implementation is mapped to some dependency injection
 * mechanism of the application container itself.
 */
public interface PlatformContainer {

    /**
     * Tells, without any reservation on the part of the receiver, whether the given component interface can be resolved by the receiver.
     *
     * @param api     the component interface to check.
     * @param context the current component context.
     *
     * @return <code>true</code> if the component interface can be resolved, <code>false</code> otherwise.
     */
    boolean containsComponent(Class<?> api, final ContextDefinition context);

    /**
     * See {@link ComponentContainer#getComponent(Class)}.
     *
     * @param api     see {@link ComponentContainer#getComponent(Class)}.
     * @param context the current component context.
     *
     * @return see {@link ComponentContainer#getComponent(Class)}.
     *
     * @throws ComponentContainer.ResolutionException
     *          see {@link ComponentContainer#getComponent(Class)}.
     */
    <T> T getComponent(Class<T> api, ContextDefinition context) throws ComponentContainer.ResolutionException;

    /**
     * Tells, without any reservation on the part of the receiver, whether the given group interface can be resolved by the receiver.
     *
     * @param api     the component interface to check.
     * @param context the current component context.
     *
     * @return <code>true</code> if the group interface can be resolved, <code>false</code> otherwise.
     */
    boolean containsComponentGroup(Class<?> api, final ContextDefinition context);

    /**
     * See {@link ComponentContainer#getComponentGroup(Class)}.
     *
     * @param api     see {@link ComponentContainer#getComponentGroup(Class)}.
     * @param context the component context prevalent at the group reference site.
     *
     * @return see {@link ComponentContainer#getComponentGroup(Class)}.
     */
    <T> T[] getComponentGroup(Class<T> api, ContextDefinition context);

    /**
     * Returns a textual identifier for the container.
     *
     * @return a textual identifier for the container.
     */
    String id();
}

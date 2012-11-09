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

package org.fluidity.composition.container;

import org.fluidity.composition.ComponentContainer;

/**
 * Parent container of the root dependency injection container of an application. When available, the implementation integrates the Fluid Tools dependency
 * injection framework to a legacy dependency injection framework prevalent in the rest of the application.
 * <h3>Usage</h3>
 * <pre>
 * {@linkplain org.fluidity.composition.Containers}.{@linkplain org.fluidity.composition.Containers#prepare() prepare}().{@linkplain org.fluidity.composition.ContainerBoundary#setSuperContainer(SuperContainer) setSuperContainer}(new <span class="hl1">SuperContainer</span>() {
 *   &hellip;
 * });
 * </pre>
 */
@SuppressWarnings("JavadocReference")
public interface SuperContainer {

    /**
     * Tells, without any reservation on the part of the receiver, whether the given component interface can be resolved by the receiver.
     *
     * @param api     the component interface to check.
     * @param context the current component context.
     *
     * @return <code>true</code> if the component interface can be resolved, <code>false</code> otherwise.
     */
    boolean containsComponent(Class<?> api, ContextDefinition context);

    /**
     * See {@link org.fluidity.composition.OpenContainer#getComponent(Class)}.
     *
     * @param api     see {@link org.fluidity.composition.OpenContainer#getComponent(Class)}.
     * @param context the current component context.
     * @param <T>     the component type to return.
     *
     * @return see {@link org.fluidity.composition.OpenContainer#getComponent(Class)}.
     *
     * @throws ComponentContainer.ResolutionException
     *          see {@link org.fluidity.composition.OpenContainer#getComponent(Class)}.
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
    boolean containsComponentGroup(Class<?> api, ContextDefinition context);

    /**
     * See {@link org.fluidity.composition.OpenContainer#getComponentGroup(Class)}.
     *
     * @param api     see {@link org.fluidity.composition.OpenContainer#getComponentGroup(Class)}.
     * @param context the component context prevalent at the group reference site.
     * @param <T>     the component group type to return.
     *
     * @return see {@link org.fluidity.composition.OpenContainer#getComponentGroup(Class)}.
     */
    <T> T[] getComponentGroup(Class<T> api, ContextDefinition context);

    /**
     * Returns a textual identifier for the container. Used in {@link Object#toString() ComponentContainer.toString()}.
     *
     * @return a textual identifier for the container.
     */
    String id();
}
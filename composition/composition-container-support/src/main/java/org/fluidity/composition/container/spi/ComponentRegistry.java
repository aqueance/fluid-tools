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
import org.fluidity.composition.Components;
import org.fluidity.composition.MutableContainer;

/**
 * Implemented by a {@linkplain org.fluidity.composition.MutableContainer component container} to allow registration of component bindings. This is an internal
 * interface implemented through {@link EmptyComponentContainer}.
 * <h3>Usage</h3>
 * You don't interact with an internal interface.
 *
 * @author Tibor Varga
 */
public interface ComponentRegistry {

    /**
     * Binds a component class to a list of component interfaces and group interfaces.
     *
     * @param interfaces the component and group interfaces; never <code>null</code>.
     *
     * @throws ComponentContainer.BindingException
     *          when the implementation cannot be bound to some interface.
     */
    void bindComponent(Components.Interfaces interfaces) throws ComponentContainer.BindingException;

    /**
     * Binds a component instance to a list of component interfaces and group interfaces.
     *
     * @param instance   the component instance.
     * @param interfaces the component and group interfaces; never <code>null</code>.
     *
     * @throws ComponentContainer.BindingException
     *          when the implementation cannot be bound to some interface.
     */
    void bindInstance(Object instance, Components.Interfaces interfaces) throws ComponentContainer.BindingException;

    /**
     * Creates a child container and binds the given component while making its bindings also visible in the container behind this registry.
     *
     * @param interfaces the component and group interfaces; never <code>null</code>.
     *
     * @return the child container to bind further components in.
     *
     * @throws ComponentContainer.BindingException
     *          when the implementation cannot be bound to some interface.
     */
    MutableContainer makeChildContainer(Components.Interfaces interfaces) throws ComponentContainer.BindingException;
}

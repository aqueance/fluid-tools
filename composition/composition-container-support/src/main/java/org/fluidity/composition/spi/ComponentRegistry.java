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

import org.fluidity.composition.Components;
import org.fluidity.composition.OpenComponentContainer;

/**
 * Allows registration of component bindings to a backing {@link OpenComponentContainer component container}.
 *
 * @author Tibor Varga
 */
public interface ComponentRegistry {

    /**
     * Binds a component class to a list of component interfaces and group interfaces.
     *
     * @param interfaces     the component and group interfaces; never <code>null</code>.
     *
     * @throws OpenComponentContainer.BindingException
     *          when the implementation cannot be bound to some interface
     */
    void bindComponent(Components.Interfaces interfaces) throws OpenComponentContainer.BindingException;

    /**
     * Binds a component instance to a list of component interfaces and group interfaces.
     *
     * @param instance   the component instance.
     * @param interfaces the component and group interfaces; never <code>null</code>.
     *
     * @throws OpenComponentContainer.BindingException
     *          when the implementation cannot be bound to some interface
     */
    void bindInstance(Object instance, Components.Interfaces interfaces) throws OpenComponentContainer.BindingException;

    /**
     * Creates a child container and calls {@link #bindComponent(Components.Interfaces)} with the given parameters. The component implementation will be
     * bound to all component interfaces and group interfaces also in the container this is a registry for.
     *
     * @param interfaces     the component and group interfaces; never <code>null</code>.
     *
     * @return the child container to bind further components in.
     *
     * @throws OpenComponentContainer.BindingException
     *          when the implementation cannot be bound to some interface
     */
    OpenComponentContainer makeChildContainer(Components.Interfaces interfaces) throws OpenComponentContainer.BindingException;

    /**
     * Creates an empty child container.
     *
     * @return an empty child container.
     */
    OpenComponentContainer makeChildContainer();
}

/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.fluidity.composition.spi;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.Components;
import org.fluidity.composition.OpenComponentContainer;

/**
 * Core registration API for dependency injection container implementations.
 *
 * @author Tibor Varga
 */
public interface ComponentRegistry {

    /**
     * Binds a component class to a list of component interfaces and group interfaces.
     *
     * @param interfaces     the component and group interfaces; never <code>null</code>.
     *
     * @throws ComponentContainer.BindingException
     *          when the implementation cannot be bound to some interface
     */
    void bindComponent(Components.Interfaces interfaces) throws ComponentContainer.BindingException;

    /**
     * Binds a component instance to a list of component interfaces and group interfaces.
     *
     * @param instance   the component instance.
     * @param interfaces the component and group interfaces; never <code>null</code>.
     *
     * @throws ComponentContainer.BindingException
     *          when the implementation cannot be bound to some interface
     */
    void bindInstance(Object instance, Components.Interfaces interfaces) throws ComponentContainer.BindingException;

    /**
     * Creates a child container and calls {@link #bindComponent(Components.Interfaces)} with the given parameters. The component implementation will be
     * bound to all component interfaces and group interfaces also in the container the receiver is a registry for.
     *
     * @param interfaces     the component and group interfaces; never <code>null</code>.
     *
     * @return the child container to bind further components in.
     *
     * @throws ComponentContainer.BindingException
     *          when the implementation cannot be bound to some interface
     */
    OpenComponentContainer makeChildContainer(Components.Interfaces interfaces) throws ComponentContainer.BindingException;

    /**
     * Creates an empty child container.
     *
     * @return an empty child container.
     */
    OpenComponentContainer makeChildContainer();
}

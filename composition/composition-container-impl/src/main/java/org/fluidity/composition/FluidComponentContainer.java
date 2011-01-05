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

package org.fluidity.composition;

import java.util.Collection;

import org.fluidity.composition.spi.ContainerServices;

/**
 * In-house dependency injection container. Supports hierarchies of containers, context dependent variants of singletons, seamless resolution of cyclic
 * dependencies when target is interface.
 * <p/>
 * This is a shell around a {@link SimpleContainer} object.
 *
 * @author Tibor Varga
 */
final class FluidComponentContainer extends AbstractComponentContainer implements OpenComponentContainer {

    private final ComponentContainer.Registry registry;

    public FluidComponentContainer(final SimpleContainer container, boolean child) {
        this(container, container.services(), child);
    }

    public FluidComponentContainer(final SimpleContainer container, final ContainerServices services, final boolean child) {
        super(child ? new SimpleContainerImpl(container, services) : container);
        this.registry = new FluidComponentRegistry(this.container);
    }

    public <T> T getComponent(final Class<T> componentClass) {
        return container.get(componentClass);
    }

    public OpenComponentContainer makeNestedContainer() {
        return new FluidComponentContainer(container, true);
    }

    public <T> T initialize(final T component) {
        return container.initialize(component);
    }

    public Registry getRegistry() {
        return registry;
    }

    public <T> Collection<T> getComponents(final Class<T> componentInterface) {
        return container.allSingletons(componentInterface);
    }
}

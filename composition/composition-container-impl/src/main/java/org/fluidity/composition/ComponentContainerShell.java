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

import java.util.List;

/**
 * This is a shell around a {@link SimpleContainer} object.
 *
 * @author Tibor Varga
 */
final class ComponentContainerShell extends AbstractComponentContainer implements OpenComponentContainer {

    private final ComponentContainer.Registry registry;

    public ComponentContainerShell(final SimpleContainer container, boolean child) {
        this(container, container.services(), child);
    }

    public ComponentContainerShell(final SimpleContainer container, final ContainerServices services, final boolean child) {
        super(child ? new SimpleContainerImpl(container, services) : container);
        this.registry = new ComponentRegistryShell(this.container);
    }

    public <T> T getComponent(final Class<T> api) {
        return container.get(api);
    }

    public OpenComponentContainer makeChildContainer() {
        return new ComponentContainerShell(container, true);
    }

    public <T> T initialize(final T component) {
        return container.initialize(component);
    }

    public OpenComponentContainer getParentContainer() {
        return new ComponentContainerShell(container, false);
    }

    public Registry getRegistry() {
        return registry;
    }

    public <T> List<T> getAllComponents(final Class<T> api) {
        return container.allSingletons(api);
    }
}

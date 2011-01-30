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

import org.fluidity.composition.spi.EmptyComponentContainer;

/**
 * This is a shell around a {@link SimpleContainer} object.
 *
 * @author Tibor Varga
 */
final class ComponentRegistryShell extends EmptyComponentContainer.EmptyRegistry {

    private final SimpleContainer container;

    public ComponentRegistryShell(final SimpleContainer container) {
        this.container = container;
    }

    public <T> void bindDefault(final Class<? extends T> implementation) {
        container.bindComponent(componentInterface(implementation), implementation);
    }

    public <T> void bindComponent(final Class<T> key, final Class<? extends T> implementation) {
        container.bindComponent(key, implementation);
    }

    public <T> void bindInstance(final Class<? super T> key, final T instance) {
        container.bindInstance(key, instance);
    }

    public OpenComponentContainer makeChildContainer() {
        return new ComponentContainerShell(container.newChildContainer(), true);
    }

    public <T> OpenComponentContainer makeChildContainer(final Class<T> key, final Class<? extends T> implementation) {
        return new ComponentContainerShell(container.linkComponent(key, implementation), false);
    }
}

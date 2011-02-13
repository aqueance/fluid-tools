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
    private final ContextDefinition context;

    public ComponentRegistryShell(final SimpleContainer container, final ContextDefinition context) {
        this.container = container;
        this.context = context;
    }

    @Override
    protected void bindComponent(final Class<?> implementation, final Class<?>[] interfaces, final Class<?>[] groups)
            throws ComponentContainer.BindingException {
        container.bindComponent(implementation, interfaces, groups);
    }

    @Override
    protected void bindInstance(final Object instance, final Class<?>[] interfaces, final Class<?>[] groups)
            throws ComponentContainer.BindingException {
        container.bindInstance(instance, interfaces, groups);
    }

    @Override
    protected OpenComponentContainer makeChildContainer(final Class<?> implementation, final Class<?>[] interfaces, final Class<?>[] groups)
            throws ComponentContainer.BindingException {
        return new ComponentContainerShell(container.linkComponent(implementation, interfaces, groups), context, false);
    }

    public OpenComponentContainer makeChildContainer() {
        return new ComponentContainerShell(container.newChildContainer(), context, true);
    }
}

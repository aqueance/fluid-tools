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
 * Implements basic method relationships and useful functionality to registry implementations.
 */
final class EmptyRegistry implements ComponentContainer.Registry {

    private final ComponentRegistry delegate;

    public EmptyRegistry(final ComponentRegistry delegate) {
        this.delegate = delegate;
    }

    public OpenComponentContainer makeChildContainer() {
        return delegate.makeChildContainer();
    }

    @SuppressWarnings("unchecked")
    public final <T> void bindComponent(final Class<T> implementation, final Class<? super T>... interfaces) throws ComponentContainer.BindingException {
        delegate.bindComponent(Components.inspect(implementation, interfaces));
    }

    @SuppressWarnings("unchecked")
    public final <T> void bindInstance(final T instance, final Class<? super T>... interfaces) throws ComponentContainer.BindingException {
        assert instance != null;
        final Class<T> implementation = (Class<T>) instance.getClass();
        delegate.bindInstance(instance, Components.inspect(implementation, interfaces));
    }

    @SuppressWarnings("unchecked")
    public final <T> OpenComponentContainer makeChildContainer(final Class<T> implementation, final Class<? super T>... interfaces) throws ComponentContainer.BindingException {
        return delegate.makeChildContainer(Components.inspect(implementation, interfaces));
    }
}

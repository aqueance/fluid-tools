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
    public final <T> void bindComponent(final Class<T> type, final Class<? super T>... interfaces) throws ComponentContainer.BindingException {
        delegate.bindComponent(Components.inspect(type, interfaces));
    }

    @SuppressWarnings("unchecked")
    public final <T> void bindInstance(final T instance, final Class<? super T>... interfaces) throws ComponentContainer.BindingException {
        assert instance != null;
        final Class<T> implementation = (Class<T>) instance.getClass();
        delegate.bindInstance(instance, Components.inspect(implementation, interfaces));
    }

    @SuppressWarnings("unchecked")
    public final <T> ComponentContainer.Registry isolateComponent(final Class<T> type, final Class<? super T>... interfaces) throws ComponentContainer.BindingException {
        return delegate.makeChildContainer(Components.inspect(type, interfaces)).getRegistry();
    }
}

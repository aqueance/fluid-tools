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

import java.util.Collection;
import java.util.Collections;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.Components;
import org.fluidity.composition.spi.ComponentFactory;

/**
 * Implements basic method relationships and useful functionality to registry implementations.
 */
final class EmptyRegistry implements ComponentContainer.Registry {

    private final ComponentRegistry delegate;

    EmptyRegistry(final ComponentRegistry delegate) {
        this.delegate = delegate;
    }

    public final <T> void bindComponent(final Class<T> type, final Class<? super T>... interfaces) throws ComponentContainer.BindingException {
        delegate.bindComponent(Components.inspect(type, interfaces));
    }

    @SuppressWarnings("unchecked")
    public final <T> void bindInstance(final T instance, final Class<? super T>... interfaces) throws ComponentContainer.BindingException {
        if (instance != null) {
            delegate.bindInstance(instance, Components.inspect((Class<T>) instance.getClass(), interfaces));
        }
    }

    public <T> void bindComponentGroup(final Class<T> group, final Class<? extends T>[] types) {
        final Collection<Class<?>> groups = Collections.<Class<?>>singletonList(group);

        for (final Class<? extends T> type : types) {
            delegate.bindComponent(new Components.Interfaces(type, new Components.Specification[] {
                    new Components.Specification(type, groups)
            }));
        }
    }

    @SuppressWarnings("unchecked")
    public void bindFactory(final ComponentFactory factory, final Class<?>... interfaces) throws ComponentContainer.BindingException {
        if (factory != null) {
            delegate.bindInstance(factory, Components.inspect(factory.getClass(), (Class[]) interfaces));
        }
    }

    public final <T> ComponentContainer.Registry isolateComponent(final Class<T> type, final Class<? super T>... interfaces) throws ComponentContainer.BindingException {
        return delegate.makeChildContainer(Components.inspect(type, interfaces)).getRegistry();
    }
}

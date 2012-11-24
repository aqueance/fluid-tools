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

package org.fluidity.composition.container;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.fluidity.composition.Components;
import org.fluidity.composition.MutableContainer;
import org.fluidity.composition.ObservedContainer;
import org.fluidity.composition.OpenContainer;
import org.fluidity.composition.container.spi.EmptyComponentContainer;

/**
 * This is a shell around a {@link SimpleContainer} object.
 *
 * @author Tibor Varga
 */
final class ComponentContainerShell extends EmptyComponentContainer<SimpleContainer> {

    ComponentContainerShell(final ContainerServices services) {
        this(new SimpleContainerImpl(services), services.emptyContext(), false, false, null);
    }

    ComponentContainerShell(final SimpleContainer container, final ContextDefinition context, final boolean child) {
        this(container, context, child, false, null);
    }

    private ComponentContainerShell(final SimpleContainer container,
                                    final ContextDefinition context,
                                    final boolean child,
                                    final boolean domain,
                                    final Observer observer) {
        super(child ? container.newChildContainer(domain) : container, container.services(), context.copy(), observer);
    }

    public ObservedContainer container(final SimpleContainer container, final ContextDefinition context, final Observer observer) {
        return new ComponentContainerShell(container, context, false, false, observer);
    }

    @SuppressWarnings("unchecked")
    public <T> T initialize(final T component) {
        return (T) container.initialize(component, context.copy(), observer);
    }

    public Object invoke(final Object component, final boolean explicit, final Method method, final Object... arguments)
            throws ResolutionException, InvocationTargetException {
        return container.invoke(component, method, context.copy(), arguments, explicit);
    }

    public MutableContainer makeChildContainer() {
        return new ComponentContainerShell(container, context, true, false, observer);
    }

    public OpenContainer makeChildContainer(final Bindings... bindings) {
        return new ComponentContainerShell(container, context, true, false, observer).addBindings(bindings);
    }

    public OpenContainer makeDomainContainer(final Bindings... bindings) {
        return new ComponentContainerShell(container, context, true, true, observer).addBindings(bindings);
    }

    public void bindComponent(final Components.Interfaces interfaces) throws BindingException {
        container.bindComponent(interfaces);
    }

    public void bindInstance(final Object instance, final Components.Interfaces interfaces) throws BindingException {
        container.bindInstance(instance, interfaces);
    }

    public MutableContainer makeChildContainer(final Components.Interfaces interfaces) throws BindingException {
        return new ComponentContainerShell(container.linkComponent(interfaces), context, false, false, observer);
    }

    @Override
    public int hashCode() {
        return container.hashCode();
    }

    @Override
    public boolean equals(final Object object) {
        return object instanceof ComponentContainerShell ? container.equals(((ComponentContainerShell) object).container) : super.equals(object);
    }

    public String toString() {
        return container.toString();
    }
}

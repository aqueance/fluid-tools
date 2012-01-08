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

package org.fluidity.composition;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.fluidity.composition.impl.CompositeObserver;
import org.fluidity.composition.spi.ComponentResolutionObserver;
import org.fluidity.composition.spi.ContainerServices;
import org.fluidity.composition.spi.ContextDefinition;
import org.fluidity.composition.spi.DependencyGraph;
import org.fluidity.composition.spi.EmptyComponentContainer;
import org.fluidity.composition.spi.PlatformContainer;

/**
 * This is a shell around a {@link SimpleContainer} object.
 *
 * @author Tibor Varga
 */
final class ComponentContainerShell extends EmptyComponentContainer {

    private final SimpleContainer container;
    private final ContextDefinition context;
    private final ComponentResolutionObserver observer;

    public ComponentContainerShell(final ContainerServices services, final PlatformContainer platform) {
        this(new SimpleContainerImpl(services, platform), services.emptyContext(), false, false, null);
    }

    public ComponentContainerShell(final SimpleContainer container, final ContextDefinition context, final boolean child) {
        this(container, context, child, false, null);
    }

    public ComponentContainerShell(final SimpleContainer container,
                                   final ContextDefinition context,
                                   final boolean child,
                                   final boolean domain,
                                   final ComponentResolutionObserver observer) {
        assert container != null;
        assert context != null;
        this.container = child ? container.newChildContainer(domain) : container;
        this.context = context.copy();
        this.observer = observer;
    }

    @SuppressWarnings("unchecked")
    public <T> T getComponent(final Class<T> api) {
        return container.observe(observer, new SimpleContainer.Observed<T>() {
            public T run(final DependencyGraph.Traversal traversal) {
                final DependencyGraph.Node node = container.resolveComponent(api, context, traversal);
                return node == null ? null : (T) node.instance(traversal);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public <T> T[] getComponentGroup(final Class<T> api) {
        return container.observe(observer, new SimpleContainer.Observed<T[]>() {
            public T[] run(final DependencyGraph.Traversal traversal) {
                final DependencyGraph.Node node = container.resolveGroup(api, context, traversal);
                return node == null ? null : (T[]) node.instance(traversal);
            }
        });
    }

    public void resolveComponent(final Class<?> api) {
        container.observe(observer, new SimpleContainer.Observed<Void>() {
            public Void run(final DependencyGraph.Traversal traversal) {
                container.resolveComponent(api, context, traversal);
                return null;
            }
        });
    }

    public void resolveGroup(final Class<?> api) {
        container.observe(observer, new SimpleContainer.Observed<Void>() {
            public Void run(final DependencyGraph.Traversal traversal) {
                container.resolveGroup(api, context, traversal);
                return null;
            }
        });
    }

    @SuppressWarnings("unchecked")
    public <T> T initialize(final T component) {
        return (T) container.initialize(component, context.copy(), null);
    }

    public Object invoke(final Object component, final boolean explicit, final Method method, final Object... arguments)
            throws ResolutionException, InvocationTargetException {
        return container.invoke(component, method, context.copy(), arguments, explicit);
    }

    public OpenComponentContainer makeChildContainer() {
        return new ComponentContainerShell(container, context, true, false, observer);
    }

    public OpenComponentContainer makeDomainContainer() {
        return new ComponentContainerShell(container, context, true, true, observer);
    }

    public void bindComponent(final Components.Interfaces interfaces) throws BindingException {
        container.bindComponent(interfaces);
    }

    public void bindInstance(final Object instance, final Components.Interfaces interfaces) throws BindingException {
        container.bindInstance(instance, interfaces);
    }

    public OpenComponentContainer makeChildContainer(final Components.Interfaces interfaces) throws BindingException {
        return new ComponentContainerShell(container.linkComponent(interfaces), context, false, false, observer);
    }

    public ObservedComponentContainer observed(final ComponentResolutionObserver observer) {
        return observer == null ? this : new ComponentContainerShell(container, context, false, false, CompositeObserver.combine(this.observer, observer));
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

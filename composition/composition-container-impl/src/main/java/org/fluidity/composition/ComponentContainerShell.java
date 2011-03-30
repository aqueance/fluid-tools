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

import org.fluidity.composition.spi.ComponentResolutionObserver;
import org.fluidity.composition.spi.EmptyComponentContainer;

/**
 * This is a shell around a {@link SimpleContainer} object.
 *
 * @author Tibor Varga
 */
final class ComponentContainerShell extends EmptyComponentContainer {

    private final SimpleContainer container;
    private final ContextDefinition context;
    private final ComponentResolutionObserver observer;

    public ComponentContainerShell(final ContainerServices services) {
        this.container = new SimpleContainerImpl(services);
        this.context = services.emptyContext();
        this.observer = null;
    }

    public ComponentContainerShell(final SimpleContainer container, final ContextDefinition context, final boolean child) {
        this(container, context, child, null);
    }

    public ComponentContainerShell(final SimpleContainer container,
                                   final ContextDefinition context,
                                   final boolean child,
                                   final ComponentResolutionObserver observer) {
        assert container != null;
        assert context != null;
        this.container = child ? container.newChildContainer() : container;
        this.context = context;
        this.observer = observer;
    }

    @SuppressWarnings("unchecked")
    public <T> T getComponent(final Class<T> api) {
        return container.observe(observer, new SimpleContainer.Observed<T>() {
            public T run(final DependencyGraph.Traversal traversal) {
                final DependencyGraph.Node node = container.resolveComponent(api, context, traversal);
                return node == null ? null : (T) node.instance();
            }
        });
    }

    @SuppressWarnings("unchecked")
    public <T> T[] getComponentGroup(final Class<T> api) {
        return container.observe(observer, new SimpleContainer.Observed<T[]>() {
            public T[] run(final DependencyGraph.Traversal traversal) {
                final DependencyGraph.Node node = container.resolveGroup(api, context, traversal);
                return node == null ? null : (T[]) node.instance();
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
        return (T) container.initialize(component, context, null);
    }

    public OpenComponentContainer makeChildContainer() {
        return new ComponentContainerShell(container, context, true);
    }

    public void bindComponent(final Components.Interfaces interfaces) throws ComponentContainer.BindingException {
        container.bindComponent(interfaces);
    }

    public void bindInstance(final Object instance, final Components.Interfaces interfaces) throws ComponentContainer.BindingException {
        container.bindInstance(instance, interfaces);
    }

    public OpenComponentContainer makeChildContainer(final Components.Interfaces interfaces)
            throws ComponentContainer.BindingException {
        return new ComponentContainerShell(container.linkComponent(interfaces), context, false);
    }

    public ObservedComponentContainer observed(final ComponentResolutionObserver observer) {
        return observer == null ? this : new ComponentContainerShell(container, context, false,
                                                                     this.observer == null ? observer : new CompositeObserver(this.observer, observer));
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

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

import org.fluidity.composition.network.ContextDefinition;
import org.fluidity.composition.network.Graph;
import org.fluidity.composition.spi.EmptyComponentContainer;

/**
 * This is a shell around a {@link SimpleContainer} object.
 *
 * @author Tibor Varga
 */
final class ComponentContainerShell extends EmptyComponentContainer {

    private final SimpleContainer container;
    private final ContextDefinition context;

    public ComponentContainerShell(final ContainerServices services) {
        this.container = new SimpleContainerImpl(services);
        this.context = services.emptyContext();
    }

    public ComponentContainerShell(final SimpleContainer container, final ContextDefinition context, final boolean child) {
        assert container != null;
        assert context != null;
        this.container = child ? container.newChildContainer() : container;
        this.context = context;
    }

    @SuppressWarnings("unchecked")
    public <T> T getComponent(final Class<T> api) {
        final Graph.Node node = container.resolveComponent(api, context, container.services().graphTraversal());
        assert node != null : api;
        return node == null ? null : (T) node.instance(null);
    }

    @SuppressWarnings("unchecked")
    public <T> T[] getComponentGroup(final Class<T> api) {
        final Graph.Node node = container.resolveGroup(api, context, container.services().graphTraversal());
        assert node != null : api;
        return node == null ? null : (T[]) node.instance(null);
    }

    @SuppressWarnings("unchecked")
    public <T> T initialize(final T component) {
        return (T) container.initialize(component, context, null);
    }

    public OpenComponentContainer makeChildContainer() {
        return new ComponentContainerShell(container, context, true);
    }

    public void bindComponent(final Class<?> implementation, final Class<?>[] interfaces, final Class<?>[] groups)
            throws ComponentContainer.BindingException {
        container.bindComponent(implementation, interfaces, groups);
    }

    public void bindInstance(final Object instance, final Class<?>[] interfaces, final Class<?>[] groups)
            throws ComponentContainer.BindingException {
        container.bindInstance(instance, interfaces, groups);
    }

    public OpenComponentContainer makeChildContainer(final Class<?> implementation, final Class<?>[] interfaces, final Class<?>[] groups)
            throws ComponentContainer.BindingException {
        return new ComponentContainerShell(container.linkComponent(implementation, interfaces, groups), context, false);
    }

    @SuppressWarnings("unchecked")
    public <T> T resolveComponent(final Class<T> api, final Graph.Traversal.Strategy strategy, final Graph.Traversal.Observer observer) {
        final Graph.Node node = container.resolveComponent(api, context, container.services().graphTraversal(strategy));
        assert node != null : api;
        return node == null ? null : (T) node.instance(observer);
    }

    @SuppressWarnings("unchecked")
    public <T> T[] resolveGroup(final Class<T> api, final Graph.Traversal.Strategy strategy, final Graph.Traversal.Observer observer) {
        final Graph.Node node = container.resolveGroup(api, context, container.services().graphTraversal(strategy));
        assert node != null : api;
        return node == null ? null : (T[]) node.instance(observer);
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

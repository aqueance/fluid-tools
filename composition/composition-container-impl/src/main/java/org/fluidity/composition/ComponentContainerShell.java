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
import org.fluidity.composition.spi.DependencyPath;
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
        final DependencyGraph.Node node = container.resolveComponent(api, context, container.services().graphTraversal(observer));
        return node == null ? null : (T) node.instance();
    }

    @SuppressWarnings("unchecked")
    public <T> T[] getComponentGroup(final Class<T> api) {
        final DependencyGraph.Node node = container.resolveGroup(api, context, container.services().graphTraversal(observer));
        return node == null ? null : (T[]) node.instance();
    }

    public void resolveComponent(final Class<?> api) {
        container.resolveComponent(api, context, container.services().graphTraversal(observer));
    }

    public void resolveGroup(final Class<?> api) {
        container.resolveGroup(api, context, container.services().graphTraversal(observer));
    }

    @SuppressWarnings("unchecked")
    public <T> T initialize(final T component) {
        return (T) container.initialize(component, context, null);
    }

    public OpenComponentContainer makeChildContainer() {
        return new ComponentContainerShell(container, context, true, observer);
    }

    public void bindComponent(final Class<?> implementation, final Class<?>[] interfaces, final Class<?>[] groups) throws ComponentContainer.BindingException {
        container.bindComponent(implementation, interfaces, groups);
    }

    public void bindInstance(final Object instance, final Class<?>[] interfaces, final Class<?>[] groups) throws ComponentContainer.BindingException {
        container.bindInstance(instance, interfaces, groups);
    }

    public OpenComponentContainer makeChildContainer(final Class<?> implementation, final Class<?>[] interfaces, final Class<?>[] groups)
            throws ComponentContainer.BindingException {
        return new ComponentContainerShell(container.linkComponent(implementation, interfaces, groups), context, false, observer);
    }

    private ComponentResolutionObserver composite(final ComponentResolutionObserver observer) {
        return this.observer == null ? observer : new ComponentResolutionObserver() {
            public void resolved(final DependencyPath path, final Class<?> type) {
                ComponentContainerShell.this.observer.resolved(path, type);
                observer.resolved(path, type);
            }
        };
    }

    public ObservedComponentContainer observed(final ComponentResolutionObserver observer) {
        return observer == null ? this : new ComponentContainerShell(container, context, false, composite(observer));
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

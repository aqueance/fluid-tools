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
    private final Graph.Traversal.Strategy strategy;
    private final Graph.Traversal.Observer observer;

    public ComponentContainerShell(final ContainerServices services) {
        this.container = new SimpleContainerImpl(services);
        this.context = services.emptyContext();
        this.strategy = null;
        this.observer = null;
    }

    public ComponentContainerShell(final SimpleContainer container, final ContextDefinition context, final boolean child) {
        this(container, context, child, null, null);
    }

    public ComponentContainerShell(final SimpleContainer container,
                                   final ContextDefinition context,
                                   final boolean child,
                                   final Graph.Traversal.Strategy strategy,
                                   final Graph.Traversal.Observer observer) {
        assert container != null;
        assert context != null;
        this.container = child ? container.newChildContainer() : container;
        this.context = context;
        this.strategy = strategy;
        this.observer = observer;
    }

    @SuppressWarnings("unchecked")
    public <T> T getComponent(final Class<T> api) {
        final Graph.Node node = container.resolveComponent(api, context, container.services().graphTraversal(strategy, observer));
        return node == null ? null : (T) node.instance();
    }

    @SuppressWarnings("unchecked")
    public <T> T[] getComponentGroup(final Class<T> api) {
        final Graph.Node node = container.resolveGroup(api, context, container.services().graphTraversal(strategy, observer));
        assert node != null : api;
        return (T[]) node.instance();
    }

    @SuppressWarnings("unchecked")
    public <T> T initialize(final T component) {
        return (T) container.initialize(component, context, null);
    }

    public OpenComponentContainer makeChildContainer() {
        return new ComponentContainerShell(container, context, true, strategy, observer);
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
        return new ComponentContainerShell(container.linkComponent(implementation, interfaces, groups), context, false, strategy, observer);
    }

    private Graph.Traversal.Strategy composite(final Graph.Traversal.Strategy strategy) {
        return this.strategy == null ? strategy : new Graph.Traversal.Strategy() {
            public Graph.Node resolve(final boolean circular, final Graph graph, final Graph.Traversal traversal, final Graph.Traversal.Trail trail) {
                final Graph.Node resolved = strategy.resolve(circular, graph, traversal, trail);
                return resolved == null ? ComponentContainerShell.this.strategy.resolve(circular, graph, traversal, trail) : resolved;
            }
        };
    }

    private Graph.Traversal.Observer composite(final Graph.Traversal.Observer observer) {
        return this.observer == null ? observer : new Graph.Traversal.Observer() {
            public void resolved(final Graph.Path path, final Class<?> type) {
                ComponentContainerShell.this.observer.resolved(path, type);
                observer.resolved(path, type);
            }
        };
    }

    public ComponentContainer observed(final Graph.Traversal.Strategy strategy, final Graph.Traversal.Observer observer) {
        return new ComponentContainerShell(container, context, false, composite(strategy), composite(observer));
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

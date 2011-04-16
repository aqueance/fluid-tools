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

import org.fluidity.composition.spi.ComponentResolutionObserver;
import org.fluidity.composition.spi.EmptyComponentContainer;
import org.fluidity.composition.spi.PlatformContainer;

/**
 * This is a shell around a {@link SimpleContainer} object.
 *
 * @author Tibor Varga
 */
final class ComponentContainerShell extends EmptyComponentContainer {

    private final SimpleContainer container;
    private final ContextDefinition innerContext;
    private final ContextDefinition outerContext;
    private final ComponentResolutionObserver observer;

    public ComponentContainerShell(final ContainerServices services, final PlatformContainer platform) {
        this(new SimpleContainerImpl(services, platform), services.emptyContext(), services.emptyContext(), false, null);
    }

    public ComponentContainerShell(final SimpleContainer container, final ContextDefinition context, final boolean child) {
        this(container, context, child, null);
    }

    public ComponentContainerShell(final SimpleContainer container,
                                   final ContextDefinition context,
                                   final boolean child,
                                   final ComponentResolutionObserver observer) {
      this(container, context, context.copy(), child, observer);
    }

    public ComponentContainerShell(final SimpleContainer container,
                                   final ContextDefinition outerContext,
                                   final ContextDefinition innerContext,
                                   final boolean child,
                                   final ComponentResolutionObserver observer) {
        assert container != null;
        assert innerContext != null;
        assert outerContext != null;
        this.container = child ? container.newChildContainer() : container;
        this.outerContext = outerContext;
        this.innerContext = innerContext;
        this.observer = observer;
    }

    private static interface Command<T> {
        T run(ContextDefinition context);
    }

    private <T> T collect(final Command<T> command) {
      final ContextDefinition context = innerContext.copy();
      try {
        return command.run(context);
      } finally {
        outerContext.collect(context);
      }
    }

    @SuppressWarnings("unchecked")
    public <T> T getComponent(final Class<T> api) {
        return collect(new Command<T>() {
            public T run(final ContextDefinition context) {
                return container.observe(observer, new SimpleContainer.Observed<T>() {
                    public T run(final DependencyGraph.Traversal traversal) {
                        final DependencyGraph.Node node = container.resolveComponent(api, context, traversal);
                        return node == null ? null : (T) node.instance(traversal);
                    }
                });
            }
        });
    }

    @SuppressWarnings("unchecked")
    public <T> T[] getComponentGroup(final Class<T> api) {
        return collect(new Command<T[]>() {
            public T[] run(final ContextDefinition context) {
                return container.observe(observer, new SimpleContainer.Observed<T[]>() {
                    public T[] run(final DependencyGraph.Traversal traversal) {
                        final DependencyGraph.Node node = container.resolveGroup(api, context, traversal);
                        return node == null ? null : (T[]) node.instance(traversal);
                   }
                });
            }
        });
    }

    public void resolveComponent(final Class<?> api) {
        collect(new Command<Void>() {
            public Void run(final ContextDefinition context) {
                return container.observe(observer, new SimpleContainer.Observed<Void>() {
                    public Void run(final DependencyGraph.Traversal traversal) {
                        container.resolveComponent(api, innerContext, traversal);
                        return null;
                    }
                });
            }
        });
    }

    public void resolveGroup(final Class<?> api) {
        collect(new Command<Void>() {
          public Void run(final ContextDefinition context) {
            return container.observe(observer, new SimpleContainer.Observed<Void>() {
              public Void run(final DependencyGraph.Traversal traversal) {
                container.resolveGroup(api, innerContext, traversal);
                return null;
              }
            });
          }
        });
    }

    @SuppressWarnings("unchecked")
    public <T> T initialize(final T component) {
        return (T) container.initialize(component, innerContext.copy(), null);
    }

    public OpenComponentContainer makeChildContainer() {
        return new ComponentContainerShell(container, outerContext, innerContext, true, observer);
    }

    public void bindComponent(final Components.Interfaces interfaces) throws ComponentContainer.BindingException {
        container.bindComponent(interfaces);
    }

    public void bindInstance(final Object instance, final Components.Interfaces interfaces) throws ComponentContainer.BindingException {
        container.bindInstance(instance, interfaces);
    }

    public OpenComponentContainer makeChildContainer(final Components.Interfaces interfaces)
            throws ComponentContainer.BindingException {
        return new ComponentContainerShell(container.linkComponent(interfaces), outerContext, innerContext, false, observer);
    }

    public ObservedComponentContainer observed(final ComponentResolutionObserver observer) {
        return observer == null ? this : new ComponentContainerShell(container, outerContext, innerContext, false, CompositeObserver.combine(this.observer, observer));
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

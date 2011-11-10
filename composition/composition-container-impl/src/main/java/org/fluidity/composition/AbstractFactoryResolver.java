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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Encapsulates common functionality among various factory resolvers.
 */
abstract class AbstractFactoryResolver extends AbstractResolver {

    private final Class<? extends ComponentFactory> factoryClass;

    public AbstractFactoryResolver(final Class<? extends ComponentFactory> factoryClass,
                                   final int priority,
                                   final Class<?> api,
                                   final ComponentCache cache,
                                   final LogFactory logs) {
        super(priority, api, cache, logs);
        this.factoryClass = factoryClass;
    }

    protected final Class<? extends ComponentFactory> factoryClass() {
        return factoryClass;
    }

    /**
     * Returns the {@link ComponentFactory} instance this is a mapping for.
     *
     * @param container  the container in which to resolve dependencies of the factory.
     * @param traversal  the current graph traversal.
     * @param definition the context in which the resolution takes place.
     *
     * @return the {@link ComponentFactory} instance this is a mapping for.
     */
    protected abstract ComponentFactory factory(final SimpleContainer container, final DependencyGraph.Traversal traversal, final ContextDefinition definition);

    /**
     * Invokes the factory and performs proper context housekeeping.
     *
     * @param domain    the domain container.
     * @param traversal the current dependency traversal.
     * @param container the original container.
     * @param context   the current component context.
     * @param child     the child of the original container to pass to the factory.
     *
     * @return the graph node for the component.
     */
    protected final DependencyGraph.Node resolve(final ParentContainer domain,
                                                 final DependencyGraph.Traversal traversal,
                                                 final SimpleContainer container,
                                                 final ContextDefinition context,
                                                 final SimpleContainer child) {
        final ContextDefinition reduced = context.copy().accept(acceptedContext());
        final ContextDefinition collected = context.copy().accept(null);
        final ComponentContext passed = reduced.create();

        final ComponentFactory factory = factory(container, traversal, context);

        final List<ContextDefinition> list = new ArrayList<ContextDefinition>();
        list.add(reduced);

        final DependencyInjector injector = container.services().dependencyInjector();

        final List<RestrictedContainer> containers = new ArrayList<RestrictedContainer>();
        final ComponentFactory.Instance instance = factory.resolve(passed, new ComponentFactory.Resolver() {
            public <T> ComponentFactory.Dependency<T> resolve(final Class<T> api) {
                return new NodeDependency<T>(resolve(api, null), traversal);
            }

            public DependencyGraph.Node resolve(final Class<?> api, final Annotation[] annotations) {
                final Class<?> resolving = AbstractFactoryResolver.this.api;

                return injector.resolve(api, new DependencyInjector.Resolution() {
                    public ComponentContext context() {
                        traversal.resolving(resolving, api, null, null);
                        return passed;
                    }

                    public ComponentContainer container() {
                        traversal.resolving(resolving, api, null, null);
                        return new ComponentContainerShell(child, reduced.accept(null), false);
                    }

                    public DependencyGraph.Node regular() {
                        traversal.resolving(resolving, api, null, annotations);
                        final ContextDefinition copy = collected.copy();
                        list.add(copy);
                        return child.resolveComponent(api, annotations == null ? copy : copy.expand(annotations), traversal);
                    }

                    public void handle(final RestrictedContainer container) {
                        containers.add(container);
                    }
                });
            }

            public ComponentFactory.Dependency<?>[] discover(final Class<?> type) {
                return discover(injector.findConstructor(type));
            }

            public ComponentFactory.Dependency<?>[] discover(final Constructor<?> constructor) {
                return discover(constructor.getParameterTypes(), constructor.getParameterAnnotations());
            }

            public ComponentFactory.Dependency<?>[] discover(final Method method) {
                return discover(method.getParameterTypes(), method.getParameterAnnotations());
            }

            private ComponentFactory.Dependency<?>[] discover(final Class<?>[] types, final Annotation[][] annotations) {
                final List<ComponentFactory.Dependency<?>> nodes = new ArrayList<ComponentFactory.Dependency<?>>();
                for (int i = 0, limit = types.length; i < limit; i++) {
                    nodes.add(new NodeDependency<Object>(resolve(types[i], annotations[i]), traversal));
                }

                return nodes.toArray(new ComponentFactory.Dependency<?>[nodes.size()]);
            }
        });

        final ContextDefinition saved = context.collect(list).copy();
        final ComponentContext actual = saved.create();

        return cachingNode(domain, new DependencyGraph.Node() {
            public Class<?> type() {
                return api;
            }

            public Object instance(final DependencyGraph.Traversal traversal) {
                try {
                    instance.bind(new RegistryWrapper(api, child));
                    return child.resolveComponent(api, saved, traversal).instance(traversal);
                } finally {
                    for (final RestrictedContainer restricted : containers) {
                        restricted.enable();
                    }
                }
            }

            public ComponentContext context() {
                return actual;
            }
        }, child);
    }

    @SuppressWarnings("unchecked")
    private static class RegistryWrapper implements ComponentFactory.Registry {
        private final Class<?> api;
        private final SimpleContainer container;

        public RegistryWrapper(final Class<?> api, final SimpleContainer container) {
            this.api = api;
            this.container = container;
        }

        public <T> void bindComponent(final Class<T> implementation, final Class<? super T>... interfaces) throws OpenComponentContainer.BindingException {
            container.bindComponent(Components.inspect(implementation, interfaces));
        }

        public <T> void bindInstance(final T instance, final Class<? super T>... interfaces) throws OpenComponentContainer.BindingException {
            assert instance != null;
            container.bindInstance(instance, Components.inspect((Class<T>) instance.getClass(), interfaces));
        }

        public ComponentFactory.Registry makeChildContainer() {
            return new RegistryWrapper(api, container.newChildContainer(false));
        }
    }

    private static class NodeDependency<T> implements ComponentFactory.Dependency<T> {
        private final DependencyGraph.Node node;
        private final DependencyGraph.Traversal traversal;

        public NodeDependency(final DependencyGraph.Node node, final DependencyGraph.Traversal traversal) {
            this.node = node;
            this.traversal = traversal;
        }

        @SuppressWarnings("unchecked")
        public T instance() {
            return node == null ? null : (T) node.instance(traversal);
        }
    }
}

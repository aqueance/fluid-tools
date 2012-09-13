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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.Components;
import org.fluidity.composition.Inject;
import org.fluidity.composition.container.spi.DependencyGraph;
import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.foundation.Generics;
import org.fluidity.foundation.Lists;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Component resolver for a {@link ComponentFactory} component.
 *
 * @author Tibor Varga
 */
abstract class FactoryResolver extends AbstractResolver {

    /**
     * Returns the {@link ComponentFactory} instance this is a mapping for.
     *
     * @param container  the container in which to resolve dependencies of the factory.
     * @param traversal  the current graph traversal.
     * @param definition the context in which the resolution takes place.
     * @param reference  the parameterized type of the dependency reference.
     *
     * @return the {@link ComponentFactory} instance this is a mapping for.
     */
    protected abstract ComponentFactory factory(SimpleContainer container, DependencyGraph.Traversal traversal, ContextDefinition definition, Type reference);

    private final Class<? extends ComponentFactory> factoryClass;

    public FactoryResolver(final Class<? extends ComponentFactory> factoryClass,
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

    public final Annotation[] providedContext() {
        return null;
    }

    public final Class<?> contextConsumer() {
        return factoryClass();
    }

    public final DependencyGraph.Node resolve(final ParentContainer domain,
                                              final DependencyGraph.Traversal traversal,
                                              final SimpleContainer container,
                                              final ContextDefinition context,
                                              final Type reference) {
        final SimpleContainer child = container.newChildContainer(false);
        final List<RestrictedContainer> containers = new ArrayList<RestrictedContainer>();
        final List<ContextDefinition> contexts = new ArrayList<ContextDefinition>();

        final ComponentFactory factory = factory(container, traversal, context, reference);
        final Class<?> consumer = contextConsumer();

        final DependencyInjector injector = child.services().dependencyInjector();
        final ComponentFactory.Instance instance = resolve(injector, traversal, context, child, reference, containers, contexts, factory, consumer);

        final ContextDefinition saved = context.accept(consumer).collect(contexts).copy();
        final ComponentFactory.Registry registry = new RegistryWrapper(child, saved, traversal);

        return cachingNode(domain, container, new DependencyGraph.Node() {
            private final ComponentContext actual = saved.create();

            public Class<?> type() {
                return api;
            }

            public Object instance(final DependencyGraph.Traversal traversal) {
                try {
                    if (instance == null) {
                        return null;
                    } else {
                        instance.bind(registry);
                        return child.resolveComponent(api, saved, traversal, reference).instance(traversal);
                    }
                } finally {
                    for (final RestrictedContainer restricted : containers) {
                        restricted.enable();
                    }
                }
            }

            public ComponentContext context() {
                return actual;
            }
        });
    }

    private ComponentFactory.Instance resolve(final DependencyInjector injector,
                                              final DependencyGraph.Traversal traversal,
                                              final ContextDefinition context,
                                              final SimpleContainer child,
                                              final Type reference,
                                              final List<RestrictedContainer> containers,
                                              final List<ContextDefinition> contexts,
                                              final ComponentFactory factory,
                                              final Class<?> consumer) {
        final ContextDefinition collected = context.copy().accept(null);
        final ContextDefinition reduced = context.copy().accept(consumer);
        final ComponentContext passed = reduced.create();

        return factory.resolve(passed, new ComponentFactory.Resolver() {
            public <T> ComponentFactory.Dependency<T> resolve(final Class<T> api, final Class<?> type, final Field field) {
                return resolve(api, field.getGenericType(), Lists.concatenate(Annotation.class,
                                                                              field.getDeclaringClass().getAnnotations(),
                                                                              field.getAnnotations()));
            }

            public <T> ComponentFactory.Dependency<T> resolve(final Class<T> api, final Constructor<T> constructor, final int parameter) {
                final Generics.Parameters parameters = Generics.describe(constructor);

                return resolve(api,
                               parameters.genericType(parameter),
                               Lists.concatenate(Annotation.class,
                                                 constructor.getDeclaringClass().getAnnotations(),
                                                 constructor.getAnnotations(),
                                                 parameters.getAnnotations(parameter)));
            }

            public <T> ComponentFactory.Dependency<T> resolve(final Class<T> api, final Class<?> type, final Method method, final int parameter) {
                return resolve(api,
                               method.getGenericParameterTypes()[parameter],
                               Lists.concatenate(Annotation.class,
                                                 method.getDeclaringClass().getAnnotations(),
                                                 method.getAnnotations(),
                                                 method.getParameterAnnotations()[parameter]));
            }

            public <T> ComponentFactory.Dependency<T> resolve(final Class<T> api, final Type reference, final Annotation[] annotations) {
                if (api == null && reference == null) {
                    throw new IllegalArgumentException("Both parameters are null");
                }

                if (api != null && reference != null && !Generics.rawType(reference).isAssignableFrom(api)) {
                    throw new ComponentContainer.ResolutionException("Type %s cannot be assigned to %s", api, reference);
                }

                final DependencyGraph.Node node = descend(api == null ? Generics.rawType(reference) : api, reference == null ? api : reference, annotations);
                return new NodeDependency<T>(node, traversal);
            }

            private DependencyGraph.Node descend(final Class<?> api, final Type reference, final Annotation[] annotations) {
                assert reference != null : api;
                final Class<?> resolving = FactoryResolver.this.api;

                return injector.resolve(api, new DependencyInjector.Resolution() {
                    public ComponentContext context() {
                        traversal.descend(resolving, api, null, null);
                        try {
                            return passed;
                        } finally {
                            traversal.ascend(resolving, api);
                        }
                    }

                    public ComponentContainer container() {
                        traversal.descend(resolving, api, null, null);
                        try {
                            return new ComponentContainerShell(child, reduced.accept(null), false);
                        } finally {
                            traversal.ascend(resolving, api);
                        }
                    }

                    public DependencyGraph.Node regular() {
                        traversal.descend(resolving, api, null, annotations);
                        try {
                            final ContextDefinition copy = collected.advance(reference, false).expand(annotations);

                            contexts.add(copy);

                            return isGroup(api, annotations)
                                   ? child.resolveGroup(api.getComponentType(), copy, traversal, api)
                                   : child.resolveComponent(api, copy, traversal, api);
                        } finally {
                            traversal.ascend(resolving, api);
                        }
                    }

                    public void handle(final RestrictedContainer container) {
                        containers.add(container);
                    }
                });
            }

            public ComponentFactory.Dependency<?>[] discover(final Class<?> type) {
                assert type != null;
                fields(type);
                return discover(constructor(type));
            }

            public void fields(final Class<?> type) {
                assert type != null;
                for (final Field field : type.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Inject.class)) {
                        descend(field.getType(), field.getGenericType(), field.getAnnotations());
                    }
                }

                final Class<?> superClass = type.getSuperclass();

                if (superClass != null && superClass != Object.class) {
                    fields(superClass);
                }
            }

            public Constructor<?> constructor(final Class<?> type) {
                assert type != null;
                return injector.findConstructor(type);
            }

            public ComponentFactory.Dependency<?>[] discover(final Constructor<?> constructor) {
                assert constructor != null;
                return discover(constructor.getGenericParameterTypes(), constructor.getParameterAnnotations());
            }

            public ComponentFactory.Dependency<?>[] discover(final Method method) {
                assert method != null;
                return discover(method.getGenericParameterTypes(), method.getParameterAnnotations());
            }

            private ComponentFactory.Dependency<?>[] discover(final Type[] types, final Annotation[][] annotations) {
                final List<ComponentFactory.Dependency<?>> nodes = new ArrayList<ComponentFactory.Dependency<?>>();

                for (int i = 0, limit = types.length; i < limit; i++) {
                    final Type type = types[i];
                    nodes.add(new NodeDependency<Object>(descend(Generics.rawType(type), Generics.propagate(reference, type), annotations[i]), traversal));
                }

                return nodes.toArray(new ComponentFactory.Dependency<?>[nodes.size()]);
            }

            public ComponentFactory.Container local(final ComponentFactory.Container.Bindings bindings) {
                final SimpleContainer container = child.newChildContainer(false);
                final ComponentContainerShell shell = new ComponentContainerShell(container, context, false, false, null);
                final ComponentContainer.Registry registry = shell.getRegistry();

                bindings.bindComponents(new ComponentFactory.Container.Registry() {
                    public <T> void bindComponent(final Class<T> implementation, final Class<? super T>... interfaces) throws ComponentContainer.BindingException {
                        registry.bindComponent(implementation, interfaces);
                    }
                });

                return new ComponentFactory.Container() {

                    public <T> ComponentFactory.Dependency<T> resolve(final Class<T> api, final Constructor<?> constructor, final int parameter) {
                        if (constructor == null) {
                            throw new ComponentContainer.BindingException("Provided constructor is null");
                        }

                        final Generics.Parameters parameters = Generics.describe(constructor);
                        final Annotation[] parameterAnnotations = parameters.getAnnotations(parameter);

                        return dependency(api,
                                          parameters.genericType(parameter),
                                          Lists.concatenate(Annotation.class,
                                                            constructor.getDeclaringClass().getAnnotations(),
                                                            constructor.getAnnotations(),
                                                            parameterAnnotations),
                                          parameterAnnotations);
                    }

                    public <T> ComponentFactory.Dependency<T> resolve(final Class<T> api, final Class<?> type, final Method method, final int parameter) {
                        if (method == null) {
                            throw new ComponentContainer.BindingException("Provided method is null");
                        }

                        final Annotation[] parameterAnnotations = method.getParameterAnnotations()[parameter];

                        return dependency(api,
                                          method.getGenericParameterTypes()[parameter],
                                          Lists.concatenate(Annotation.class,
                                                            type == null ? method.getDeclaringClass().getAnnotations() : type.getAnnotations(),
                                                            method.getAnnotations(),
                                                            parameterAnnotations),
                                          parameterAnnotations);
                    }

                    public <T> ComponentFactory.Dependency<T> resolve(final Class<T> api, final Class<?> type, final Field field) {
                        if (field == null) {
                            throw new ComponentContainer.BindingException("Provided field is null");
                        }

                        return dependency(api,
                                          field.getGenericType(),
                                          Lists.concatenate(Annotation.class,
                                                            type == null ? field.getDeclaringClass().getAnnotations() : type.getAnnotations(),
                                                            field.getAnnotations()),
                                          field.getAnnotations());
                    }

                    private <T> ComponentFactory.Dependency<T> dependency(final Class<T> api,
                                                                          final Type reference,
                                                                          final Annotation[] annotations,
                                                                          final Annotation[] params) {
                        final ContextDefinition copy = context.copy().advance(reference, false).expand(annotations);

                        final DependencyGraph.Node node = isGroup(api, params)
                                                          ? container.resolveGroup(api.getComponentType(), copy, traversal, api)
                                                          : container.resolveComponent(api, copy, traversal, api);

                        return new ComponentFactory.Dependency<T>() {
                            @SuppressWarnings("unchecked")
                            public T instance() {
                                return node == null ? null : (T) node.instance(traversal);
                            }
                        };
                    }
                };
            }
        });
    }

    @Override
    public String toString() {
        return String.format("%s (via %s)", api.getName(), factoryClass().getName());
    }

    @SuppressWarnings("unchecked")
    private static class RegistryWrapper implements ComponentFactory.Registry {

        private final SimpleContainer container;
        private final ContextDefinition context;
        private final DependencyGraph.Traversal traversal;

        public RegistryWrapper(final SimpleContainer container, final ContextDefinition context, final DependencyGraph.Traversal traversal) {
            this.container = container;
            this.context = context;
            this.traversal = traversal;
        }

        public <T> void bindComponent(final Class<T> implementation, final Class<? super T>... interfaces) throws ComponentContainer.BindingException {
            container.bindComponent(Components.inspect(implementation, interfaces));
        }

        public <T> void bindInstance(final T instance, final Class<? super T>... interfaces) throws ComponentContainer.BindingException {
            container.bindInstance(instance, instance == null ? null : Components.inspect((Class<T>) instance.getClass(), interfaces));

            if (instance != null) {
                container.initialize(instance, context, traversal);
            }
        }
    }

    static boolean isGroup(final Class<?> api, final Annotation[] annotations) {
        if (api.isArray()) {
            for (final Annotation annotation : annotations) {
                if (annotation.annotationType() == ComponentGroup.class) {
                    return true;
                }
            }
        }

        return false;
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

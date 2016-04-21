/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.Components;
import org.fluidity.composition.Inject;
import org.fluidity.composition.container.spi.DependencyGraph;
import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.foundation.Generics;
import org.fluidity.foundation.Lists;
import org.fluidity.foundation.Security;
import org.fluidity.foundation.Strings;

/**
 * Component resolver for a {@link ComponentFactory} component.
 *
 * @author Tibor Varga
 */
@SuppressWarnings("WeakerAccess")
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

    protected FactoryResolver(final Class<? extends ComponentFactory> factoryClass, final int priority, final Class<?> api, final ComponentCache cache) {
        super(priority, api, cache);

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
                                              final ParentContainer container,
                                              final DependencyGraph.Traversal traversal,
                                              final ContextDefinition context,
                                              final Type reference) {
        final ParentContainer resolver = resolver(domain, container);

        final SimpleContainer nested = resolver.newChildContainer(false);
        final List<ContextDefinition> contexts = new ArrayList<>();

        final ComponentFactory factory = factory(resolver, traversal, context, reference);
        final Class<?> consumer = contextConsumer();

        final DependencyInjector injector = nested.services().dependencyInjector();

        final AccessGuard<ComponentContainer> containers = injector.containerGuard();
        final AccessGuard<DependencyGraph.Node> instantiation = new AccessGuard<>("Dependencies must be instantiated in ComponentFactory.Instance.bind(...).");

        final ComponentFactory.Instance instance = resolve(injector, traversal, context, nested, reference, contexts, factory, consumer, instantiation, containers);

        final ContextDefinition saved = context.accept(consumer).collect(contexts).copy();
        final RegistryWrapper registry = new RegistryWrapper(nested, saved, traversal);

        return cachingNode(resolver, new DependencyGraph.Node() {
            private final ComponentContext actual = saved.create();

            public Class<?> type() {
                return api;
            }

            public Object instance(final DependencyGraph.Traversal traversal) {
                instantiation.enable();

                try {
                    if (instance == null) {
                        return null;
                    } else {
                        try {
                            instance.bind(registry);

                            if (!registry.contains(api)) {
                                throw new ComponentContainer.BindingException("Factory %s did not bind component for requested interface %s",
                                                                              Strings.formatClass(false, true, factoryClass),
                                                                              Strings.formatClass(false, true, api));
                            }
                        } catch (final ComponentContainer.InjectionException e) {
                            throw e;
                        } catch (final Exception e) {
                            throw new ComponentContainer.BindingException(e, "Component factory error (%s)", factoryClass);
                        }

                        return nested.resolveComponent(api, saved, traversal, reference).instance(traversal);
                    }
                } finally {
                    containers.enable();
                }
            }

            public ComponentContext context() {
                return actual;
            }
        });
    }

    /**
     * Internal interface to specialize {@link DependencyInjector.Resolution}.
     *
     * @author Tibor Varga
     */
    private interface Resolution {

        /**
         * See {@link DependencyInjector.Resolution#context()}.
         */
        ComponentContext context(Class<?> api);

        /**
         * See {@link DependencyInjector.Resolution#container()}.
         */
        ComponentContainer container(Class<?> api);

        /**
         * See {@link DependencyInjector.Resolution#regular()}.
         */
        DependencyGraph.Node regular(Class<?> api, Type reference, final Annotation[] annotations, Annotation[] params);
    }

    private ComponentFactory.Instance resolve(final DependencyInjector injector,
                                              final DependencyGraph.Traversal traversal,
                                              final ContextDefinition context,
                                              final SimpleContainer nested,
                                              final Type reference,
                                              final List<ContextDefinition> contexts,
                                              final ComponentFactory factory,
                                              final Class<?> consumer,
                                              final AccessGuard<DependencyGraph.Node> instances,
                                              final AccessGuard<ComponentContainer> containers) {
        final ContextDefinition collected = context.copy().accept(null);
        final ContextDefinition reduced = context.copy().accept(consumer);
        final ComponentContext passed = reduced.create();

        final Resolution resolution = new Resolution() {
            public ComponentContext context(final Class<?> type) {
                traversal.descend(api, type, null, null);

                try {
                    return passed;
                } finally {
                    traversal.ascend(api, type);
                }
            }

            public ComponentContainer container(final Class<?> type) {
                traversal.descend(api, type, null, null);

                try {
                    return new ComponentContainerShell(nested, reduced.accept(null), false);
                } finally {
                    traversal.ascend(api, type);
                }
            }

            public DependencyGraph.Node regular(final Class<?> type, final Type reference, final Annotation[] annotations, final Annotation[] params) {
                traversal.descend(api, type, null, annotations);

                try {
                    final ContextDefinition copy = collected.advance(reference, false).expand(annotations);

                    contexts.add(copy);

                    return isGroup(type, params)
                           ? nested.resolveGroup(type.getComponentType(), copy, traversal, type)
                           : nested.resolveComponent(type, copy, traversal, type);
                } finally {
                    traversal.ascend(api, type);
                }
            }
        };

        final ComponentFactory.Container container = new ContainerImpl(instances, containers, traversal, injector, resolution);

        final ComponentFactory.Resolver resolver = new ComponentFactory.Resolver() {
            public <T> ComponentFactory.Dependency<T> resolve(final Class<T> api, final Class<?> type, final Field field) {
                return container.resolve(api, type, field);
            }

            public <T> ComponentFactory.Dependency<T> resolve(final Class<T> api, final Constructor<?> constructor, final int parameter) {
                return container.resolve(api, constructor, parameter);
            }

            public <T> ComponentFactory.Dependency<T> resolve(final Class<T> api, final Class<?> type, final Method method, final int parameter) {
                return container.resolve(api, type, method, parameter);
            }

            public <T> ComponentFactory.Dependency<T> resolve(final Class<T> api, final Type reference, final Annotation[] annotations) {
                if (api == null && reference == null) {
                    throw new IllegalArgumentException("Both the api and the reference parameters are null");
                }

                if (api != null && reference != null && !Generics.rawType(reference).isAssignableFrom(api)) {
                    throw new ComponentContainer.ResolutionException("Type %s cannot be assigned to %s", api, reference);
                }

                return new NodeDependency<>(traversal, descend(api == null ? Generics.rawType(reference) : api, reference == null ? api : reference, annotations));
            }

            public ComponentFactory.Dependency<?>[] resolve(final Class<?> type, final Method method) {
                return container.resolve(type, method);
            }

            public ComponentFactory.Dependency<?>[] resolve(final Constructor<?> constructor) {
                return container.resolve(constructor);
            }

            private DependencyGraph.Node descend(final Class<?> api, final Type reference, final Annotation[] annotations) {
                assert reference != null : api;

                return injector.resolve(api, containers, new DependencyInjector.Resolution() {
                    public ComponentContext context() {
                        return resolution.context(api);
                    }

                    public ComponentContainer container() {
                        return resolution.container(api);
                    }

                    public DependencyGraph.Node regular() {
                        return resolution.regular(api, reference, annotations, annotations);
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
                final Field[] fields = !Security.CONTROLLED ? type.getDeclaredFields() : AccessController.doPrivileged((PrivilegedAction<Field[]>) type::getDeclaredFields);

                for (final Field field : fields) {
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
                final List<ComponentFactory.Dependency<?>> nodes = new ArrayList<>();

                for (int i = 0, limit = types.length; i < limit; i++) {
                    final Type type = types[i];
                    nodes.add(new NodeDependency<>(traversal, descend(Generics.rawType(type), Generics.propagate(reference, type), annotations[i])));
                }

                return Lists.asArray(ComponentFactory.Dependency.class, nodes);
            }

            public ComponentFactory.Container local(final Class<?> type, final Bindings bindings) {
                final ContextDefinition reduced = context.copy().accept(type);
                final ComponentContext passed = reduced.create();
                final SimpleContainer container = nested.newChildContainer(false);

                bindings.bindComponents(new Registry() {
                    public <T> void bindComponent(final Class<T> implementation, final Class<? super T>... interfaces) throws ComponentContainer.BindingException {
                        container.bindComponent(Components.inspect(implementation, interfaces));
                    }
                });

                return new ContainerImpl(instances, containers, traversal, injector, new Resolution() {
                    public ComponentContext context(final Class<?> api) {
                        return passed;
                    }

                    public ComponentContainer container(final Class<?> api) {
                        return new ComponentContainerShell(container, reduced, false);
                    }

                    public DependencyGraph.Node regular(final Class<?> api,
                                                        final Type reference,
                                                        final Annotation[] annotations,
                                                        final Annotation[] params) {
                        final ContextDefinition copy = context.copy().advance(reference, false).expand(annotations);

                        return isGroup(api, params)
                               ? container.resolveGroup(api.getComponentType(), copy, traversal, api)
                               : container.resolveComponent(api, copy, traversal, api);
                    }
                });
            }

            public <T> ComponentFactory.Dependency<T> constant(final T object) {
                return () -> object;
            }

            public Object[] instantiate(final ComponentFactory.Dependency<?>... dependencies) {
                final Object[] instances = new Object[dependencies.length];

                for (int i = 0, limit = dependencies.length; i < limit; i++) {
                    instances[i] = dependencies[i].instance();
                }

                return instances;
            }
        };

        try {
            return factory.resolve(passed, resolver);
        } catch (final ComponentContainer.InjectionException e) {
            throw e;
        } catch (final Exception e) {
            throw new ComponentContainer.ResolutionException(e, "Component factory error (%s)", factoryClass);
        }
    }

    @Override
    public String toString() {
        return String.format("%s (via %s)", api.getName(), factoryClass().getName());
    }

    @SuppressWarnings("unchecked")
    private static final class RegistryWrapper implements ComponentFactory.Registry {

        private final SimpleContainer container;
        private final ContextDefinition context;
        private final DependencyGraph.Traversal traversal;

        private final Set<Class> components = new HashSet<>();

        RegistryWrapper(final SimpleContainer container, final ContextDefinition context, final DependencyGraph.Traversal traversal) {
            this.container = container;
            this.context = context;
            this.traversal = traversal;
        }

        public <T> void bindComponent(final Class<T> implementation, final Class<? super T>... interfaces) throws ComponentContainer.BindingException {
            container.bindComponent(bound(implementation, interfaces));
        }

        public <T> void bindInstance(final T instance, final Class<? super T>... interfaces) throws ComponentContainer.BindingException {
            if (instance != null) {
                container.bindInstance(instance, bound((Class<T>) instance.getClass(), interfaces));
                container.initialize(instance, context, traversal);
            }
        }

        private <T> Components.Interfaces bound(final Class<T> implementation, final Class<? super T>[] interfaces) {
            final Components.Interfaces specifications = Components.inspect(implementation, interfaces);

            for (final Components.Specification specification : specifications.api) {
                components.add(specification.api);
            }

            return specifications;
        }

        boolean contains(final Class<?> api) {
            return components.contains(api);
        }
    }

    private static boolean isGroup(final Class<?> api, final Annotation[] annotations) {
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

        NodeDependency(final DependencyGraph.Traversal traversal, final DependencyGraph.Node node) {
            this.node = node;
            this.traversal = traversal;
        }

        @SuppressWarnings("unchecked")
        public T instance() {
            return node == null ? null : (T) node.instance(traversal);
        }
    }

    /**
     * @author Tibor Varga
     */
    private static class ContainerImpl implements ComponentFactory.Container {

        private final AccessGuard<DependencyGraph.Node> instances;
        private final AccessGuard<ComponentContainer> containers;
        private final DependencyGraph.Traversal traversal;
        private final DependencyInjector injector;
        private final Resolution resolution;

        ContainerImpl(final AccessGuard<DependencyGraph.Node> instances,
                      final AccessGuard<ComponentContainer> containers,
                      final DependencyGraph.Traversal traversal,
                      final DependencyInjector injector,
                      final Resolution resolution) {
            this.instances = instances;
            this.containers = containers;
            this.traversal = traversal;
            this.injector = injector;
            this.resolution = resolution;
        }

        public final <T> ComponentFactory.Dependency<T> resolve(final Class<T> api, final Constructor<?> constructor, final int parameter) {
            if (constructor == null) {
                throw new ComponentContainer.BindingException("Provided constructor is null");
            }

            final Generics.Parameters parameters = Generics.describe(constructor);
            final Annotation[] parameterAnnotations = parameters.annotations(parameter);

            return dependency(api,
                              parameters.genericType(parameter),
                              Lists.concatenate(Annotation.class,
                                                constructor.getDeclaringClass().getAnnotations(),
                                                constructor.getAnnotations(),
                                                parameterAnnotations),
                              parameterAnnotations,
                              instances,
                              containers);
        }

        public final <T> ComponentFactory.Dependency<T> resolve(final Class<T> api, final Class<?> type, final Method method, final int parameter) {
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
                              parameterAnnotations,
                              instances,
                              containers);
        }

        public final <T> ComponentFactory.Dependency<T> resolve(final Class<T> api, final Class<?> type, final Field field) {
            if (field == null) {
                throw new ComponentContainer.BindingException("Provided field is null");
            }

            return dependency(api,
                              field.getGenericType(),
                              Lists.concatenate(Annotation.class,
                                                type == null ? field.getDeclaringClass().getAnnotations() : type.getAnnotations(),
                                                field.getAnnotations()),
                              field.getAnnotations(),
                              instances,
                              containers);
        }

        public final ComponentFactory.Dependency<?>[] resolve(final Class<?> type, final Method method) {
            final Class<?>[] types = method.getParameterTypes();
            final ComponentFactory.Dependency<?>[] dependencies = new ComponentFactory.Dependency<?>[types.length];

            for (int i = 0, limit = types.length; i < limit; i++) {
                dependencies[i] = resolve(types[i], type, method, i);
            }

            return dependencies;
        }

        public final ComponentFactory.Dependency<?>[] resolve(final Constructor<?> constructor) {
            final Class<?>[] types = constructor.getParameterTypes();
            final ComponentFactory.Dependency<?>[] dependencies = new ComponentFactory.Dependency<?>[types.length];

            for (int i = 0, limit = types.length; i < limit; i++) {
                dependencies[i] = resolve(types[i], constructor, i);
            }

            return dependencies;
        }

        @SuppressWarnings("unchecked")
        protected final <T> ComponentFactory.Dependency<T> dependency(final Class<T> api,
                                                                      final Type reference,
                                                                      final Annotation[] annotations,
                                                                      final Annotation[] params,
                                                                      final AccessGuard<DependencyGraph.Node> instances,
                                                                      final AccessGuard<ComponentContainer> containers) {
            final DependencyGraph.Node node = injector.resolve(api, containers, new DependencyInjector.Resolution() {
                public ComponentContext context() {
                    return resolution.context(api);
                }

                public ComponentContainer container() {
                    return resolution.container(api);
                }

                public DependencyGraph.Node regular() {
                    return resolution.regular(api, reference, annotations, params);
                }
            });

            return () -> instances.access(node) == null ? null : (T) node.instance(traversal);
        }
    }
}

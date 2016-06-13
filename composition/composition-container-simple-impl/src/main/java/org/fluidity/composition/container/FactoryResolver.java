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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.Components;
import org.fluidity.composition.DependencyPath;
import org.fluidity.composition.Inject;
import org.fluidity.composition.Optional;
import org.fluidity.composition.container.spi.DependencyGraph;
import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.composition.spi.Dependency;
import org.fluidity.foundation.Exceptions;
import org.fluidity.foundation.Generics;
import org.fluidity.foundation.Lists;
import org.fluidity.foundation.Strings;
import org.fluidity.foundation.security.Security;

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
        final AccessGuard<DependencyGraph.Node> instantiation = new AccessGuard<>("Dependencies cannot be instantiated outside ComponentFactory.Instance.bind(...).");

        final ComponentFactory.Instance instance = resolve(injector, traversal, context, nested, contexts, factory, consumer, instantiation, containers);

        final ContextDefinition saved = context.accept(consumer).collect(contexts).copy();
        final RegistryWrapper registry = new RegistryWrapper(nested, saved, traversal);

        return cachingNode(resolver, new DependencyGraph.Node() {
            private final ComponentContext actual = saved.create();

            public Class<?> type() {
                return instance == null ? api : instance.type();
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
        DependencyGraph.Node regular(Class<?> api, Type reference, Annotation[] annotations, Annotation[] params);
    }

    private ComponentFactory.Instance resolve(final DependencyInjector injector,
                                              final DependencyGraph.Traversal traversal,
                                              final ContextDefinition context,
                                              final SimpleContainer nested,
                                              final List<ContextDefinition> contexts,
                                              final ComponentFactory factory,
                                              final Class<?> consumer,
                                              final AccessGuard<DependencyGraph.Node> instances,
                                              final AccessGuard<ComponentContainer> containers) {
        final ContextDefinition reduced = context.copy().accept(consumer);
        final ComponentContext passed = reduced.create();

        final ComponentFactory.Container container = new ComponentFactory.Container() {

            @Override
            public ComponentFactory.Resolver resolver() {
                return Exceptions.wrap(() -> resolver(null, null));
            }

            @Override
            public ComponentFactory.Resolver resolver(final Class<?> type) {
                return Exceptions.wrap(() -> resolver(type, null));
            }

            @Override
            public ComponentFactory.Resolver resolver(ComponentFactory.Bindings bindings) {
                return Exceptions.wrap(() -> resolver(null, bindings));
            }

            @Override
            public ComponentFactory.Resolver resolver(final Class<?> type, final ComponentFactory.Bindings bindings) throws Exception {
                final ContextDefinition reduced = context.copy().accept(type);
                final ComponentContext passed = reduced.create();
                final SimpleContainer container = nested.newChildContainer(false);

                if (bindings != null) {
                    bindings.bind(new ComponentFactory.Registry() {
                        @Override
                        @SafeVarargs
                        public final <T> void bindComponent(final Class<T> implementation, final Class<? super T>... interfaces) throws ComponentContainer.BindingException {
                            container.bindComponent(Components.inspect(implementation, interfaces));
                        }

                        @Override
                        @SuppressWarnings("unchecked")
                        public <T> void bindInstance(final T instance, final Class<? super T>... interfaces) throws ComponentContainer.BindingException {
                            if (instance != null) {
                                container.bindInstance(instance, Components.inspect((Class) instance.getClass(), interfaces));
                            }
                        }
                    });
                } else if (type != null) {
                    container.bindComponent(Components.inspect(type));
                }

                final LocalResolution resolution = new LocalResolution(traversal, container, passed, reduced, context.copy(), contexts);
                return new LocalResolver(instances, containers, traversal, injector, resolution);
            }

            @Override
            public ComponentFactory.Instance instance(final Class<?> type, final ComponentFactory.Bindings bindings) throws Exception {
                resolver(bindings).discover(type);
                return ComponentFactory.Instance.of(type, bindings);
            }

            @Override
            @SuppressWarnings("unchecked")
            public ComponentFactory.Instance instance(final Class<?> type) {
                return Exceptions.wrap(() -> instance(type, registry -> registry.bindComponent(type)));
            }
        };

        try {
            return factory.resolve(passed, container);
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

        @SuppressWarnings("unchecked")
        public <T> void bindComponent(final Class<T> implementation, final Class<? super T>... interfaces) throws ComponentContainer.BindingException {
            container.bindComponent(bound(implementation, interfaces));
        }

        @SuppressWarnings("unchecked")
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

    /**
     * @author Tibor Varga
     */
    private static final class LocalResolver implements ComponentFactory.Resolver {

        private final AccessGuard<DependencyGraph.Node> instances;
        private final AccessGuard<ComponentContainer> containers;
        private final DependencyGraph.Traversal traversal;
        private final DependencyInjector injector;
        private final Resolution resolution;

        LocalResolver(final AccessGuard<DependencyGraph.Node> instances,
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

        public Dependency<?> resolve(final Constructor<?> constructor, final int parameter) {
            if (constructor == null) {
                throw new ComponentContainer.BindingException("Provided constructor is null");
            }

            final Generics.Parameters parameters = Generics.describe(constructor);
            final Annotation[] parameterAnnotations = parameters.annotations(parameter);

            final Class<?> type = constructor.getDeclaringClass();
            return dependency(type,
                              parameters.type(parameter),
                              parameters.genericType(parameter),
                              Lists.concatenate(Annotation.class,
                                                type.getAnnotations(),
                                                constructor.getAnnotations(),
                                                parameterAnnotations),
                              parameterAnnotations);
        }

        public Dependency<?> resolve(final Class<?> type, final Method method, final int parameter) {
            if (method == null) {
                throw new ComponentContainer.ResolutionException("Provided method is null");
            }

            final Annotation[] parameterAnnotations = method.getParameterAnnotations()[parameter];

            final Class<?> api = type == null ? method.getDeclaringClass() : type;
            return dependency(api,
                              method.getParameterTypes()[parameter],
                              method.getGenericParameterTypes()[parameter],
                              Lists.concatenate(Annotation.class,
                                                api.getAnnotations(),
                                                method.getAnnotations(),
                                                parameterAnnotations),
                              parameterAnnotations);
        }

        public Dependency<?> resolve(final Class<?> type, final Field field) {
            if (field == null) {
                throw new ComponentContainer.ResolutionException("Provided field is null");
            }

            final Class<?> api = type == null ? field.getDeclaringClass() : type;
            return dependency(api,
                              field.getType(),
                              field.getGenericType(),
                              Lists.concatenate(Annotation.class,
                                                api.getAnnotations(),
                                                field.getAnnotations()),
                              field.getAnnotations());
        }

        public Dependency<?>[] resolve(final Class<?> type, final Method method) {
            final Class<?>[] types = method.getParameterTypes();
            final Dependency<?>[] dependencies = new Dependency<?>[types.length];

            for (int i = 0, limit = types.length; i < limit; i++) {
                dependencies[i] = resolve(type, method, i);
            }

            return dependencies;
        }

        public Dependency<?>[] resolve(final Constructor<?> constructor) {
            final Class<?>[] types = constructor.getParameterTypes();
            final Dependency<?>[] dependencies = new Dependency<?>[types.length];

            for (int i = 0, limit = types.length; i < limit; i++) {
                dependencies[i] = resolve(constructor, i);
            }

            return dependencies;
        }

        public Dependency<?>[] discover(final Class<?> type) {
            assert type != null;
            return discover(constructor(type));
        }

        @Override
        public Dependency<?> lookup(final Type reference) {
            if (reference == null) {
                throw new IllegalArgumentException("Provided reference is null");
            }

            final Class<?> api = Generics.rawType(reference);
            final DependencyGraph.Node node = dependency(null, api, null, descend(api, reference, null, null));

            return Dependency.to(node::type, () -> node.instance(traversal));
        }

        public Constructor<?> constructor(final Class<?> type) {
            assert type != null;
            return injector.findConstructor(type);
        }

        public Dependency<?>[] discover(final Constructor<?> constructor) {
            assert constructor != null;

            final Class<?> reference = constructor.getDeclaringClass();

            if (reference.getAnnotation(Component.class) != null) {
                fields(reference);
            }

            return discover(reference,
                            constructor.getGenericParameterTypes(),
                            Lists.concatenate(Annotation.class, reference.getAnnotations(), constructor.getAnnotations()),
                            constructor.getParameterAnnotations());
        }

        private void fields(final Class<?> type) {
            assert type != null;

            for (final Field field : Security.invoke(type::getDeclaredFields)) {
                if (field.isAnnotationPresent(Inject.class)) {
                    final Annotation[] fieldAnnotations = field.getAnnotations();

                    descend(field.getType(),
                            field.getGenericType(),
                            Lists.concatenate(Annotation.class, field.getDeclaringClass().getAnnotations(), fieldAnnotations),
                            fieldAnnotations);
                }
            }

            final Class<?> superClass = type.getSuperclass();

            if (superClass != null && superClass != Object.class) {
                fields(superClass);
            }
        }

        public Dependency<?>[] discover(final Class<?> type, final Method method) {
            assert type != null;
            assert method != null;

            return discover(type,
                            method.getGenericParameterTypes(),
                            Lists.concatenate(Annotation.class, type.getAnnotations(), method.getAnnotations()),
                            method.getParameterAnnotations());
        }

        private Dependency<?>[] discover(final Class reference, final Type[] types, final Annotation[] annotations, final Annotation[][] parameters) {
            final List<Dependency<?>> nodes = new ArrayList<>();

            for (int i = 0, limit = types.length; i < limit; i++) {
                final Type type = types[i];
                final Class<?> api = Generics.rawType(type);

                final Annotation[] params = parameters[i];
                final DependencyGraph.Node node = dependency(reference,
                                                             api,
                                                             params,
                                                             descend(api, type, Lists.concatenate(Annotation.class, annotations, params), params));

                nodes.add(Dependency.to(() -> node == null ? api : node.type(), () -> node == null ? null : node.instance(traversal)));
            }

            return Lists.asArray(Dependency.class, nodes);
        }

        @SuppressWarnings("unchecked")
        public <T> Dependency<T> constant(final T object) {
            return object == null ? null : Dependency.to((Class<? extends T>) object.getClass(), () -> object);
        }

        public Object[] instantiate(final Dependency<?>... dependencies) {
            final Object[] instances = new Object[dependencies.length];

            for (int i = 0, limit = dependencies.length; i < limit; i++) {
                instances[i] = dependencies[i].instance();
            }

            return instances;
        }

        @Override
        public <T> T invoke(final Constructor<T> constructor, final Dependency<?>... arguments) throws Exception {
            return Security.access(constructor).newInstance(instantiate(arguments));
        }

        @Override
        public Object invoke(final Object target, final Method method, final Dependency<?>... arguments) throws Exception {
            return Security.access(method).invoke(target, instantiate(arguments));
        }

        private DependencyGraph.Node descend(final Class<?> api, final Type reference, final Annotation[] annotations, final Annotation[] params) {
            assert reference != null : api;

            return injector.resolve(api, containers, new DependencyInjector.Resolution() {
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
        }

        @SuppressWarnings("unchecked")
        private <T> Dependency<T> dependency(final Class<?> type,
                                             final Class<T> api,
                                             final Type reference,
                                             final Annotation[] annotations,
                                             final Annotation[] params) {
            final DependencyGraph.Node node = dependency(type, api, params, descend(api, reference, annotations, params));

            // instances.access(node) throws if access not allowed
            return node == null ? null : (Dependency<T>) Dependency.to(node::type, () -> instances.access(node) == null ? null : node.instance(traversal));
        }

        DependencyGraph.Node dependency(final Class<?> reference, final Class<?> api, final Annotation[] annotations, final DependencyGraph.Node node) {
            if (node == null) {
                final Class<?> type = reference == null ? host(api) : reference;
                final Component component = type.getAnnotation(Component.class);

                if (component == null) {
                    return null;
                }

                for (final Annotation annotation : annotations) {
                    if (annotation.annotationType() == Optional.class) {
                        return null;
                    }
                }

                throw new ComponentContainer.ResolutionException("Dependency %s of %s cannot be satisfied", Strings.formatClass(true, true, api), Strings.formatClass(true, true, type));
            }

            return node;
        }

        public Class<?> host(final Class<?> api) {
            final List<? extends DependencyPath.Element> path = traversal.path().path();
            assert path.size() > 1 : api;
            return path.get(path.size() - 2).type();
        }
    }

    private class LocalResolution implements Resolution {

        private final DependencyGraph.Traversal traversal;
        private final SimpleContainer container;
        private final ComponentContext passed;
        private final ContextDefinition reduced;
        private final ContextDefinition collected;
        private final List<ContextDefinition> contexts;

        public LocalResolution(final DependencyGraph.Traversal traversal,
                               final SimpleContainer container,
                               final ComponentContext passed,
                               final ContextDefinition reduced,
                               final ContextDefinition collected,
                               final List<ContextDefinition> contexts) {
            this.traversal = traversal;
            this.container = container;
            this.passed = passed;
            this.reduced = reduced;
            this.collected = collected;
            this.contexts = contexts;
        }

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
                return new ComponentContainerShell(container, reduced.accept(null), false);
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
                       ? container.resolveGroup(type.getComponentType(), copy, traversal, type)
                       : container.resolveComponent(type, copy, traversal, type);
            } finally {
                traversal.ascend(api, type);
            }
        }
    }
}

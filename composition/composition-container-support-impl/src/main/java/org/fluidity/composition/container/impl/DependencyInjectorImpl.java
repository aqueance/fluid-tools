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

package org.fluidity.composition.container.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.Components;
import org.fluidity.composition.Inject;
import org.fluidity.composition.Optional;
import org.fluidity.composition.container.ContextDefinition;
import org.fluidity.composition.container.DependencyInjector;
import org.fluidity.composition.container.ResolvedNode;
import org.fluidity.composition.container.RestrictedContainer;
import org.fluidity.composition.container.spi.ContextNode;
import org.fluidity.composition.container.spi.DependencyGraph;
import org.fluidity.composition.container.spi.DependencyResolver;
import org.fluidity.foundation.Deferred;
import org.fluidity.foundation.Exceptions;
import org.fluidity.foundation.Generics;
import org.fluidity.foundation.Lists;
import org.fluidity.foundation.Strings;

import static org.fluidity.foundation.Command.Process;

/**
 * Finds, resolves and sets, using the given container, all {@link Inject @Inject} annotated fields of an object that have not yet been set.
 *
 * @author Tibor Varga
 */
final class DependencyInjectorImpl implements DependencyInjector {

    private final DependencyInterceptors interceptors;

    DependencyInjectorImpl(final DependencyInterceptors interceptors) {
        this.interceptors = interceptors;
    }

    public <T> T fields(final T instance,
                        final DependencyGraph.Traversal traversal,
                        final DependencyResolver container,
                        final ContextNode contexts,
                        final ContextDefinition context) {
        assert container != null;

        if (instance != null) {
            final Class<?> componentClass = instance.getClass();
            final Map<Field, DependencyGraph.Node> fieldNodes = new IdentityHashMap<Field, DependencyGraph.Node>();

            context.collect(resolveFields(traversal, container, contexts, context, componentClass, fieldNodes));

            final List<RestrictedContainer> containers = new ArrayList<RestrictedContainer>();
            injectFields(fieldNodes, traversal, containers, instance);
            enableContainers(containers);
        }

        return instance;
    }

    public Object invoke(final Object component,
                         final Method method,
                         final Object[] arguments,
                         final DependencyGraph.Traversal traversal,
                         final DependencyResolver container,
                         final ContextNode contexts,
                         final ContextDefinition context,
                         final boolean explicit) throws ComponentContainer.ResolutionException, InvocationTargetException {
        assert method != null;
        assert container != null;

        final Class<?> componentClass = method.getDeclaringClass();
        final Annotation[] methodAnnotations = method.getAnnotations();
        final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        final Type[] types = method.getGenericParameterTypes();
        final Object[] parameters = arguments == null ? new Object[types.length] : Arrays.copyOf(arguments, types.length);
        final boolean parameterized = Components.isParameterized(componentClass);

        for (int i = 0, length = types.length; i < length; ++i) {
            final int index = i;
            if (parameters[index] == null && (explicit || contains(parameterAnnotations[index], Inject.class))) {
                injectDependency(false, traversal, container, contexts, context, componentClass, parameterized, new Dependency() {
                    public Type reference() {
                        return types[index];
                    }

                    public <T extends Annotation> T annotation(final Class<T> annotationClass) {
                        return find(annotations(), annotationClass);
                    }

                    public Annotation[] annotations() {
                        return Lists.concatenate(Annotation.class, methodAnnotations, parameterAnnotations[index]);
                    }

                    public void set(final DependencyGraph.Node node) {
                        parameters[index] = node.instance(traversal);
                    }
                });
            }
        }

        try {
            method.setAccessible(true);
            return method.invoke(component, parameters);
        } catch (final IllegalAccessException e) {
            throw new ComponentContainer.ResolutionException(e, "Invoking %s", method);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Annotation> T find(final Annotation[] annotations, final Class<T> check) {
        for (final Annotation annotation : annotations) {
            if (annotation.annotationType() == check) {
                return (T) annotation;
            }
        }

        return null;
    }

    private <T extends Annotation> boolean contains(final T[] list, final Class<? extends T> check) {
        for (final T item : list) {
            if (item.annotationType() == check) {
                return true;
            }
        }

        return false;
    }

    public Constructor<?> findConstructor(final Class<?> componentClass) throws ComponentContainer.ResolutionException {
        Constructor<?> designated = null;

        final List<Constructor<?>> validConstructors = new ArrayList<Constructor<?>>();

        for (final Constructor<?> constructor : componentClass.getDeclaredConstructors()) {
            if (!constructor.isSynthetic()) {
                if (designated == null) {
                    validConstructors.add(constructor);
                }

                if (constructor.isAnnotationPresent(Inject.class)) {
                    if (designated != null) {
                        throw new ComponentContainer.ResolutionException("Multiple @%s constructors found for %s", Inject.class.getName(), componentClass);
                    } else {
                        designated = constructor;
                    }
                }
            }
        }

        if (designated != null) {
            return designated;
        }

        try {
            return selectConstructor(validConstructors, componentClass);
        } catch (final MultipleConstructorsException e) {
            final List<Constructor<?>> publicConstructors = new ArrayList<Constructor<?>>();

            for (final Constructor<?> constructor1 : validConstructors) {
                if (Modifier.isPublic(constructor1.getModifiers())) {
                    publicConstructors.add(constructor1);
                }
            }

            return selectConstructor(publicConstructors, componentClass);
        }
    }

    public DependencyGraph.Node resolve(final Class<?> api, final Resolution resolution) {
        assert api != null;
        if (api == ComponentContext.class) {
            return new ResolvedNode(ComponentContext.class, resolution.context(), null);
        } else if (api == DependencyInjector.class) {
            return new ResolvedNode(DependencyInjector.class, this, null);
        } else if (api == ComponentContainer.class) {
            final ComponentContainer container = resolution.container();

            if (container == null) {
                return new ResolvedNode(ComponentContainer.class, null, null);
            } else {
                final RestrictedContainer restricted = new RestrictedContainerImpl(container);
                resolution.handle(restricted);
                return new ResolvedNode(RestrictedContainer.class, restricted, null);
            }
        } else {
            return resolution.regular();
        }
    }

    private Constructor<?> selectConstructor(final List<Constructor<?>> constructors, final Class<?> componentClass) {
        switch (constructors.size()) {
        case 0:
            throw new NoConstructorException(componentClass);
        case 1:
            return constructors.get(0);
        case 2:

            // return the one with more than 0 parameters
            final int parameterCount0 = constructors.get(0).getParameterTypes().length;
            final int parameterCount1 = constructors.get(1).getParameterTypes().length;

            if (parameterCount0 == 0 && parameterCount1 != 0) {
                return constructors.get(1);
            } else if (parameterCount0 != 0 && parameterCount1 == 0) {
                return constructors.get(0);
            }

            // fall through
        default:
            throw new MultipleConstructorsException(componentClass);
        }
    }

    private static class MultipleConstructorsException extends ComponentContainer.ResolutionException {

        MultipleConstructorsException(final Class<?> componentClass) {
            super("Multiple constructors found for %s", componentClass);
        }
    }

    private static class NoConstructorException extends ComponentContainer.ResolutionException {

        NoConstructorException(final Class<?> componentClass) {
            super("No suitable constructor found for %s", componentClass);
        }
    }

    public DependencyGraph.Node constructor(final Class<?> api,
                                            final DependencyGraph.Traversal traversal,
                                            final DependencyResolver container,
                                            final ContextNode contexts,
                                            final ContextDefinition context,
                                            final Constructor<?> constructor) {
        final List<ContextDefinition> consumed = new ArrayList<ContextDefinition>();

        final Class<?> componentClass = constructor.getDeclaringClass();

        final Generics.Parameters descriptor = Generics.describe(constructor);

        final Class[] params = constructor.getParameterTypes();
        final Annotation[] constructorAnnotations = Lists.notNull(Annotation.class, constructor.getAnnotations());

        final DependencyGraph.Node[] parameters = new DependencyGraph.Node[params.length];
        final boolean parameterized = Components.isParameterized(componentClass);

        for (int i = 0, length = params.length; i < length; ++i) {
            final int index = i;
            consumed.add(injectDependency(true, traversal, container, contexts, context, componentClass, parameterized, new Dependency() {
                public Type reference() {
                    return descriptor.genericType(index);
                }

                public <T extends Annotation> T annotation(final Class<T> annotationClass) {
                    return find(annotations(), annotationClass);
                }

                public Annotation[] annotations() {
                    return Lists.concatenate(Annotation.class, constructorAnnotations, descriptor.annotations(index));
                }

                public void set(final DependencyGraph.Node node) {
                    parameters[index] = node;
                }
            }));
        }

        final Map<Field, DependencyGraph.Node> fields = new IdentityHashMap<Field, DependencyGraph.Node>();
        consumed.addAll(resolveFields(traversal, container, contexts, context, componentClass, fields));

        final ComponentContext componentContext = context.accept(contexts.contextConsumer()).collect(consumed).create();

        return new DependencyGraph.Node() {
            public Class<?> type() {
                return componentClass;
            }

            public Object instance(final DependencyGraph.Traversal traversal) {
                final List<RestrictedContainer> containers = new ArrayList<RestrictedContainer>();

                try {
                    return injectFields(fields, traversal, containers, instantiate(containers, traversal));
                } finally {
                    enableContainers(containers);
                }
            }

            public ComponentContext context() {
                return componentContext;
            }

            private Object instantiate(final List<RestrictedContainer> containers, final DependencyGraph.Traversal traversal) {
                final Object[] arguments = arguments(constructor.getDeclaringClass(), traversal, containers, parameters);

                final Object cached = container.cached(api, componentContext);

                if (cached == null) {
                    final Deferred.Label label = Deferred.label(new Deferred.Factory<String>() {
                        public String create() {
                            return String.format("Invoking %s with %s", constructor, Strings.formatId(arguments));
                        }
                    });

                    traversal.instantiating(componentClass);
                    return traversal.instantiated(componentClass, Exceptions.wrap(label, ComponentContainer.ResolutionException.class, new Process<Object, Exception>() {
                        public Object run() throws Exception {
                            constructor.setAccessible(true);
                            return constructor.newInstance(arguments);
                        }
                    }));
                } else {
                    return cached;
                }
            }
        };
    }

    private void enableContainers(final List<RestrictedContainer> containers) {
        for (final RestrictedContainer container : containers) {
            container.enable();
        }
    }

    private Object injectFields(final Map<Field, DependencyGraph.Node> fieldNodes,
                                final DependencyGraph.Traversal traversal,
                                final List<RestrictedContainer> containers,
                                final Object instance) {
        final Deferred.Label label = Deferred.label(new Deferred.Factory<String>() {
            public String create() {
                return String.format("Setting %s fields", Strings.formatClass(false, true, instance.getClass()));
            }
        });

        return Exceptions.wrap(label, ComponentContainer.ResolutionException.class, new Process<Object, Exception>() {
            public Object run() throws Exception {
                for (final Map.Entry<Field, DependencyGraph.Node> entry : fieldNodes.entrySet()) {
                    final Field field = entry.getKey();

                    if (field.get(instance) == null) {
                        field.set(instance, arguments(field.getDeclaringClass(), traversal, containers, entry.getValue())[0]);
                    }
                }

                return instance;
            }
        });
    }

    public Object[] arguments(final Class<?> type,
                              final DependencyGraph.Traversal traversal,
                              final List<RestrictedContainer> containers,
                              final DependencyGraph.Node... nodes) {
        final Object[] values = new Object[nodes.length];

        for (int i = 0, limit = nodes.length; i < limit; i++) {
            final Object value = nodes[i].instance(traversal);

            if (value instanceof RestrictedContainer) {
                containers.add((RestrictedContainer) value);
            } else if (value instanceof ComponentContext) {
                final Component.Reference reference = ((ComponentContext) value).annotation(Component.Reference.class, null);

                if (reference != null) {
                    final Type[] variables = Generics.unresolved((reference).type());

                    if (variables != null) {
                        throw new ComponentContainer.ResolutionException("Parameterized component of type %s with unresolved type variables: %s",
                                                                         type.getName(),
                                                                         Arrays.toString(variables));
                    }
                }
            }

            values[i] = value;
        }

        return values;
    }

    /**
     * @author Tibor Varga
     */
    private interface FieldCommand {

        void process(Field field);
    }

    private void processFields(final Class<?> type, final FieldCommand command) {
        for (final Field field : type.getDeclaredFields()) {
            field.setAccessible(true);

            if ((field.getModifiers() & Modifier.FINAL) != 0) {
                continue;
            }

            if (field.isAnnotationPresent(Inject.class)) {
                command.process(field);
            }
        }

        final Class<?> ancestor = type.getSuperclass();

        if (ancestor != null) {
            processFields(ancestor, command);
        }
    }

    private List<ContextDefinition> resolveFields(final DependencyGraph.Traversal traversal,
                                                  final DependencyResolver container,
                                                  final ContextNode contexts,
                                                  final ContextDefinition context,
                                                  final Class<?> declaringType,
                                                  final Map<Field, DependencyGraph.Node> nodes) {
        final List<ContextDefinition> consumed = new ArrayList<ContextDefinition>();
        final boolean parameterized = Components.isParameterized(declaringType);

        processFields(declaringType, new FieldCommand() {
            public void process(final Field field) {
                consumed.add(injectDependency(false, traversal, container, contexts, context, declaringType, parameterized, new Dependency() {
                    public Type reference() {
                        return field.getGenericType();
                    }

                    public <T extends Annotation> T annotation(final Class<T> annotationClass) {
                        return field.getAnnotation(annotationClass);
                    }

                    public Annotation[] annotations() {
                        return field.getAnnotations();
                    }

                    public void set(final DependencyGraph.Node node) {
                        nodes.put(field, node);
                    }
                }));
            }
        });

        return consumed;
    }

    private ContextDefinition injectDependency(final boolean resolving,
                                               final DependencyGraph.Traversal traversal,
                                               final DependencyResolver container,
                                               final ContextNode contexts,
                                               final ContextDefinition original,
                                               final Class<?> declaringType,
                                               final boolean parameterized,
                                               final Dependency dependency) {
        final Component.Reference inbound = original.reference();
        final Type reference = inbound == null || !parameterized ? dependency.reference() : Generics.propagate(inbound.type(), dependency.reference());

        final ComponentGroup componentGroup = dependency.annotation(ComponentGroup.class);
        final Class<?> dependencyType = findDependencyType(dependency.annotation(Component.class), reference, declaringType);
        final boolean mandatory = dependency.annotation(Optional.class) == null;

        assert contexts != null : declaringType;
        final Annotation[] typeContext = contexts.providedContext();
        final Annotation[] dependencyContext = dependency.annotations();

        if (resolving) {
            traversal.descend(declaringType, dependencyType, Lists.notNull(Annotation.class, typeContext), dependencyContext);
        }

        final ContextDefinition downstream;
        final DependencyGraph.Node node;

        try {
            final Annotation[] definitions = Lists.concatenate(Annotation.class, typeContext, dependencyContext);

            downstream = original.advance(reference, false).expand(definitions);

            if (componentGroup != null) {
                final Class<?> itemType = dependencyType.getComponentType();

                if (itemType == null) {
                    throw new ComponentContainer.ResolutionException("Group dependency %s of %s must be an array",
                                                                     Strings.formatClass(false, true, dependencyType),
                                                                     declaringType);
                }

                if (itemType.isArray()) {
                    throw new ComponentContainer.ResolutionException("Group dependency %s of %s must be an array of non-arrays",
                                                                     Strings.formatClass(false, true, dependencyType),
                                                                     declaringType);
                }

                final Class<?> groupType = componentGroup != null && componentGroup.api() != null && componentGroup.api().length > 0
                                           ? componentGroup.api()[0]
                                           : null;

                if (groupType != null) {
                    if (componentGroup.api().length > 1) {
                        throw new ComponentContainer.ResolutionException("Multiple component group APIs specified for dependency %s of %s: %s",
                                                                         Strings.formatClass(false, true, dependencyType),
                                                                         declaringType,
                                                                         Strings.describeAnnotation(false, componentGroup));
                    }

                    if (!itemType.isAssignableFrom(groupType)) {
                        throw new ComponentContainer.ResolutionException("The specified component type is not assignable to the dependency type %s of %s: %s",
                                                                         Strings.formatClass(false, true, dependencyType),
                                                                         declaringType,
                                                                         Strings.describeAnnotation(false, componentGroup));
                    }
                }

                node = container.resolveGroup(groupType == null ? itemType : groupType, downstream.advance(reference, true), traversal, reference);
            } else {
                node = resolve(dependencyType, new Resolution() {
                    public ComponentContext context() {

                        // injected context must only contain annotations accepted by the instantiated component;
                        // accepted context depends only on defined context, which is propagated forward
                        // therefore we have all necessary information in the original context
                        return original.copy().accept(contexts.contextConsumer()).create();
                    }

                    public ComponentContainer container() {

                        // injected container is not interested in accepted contexts, only defined ones;
                        // defined context is propagated forward therefore we have all necessary information
                        // in the original context
                        return container.container(original.copy().expand(definitions));
                    }

                    public DependencyGraph.Node regular() {
                        return interceptors.replace(container,
                                                    downstream,
                                                    traversal,
                                                    reference,
                                                    container.resolveComponent(dependencyType, downstream, traversal, reference));
                    }

                    public void handle(final RestrictedContainer container) {
                        // empty
                    }
                });
            }
        } finally {
            if (resolving) {
                traversal.ascend(declaringType, dependencyType);
            }
        }

        dependency.set(new DependencyNode(mandatory, node, declaringType, dependencyType));

        return downstream;
    }

    private Class<?> findDependencyType(final Component component, final Type reference, final Class<?> declaringType) {
        if (reference instanceof TypeVariable || reference instanceof WildcardType) {
            throw new ComponentContainer.ResolutionException("Unresolved type parameter [%s] in a dependency of %s", reference, declaringType);
        }

        final Class<?> defaultType = Generics.rawType(reference);
        final Class<?>[] types = component == null ? new Class<?>[] { defaultType } : component.api();

        switch (types.length) {
        case 0:
            return defaultType;
        case 1:
            return types[0];
        default:
            throw new ComponentContainer.ResolutionException("Multiple types specified for dependency %s of %s", defaultType, declaringType);
        }
    }

    /**
     * Internal interface describing a dependency of a component.
     *
     * @author Tibor Varga
     */
    private interface Dependency {

        /**
         * Returns the parameterized reference to the dependency.
         *
         * @return the parameterized reference to the dependency.
         */
        Type reference();

        /**
         * Returns the annotation, if any, of the given type attached to the reference.
         *
         * @param annotationClass the annotation type to return an instance of, if any.
         *
         * @return the annotation of the given type attached to the reference, or <code>null</code> if no such annotation is present.
         */
        <T extends Annotation> T annotation(Class<T> annotationClass);

        /**
         * Returns all annotations present at the reference.
         *
         * @return all annotations present at the reference; never <code>null</code>.
         */
        Annotation[] annotations();

        /**
         * Sets the resolved value of the reference.
         *
         * @param node the future value of the field.
         */
        void set(DependencyGraph.Node node);
    }

    /**
     * @author Tibor Varga
     */
    private static class DependencyNode implements DependencyGraph.Node {

        private final boolean mandatory;
        private final DependencyGraph.Node node;
        private final Class<?> declaringType;
        private final Class<?> dependencyType;

        DependencyNode(final boolean mandatory, final DependencyGraph.Node node, final Class<?> declaringType, final Class<?> dependencyType) {
            this.mandatory = mandatory;
            this.node = node;
            this.declaringType = declaringType;
            this.dependencyType = dependencyType;
        }

        public Class<?> type() {
            return node.type();
        }

        public Object instance(final DependencyGraph.Traversal traversal) {
            final Object instance = node == null ? null : node.instance(traversal);

            if (instance == null && mandatory) {
                throw new ComponentContainer.ResolutionException("Dependency %s of %s cannot be satisfied",
                                                                 Strings.formatClass(true, true, dependencyType),
                                                                 declaringType);
            } else {
                return instance;
            }
        }

        public ComponentContext context() {
            return node.context();
        }
    }
}

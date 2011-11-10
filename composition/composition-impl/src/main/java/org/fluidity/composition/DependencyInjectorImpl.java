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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.fluidity.composition.spi.ComponentDescriptor;
import org.fluidity.composition.spi.DependencyPath;
import org.fluidity.composition.spi.DependencyResolver;
import org.fluidity.foundation.Exceptions;
import org.fluidity.foundation.Strings;

/**
 * Finds, resolves and sets, using the given container, all {@link Inject @Inject} annotated fields of an object that have not yet been set.
 *
 * @author Tibor Varga
 */
final class DependencyInjectorImpl implements DependencyInjector {

    public <T> T fields(final T instance,
                        final DependencyGraph.Traversal traversal,
                        final DependencyResolver container,
                        final ComponentDescriptor descriptor,
                        final ContextDefinition context) {
        assert container != null;

        if (instance != null) {
            final Class<?> componentClass = instance.getClass();
            final Map<Field, DependencyGraph.Node> fieldNodes = new IdentityHashMap<Field, DependencyGraph.Node>();

            context.collect(resolveFields(traversal, container, descriptor, context, componentClass, fieldNodes));

            final List<RestrictedContainer> containers = new ArrayList<RestrictedContainer>();
            injectFields(fieldNodes, traversal, containers, instance);
            enableContainers(containers);
        }

        return instance;
    }

    public Object invoke(final Object component,
                         final Method method,
                         final DependencyGraph.Traversal traversal,
                         final DependencyResolver container,
                         final ComponentDescriptor descriptor,
                         final ContextDefinition context) throws ComponentContainer.ResolutionException {
        assert method != null;
        assert container != null;

        final Class<?> componentClass = method.getDeclaringClass();
        final Annotation[] methodAnnotations = method.getAnnotations();
        final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        final Class[] types = method.getParameterTypes();
        final Object[] parameters = new Object[types.length];

        for (int i = 0, length = types.length; i < length; ++i) {
            final int index = i;
            injectDependency(false, traversal, container, descriptor, context.copy(), componentClass, new Dependency() {

                public Class<?> type() {
                    return types[index];
                }

                @SuppressWarnings("unchecked")
                public <T extends Annotation> T annotation(final Class<T> annotationClass) {
                    for (final Annotation annotation : annotations()) {
                        if (annotationClass.isAssignableFrom(annotation.getClass())) {
                            return (T) annotation;
                        }
                    }

                    return null;
                }

                public Annotation[] annotations() {
                    return parameterAnnotations(methodAnnotations, parameterAnnotations[index]);
                }

                public void set(final DependencyGraph.Node node) {
                    parameters[index] = node.instance(traversal);
                }
            });
        }

        try {
            method.setAccessible(true);
            return method.invoke(component, parameters);
        } catch (final IllegalAccessException e) {
            throw new ComponentContainer.ResolutionException(e, "Invoking %s", method);
        } catch (final InvocationTargetException e) {
            throw new ComponentContainer.ResolutionException(e.getCause(), "Invoking %s", method);
        }
    }

    public Object invoke(final Object component,
                         final Method method,
                         final Object[] arguments,
                         final DependencyGraph.Traversal traversal,
                         final DependencyResolver container,
                         final ComponentDescriptor descriptor,
                         final ContextDefinition context) throws ComponentContainer.ResolutionException {
        assert method != null;
        assert container != null;

        final Class<?> componentClass = method.getDeclaringClass();
        final Annotation[] methodAnnotations = method.getAnnotations();
        final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        final Class[] types = method.getParameterTypes();
        final Object[] parameters = new Object[types.length];

        for (int i = 0, length = types.length; i < length; ++i) {
            final int index = i;

            if (arguments[index] == null && contains(parameterAnnotations[index], Inject.class)) {
                injectDependency(false, traversal, container, descriptor, context.copy(), componentClass, new Dependency() {

                    public Class<?> type() {
                        return types[index];
                    }

                    @SuppressWarnings("unchecked")
                    public <T extends Annotation> T annotation(final Class<T> annotationClass) {
                        for (final Annotation annotation : annotations()) {
                            if (annotationClass.isAssignableFrom(annotation.getClass())) {
                                return (T) annotation;
                            }
                        }

                        return null;
                    }

                    public Annotation[] annotations() {
                        return parameterAnnotations(methodAnnotations, parameterAnnotations[index]);
                    }

                    public void set(final DependencyGraph.Node node) {
                        parameters[index] = node.instance(traversal);
                    }
                });
            } else {
                parameters[index] = arguments[index];
            }
        }

        try {
            method.setAccessible(true);
            return method.invoke(component, parameters);
        } catch (final IllegalAccessException e) {
            throw new ComponentContainer.ResolutionException(e, "Invoking %s", method);
        } catch (final InvocationTargetException e) {
            throw new ComponentContainer.ResolutionException(e.getCause(), "Invoking %s", method);
        }
    }

    private <T> boolean contains(final T[] list, final Class<? extends T> check) {
        for (final T item : list) {
            if (item.getClass() == check) {
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
        if (ComponentContext.class.isAssignableFrom(api)) {
            return new DependencyGraph.Node.Constant(ComponentContext.class, resolution.context(), null);
        } else if (DependencyPath.class.isAssignableFrom(api)) {
            return new PathNode();
        } else if (DependencyInjector.class.isAssignableFrom(api)) {
            return new DependencyGraph.Node.Constant(DependencyInjector.class, this, null);
        } else if (ComponentContainer.class.isAssignableFrom(api)) {
            final ComponentContainer container = resolution.container();

            if (container == null) {
                return new DependencyGraph.Node.Constant(ComponentContainer.class, null, null);
            } else {
                final RestrictedContainer restricted = new RestrictedContainerImpl(container);
                resolution.handle(restricted);
                return new DependencyGraph.Node.Constant(RestrictedContainer.class, restricted, null);
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

        public MultipleConstructorsException(final Class<?> componentClass) {
            super("Multiple constructors found for %s", componentClass);
        }
    }

    private static class NoConstructorException extends ComponentContainer.ResolutionException {

        public NoConstructorException(final Class<?> componentClass) {
            super("No suitable constructor found for %s", componentClass);
        }
    }

    public DependencyGraph.Node constructor(final DependencyGraph.Traversal traversal,
                                            final DependencyResolver container,
                                            final ComponentDescriptor descriptor,
                                            final ContextDefinition context,
                                            final Constructor<?> constructor) {
        final List<ContextDefinition> consumed = new ArrayList<ContextDefinition>();

        final Class<?> componentClass = constructor.getDeclaringClass();
        final Annotation[] constructorAnnotations = neverNull(constructor.getAnnotations());
        final Annotation[][] parameterAnnotations = constructor.getParameterAnnotations();
        final Class[] types = constructor.getParameterTypes();
        final DependencyGraph.Node[] arguments = new DependencyGraph.Node[types.length];

        for (int i = 0, length = types.length; i < length; ++i) {
            final int index = i;
            consumed.add(injectDependency(true, traversal, container, descriptor, context.copy(), componentClass, new Dependency() {

                public Class<?> type() {
                    return types[index];
                }

                @SuppressWarnings("unchecked")
                public <T extends Annotation> T annotation(final Class<T> annotationClass) {
                    for (final Annotation annotation : annotations()) {
                        if (annotationClass.isAssignableFrom(annotation.getClass())) {
                            return (T) annotation;
                        }
                    }

                    return null;
                }

                public Annotation[] annotations() {
                    return parameterAnnotations(constructorAnnotations, parameterAnnotations[index]);
                }

                public void set(final DependencyGraph.Node node) {
                    arguments[index] = node;
                }
            }));
        }

        final Map<Field, DependencyGraph.Node> fields = new IdentityHashMap<Field, DependencyGraph.Node>();
        consumed.addAll(resolveFields(traversal, container, descriptor, context, componentClass, fields));

        final ComponentContext componentContext = context.collect(consumed).create();

        return new DependencyGraph.Node() {
            public Class<?> type() {
                return componentClass;
            }

            public Object instance(final DependencyGraph.Traversal traversal) {
                final List<RestrictedContainer> containers = new ArrayList<RestrictedContainer>();

                try {
                    return injectFields(fields, traversal, containers, Exceptions.wrap(String.format("instantiating %s", componentClass), new Exceptions.Command<Object>() {
                        public Object run() throws Exception {
                            constructor.setAccessible(true);
                            traversal.instantiating(componentClass);
                            final Object component = constructor.newInstance(create(traversal, containers, arguments));
                            traversal.instantiated(componentClass, component);
                            return component;
                        }
                    }));
                } finally {
                    enableContainers(containers);
                }
            }

            public ComponentContext context() {
                return componentContext;
            }
        };
    }

    private Annotation[] parameterAnnotations(final Annotation[] method, final Annotation[] params) {
        final Annotation[] annotations = neverNull(params);
        final Annotation[] all = Arrays.copyOf(method, method.length + annotations.length);
        System.arraycopy(annotations, 0, all, method.length, annotations.length);
        return all;
    }

    private void enableContainers(final List<RestrictedContainer> containers) {
        for (final RestrictedContainer container : containers) {
            container.enable();
        }
    }

    private Object injectFields(final Map<Field, DependencyGraph.Node> fieldNodes,
                                final DependencyGraph.Traversal traversal, final List<RestrictedContainer> containers, final Object instance) {
        return Exceptions.wrap(String.format("setting %s fields", instance.getClass()), new Exceptions.Command<Object>() {
            public Object run() throws Exception {
                for (final Map.Entry<Field, DependencyGraph.Node> entry : fieldNodes.entrySet()) {
                    entry.getKey().set(instance, create(traversal, containers, entry.getValue())[0]);
                }

                return instance;
            }
        });
    }

    public Object[] create(final DependencyGraph.Traversal traversal, final List<RestrictedContainer> containers, final DependencyGraph.Node... nodes) {
        final Object[] values = new Object[nodes.length];

        for (int i = 0, limit = nodes.length; i < limit; i++) {
            final Object value = nodes[i].instance(traversal);

            if (value instanceof RestrictedContainer) {
                containers.add((RestrictedContainer) value);
            }

            values[i] = value;
        }

        return values;
    }

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
                                                  final ComponentDescriptor descriptor,
                                                  final ContextDefinition context,
                                                  final Class<?> declaringType,
                                                  final Map<Field, DependencyGraph.Node> nodes) {
        final List<ContextDefinition> consumed = new ArrayList<ContextDefinition>();

        processFields(declaringType, new FieldCommand() {
            public void process(final Field field) {
                consumed.add(injectDependency(false, traversal, container, descriptor, context.copy(), declaringType, new Dependency() {

                    public Class<?> type() {
                        return field.getType();
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
                                               final ComponentDescriptor descriptor,
                                               final ContextDefinition context,
                                               final Class<?> declaringType,
                                               final Dependency dependency) {
        final ComponentGroup componentGroup = dependency.annotation(ComponentGroup.class);
        final Class<?> dependencyType = findDependencyType(dependency.annotation(Component.class), dependency.type(), declaringType);
        final boolean mandatory = dependency.annotation(Optional.class) == null;

        assert descriptor != null : declaringType;
        final Annotation[] typeContext = neverNull(descriptor.annotations());
        final Annotation[] dependencyContext = neverNull(dependency.annotations());

        if (resolving) {
            traversal.resolving(declaringType, dependencyType, typeContext, dependencyContext);
        }

        final Annotation[] definitions = new Annotation[typeContext.length + dependencyContext.length];
        System.arraycopy(typeContext, 0, definitions, 0, typeContext.length);
        System.arraycopy(dependencyContext, 0, definitions, typeContext.length, dependencyContext.length);

        context.expand(definitions);

        final DependencyGraph.Node node;

        if (componentGroup != null) {
            if (!dependencyType.isArray()) {
                throw new ComponentContainer.ResolutionException("Group dependency %s of %s must be an array", dependencyType, declaringType);
            }

            final Class<?> itemType = dependencyType.getComponentType();
            if (itemType.isArray()) {
                throw new ComponentContainer.ResolutionException("Group dependency %s of %s must be an array of non-arrays", dependencyType, declaringType);
            }

            final Class<?> groupType = componentGroup.api() == null || componentGroup.api().length != 1 ? itemType : componentGroup.api()[0];

            if (!itemType.isAssignableFrom(groupType)) {
                throw new ComponentContainer.ResolutionException(
                        "The component type of dependency specified in the %s annotation is not assignable to the dependency type %s of %s",
                        dependencyType,
                        ComponentGroup.class,
                        declaringType);
            }

            node = container.resolveGroup(itemType, context, traversal, dependency.annotations());
        } else {
            node = resolve(dependencyType, new Resolution() {
                public ComponentContext context() {

                    // always reduce the context to what the component accepts to avoid leaking contextual information to the component that it may inadvertently use
                    // without explicitly declaring it as accepted
                    return context.accept(descriptor.acceptedContext()).create();
                }

                public ComponentContainer container() {
                    return container.container(context);
                }

                public DependencyGraph.Node regular() {
                    final ComponentDescriptor descriptor = container.describe(dependencyType, context);
                    return descriptor != null ? container.resolveComponent(dependencyType, context.accept(descriptor.acceptedContext()), traversal) : null;
                }

                public void handle(final RestrictedContainer container) {
                    // empty
                }
            });

        }

        dependency.set(new DependencyNode(mandatory, node, declaringType, dependencyType));

        return context;
    }

    private static Annotation[] neverNull(final Annotation[] array) {
        return array == null ? new Annotation[0] : array;
    }

    private Class<?> findDependencyType(final Component component, final Class<?> defaultType, Class<?> declaringType) {
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
     */
    private interface Dependency {

        /**
         * Returns the type by which the dependency is referred to.
         *
         * @return the type by which the dependency is referred to.
         */
        Class<?> type();

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
         * @return all annotations present at the reference.
         */
        Annotation[] annotations();

        /**
         * Sets the resolved value of the reference.
         *
         * @param node the future value of the field.
         */
        void set(DependencyGraph.Node node);
    }

    private static class DependencyNode implements DependencyGraph.Node {

        private final boolean mandatory;
        private final DependencyGraph.Node node;
        private final Class<?> declaringType;
        private final Class<?> dependencyType;

        public DependencyNode(final boolean mandatory, final DependencyGraph.Node node, final Class<?> declaringType, final Class<?> dependencyType) {
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
                                                                 Strings.arrayNotation(dependencyType),
                                                                 declaringType);
            } else {
                return instance;
            }
        }

        public ComponentContext context() {
            return node.context();
        }
    }

    private static class PathNode implements DependencyGraph.Node {
        public Class<?> type() {
            return DependencyPath.class;
        }

        public Object instance(final DependencyGraph.Traversal traversal) {
            return traversal.path();
        }

        public ComponentContext context() {
            return null;
        }
    }
}

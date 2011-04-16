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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.fluidity.composition.spi.ComponentMapping;
import org.fluidity.composition.spi.DependencyResolver;
import org.fluidity.foundation.Exceptions;
import org.fluidity.foundation.Strings;

/**
 * Finds, resolves and sets, using the given container, all @{@link Inject} annotated fields of an object that have not yet been set.
 *
 * @author Tibor Varga
 */
final class DependencyInjectorImpl implements DependencyInjector {

    public <T> T fields(final DependencyGraph.Traversal traversal,
                        final DependencyResolver container,
                        final ComponentMapping mapping,
                        final ContextDefinition context,
                        final T instance) {
        assert container != null;

        if (instance != null) {
            final Class<?> componentClass = instance.getClass();
            final Map<Field, DependencyGraph.Node> fieldNodes = new IdentityHashMap<Field, DependencyGraph.Node>();

            context.collect(resolveFields(traversal, container, mapping, context, componentClass, fieldNodes));

            injectFields(fieldNodes, traversal, instance);
        }

        return instance;
    }

    public DependencyGraph.Node constructor(final DependencyGraph.Traversal traversal,
                                            final DependencyResolver container,
                                            final ComponentMapping mapping,
                                            final ContextDefinition context,
                                            final Constructor<?> constructor) {
        final List<ContextDefinition> consumed = new ArrayList<ContextDefinition>();

        final Class<?> componentClass = constructor.getDeclaringClass();
        final Annotation[][] annotations = constructor.getParameterAnnotations();
        final Class[] types = constructor.getParameterTypes();
        final DependencyGraph.Node[] arguments = new DependencyGraph.Node[types.length];

        for (int i = 0, length = types.length; i < length; ++i) {
            final int index = i;
            consumed.add(injectDependency(traversal, container, mapping, context.copy(), componentClass, new Dependency() {

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
                    return annotations[index];
                }

                public void set(final DependencyGraph.Node node) {
                    arguments[index] = node;
                }
            }));
        }

        final Map<Field, DependencyGraph.Node> fields = new IdentityHashMap<Field, DependencyGraph.Node>();
        consumed.addAll(resolveFields(traversal, container, mapping, context, componentClass, fields));

        final ComponentContext componentContext = context.collect(consumed).create();

        return new DependencyGraph.Node() {
            public Class<?> type() {
                return componentClass;
            }

            public Object instance(final DependencyGraph.Traversal traversal) {
                return injectFields(fields, traversal, Exceptions.wrap(String.format("instantiating %s", componentClass), new Exceptions.Command<Object>() {
                    public Object run() throws Exception {
                        constructor.setAccessible(true);
                        traversal.instantiating(componentClass);
                        final Object component = constructor.newInstance(create(traversal, arguments));
                        traversal.instantiated(componentClass);
                        return component;
                    }
                }));
            }

            public ComponentContext context() {
                return componentContext;
            }
        };
    }

    private Object injectFields(final Map<Field, DependencyGraph.Node> fieldNodes,
                                final DependencyGraph.Traversal traversal,
                                final Object instance) {
        return Exceptions.wrap(String.format("setting %s fields", instance.getClass()), new Exceptions.Command<Object>() {
            public Object run() throws Exception {
                for (final Map.Entry<Field, DependencyGraph.Node> entry : fieldNodes.entrySet()) {
                    entry.getKey().set(instance, create(traversal, entry.getValue())[0]);
                }

                return instance;
            }
        });
    }

    public Object[] create(final DependencyGraph.Traversal traversal, final DependencyGraph.Node... nodes) {
        final Object[] values = new Object[nodes.length];

        for (int i = 0, limit = nodes.length; i < limit; i++) {
            values[i] = nodes[i].instance(traversal);
        }

        return values;
    }

    private List<ContextDefinition> resolveFields(final DependencyGraph.Traversal traversal,
                                                  final DependencyResolver container,
                                                  final ComponentMapping mapping,
                                                  final ContextDefinition context,
                                                  final Class<?> declaringType,
                                                  final Map<Field, DependencyGraph.Node> nodes) {
        final List<ContextDefinition> consumed = new ArrayList<ContextDefinition>();

        for (final Field field : declaringType.getDeclaredFields()) {
            field.setAccessible(true);

            if ((field.getModifiers() & Modifier.FINAL) != 0) {
                continue;
            }

            if (field.isAnnotationPresent(Inject.class)) {
                consumed.add(injectDependency(traversal, container, mapping, context.copy(), declaringType, new Dependency() {

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
        }

        final Class<?> ancestor = declaringType.getSuperclass();
        if (ancestor != null) {
            consumed.addAll(resolveFields(traversal, container, mapping, context, ancestor, nodes));
        }

        return consumed;
    }

    private ContextDefinition injectDependency(final DependencyGraph.Traversal traversal,
                                               final DependencyResolver container,
                                               final ComponentMapping mapping,
                                               final ContextDefinition context,
                                               final Class<?> declaringType,
                                               final Dependency dependency) {
        final ComponentGroup componentGroup = dependency.annotation(ComponentGroup.class);
        final Class<?> dependencyType = findDependencyType(dependency.annotation(Component.class), dependency.type(), declaringType);
        final boolean mandatory = dependency.annotation(Optional.class) == null;

        assert mapping != null : declaringType;
        final Annotation[] typeContext = neverNull(mapping.annotations());
        final Annotation[] dependencyContext = neverNull(dependency.annotations());

        final Annotation[] definitions = new Annotation[typeContext.length + dependencyContext.length];
        System.arraycopy(typeContext, 0, definitions, 0, typeContext.length);
        System.arraycopy(dependencyContext, 0, definitions, typeContext.length, dependencyContext.length);

        context.expand(definitions);

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

            dependency.set(new DependencyNode(mandatory, container.resolveGroup(itemType, context, traversal), declaringType, dependencyType));
        } else if (dependency.type() == ComponentContext.class) {

            // always reduce the context to what the component accepts to avoid leaking contextual information to the component that it may inadvertently use without explicitly declaring it as accepted
            dependency.set(new DependencyGraph.Node.Constant(ComponentContext.class, context.reduce(mapping.acceptedContext()).create(), null));
        } else {
            DependencyGraph.Node node = null;

            if (ComponentContainer.class.isAssignableFrom(dependencyType)) {
                final ComponentContainer value = container.container(context);
                node = new DependencyGraph.Node.Constant(value.getClass(), value, null);
            }

            if (node == null) {
                final ComponentMapping dependencyMapping = container.mapping(dependencyType, context);

                if (dependencyMapping != null) {
                    node = container.resolveComponent(dependencyType, context.reduce(dependencyMapping.acceptedContext()), traversal);
                }
            }

            dependency.set(new DependencyNode(mandatory, node, declaringType, dependencyType));
        }

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
    private static interface Dependency {

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
}

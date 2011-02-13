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

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.fluidity.composition.spi.ComponentMapping;
import org.fluidity.composition.spi.DependencyResolver;
import org.fluidity.foundation.Exceptions;
import org.fluidity.foundation.Strings;

/**
 * Finds, resolves and sets, using the given container, all @{@link Component} annotated fields of an object that have not yet been set.
 *
 * @author Tibor Varga
 */
final class DependencyInjectorImpl implements DependencyInjector {

    public <T> T[] injectFields(final DependencyResolver resolver, final ComponentMapping mapping, final ContextDefinition context, final T... instances) {
        assert resolver != null;

        if (instances != null) {
            for (final T instance : instances) {
                final Class<?> componentType = instance.getClass();
                context.collect(injectFields(resolver, mapping, context, instance, componentType, componentType));
            }
        }

        return instances;
    }

    public <T> T injectConstructor(final DependencyResolver resolver,
                                   final ComponentMapping mapping,
                                   final ContextDefinition context,
                                   final Constructor<T> constructor) {
        final List<ContextDefinition> consumed = new ArrayList<ContextDefinition>();

        final Class<?> componentType = constructor.getDeclaringClass();
        final Annotation[][] annotations = constructor.getParameterAnnotations();
        final Class[] types = constructor.getParameterTypes();
        final Object[] arguments = new Object[types.length];

        for (int i = 0, length = types.length; i < length; ++i) {
            final int index = i;
            consumed.add(injectDependency(resolver, mapping, context.copy(), componentType, componentType, new Dependency() {
                public Object itself() {
                    return null;
                }

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

                public void set(final Object value) {
                    arguments[index] = value;
                }
            }));
        }

        context.collect(consumed);

        return Exceptions.wrap(String.format("instantiating %s", constructor.getDeclaringClass()), new Exceptions.Command<T>() {
            public T run() throws Exception {
                constructor.setAccessible(true);
                return constructor.newInstance(arguments);
            }
        });
    }

    private <T> List<ContextDefinition> injectFields(final DependencyResolver resolver,
                                                     final ComponentMapping mapping,
                                                     final ContextDefinition context,
                                                     final T instance,
                                                     final Class<?> componentType,
                                                     final Class<?> declaringType) {
        final List<ContextDefinition> consumed = new ArrayList<ContextDefinition>();

        for (final Field field : declaringType.getDeclaredFields()) {
            field.setAccessible(true);

            final Exceptions.Command<?> fieldValue = new Exceptions.Command<Object>() {
                public Object run() throws Exception {
                    return field.get(instance);
                }
            };

            if ((field.getModifiers() & Modifier.FINAL) != 0 || Exceptions.wrap(String.format("getting %s", field), fieldValue) != null) {
                continue;
            }

            if (field.isAnnotationPresent(Component.class) || field.isAnnotationPresent(ComponentGroup.class)) {
                consumed.add(injectDependency(resolver, mapping, context.copy(), componentType, declaringType, new Dependency() {
                    public Object itself() {
                        return instance;
                    }

                    public Class<?> type() {
                        return field.getType();
                    }

                    public <T extends Annotation> T annotation(final Class<T> annotationClass) {
                        return field.getAnnotation(annotationClass);
                    }

                    public Annotation[] annotations() {
                        return field.getAnnotations();
                    }

                    public void set(final Object value) {
                        Exceptions.wrap(String.format("setting %s", field), new Exceptions.Command<Void>() {
                            public Void run() throws Exception {
                                field.set(instance, value);
                                return null;
                            }
                        });
                    }
                }));
            }
        }

        final Class<?> ancestor = declaringType.getSuperclass();
        if (ancestor != null) {
            consumed.addAll(injectFields(resolver, mapping, context, instance, componentType, ancestor));
        }

        return consumed;
    }

    private ContextDefinition injectDependency(final DependencyResolver resolver,
                                               final ComponentMapping mapping,
                                               final ContextDefinition context,
                                               final Class<?> componentType,
                                               final Class<?> declaringType,
                                               final Dependency dependency) {
        final ComponentGroup componentGroup = dependency.annotation(ComponentGroup.class);
        final Class<?> dependencyType = findDependencyType(dependency.annotation(Component.class), dependency.type(), declaringType);

        assert mapping != null : declaringType;
        final Annotation[] typeContext = neverNull(mapping.providedContext());

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

            final Object[] group = resolver.resolveGroup(groupType, context);
            dependency.set(group == null ? Array.newInstance(groupType, 0) : group);
        } else if (dependency.type() == ComponentContext.class) {
            dependency.set(context.reduce(mapping.contextSpecification(Context.class)).create());
        } else {
            Object value = null;

            if (dependencyType.isAssignableFrom(componentType)) {
                value = dependency.itself();
            }

            final Annotation[] dependencyContext = neverNull(dependency.annotations());

            final Annotation[] definitions = new Annotation[typeContext.length + dependencyContext.length];
            System.arraycopy(typeContext, 0, definitions, 0, typeContext.length);
            System.arraycopy(dependencyContext, 0, definitions, typeContext.length, dependencyContext.length);

            context.expand(definitions);

            if (value == null && ComponentContainer.class.isAssignableFrom(dependencyType)) {
                value = resolver.container(context);
            }

            if (value == null) {
                final ComponentMapping dependencyMapping = resolver.mapping(dependencyType);
                if (dependencyMapping != null) {
                    value = resolver.resolveComponent(dependencyType, context.reduce(dependencyMapping.contextSpecification(Context.class)));
                }
            }

            if (value == null && dependency.annotation(Optional.class) == null) {
                throw new ComponentContainer.ResolutionException("Dependency %s of %s cannot be satisfied",
                                                                 Strings.arrayNotation(dependencyType),
                                                                 declaringType);
            }

            dependency.set(value);
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
         * Returns the referring object itself.
         *
         * @return the referring object itself or <code>null</code> if there is none.
         */
        Object itself();

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
         * @param value the resolved value of the reference.
         */
        void set(Object value);
    }
}

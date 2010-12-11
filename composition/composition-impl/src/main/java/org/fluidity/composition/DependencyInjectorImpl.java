/*
 * Copyright (c) 2006-2010 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import org.fluidity.composition.spi.ContextChain;
import org.fluidity.composition.spi.ContextFactory;
import org.fluidity.composition.spi.DependencyInjector;
import org.fluidity.composition.spi.ReferenceChain;
import org.fluidity.foundation.ClassLoaderUtils;
import org.fluidity.foundation.Exceptions;
import org.fluidity.foundation.Reflection;

/**
 * Finds, resolves and sets, using the given container, all @{@link org.fluidity.composition.Component} annotated fields of an object that have not yet been
 * set.
 *
 * @author Tibor Varga
 */
final class DependencyInjectorImpl implements DependencyInjector {

    final ClassDiscovery discovery;

    public DependencyInjectorImpl(final ClassDiscovery discovery) {
        this.discovery = discovery;
    }

    public <T> T injectFields(final Resolver resolver, final Class<?> componentApi, final ComponentContext context, final T instance) {
        assert resolver != null;

        if (instance != null) {
            final Class<?> componentType = instance.getClass();
            injectFields(resolver, context, instance, componentApi, componentType, componentType);
        }

        return instance;
    }

    public Object[] injectConstructor(final Resolver resolver, final Class<?> componentApi, final ComponentContext context, final Constructor<?> constructor) {
        final Class<?> componentType = constructor.getDeclaringClass();
        final Annotation[][] annotations = constructor.getParameterAnnotations();
        final Class[] types = constructor.getParameterTypes();
        final Object[] arguments = new Object[types.length];

        for (int i = 0, length = types.length; i < length; ++i) {
            final int index = i;
            injectDependency(resolver, context, componentApi, componentType, componentType, new Dependency() {
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
            });
        }

        return arguments;
    }

    private <T> void injectFields(final Resolver resolver,
                                  final ComponentContext context,
                                  final T instance,
                                  final Class<?> componentApi,
                                  final Class<?> componentType,
                                  final Class<?> declaringType) {
        for (final Field field : declaringType.getDeclaredFields()) {
            final boolean accessible = Reflection.isAccessible(field);

            if (!accessible) {
                field.setAccessible(true);
            }

            try {
                final Exceptions.Command<?> fieldValue = new Exceptions.Command<Object>() {
                    public Object run() throws Exception {
                        return field.get(instance);
                    }
                };

                if ((field.getModifiers() & Modifier.FINAL) != 0 || Exceptions.wrap(String.format("getting %s", field), fieldValue) != null) {
                    continue;
                }

                if (field.isAnnotationPresent(Component.class) || field.isAnnotationPresent(ServiceProvider.class)) {
                    injectDependency(resolver, context, componentApi, componentType, declaringType, new Dependency() {
                        public Object itself() {
                            return instance;
                        }

                        public Class<?> type() {
                            return field.getType();
                        }

                        public <T extends Annotation> T annotation(Class<T> annotationClass) {
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
                    });
                }
            } finally {
                if (!accessible) {
                    field.setAccessible(accessible);
                }
            }
        }

        final Class<?> ancestor = declaringType.getSuperclass();
        if (ancestor != null) {
            injectFields(resolver, context, instance, componentApi, componentType, ancestor);
        }
    }

    private void injectDependency(final Resolver resolver,
                                  final ComponentContext context,
                                  final Class<?> componentApi,
                                  final Class<?> componentType,
                                  final Class<?> declaringType,
                                  final Dependency dependency) {
        final ContextChain contextChain = resolver.contextChain();
        final ContextFactory contextFactory = resolver.contextFactory();
        final ReferenceChain referenceChain = resolver.referenceChain();

        final ServiceProvider serviceProvider = dependency.annotation(ServiceProvider.class);
        final Class<?> dependencyType = findInterfaceType(dependency.annotation(Component.class), dependency.type());

        if (serviceProvider != null) {
            if (!dependencyType.isArray()) {
                throw new ComponentContainer.ResolutionException("Service provider dependency %s of %s must be an array", dependencyType, declaringType);
            }

            if (dependencyType.getComponentType().isArray()) {
                throw new ComponentContainer.ResolutionException("Service provider dependency %s of %s must be an array of non-arrays",
                                                                 dependencyType,
                                                                 declaringType);
            }

            final Class<?> providerType = serviceProvider.api() == null || serviceProvider.api().length != 1
                                          ? dependencyType.getComponentType()
                                          : serviceProvider.api()[0];

            if (!dependencyType.getComponentType().isAssignableFrom(providerType)) {
                throw new ComponentContainer.ResolutionException(
                        "The component type of dependency specified in the %s annotation is not assignable to the dependency type %s of %s",
                        dependencyType,
                        ServiceProvider.class,
                        declaringType);
            }

            final List<Object> list = new ArrayList<Object>();

            @SuppressWarnings("unchecked")
            final Class<Object>[] componentClasses = (Class<Object>[]) discovery.findComponentClasses(providerType,
                                                                                                      ClassLoaderUtils.findClassLoader(declaringType),
                                                                                                      false);

            for (final Class<Object> componentClass : componentClasses) {
                final Object component = resolver.resolve(componentClass, context);
                list.add(component == null ? resolver.create(componentClass, context) : component);
            }

            dependency.set(list.toArray((Object[]) Array.newInstance(providerType, list.size())));
        } else if (dependency.type() == ComponentContext.class) {
            dependency.set(contextChain.consumedContext(componentApi, componentType, context, referenceChain));
        } else {
            Object value = null;

            if (dependencyType.isAssignableFrom(componentType)) {
                value = dependency.itself();
            }

            if (value == null && ComponentContainer.class.isAssignableFrom(dependencyType)) {
                value = resolver.container(context);
            }

            if (value == null) {
                final ComponentContext extracted = contextFactory.extractContext(dependency.annotations());

                value = extracted == null ? resolver.resolve(dependencyType, context) : contextChain.nested(extracted, new ContextChain.Command<Object>() {
                    public Object run(final ComponentContext context) {
                        return resolver.resolve(dependencyType, context);
                    }
                });
            }

            if (value == null) {
                if (dependency.annotation(Optional.class) == null) {
                    throw new ComponentContainer.ResolutionException("Dependency %s of %s cannot be satisfied", dependencyType, declaringType);
                }

                dependency.set(value);
            } else {
                dependency.set(value);
            }
        }
    }

    private Class<?> findInterfaceType(final Component component, final Class<?> defaultType) {
        final Class<?> dependencyType = component == null ? Object.class : component.api();
        return dependencyType == Object.class ? defaultType : dependencyType;
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

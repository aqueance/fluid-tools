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

package org.fluidity.composition.container.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.security.AccessController;
import java.security.PrivilegedAction;
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
import org.fluidity.composition.container.AccessGuard;
import org.fluidity.composition.container.ContextDefinition;
import org.fluidity.composition.container.DependencyInjector;
import org.fluidity.composition.container.ResolvedNode;
import org.fluidity.composition.container.spi.ContextNode;
import org.fluidity.composition.container.spi.DependencyGraph;
import org.fluidity.composition.container.spi.DependencyResolver;
import org.fluidity.foundation.Deferred;
import org.fluidity.foundation.Exceptions;
import org.fluidity.foundation.Generics;
import org.fluidity.foundation.Lists;
import org.fluidity.foundation.Methods;
import org.fluidity.foundation.Proxies;
import org.fluidity.foundation.Security;
import org.fluidity.foundation.Strings;

import static org.fluidity.composition.ComponentContainer.ResolutionException;
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

    public AccessGuard<ComponentContainer> containerGuard() {
        return new AccessGuard<>("No dynamic dependencies allowed; use a ComponentFactory if you need such functionality");
    }

    public <T> T fields(final T instance,
                        final DependencyGraph.Traversal traversal,
                        final DependencyResolver container,
                        final ContextNode contexts,
                        final ContextDefinition context) {
        assert container != null;

        if (instance != null) {
            final Class<?> componentClass = instance.getClass();
            final Map<Field, DependencyGraph.Node> fields = new IdentityHashMap<>();
            final AccessGuard<ComponentContainer> guard = containerGuard();

            context.collect(resolveFields(traversal, container, contexts, context, componentClass, fields, guard));

            try {
                injectFields(fields, traversal, instance);
            } finally {
                guard.enable();
            }
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
                         final boolean explicit) throws ResolutionException, Exceptions.Wrapper {
        assert method != null;
        assert container != null;

        final Class<?> componentClass = method.getDeclaringClass();
        final Annotation[] methodAnnotations = method.getAnnotations();
        final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        final Type[] types = method.getGenericParameterTypes();
        final Object[] parameters = arguments == null ? new Object[types.length] : Arrays.copyOf(arguments, types.length);
        final boolean parameterized = Components.isParameterized(componentClass);

        final AccessGuard<ComponentContainer> guard = containerGuard();

        for (int i = 0, length = types.length; i < length; ++i) {
            final int index = i;
            if (parameters[index] == null && (explicit || contains(parameterAnnotations[index], Inject.class))) {
                injectDependency(false, traversal, container, contexts, context, componentClass, parameterized, guard, new Dependency() {
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

        guard.enable();

        final PrivilegedAction<Method> access = Security.setAccessible(method);
        return Methods.invoke(access == null ? method : AccessController.doPrivileged(access), component, parameters);
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

    public Constructor<?> findConstructor(final Class<?> componentClass) throws ResolutionException {
        Constructor<?> designated = null;

        final List<Constructor<?>> privateConstructors = new ArrayList<>();
        final List<Constructor<?>> packageConstructors = new ArrayList<>();
        final List<Constructor<?>> publicConstructors = new ArrayList<>();

        final Constructor<?>[] constructors = !Security.CONTROLLED
                                              ? componentClass.getDeclaredConstructors()
                                              : AccessController.doPrivileged((PrivilegedAction<Constructor<?>[]>) componentClass::getDeclaredConstructors);

        for (final Constructor<?> constructor : constructors) {
            if (!constructor.isSynthetic()) {
                if (designated == null) {
                    final int modifiers = constructor.getModifiers();

                    if (Modifier.isPublic(modifiers)) {
                        publicConstructors.add(constructor);
                    } else if (Modifier.isPrivate(modifiers)) {
                        privateConstructors.add(constructor);
                    } else if (!Modifier.isProtected(modifiers)) {
                        packageConstructors.add(constructor);
                    }

                }

                if (constructor.isAnnotationPresent(Inject.class)) {
                    if (designated != null) {
                        throw new ResolutionException("Multiple @%s annotated constructors found in %s",
                                                      Strings.formatClass(false, false, Inject.class),
                                                      Strings.formatClass(false, true, componentClass));
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
            return selectConstructor(publicConstructors, componentClass);
        } catch (final NoConstructorException dropped1) {
            try {
                return selectConstructor(packageConstructors, componentClass);
            } catch (final NoConstructorException dropped2) {

                // use compiler generated default constructor if present, complain otherwise
                if (privateConstructors.size() == 1 && Modifier.isPrivate(componentClass.getModifiers())) {
                    return selectConstructor(privateConstructors, componentClass);
                } else {
                    throw new ResolutionException("No suitable constructor found for %s", componentClass);
                }
            }
        }
    }

    public DependencyGraph.Node resolve(final Class<?> api, final AccessGuard<ComponentContainer> guard, final Resolution resolution) {
        assert api != null;
        if (api == ComponentContext.class) {
            return new ResolvedNode(ComponentContext.class, resolution.context(), null);
        } else if (api == DependencyInjector.class) {
            return new ResolvedNode(DependencyInjector.class, this, null);
        } else if (api == ComponentContainer.class) {
            return new ResolvedNode(ComponentContainer.class,
                                    Proxies.create(ComponentContainer.class, new RestrictedContainer(guard, resolution.container())),
                                    null);
        } else {
            return resolution.regular();
        }
    }

    private Constructor<?> selectConstructor(final List<Constructor<?>> constructors, final Class<?> componentClass) {
        switch (constructors.size()) {
        case 0:
            throw new NoConstructorException();
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
            throw new ResolutionException("Multiple constructors found for %s", componentClass);
        }
    }

    private static class NoConstructorException extends ResolutionException { }

    public DependencyGraph.Node constructor(final Class<?> api,
                                            final DependencyGraph.Traversal traversal,
                                            final DependencyResolver container,
                                            final ContextNode contexts,
                                            final ContextDefinition context,
                                            final Constructor<?> constructor) {
        final List<ContextDefinition> consumed = new ArrayList<>();

        final Class<?> componentClass = constructor.getDeclaringClass();

        final Generics.Parameters descriptor = Generics.describe(constructor);

        final Class[] params = constructor.getParameterTypes();
        final Annotation[] constructorAnnotations = Lists.notNull(Annotation.class, constructor.getAnnotations());

        final DependencyGraph.Node[] parameters = new DependencyGraph.Node[params.length];
        final boolean parameterized = Components.isParameterized(componentClass);

        final AccessGuard<ComponentContainer> guard = containerGuard();

        for (int i = 0, length = params.length; i < length; ++i) {
            final int index = i;
            consumed.add(injectDependency(true, traversal, container, contexts, context, componentClass, parameterized, guard, new Dependency() {
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

        final Map<Field, DependencyGraph.Node> fields = new IdentityHashMap<>();

        consumed.addAll(resolveFields(traversal, container, contexts, context, componentClass, fields, guard));

        final ComponentContext componentContext = context.accept(contexts.contextConsumer()).collect(consumed).create();

        return new DependencyGraph.Node() {
            public Class<?> type() {
                return componentClass;
            }

            public Object instance(final DependencyGraph.Traversal traversal) {
                try {
                    return injectFields(fields, traversal, instantiate(traversal));
                } finally {
                    guard.enable();
                }
            }

            public ComponentContext context() {
                return componentContext;
            }

            private Object instantiate(final DependencyGraph.Traversal traversal) {
                final Object[] arguments = arguments(constructor.getDeclaringClass(), traversal, parameters);

                final Object cached = container.cached(api, componentContext);

                if (cached == null) {
                    final PrivilegedAction<Constructor> access = Security.setAccessible((Constructor) constructor);
                    final Process<Object, Exception> action = () -> (access == null ? constructor : AccessController.doPrivileged(access)).newInstance(arguments);

                    traversal.instantiating(componentClass);

                    final Deferred.Label label = Deferred.label(() -> String.format("Invoking %s with %s", constructor, Strings.formatId(arguments)));
                    return traversal.instantiated(componentClass, Exceptions.wrap(label, ResolutionException.class, action));
                } else {
                    return cached;
                }
            }
        };
    }

    private Object injectFields(final Map<Field, DependencyGraph.Node> fields, final DependencyGraph.Traversal traversal, final Object instance) {
        final Deferred.Label label = Deferred.label(() -> String.format("Setting %s fields", Strings.formatClass(false, true, instance.getClass())));

        return Exceptions.wrap(label, ResolutionException.class, () -> {
            for (final Map.Entry<Field, DependencyGraph.Node> entry : fields.entrySet()) {
                final Field field = entry.getKey();

                // only unassigned fields get assigned
                if (field.get(instance) == null) {
                    field.set(instance, arguments(field.getDeclaringClass(), traversal, entry.getValue())[0]);
                }
            }

            return instance;
        });
    }

    public Object[] arguments(final Class<?> type, final DependencyGraph.Traversal traversal, final DependencyGraph.Node... nodes) {
        final Object[] values = new Object[nodes.length];

        for (int i = 0, limit = nodes.length; i < limit; i++) {
            final Object value = nodes[i].instance(traversal);

            if (value instanceof ComponentContext) {
                final Component.Reference reference = ((ComponentContext) value).qualifier(Component.Reference.class, null);

                if (reference != null) {
                    final Type[] variables = Generics.unresolved((reference).type());

                    if (variables != null) {
                        throw new ResolutionException("Parameterized component of type %s with unresolved type variables: %s",
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
     * Processes a field. Used in {@link DependencyInjectorImpl#processFields(Class, DependencyInjectorImpl.FieldCommand)}.
     *
     * @author Tibor Varga
     */
    @FunctionalInterface
    private interface FieldCommand {

        /**
         * Processes a field.
         *
         * @param field the field to process.
         */
        void process(Field field);
    }

    private void processFields(final Class<?> type, final FieldCommand command) {
        final Field[] fields = AccessController.doPrivileged((PrivilegedAction<Field[]>) () -> {
            final Field[] _fields = type.getDeclaredFields();
            AccessibleObject.setAccessible(_fields, true);
            return _fields;
        });

        for (final Field field : fields) {
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
                                                  final Map<Field, DependencyGraph.Node> nodes,
                                                  final AccessGuard<ComponentContainer> guard) {
        final List<ContextDefinition> consumed = new ArrayList<>();
        final boolean parameterized = Components.isParameterized(declaringType);

        processFields(declaringType, field -> consumed.add(injectDependency(false, traversal, container, contexts, context, declaringType, parameterized, guard, new Dependency() {
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
        })));

        return consumed;
    }

    private ContextDefinition injectDependency(final boolean resolving,
                                               final DependencyGraph.Traversal traversal,
                                               final DependencyResolver container,
                                               final ContextNode contexts,
                                               final ContextDefinition original,
                                               final Class<?> declaringType,
                                               final boolean parameterized,
                                               final AccessGuard<ComponentContainer> containerGuard,
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
                    throw new ResolutionException("Group dependency %s of %s must be an array",
                                                  Strings.formatClass(false, true, dependencyType),
                                                  declaringType);
                }

                if (itemType.isArray()) {
                    throw new ResolutionException("Group dependency %s of %s must be an array of non-arrays",
                                                  Strings.formatClass(false, true, dependencyType),
                                                  declaringType);
                }

                final Class<?> groupType = componentGroup.api().length > 0 ? componentGroup.api()[0] : null;

                if (groupType != null) {
                    if (componentGroup.api().length > 1) {
                        throw new ResolutionException("Multiple component group APIs specified for dependency %s of %s: %s",
                                                      Strings.formatClass(false, true, dependencyType),
                                                      declaringType,
                                                      Strings.describeAnnotation(false, componentGroup));
                    }

                    if (!itemType.isAssignableFrom(groupType)) {
                        throw new ResolutionException("The specified component type is not assignable to the dependency type %s of %s: %s",
                                                      Strings.formatClass(false, true, dependencyType),
                                                      declaringType,
                                                      Strings.describeAnnotation(false, componentGroup));
                    }
                }

                node = container.resolveGroup(groupType == null ? itemType : groupType, downstream.advance(reference, true), traversal, reference);
            } else {
                node = resolve(dependencyType, containerGuard, new Resolution() {
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
                        return container.container(original.copy().expand(definitions)).observed(traversal.observer());
                    }

                    public DependencyGraph.Node regular() {
                        return interceptors.replace(container,
                                                    downstream,
                                                    traversal,
                                                    reference,
                                                    container.resolveComponent(dependencyType, downstream, traversal, reference));
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
            throw new ResolutionException("Unresolved type parameter [%s] in a dependency of %s", reference, declaringType);
        }

        final Class<?> defaultType = Generics.rawType(reference);
        final Class<?>[] types = component == null ? new Class<?>[] { defaultType } : component.api();

        switch (types.length) {
        case 0:
            return defaultType;
        case 1:
            return types[0];
        default:
            throw new ResolutionException("Multiple types specified for dependency %s of %s", defaultType, declaringType);
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
                throw new ResolutionException("Dependency %s of %s cannot be satisfied", Strings.formatClass(true, true, dependencyType), declaringType);
            } else {
                return instance;
            }
        }

        public ComponentContext context() {
            return node.context();
        }
    }

    /**
     * @author Tibor Varga
     */
    static final class RestrictedContainer implements InvocationHandler {

        private final AccessGuard<ComponentContainer> guard;
        private final ComponentContainer delegate;

        RestrictedContainer(final AccessGuard<ComponentContainer> guard, final ComponentContainer delegate) {
            assert delegate != null;
            this.guard = guard;
            this.delegate = delegate;
        }

        public Object invoke(final Object proxy, final Method method, final Object[] arguments) throws Throwable {
            final Class<?> returnType = method.getReturnType();

            if (ComponentContainer.class.isAssignableFrom(returnType) && !guard.enabled()) {
                final ComponentContainer container = (ComponentContainer) method.invoke(delegate, arguments);
                return container == delegate ? proxy : Proxies.create(returnType, new RestrictedContainer(guard, container));
            } else {
                return method.invoke(guard.access(delegate), arguments);
            }
        }
    }
}

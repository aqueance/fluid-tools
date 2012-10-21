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

package org.fluidity.composition;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.foundation.Generics;
import org.fluidity.foundation.Lists;
import org.fluidity.foundation.Strings;
import org.fluidity.foundation.Utility;

/**
 * This utility is used to determine the <a href="http://code.google.com/p/fluid-tools/wiki/UserGuide#Definitions">component interfaces and group
 * interfaces</a> a class should be bound to when it is {@linkplain ComponentContainer.Registry#bindComponent(Class, Class[]) bound} to a dependency injection
 * container.
 * <p/>
 * Informally, the logic employed attempts to imitate the obvious choices one would make at a cursory glance. With regard to component interfaces, these are:
 * <ul>
 * <li>no {@link Component @Component} annotation is necessary, it is merely there to automate the component binding and to provide parameters thereto</li>
 * <li>a class implementing no interface and extending no superclass is bound to itself</li>
 * <li>a class directly implementing some interfaces is bound to those interfaces</li>
 * </ul>
 * <p/>
 * With regard to component groups, a component class will be bound to all component groups that it inherits via its class ancestry and any interface
 * implemented by it and any of its ancestors.
 * <p/>
 * The formal rules for discovering the component interfaces are described by the following recursive algorithm. The term <em>original class</em> denotes the
 * class the algorithm was originally invoked with, while the term <em>the class</em> denotes the class currently under examination by the recursive algorithm.
 * <ol>
 * <li>If the class has no {@link Component @Component} annotation, the algorithm returns the <u>original class</u>, unless the class implements {@link
 * ComponentFactory}, in which case the algorithm terminates with an {@linkplain
 * ComponentContainer.BindingException error}.</li>
 * <li>If the class is annotated with <code>@Component</code> and the <code>@Component(api = &hellip;)</code> parameter is given with a non-empty array, the
 * algorithm ignores the annotated class and repeats for each class specified in <code>@Component(api = {&hellip;})</code>. However, if any of these classes are
 * themselves <code>@Component</code> annotated classes with no <code>@Component(automatic = false)</code>, or if the current class does not extend or
 * implement either all of the listed classes and interfaces or <code>ComponentFactory</code>, the algorithm terminates with an {@linkplain
 * ComponentContainer.BindingException error}.</li>
 * <li>If the super class is annotated with <code>@Component</code> but with no <code>@Component(automatic = false)</code>, the algorithm terminates with an
 * {@linkplain ComponentContainer.BindingException error}.</li>
 * <li>If the class implements no interfaces directly and its super class is <code>Object</code> then the algorithm returns the <u>original class</u>.</li>
 * <li>If the class implements no interfaces directly and its super class is not <code>Object</code> then this algorithm repeats for the super class.</li>
 * <li>If the class directly implements one or more interfaces then the algorithm returns <u>those interfaces</u>.</li>
 * </ol>
 * <p/>
 * Once the above algorithm has completed, the list of group interfaces is calculated using the algorithm below for each class returned by the algorithm.
 * <p/>
 * The rules for component group interface discovery are described by the following recursive algorithm. The algorithm is triggered individually for the
 * original class and each component interfaces identified by the recursive algorithm above, and the terms <em>original class</em> and <em>this class</em> will
 * be understood relative to each individual invocation of the algorithm.
 * <ol>
 * <li>If the class is annotated with {@link ComponentGroup @ComponentGroup} with a non-empty <code>@ComponentGroup(api = &hellip;)</code> parameter, the
 * algorithm returns <u>the classes specified</u> therein. However, if the class does not extend or implement either all of those classes and interfaces or
 * <code>ComponentFactory</code>, the algorithm terminates with an {@linkplain ComponentContainer.BindingException error}.</li>
 * <li>If the class is annotated with <code>@ComponentGroup</code> with no <code>@ComponentGroup(api = &hellip;)</code> parameter, then
 * <ol>
 * <li>if the class is an interface, the algorithm returns the <u>original class</u>.</li>
 * <li>if the class directly implements interfaces, the algorithm repeats for those interfaces and if they produce a list of groups the algorithm returns
 * that list, otherwise the algorithm returns <u>the interfaces</u> themselves.</li>
 * <li>if the class is not final, the algorithm returns the <u>original class</u>.</li>
 * </ol></li>
 * <li>The algorithm repeats for each directly implemented interface and the super class.</li>
 * <li>If the class is <code>Object</code>, the algorithm returns <u>nothing</u>.</li>
 * </ol>
 * <p/>
 * Once the above algorithm has completed, the following adjustments are made to the final result:
 * <ul>
 * <li>the component class or, in case of a <code>ComponentFactory</code> implementation, the classes referenced in its <code>@Component(api = &hellip;)</code>
 * parameter, are added to the component interface list <em>if</em> component group interfaces have been identified for that class or those classes;</li>
 * <li>all component interfaces that are also a component group interface are removed from the list of component interfaces.</li>
 * </ul>
 * <h3>Usage</h3>
 * <pre>
 * public interface <span class="hl3">MyComponent</span> {
 *   &hellip;
 * }
 * </pre>
 * <pre>
 * {@linkplain Component @Component}
 * final class <span class="hl2">MyComponentImpl</span> implements <span class="hl3">MyComponent</span> {
 *
 *     <span class="hl2">MyComponentImpl</span>() {
 *         System.out.printf("My component interfaces: %s%n", <span class="hl1">Components</span>.inspect(getClass());
 *     }
 * }
 * </pre>
 *
 * @author Tibor Varga
 * @see Component
 * @see ComponentGroup
 * @see ComponentFactory
 */
public final class Components extends Utility {

    private Components() { }

    static {
        assert Component.class.isAnnotationPresent(Retention.class);
        assert Component.class.getAnnotation(Retention.class).value() == RetentionPolicy.RUNTIME;
        assert Component.class.isAnnotationPresent(Target.class);
        assert Arrays.asList(Component.class.getAnnotation(Target.class).value()).contains(ElementType.TYPE);
        assert !Component.class.isAnnotationPresent(Inherited.class);

        assert ComponentGroup.class.isAnnotationPresent(Retention.class);
        assert ComponentGroup.class.getAnnotation(Retention.class).value() == RetentionPolicy.RUNTIME;
        assert ComponentGroup.class.isAnnotationPresent(Target.class);
        assert Arrays.asList(ComponentGroup.class.getAnnotation(Target.class).value()).contains(ElementType.TYPE);
        assert !ComponentGroup.class.isAnnotationPresent(Inherited.class);
    }

    /**
     * Returns the list of component interfaces that should resolve to the given component class, and for each component interface a list of component group
     * interfaces that the component is a member of through that component interface.
     *
     * @param componentClass the component class to inspect.
     * @param restrictions   optional list of component interfaces to use instead of whatever the algorithm would find.
     * @param <T>            the component class.
     *
     * @return an object listing all component interfaces and the set of component group interfaces for each.
     *
     * @throws ComponentContainer.BindingException
     *          thrown when an error condition is identified during inspection.
     */
    public static <T> Interfaces inspect(final Class<T> componentClass, final Class<? super T>... restrictions) throws ComponentContainer.BindingException {
        if (componentClass == null) {
            throw new IllegalStateException("Component class to inspect is null");
        }

        final Map<Type, Set<Class<?>>> interfaceMap = new LinkedHashMap<Type, Set<Class<?>>>();
        final Set<Class<?>> path = new LinkedHashSet<Class<?>>();

        if (restrictions != null && restrictions.length > 0) {
            final boolean factory = isFactory(componentClass);
            final Map<Type, Set<Class<?>>> map = new LinkedHashMap<Type, Set<Class<?>>>();

            for (final Class<?> api : restrictions) {
                if (!factory && !api.isAssignableFrom(componentClass)) {
                    throw new ComponentContainer.BindingException("Class %s refers to incompatible component interface %s", componentClass, api);
                }

                final Type declared = findDeclaredInterface(componentClass, api);
                interfaces(declared, declared, map, path, false, false);
                filter(componentClass, interfaceMap, map);
            }
        } else {
            interfaces(componentClass, componentClass, interfaceMap, path, false, true);
        }

        // handle boxing by means of treating the primitive type as the box type
        if (interfaceMap.containsKey(componentClass) && componentClass.getName().startsWith("java.lang.")) {
            try {
                final Field field = componentClass.getField("TYPE");

                if (field.getType() == Class.class) {
                    interfaceMap.put((Class<?>) field.get(null), interfaceMap.get(componentClass));
                }
            } catch (final NoSuchFieldException e) {
                // ignore
            } catch (final IllegalAccessException e) {
                assert false : e;
            }
        }

        // post-processing of component interfaces found
        final Set<Specification> interfaces = new LinkedHashSet<Specification>();

        for (final Map.Entry<Type, Set<Class<?>>> entry : interfaceMap.entrySet()) {
            final Type type = entry.getKey();
            final Class<?> api = Generics.rawType(type);

            if (isFactory(api)) {
                throw new ComponentContainer.BindingException("Component interface for %s is the factory interface itself: %s", componentClass, type);
            }

            if (isParameterized(componentClass)) {
                checkTypeParameters(componentClass, type);
            }

            interfaces.add(new Specification(api, entry.getValue()));
        }

        return new Interfaces(componentClass, Lists.asArray(Specification.class, interfaces));
    }

    /**
     * Tells if the given class accepts the {@link Component.Reference} annotation as context.
     *
     * @param type the class to check for parameterization.
     *
     * @return <code>true</code> if the given class accepts the {@link Component.Reference} annotation as context; <code>false</code> otherwise.
     */
    public static boolean isParameterized(final Class<?> type) {
        final Component.Context context = type.getAnnotation(Component.Context.class);

        if (context != null) {
            for (final Class<? extends Annotation> accepted : context.value()) {
                if (Component.Reference.class.isAssignableFrom(accepted)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static void checkTypeParameters(final Class<?> type, final Type api) {
        for (final Type check : type.getGenericInterfaces()) {
            checkTypeParameters(type, api, check);
            checkTypeParameters(Generics.rawType(check), api);
        }

        final Type check = type.getGenericSuperclass();

        if (check != null && check != Object.class) {
            checkTypeParameters(type, api, check);
            checkTypeParameters(Generics.rawType(check), api);
        }
    }

    private static void checkTypeParameters(final Class<?> type, final Type api, final Type check) {
        if (Generics.rawType(api).isAssignableFrom(Generics.rawType(check))) {
            if (api instanceof Class) {
                final TypeVariable[] parameters = ((Class) api).getTypeParameters();
                if (parameters.length > 0) {
                    throw new ComponentContainer.BindingException("Component type %s specifies parameter(s) %s from its parameterized component interface: %s",
                                                                  type.getName(),
                                                                  Arrays.toString(parameters),
                                                                  api);
                }
            } else if (check instanceof ParameterizedType) {
                final List<Type> specified = new ArrayList<Type>();
                for (final Type argument : ((ParameterizedType) check).getActualTypeArguments()) {
                    if (!(argument instanceof TypeVariable)) {
                        specified.add(argument);
                    }
                }

                if (!specified.isEmpty()) {
                    throw new ComponentContainer.BindingException("Component type %s specifies parameter(s) %s from its parameterized component interface: %s",
                                                                  type.getName(),
                                                                  specified,
                                                                  Arrays.toString(Generics.rawType(api).getTypeParameters()));
                }
            }
        }
    }

    private static Type findDeclaredInterface(final Class<?> type, final Class<?> api) {
        if (api == type) {
            return api;
        }

        for (final Type checked : type.getGenericInterfaces()) {
            if (api == Generics.rawType(checked)) {
                return checked;
            }
        }

        final Type checked = type.getGenericSuperclass();

        if (api == Generics.rawType(checked)) {
            return checked;
        }

        return api;
    }

    private static void interfaces(final Type actual,
                                   final Type type,
                                   final Map<Type, Set<Class<?>>> output,
                                   final Set<Class<?>> path,
                                   final boolean reference,
                                   final boolean resolve) {
        final Map<Type, Set<Class<?>>> interfaceMap = new LinkedHashMap<Type, Set<Class<?>>>();
        final Class<?> checked = Generics.rawType(type);

        if (path.contains(checked)) {
            interfaceMap.put(actual, groups(checked));
        } else {
            path.add(checked);

            try {
                final boolean anonymous = checked.isAnonymousClass();
                final boolean factory = isFactory(checked);
                final Component component = resolve ? checked.getAnnotation(Component.class) : null;
                final boolean automatic = component != null && component.automatic();

                if (component == null && !anonymous) {
                    if (factory) {
                        throw new ComponentContainer.BindingException("Factory %s is missing @%s", checked, Component.class.getName());
                    } else {
                        interfaceMap.put(actual, groups(checked));
                    }
                } else if (automatic && reference) {
                    throw new ComponentContainer.BindingException("Class %s referred to is itself an automatically bound component", checked.getName());
                } else if (automatic && (Modifier.isAbstract(checked.getModifiers()) || checked.isInterface())) {
                    throw new ComponentContainer.BindingException("Class %s is abstract", checked.getName());
                } else {
                    final Class<?>[] interfaces = component == null ? null : component.api();

                    if (interfaces != null && interfaces.length > 0) {
                        for (final Class<?> api : interfaces) {
                            if (!factory && !api.isAssignableFrom(checked)) {
                                throw new ComponentContainer.BindingException("Class %s refers to incompatible %s", checked.getName(), api);
                            }

                            final Type declared = findDeclaredInterface(checked, api);
                            interfaces(declared, declared, interfaceMap, path, true, resolve);
                        }
                    } else {
                        final Type[] direct = checked.getGenericInterfaces();

                        if (direct.length > 0) {
                            for (final Type api : direct) {
                                interfaceMap.put(api, groups(Generics.rawType(api)));
                            }
                        } else {
                            final Type superClass = checked.getGenericSuperclass();

                            if (superClass != Object.class && superClass != null) {
                                interfaces(actual, superClass, interfaceMap, path, true, resolve);
                            } else {
                                interfaceMap.put(actual, groups(checked));
                            }
                        }
                    }
                }
            } finally {
                path.remove(checked);
            }
        }

        filter(type, output, interfaceMap);
    }

    private static void filter(final Type componentClass, final Map<Type, Set<Class<?>>> output, final Map<Type, Set<Class<?>>> interfaceMap) {
        interfaceMap.keySet().removeAll(allGroups(interfaceMap.values()));

        final Class<?> type = Generics.rawType(componentClass);
        final Set<Class<?>> groups = groups(type);

        final ComponentGroup groupAnnotation = type.getAnnotation(ComponentGroup.class);
        if (groupAnnotation != null && groupAnnotation.api().length > 0) {
            interfaceMap.keySet().removeAll(groups);
        } else {
            for (final Set<Class<?>> set : interfaceMap.values()) {
                groups.removeAll(set);
            }
        }

        if (!groups.isEmpty() && !interfaceMap.containsKey(componentClass)) {
            output.put(componentClass, groups);
        }

        output.putAll(interfaceMap);
    }

    private static Set<Class<?>> allGroups(final Collection<Set<Class<?>>> list) {
        final Set<Class<?>> allGroups = new HashSet<Class<?>>();

        for (final Set<Class<?>> set : list) {
            allGroups.addAll(set);
        }

        return allGroups;
    }

    private static Set<Class<?>> groups(final Class<?> type) {
        final boolean factory = isFactory(type);

        final Set<Class<?>> groups = new LinkedHashSet<Class<?>>();

        if (type == Object.class) {
            return groups;
        }

        final ComponentGroup annotation = type.getAnnotation(ComponentGroup.class);

        if (annotation != null) {
            final Class<?>[] interfaces = annotation.api();

            if (interfaces.length > 0) {
                if (factory) {
                    groups.addAll(Arrays.asList(interfaces));
                } else {
                    for (final Class<?> api : interfaces) {
                        if (!api.isAssignableFrom(type)) {
                            throw new ComponentContainer.BindingException("Class %s refers to incompatible component interface %s", type, api);
                        }

                        groups.add(api);
                    }
                }
            } else {
                if (type.isInterface()) {
                    groups.add(type);
                } else {
                    groups.addAll(groups(type.getSuperclass()));

                    final Class<?>[] direct = type.getInterfaces();

                    if (direct.length > 0) {
                        for (final Class<?> api : direct) {
                            groups.addAll(groups(api));
                        }

                        if (groups.isEmpty()) {
                            groups.addAll(Arrays.asList(direct));
                        }
                    } else if (!Modifier.isFinal(type.getModifiers())) {
                        groups.add(type);
                    }
                }
            }
        }

        if (groups.isEmpty()) {
            for (final Class<?> api : type.getInterfaces()) {
                groups.addAll(groups(api));
            }

            final Class<?> superClass = type.getSuperclass();
            if (superClass != null) {
                groups.addAll(groups(superClass));
            }
        }

        return groups;
    }

    private static boolean isFactory(final Class<?> componentClass) {
        return ComponentFactory.class.isAssignableFrom(componentClass);
    }

    /**
     * List of <a href="http://code.google.com/p/fluid-tools/wiki/UserGuide#Definitions">component interfaces</a> and corresponding
     * <a href="http://code.google.com/p/fluid-tools/wiki/UserGuide#Definitions">component group interfaces</a> for a component class. Instances are produced
     * by {@link Components#inspect(Class, Class[]) Components.inspect()}.
     * <h3>Usage</h3>
     * See {@link Components}
     */
    public static final class Interfaces {

        /**
         * The component class.
         */
        public final Class<?> implementation;

        /**
         * The component interfaces and component group interfaces associated therewith.
         */
        public final Specification[] api;

        private final int hash;

        public Interfaces(final Class<?> implementation, final Specification[] interfaces) {
            this.implementation = implementation;
            this.api = interfaces == null ? new Specification[0] : interfaces;
            this.hash = calculateHash();
        }

        @Override
        public String toString() {
            final Lists.Delimited text = Lists.delimited();

            for (final Components.Specification specification : api) {
                text.add(Strings.formatClass(true, true, specification.api));

                if (specification.groups.length > 0) {
                    text.append(" group ").append(Strings.formatObject(false, true, specification.groups));
                }
            }

            return (api.length > 1 ? text.surround("[]") : text).toString();
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Interfaces that = (Interfaces) o;
            return implementation.equals(that.implementation) && Arrays.equals(api, that.api);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        private int calculateHash() {
            int result = implementation.hashCode();
            result = 31 * result + Arrays.hashCode(api);
            return result;
        }
    }

    /**
     * A <a href="http://code.google.com/p/fluid-tools/wiki/UserGuide#Definitions">component interface</a> and the associated
     * <a href="http://code.google.com/p/fluid-tools/wiki/UserGuide#Definitions">component group interfaces</a>. A list of these objects is returned as part of
     * the {@link Interfaces} by {@link Components#inspect(Class, Class[]) Components.inspect()}.
     * <h3>Usage</h3>
     * See {@link Components}
     */
    public static final class Specification {

        /**
         * The component interface
         */
        public final Class<?> api;

        /**
         * The group interfaces associated with the component interfaces
         */
        public final Class<?>[] groups;

        public Specification(final Class<?> api, final Collection<Class<?>> groups) {
            this.api = api;
            this.groups = groups == null ? new Class<?>[0] : Lists.asArray(Class.class, groups);
        }

        @Override
        public String toString() {
            return String.format("%s: %s", api, Strings.formatObject(false, true, groups));
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Specification that = (Specification) o;
            return api.equals(that.api) && Arrays.equals(groups, that.groups);
        }

        @Override
        public int hashCode() {
            int result = api.hashCode();
            result = 31 * result + Arrays.hashCode(groups);
            return result;
        }
    }
}

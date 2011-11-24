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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.foundation.Strings;
import org.fluidity.foundation.Utilities;

/**
 * Implements the component interface discovery algorithm for classes added to an {@link OpenComponentContainer}. This algorithm produces a list of component
 * interfaces for a given class and for each such interface, a list of component group interfaces for any component group that the given class is a member of
 * through that component interface.
 * <p/>
 * The rules for discovering the component interfaces are described by the following recursive algorithm:
 * <ol>
 * <li>If the class has no {@link Component @Component} annotation, the algorithm returns the <u>class itself</u>, unless the class implements {@link
 * org.fluidity.composition.spi.ComponentFactory}, in which case the algorithm terminates with an {@link OpenComponentContainer.BindingException error}.</li>
 * <li>If the class is annotated with <code>@Component</code> and the <code>@Component(api = ...)</code> parameter is given with a non-empty array, the
 * algorithm ignores the annotated class and repeats for each class specified <code>@Component(api = {...})</code>. However, if any of these classes are
 * themselves <code>@Component</code> annotated classes with no <code>@Component(automatic = false)</code>, or if the current class does not extend or
 * implement either all of the listed classes and interfaces or <code>ComponentFactory</code>, the algorithm terminates with an
 * {@link OpenComponentContainer.BindingException error}.</li>
 * <li>If the super class is annotated with <code>@Component</code> but with no <code>@Component(automatic = false)</code>, the algorithm terminates with an
 * {@link OpenComponentContainer.BindingException error}.</li>
 * <li>If the class implements no interfaces directly and its super class is <code>Object</code> then the algorithm returns the <u>annotated class</u>.</li>
 * <li>If the class implements no interfaces directly and its super class is not <code>Object</code> then this algorithm repeats for the super class with the
 * difference that the algorithm returns this class rather than the super class.</li>
 * <li>If the class directly implements one or more interfaces then the algorithm returns <u>those interfaces</u>.</li>
 * </ol>
 * <p/>
 * Once the above algorithm has completed, the following takes place:
 * <ul>
 * <li>For each class returned, the list of group interfaces is calculated using the algorithm below.</li>
 * <li>Any component interface that is also a component group interface is removed from the list of component interfaces.</li>
 * </ul>
 * <p/>
 * For the component class itself and each component interface found above, the rules for component group interface discovery are described by the following
 * recursive algorithm:
 * <ol>
 * <li>If the class is annotated with {@link ComponentGroup @ComponentGroup} with a non-empty <code>@ComponentGroup(api = ...)</code> parameter, the
 * algorithm returns <u>the classes specified</u> therein. However, if the class does not extend or implement either all of those classes and interfaces or
 * <code>ComponentFactory</code>, the algorithm terminates with an {@link OpenComponentContainer.BindingException error}.</li>
 * <li>If the class is annotated with <code>@ComponentGroup</code> with no <code>@ComponentGroup(api = ...)</code> parameter, then
 * <ol>
 * <li>if the class is an interface, the algorithm returns <u>the annotated class</u>.</li>
 * <li>if the class directly implements interfaces, the algorithm repeats for those interfaces and if they produce a list of groups the algorithm returns
 * that list, otherwise the algorithm returns <u>the interfaces</u> themselves.</li>
 * <li>if the class is not final, the algorithm returns <u>the annotated class</u>.</li>
 * </ol></li>
 * <li>The algorithm repeats for each directly implemented interface and the super class.</li>
 * <li>If the class is <code>Object</code>, the algorithm returns <u>nothing</u>.</li>
 * </ol>
 * <p/>
 * Once the above algorithm has completed, the following adjustments are made to the final result:
 * <ul>
 * <li>the component class or, in case of a <code>ComponentFactory</code> implementation, the classes referenced in its <code>@Component(api = ...)</code>
 * parameter, are added to the component interface list <em>if</em> component group interfaces have been identified for that class or those classes.</li>
 * </ul>
 *
 * @author Tibor Varga
 * @see Component
 * @see ComponentGroup
 * @see ComponentFactory
 */
public final class Components extends Utilities {

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
        assert ComponentGroup.class.isAnnotationPresent(Inherited.class);
    }

    /**
     * Returns the list of component interfaces that should resolve to the given component class, with a list of component group interfaces for each component
     * interface that the component may be a member of through that component interface.
     *
     * @param componentClass the component class to inspect.
     * @param restrictions   optional list of interfaces to use instead of whatever the algorithm would find.
     * @param <T>            the component class.
     *
     * @return an object listing all component interfaces and the set of component group interfaces for each.
     *
     * @throws OpenComponentContainer.BindingException
     *          thrown when an error condition is identified during inspection.
     */
    public static <T> Interfaces inspect(final Class<T> componentClass, final Class<? super T>... restrictions) throws OpenComponentContainer.BindingException {
        final Map<Class<?>, Set<Class<?>>> interfaceMap = new LinkedHashMap<Class<?>, Set<Class<?>>>();

        final Set<Class<?>> path = new LinkedHashSet<Class<?>>();

        if (restrictions != null && restrictions.length > 0) {
            final boolean factory = isFactory(componentClass);
            final Map<Class<?>, Set<Class<?>>> map = new LinkedHashMap<Class<?>, Set<Class<?>>>();

            for (final Class<?> api : restrictions) {
                if (!factory && !api.isAssignableFrom(componentClass)) {
                    throw new OpenComponentContainer.BindingException("Class %s refers to incompatible component interface %s", componentClass, api);
                }

                interfaces(api, api, map, path, false);
                filter(componentClass, interfaceMap, map);
            }
        } else {
            interfaces(componentClass, componentClass, interfaceMap, path, false);
        }

        // handle boxing by means of treating the primitive type same as the box type
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

        for (final Map.Entry<Class<?>, Set<Class<?>>> entry : interfaceMap.entrySet()) {
            final Class<?> type = entry.getKey();

            if (ComponentFactory.class.isAssignableFrom(type)) {
                throw new OpenComponentContainer.BindingException("Component interface for %s is the factory interface itself: %s", componentClass, type);
            }

            interfaces.add(new Specification(type, entry.getValue()));
        }

        return new Interfaces(componentClass, interfaces.toArray(new Specification[interfaces.size()]));
    }

    private static void interfaces(final Class<?> actual,
                                   final Class<?> checked,
                                   final Map<Class<?>, Set<Class<?>>> output,
                                   final Set<Class<?>> path,
                                   final boolean reference) {
        final Map<Class<?>, Set<Class<?>>> interfaceMap = new LinkedHashMap<Class<?>, Set<Class<?>>>();

        if (path.contains(checked)) {
            interfaceMap.put(actual, groups(checked));
        } else {
            path.add(checked);

            try {
                final boolean anonymous = checked.isAnonymousClass();
                final boolean factory = isFactory(checked);
                final Component component = checked.getAnnotation(Component.class);
                final boolean automatic = component != null && component.automatic();

                if (component == null && !anonymous) {
                    if (factory) {
                        throw new OpenComponentContainer.BindingException("Factory %s is missing @%s", checked, Component.class.getName());
                    } else {
                        interfaceMap.put(actual, groups(checked));
                    }
                } else if (automatic && reference) {
                    throw new OpenComponentContainer.BindingException("Class %s referred to is itself an automatically bound component", checked.getName());
                } else if (automatic && (Modifier.isAbstract(checked.getModifiers()) || checked.isInterface())) {
                    throw new OpenComponentContainer.BindingException("Class %s is abstract", checked.getName());
                } else {
                    final Class<?>[] interfaces = component == null ? null : component.api();

                    if (interfaces != null && interfaces.length > 0) {
                        for (final Class<?> api : interfaces) {
                            if (!factory && !api.isAssignableFrom(checked)) {
                                throw new OpenComponentContainer.BindingException("Class %s refers to incompatible %s", checked.getName(), api);
                            }

                            interfaces(api, api, interfaceMap, path, true);
                        }
                    } else {
                        final Class<?>[] direct = checked.getInterfaces();

                        if (direct.length > 0) {
                            for (final Class<?> api : direct) {
                                interfaceMap.put(api, groups(api));
                            }
                        } else {
                            final Class<?> superClass = checked.getSuperclass();

                            if (superClass != Object.class && superClass != null) {
                                interfaces(actual, superClass, interfaceMap, path, true);
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

        filter(checked, output, interfaceMap);
    }

    private static void filter(final Class<?> componentClass, final Map<Class<?>, Set<Class<?>>> output, final Map<Class<?>, Set<Class<?>>> interfaceMap) {
        interfaceMap.keySet().removeAll(allGroups(interfaceMap));

        final Set<Class<?>> groups = groups(componentClass);

        final ComponentGroup groupAnnotation = componentClass.getAnnotation(ComponentGroup.class);
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

    private static Set<Class<?>> allGroups(final Map<Class<?>, Set<Class<?>>> interfaceMap) {
        final Set<Class<?>> allGroups = new HashSet<Class<?>>();

        for (final Set<Class<?>> set : interfaceMap.values()) {
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
                            throw new OpenComponentContainer.BindingException("Class %s refers to incompatible component interface %s", type, api);
                        }

                        groups.add(api);
                    }
                }
            } else {
                if (type.isInterface()) {
                    groups.add(type);
                } else {
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
     * List of component interfaces and corresponding component group interfaces for a component implementation. Instances are produced by {@link
     * Components#inspect(Class, Class[])}.
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

        Interfaces(final Class<?> implementation, final Specification[] interfaces) {
            this.implementation = implementation;
            this.api = interfaces == null ? new Specification[0] : interfaces;
            this.hash = calculateHash();
        }

        @Override
        public String toString() {
            final StringBuilder text = new StringBuilder();

            boolean multiple = false;
            for (final Components.Specification specification : api) {
                final Class<?> type = specification.api;

                if (text.length() > 0) {
                    text.append(", ");
                    multiple = true;
                }

                text.append(Strings.arrayNotation(type));

                if (specification.groups.length > 0) {
                    text.append(" group ").append(Arrays.toString(specification.groups));
                }
            }

            return (multiple ? text.insert(0, '[').append(']') : text).toString();
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
     * A component interface and the associated component group interfaces. A list of these objects is returned as part of the {@link Interfaces} by {@link
     * Components#inspect(Class, Class[])}.
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

        Specification(final Class<?> api, final Collection<Class<?>> groups) {
            this.api = api;
            this.groups = groups == null ? new Class<?>[0] : groups.toArray(new Class<?>[groups.size()]);
        }

        @Override
        public String toString() {
            return String.format("%s: %s", api, Arrays.toString(groups));
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

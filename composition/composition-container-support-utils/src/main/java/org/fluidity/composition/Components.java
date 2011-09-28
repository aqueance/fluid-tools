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
import org.fluidity.composition.spi.ComponentVariantFactory;
import org.fluidity.composition.spi.CustomComponentFactory;

/**
 * Component and component group interface related tools.
 *
 * @author Tibor Varga
 */
public final class Components {

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

    private Components() {
        throw new UnsupportedOperationException("No instance allowed");
    }

    /**
     * Returns the list of component interfaces the given component class can be bound to with a list of component group interfaces for each component
     * interface that the component may be a member of.
     * <p/>
     * The rules for component interface discovery are described by the following recursive algorithm:
     * <ol>
     * <li>If the class has no @{@link Component} annotation, the algorithm returns the queried class, unless the class implements {@link
     * org.fluidity.composition.spi.CustomComponentFactory} or {@link org.fluidity.composition.spi.ComponentVariantFactory}, in which case the algorithm flags
     * an error.</li>
     * <li>If the class is annotated with @{@link Component} and the @{@link Component#api()} parameter is given, the algorithm ignores the annotated
     * class and repeats for each class specified therein. However, if any of these classes are themselves @{@link Component} classes with @{@link
     * Component#automatic()} not set to <code>false</code>, or if the class does not extend or implement all of those classes and interfaces or {@link
     * org.fluidity.composition.spi.CustomComponentFactory} or {@link org.fluidity.composition.spi.ComponentVariantFactory}, the algorithm flags an error.</li>
     * <li>If the super class is annotated with @{@link Component} and its @{@link Component#automatic()} is not set to <code>false</code>, the algorithm flags
     * an error.</li>
     * <li>If the class implements no interfaces directly and its super class is {@link Object} then the algorithm returns the annotated class.</li>
     * <li>If the class implements no interfaces directly and its super class is not {@link Object} then this algorithm repeats for the super class.</li>
     * <li>If the class directly implements one or more interfaces then the algorithm returns those interfaces.</li>
     * <li>For each class returned, the list of group interfaces is calculated using the algorithm below.</li>
     * <li>Any component interface that is also a component group interface is removed from the list of component interfaces.</li>
     * </ol>
     * <p/>
     * For each component interface and the component class, the rules for component group interface discovery are described by the following recursive
     * algorithm:
     * <ol>
     * <li>If the class is annotated with @{@link ComponentGroup} with a @{@link ComponentGroup#api()} parameter, the algorithm
     * returns the classes specified therein. However, if the class does not extend or implement all of those classes and interfaces or {@link
     * org.fluidity.composition.spi.CustomComponentFactory} or {@link org.fluidity.composition.spi.ComponentVariantFactory}, the algorithm flags an error.</li>
     * <li>If the class is annotated with @{@link ComponentGroup} with no @{@link ComponentGroup#api()} parameter, then
     * <ol>
     * <li>if the class is an interface, the algorithm returns the annotated class.</li>
     * <li>if the class directly implements interfaces, the algorithm repeats for those interfaces and if they produce a list of groups the algorithm returns
     * that list, otherwise the algorithm returns the interfaces themselves.</li>
     * <li>if the class is not final, the algorithm returns the annotated class.</li>
     * </ol</li>
     * <li>The algorithm repeats for each directly implemented interface and the super class.</li>
     * <li>If the class is {@link Object}, the algorithm returns nothing.</li>
     * </ol>
     * <p/>
     * Once the above algorithms have completed, the following adjustments are made to the final result:
     * <ul>
     * <li>the component class or, in case of a {@link
     * org.fluidity.composition.spi.CustomComponentFactory} or {@link org.fluidity.composition.spi.ComponentVariantFactory} implementation, the classes
     * referenced in its @{@link Component#api()} parameter, is/are added to the component interface list if component group interfaces have been identified for
     * that class or those classes.</li>
     * </ul>
     *
     * @param componentClass the component class to inspect.
     * @param restrictions   optional list of interfaces to use instead of whatever the algorithm would find.
     *
     * @return an object listing all component interfaces and the set of component group interfaces for each.
     *
     * @throws org.fluidity.composition.ComponentContainer.BindingException
     *          thrown when an error condition is identified during inspection.
     */
    public static <T> Interfaces inspect(final Class<T> componentClass, final Class<? super T>... restrictions) throws ComponentContainer.BindingException {
        final Map<Class<?>, Set<Class<?>>> interfaceMap = new LinkedHashMap<Class<?>, Set<Class<?>>>();

        final Set<Class<?>> path = new LinkedHashSet<Class<?>>();

        if (restrictions != null && restrictions.length > 0) {
            final boolean factory = isFactory(componentClass);
            final Map<Class<?>, Set<Class<?>>> map = new LinkedHashMap<Class<?>, Set<Class<?>>>();

            for (final Class<?> api : restrictions) {
                if (!factory && !api.isAssignableFrom(componentClass)) {
                    throw new ComponentContainer.BindingException("Class %s refers to incompatible component interface %s", componentClass, api);
                }

                interfaces(api, api, map, path, false);
                filter(componentClass, interfaceMap, map);
            }
        } else {

            // call the algorithm
            interfaces(componentClass, componentClass, interfaceMap, path, false);
        }

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
                throw new ComponentContainer.BindingException("Component interface for %s is the factory interface itself: %s", componentClass, type);
            }

            interfaces.add(new Specification(type, entry.getValue()));
        }

        final Set<Class<?>> allGroups = new HashSet<Class<?>>();

        for (final Set<Class<?>> set : interfaceMap.values()) {
            allGroups.addAll(set);
        }

        final Component component = componentClass.getAnnotation(Component.class);
        return new Interfaces(componentClass,
                              component != null && !component.automatic(),
                              component == null ? allGroups.isEmpty() : !component.primary(),
                              interfaces.toArray(new Specification[interfaces.size()]));
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
                        throw new ComponentContainer.BindingException("Factory class %s is missing @%s", checked, Component.class);
                    } else {
                        interfaceMap.put(actual, groups(checked));
                    }
                } else if (automatic && reference) {
                    throw new ComponentContainer.BindingException("Class referred to by %s, %s, is also an automatically bound component", actual.getName(),
                                                                  checked.getName());
                } else if (automatic && (Modifier.isAbstract(checked.getModifiers()) || checked.isInterface())) {
                    throw new ComponentContainer.BindingException("Class %s is abstract", checked);
                } else {
                    final Class<?>[] interfaces = component == null ? null : component.api();

                    if (interfaces != null && interfaces.length > 0) {
                        for (final Class<?> api : interfaces) {
                            if (!factory && !api.isAssignableFrom(checked)) {
                                throw new ComponentContainer.BindingException("Class %s refers to incompatible component interface %s", checked.getName(), api.getName());
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
        final Set<Class<?>> allGroups = new HashSet<Class<?>>();

        for (final Set<Class<?>> set : interfaceMap.values()) {
            allGroups.addAll(set);
        }

        interfaceMap.keySet().removeAll(allGroups);

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
        return CustomComponentFactory.class.isAssignableFrom(componentClass) || ComponentVariantFactory.class.isAssignableFrom(componentClass);
    }

    /**
     * List of component interfaces and a flag that tells whether automatic processing should ignore this list or not.
     */
    public static final class Interfaces {

        public final boolean ignored;
        public final boolean fallback;
        public final Class<?> implementation;
        public final Specification[] api;

        private final int hash;

        Interfaces(final Class<?> implementation, final boolean ignored, final boolean fallback, final Specification[] interfaces) {
            this.ignored = ignored;
            this.fallback = fallback;
            this.implementation = implementation;
            this.api = interfaces == null ? new Specification[0] : interfaces;
            this.hash = calculateHash();
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
            return fallback == that.fallback && ignored == that.ignored && implementation.equals(that.implementation) && Arrays.equals(api, that.api);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        private int calculateHash() {
            int result = (ignored ? 1 : 0);
            result = 31 * result + (fallback ? 1 : 0);
            result = 31 * result + implementation.hashCode();
            result = 31 * result + Arrays.hashCode(api);
            return result;
        }
    }

    /**
     * A component interface and the associated component group interfaces.
     */
    public static final class Specification {

        public final Class<?> api;
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

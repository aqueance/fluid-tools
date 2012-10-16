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

package org.fluidity.foundation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Utility methods to access parameterized type information.
 */
public final class Generics extends Utility {

    private static final Annotation[] NO_ANNOTATION = new Annotation[0];

    private Generics() { }

    /**
     * Returns the raw type, i.e., the class, corresponding to the given parameterized type.
     *
     * @param type the parameterized type.
     *
     * @return the raw type or <code>null</code>.
     */
    public static Class<?> rawType(final Type type) {
        if (type instanceof Class) {
            return (Class) type;
        } else if (type instanceof ParameterizedType) {
            return rawType(((ParameterizedType) type).getRawType());
        } else if (type instanceof GenericArrayType) {
            final Class<?> componentClass = rawType(arrayComponentType(type));

            if (componentClass != null) {
                return Array.newInstance(componentClass, 0).getClass();
            }
        } else if (type instanceof TypeVariable) {
            final Type[] bounds = ((TypeVariable) type).getBounds();
            return bounds != null && bounds.length == 1 ? rawType(bounds[0]) : Object.class;
        } else if (type instanceof WildcardType) {
            final WildcardType wildcard = (WildcardType) type;

            final Type[] lowerBounds = wildcard.getLowerBounds();
            final Type[] upperBounds = wildcard.getUpperBounds();

            return lowerBounds.length == 1 ? rawType(lowerBounds[0]) : upperBounds.length == 1 ? rawType(upperBounds[0]) : Object.class;
        }

        return null;
    }

    /**
     * Returns the type parameter at the given index in the list of type parameters for the given type.
     *
     * @param type  the class or parameterized type.
     * @param index the index.
     *
     * @return the type parameter at the given index or <code>null</code> if the type is not a class or parameterized type, or the index is out of range.
     */
    public static Type typeParameter(final Type type, final int index) {
        if (type instanceof ParameterizedType) {
            final Type[] arguments = ((ParameterizedType) type).getActualTypeArguments();

            if (arguments != null && arguments.length > index) {
                final Type argument = arguments[index];
                return argument instanceof WildcardType ? rawType(rawType(((ParameterizedType) type).getRawType()).getTypeParameters()[index]) : argument;
            }
        } else if (type instanceof Class) {
            final TypeVariable[] parameters = ((Class) type).getTypeParameters();

            if (parameters != null && parameters.length > index) {
                final Type[] bounds = parameters[index].getBounds();
                return bounds.length == 1 ? bounds[0] : Object.class;
            }
        }

        return null;
    }

    /**
     * Returns the parameterized array component type of the given parameterized type.
     *
     * @param type the parameterized type.
     *
     * @return the array component type or <code>null</code> if the given type is not an array.
     */
    public static Type arrayComponentType(final Type type) {
        if (type instanceof GenericArrayType) {
            return ((GenericArrayType) type).getGenericComponentType();
        } else if (type instanceof Class && ((Class) type).isArray()) {
            return ((Class) type).getComponentType();
        }

        return null;
    }

    /**
     * Resolves a type variable using the given base type. The base type must either be a parameterized type or a generic array with a parameterized component
     * type, and must have all its type parameters resolved.
     *
     * @param reference the base type to resolve the type variable against.
     * @param variable  the type variable to resolve.
     *
     * @return the resolved type variable if resolution was possible, <code>null</code> otherwise.
     */
    static Type resolve(final Type reference, final TypeVariable variable) {
        if (reference instanceof ParameterizedType) {
            final String name = variable.getName();

            final TypeVariable[] parameters = rawType((reference)).getTypeParameters();
            for (int i = 0, limit = parameters.length; i < limit; i++) {
                if (name.equals(parameters[i].getName())) {
                    return typeParameter(reference, i);
                }
            }
        } else if (reference instanceof Class) {
            final String name = variable.getName();

            for (final TypeVariable parameter : ((Class) reference).getTypeParameters()) {
                if (name.equals(parameter.getName())) {
                    return rawType(parameter);
                }
            }
        } else if (reference instanceof GenericArrayType) {
            return resolve(((GenericArrayType) reference).getGenericComponentType(), variable);
        }

        return null;
    }

    private static Collection<Type> unresolved(final Type reference, Collection<Type> list) {
        if (reference instanceof Class) {
            return list;
        } else if (reference instanceof ParameterizedType) {
            final ParameterizedType original = (ParameterizedType) reference;

            for (final Type argument : original.getActualTypeArguments()) {
                unresolved(argument, list);
            }

            return list;
        } else if (reference instanceof GenericArrayType) {
            return unresolved(arrayComponentType(reference), list);
        } else if (reference instanceof TypeVariable || reference instanceof WildcardType) {
            list.add(reference);
            return list;
        } else {
            assert false : reference;
            return list;
        }
    }

    /**
     * Returns a list of unresolved type variables in the given type.
     *
     * @param reference the type to scan for unresolved type variables and wild card types.
     *
     * @return an array of {@link TypeVariable} or {@link WildcardType} objects, or <code>null</code> if no unresolved variable was found.
     */
    public static Type[] unresolved(final Type reference) {
        final Collection<Type> list = unresolved(reference, new ArrayList<Type>());
        return list.isEmpty() ? null : Lists.asArray(list, Type.class);
    }

    /**
     * Resolves all type variables of the <code>outbound</code> parameter using the <code>inbound</code> type as base type. The <code>inbound</code>
     * type must have all its parameters resolved.
     *
     * @param inbound  the base type to resolve type variables against.
     * @param outbound the outbound type whose type variables are to be resolved.
     *
     * @return a type with all type variables resolved or <code>null</code> if no resolution was possible.
     */
    public static Type propagate(final Type inbound, final Type outbound) {
        if (outbound instanceof Class) {
            return outbound;
        } else if (outbound instanceof ParameterizedType) {
            final ParameterizedType original = (ParameterizedType) outbound;

            final Type[] arguments = original.getActualTypeArguments();
            for (int i = 0, limit = arguments.length; i < limit; i++) {
                arguments[i] = propagate(inbound, arguments[i]);
            }

            return new ParameterizedTypeImpl(original, arguments);
        } else if (outbound instanceof GenericArrayType) {
            return new GenericArrayTypeImpl(propagate(inbound, arrayComponentType(outbound)));
        } else if (outbound instanceof TypeVariable) {
            return resolve(inbound, (TypeVariable) outbound);
        } else if (outbound instanceof WildcardType) {
            final WildcardType original = (WildcardType) outbound;

            final Type[] lowerBounds = original.getLowerBounds();
            final Type[] upperBounds = original.getUpperBounds();

            return lowerBounds.length == 1 ? lowerBounds[0] : upperBounds.length == 1 ? upperBounds[0] : Object.class;
        } else {
            assert false : outbound;
            return null;
        }
    }

    static String identity(final Type type) {
        if (type instanceof Class) {
            return String.format("%s@%d", Strings.printClass(false, true, (Class) type), System.identityHashCode(type));
        } else if (type instanceof ParameterizedType) {
            final Strings.Listing output = Strings.delimited(",");

            for (final Type parameter : ((ParameterizedType) type).getActualTypeArguments()) {
                output.append(identity(parameter));
            }

            return String.format("%s<%s>", identity(((ParameterizedType) type).getRawType()), output);
        } else if (type instanceof GenericArrayType) {
            return String.format("%s[]", identity(((GenericArrayType) type).getGenericComponentType()));
        } else if (type instanceof TypeVariable) {
            final Strings.Listing output = Strings.delimited(",");

            for (final Type bound : ((TypeVariable) type).getBounds()) {
                output.append(identity(bound));
            }

            return output.toString();
        } else if (type instanceof WildcardType) {
            final Strings.Listing lower = Strings.delimited(",");

            for (final Type bound : ((WildcardType) type).getLowerBounds()) {
                lower.append(identity(bound));
            }

            final Strings.Listing upper = Strings.delimited(",");

            for (final Type bound : ((WildcardType) type).getUpperBounds()) {
                upper.append(identity(bound));
            }

            return String.format("%s:%s", lower, upper);
        }

        assert false : type;
        return null;
    }

    static String toString(final Type argument) {
        return argument instanceof Class ? Strings.printClass(true, (Class) argument) : String.valueOf(argument);
    }

    /**
     * Returns the type that the given <code>specific</code> class provides as a specialization of the <code>generic</code> type. The {@linkplain
     * #rawType(Type) raw} type of the returned type will be <code>generic</code>.
     *
     * @param specific the class that is assumed to specialize the given <code>generic</code> type.
     * @param generic  the generic type that is expected to be specialized by the given <code>specific</code>.
     *
     * @return the type of the <code>specific</code> class that specializes the <code>generic</code> type; or <code>null</code> if the <code>specific</code>
     *         class is not a specialization of the <code>generic</code> type.
     */
    public static Type specializedType(final Class specific, final Class generic) {
        return specializedType(specific, generic, specific, generic);
    }

    private static Type specializedType(final Type specific, final Type generic, final Class<?> rawSpecific, final Class rawGeneric) {
        if (rawGeneric == rawSpecific) {
            return specific;
        } else {
            if (rawGeneric.isAssignableFrom(rawSpecific)) {
                final boolean specificInterface = rawSpecific.isInterface();
                final Class<?> superclass = rawSpecific.getSuperclass();

                if (!specificInterface && rawGeneric.isAssignableFrom(superclass)) {
                    if (rawGeneric == superclass) {
                        return rawSpecific.getGenericSuperclass();
                    } else {
                        return specializedType(rawSpecific.getGenericSuperclass(), generic, superclass, rawGeneric);
                    }
                } else if (specificInterface && rawGeneric.isAssignableFrom(Object.class)) {
                    return generic;
                } else {
                    final Class<?>[] interfaces = rawSpecific.getInterfaces();
                    final Type[] types = rawSpecific.getGenericInterfaces();

                    for (int i = 0, limit = types.length; i < limit; i++) {
                        final Class<?> api = interfaces[i];

                        if (rawGeneric == api) {
                            return types[i];
                        } else if (rawGeneric.isAssignableFrom(api)) {
                            return specializedType(types[i], generic, rawType(types[i]), rawGeneric);
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Performs compatibility checks on the full generic type of a reference and the type of an object being assigned to it. This is a generalization of {@link
     * Class#isAssignableFrom(Class)} to {@linkplain Type generic} types.
     *
     * @param reference the reference to which an object is being assigned.
     * @param type      the type of the object being assigned to the <code>reference</code>.
     *
     * @return <code>true</code> if the <code>type</code> is assignable to the
     */
    public static boolean isAssignable(final Type reference, final Type type) {
        return isAssignable(reference, type, rawType(reference), rawType(type));
    }

    private static boolean isAssignable(final Type reference, final Type type, final Class<?> rawReference, final Class<?> rawType) {
        if (rawType == null || !rawReference.isAssignableFrom(rawType)) {
            return false;
        } else if (rawReference.isArray() && rawType.isArray()) {
            return isAssignable(reference instanceof GenericArrayType ? ((GenericArrayType) reference).getGenericComponentType() : rawReference.getComponentType(),
                                type instanceof GenericArrayType ? ((GenericArrayType) type).getGenericComponentType() : rawType.getComponentType(),
                                rawReference.getComponentType(),
                                rawType.getComponentType());
        } else if (rawReference.isArray()) {
            return false;
        } else if (rawType.isArray()) {
            return rawReference == Object.class;
        } else if (reference instanceof WildcardType) {
            for (final Type bound : ((WildcardType) reference).getLowerBounds()) {
                if (!isAssignable(type, bound, rawType, rawType(bound))) {
                    return false;
                }
            }

            for (final Type bound : ((WildcardType) reference).getUpperBounds()) {
                if (!isAssignable(bound, type, rawType(bound), rawType)) {
                    return false;
                }
            }

            return true;
        } else if (rawReference != rawType) {
            return isAssignable(reference, specializedType(type, reference, rawType, rawReference));
        } else if (reference instanceof Class) {
            if (type instanceof Class) {
                return true;
            } else if (type instanceof ParameterizedType) {
                for (final TypeVariable variable : rawReference.getTypeParameters()) {
                    final Type resolved = resolve(type, variable);
                    final Class<?> rawResolved = rawType(resolved);

                    for (final Type bound : variable.getBounds()) {
                        if (!isAssignable(bound, resolved, rawType(bound), rawResolved)) {
                            return false;
                        }
                    }
                }

                return true;
            }
        } else if (reference instanceof ParameterizedType) {
            final Type[] referenceArguments = ((ParameterizedType) reference).getActualTypeArguments();

            if (type instanceof Class) {
                final TypeVariable[] typeParameters = rawType.getTypeParameters();

                for (int i = 0, limit = typeParameters.length; i < limit; i++) {
                    for (final Type bound : typeParameters[i].getBounds()) {
                        if (!isAssignable(referenceArguments[i], bound)) {
                            return false;
                        }
                    }
                }

                return true;
            } else if (type instanceof ParameterizedType) {
                //noinspection ConstantConditions
                assert rawReference == rawType : String.format("%s != %s", toString(reference), toString(type));
                final Type[] typeArguments = ((ParameterizedType) type).getActualTypeArguments();

                for (int i = 0, limit = typeArguments.length; i < limit; i++) {
                    if (!isAssignable(referenceArguments[i], typeArguments[i])) {
                        return false;
                    }
                }

                return true;
            }
        } else if (reference instanceof TypeVariable && type instanceof TypeVariable) {
            return true;    // we don't have enough information to distinguish between the variables
        }

        return false;
    }

    private static class GenericArrayTypeImpl implements GenericArrayType {
        private final Type componentType;

        GenericArrayTypeImpl(final Type componentType) {
            this.componentType = componentType;
        }

        public Type getGenericComponentType() {
            return componentType;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || !(o instanceof GenericArrayType)) {
                return false;
            }

            final GenericArrayType that = (GenericArrayType) o;
            return componentType.equals(that.getGenericComponentType());
        }

        @Override
        public int hashCode() {
            return componentType.hashCode();
        }

        @Override
        public String toString() {
            return String.format("%s[]", Generics.toString(componentType));
        }
    }

    private static class ParameterizedTypeImpl implements ParameterizedType {

        private ParameterizedType original;
        private final Type[] arguments;

        ParameterizedTypeImpl(final ParameterizedType original, final Type[] arguments) {
            this.arguments = arguments;
            this.original = original;
        }

        public Type[] getActualTypeArguments() {
            return arguments;
        }

        public Type getRawType() {
            return original.getRawType();
        }

        public Type getOwnerType() {
            return original.getOwnerType();
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || !(o instanceof ParameterizedType)) {
                return false;
            }

            final ParameterizedType that = (ParameterizedType) o;
            return Arrays.equals(arguments, that.getActualTypeArguments()) && original.getRawType().equals(that.getRawType()) && original.getOwnerType() == null
                   ? that.getOwnerType() == null
                   : original.getOwnerType().equals(that.getOwnerType());
        }

        @Override
        public int hashCode() {
            int result = original.getRawType().hashCode();
            result = 31 * result + (original.getOwnerType() == null ? 0 : original.getOwnerType().hashCode());
            result = 31 * result + Arrays.hashCode(arguments);
            return result;
        }

        @Override
        public String toString() {
            final Strings.Listing parameters = Strings.delimited();

            for (final Type argument : arguments) {
                parameters.add(Generics.toString(argument));
            }

            return String.format("%s<%s>", Generics.toString(getRawType()), parameters);
        }
    }

    /**
     * Creates and returns a constructor parameters descriptor that works around http://bugs.sun.com/view_bug.do?bug_id=5087240.
     *
     * @param constructor the constructor to describe the parameters of.
     *
     * @return a constructor parameters descriptor.
     */
    public static Parameters describe(final Constructor<?> constructor) {
        final Class<?> type = constructor.getDeclaringClass();
        final Class[] params = constructor.getParameterTypes();
        final Type[] types = constructor.getGenericParameterTypes();

        final int hidden = params.length - types.length;

        int levels = -1;   // top-level class is never static and yet it is an enclosing class
        for (Class enclosing = type; enclosing != null; enclosing = enclosing.getEnclosingClass()) {
            if (!Modifier.isStatic(enclosing.getModifiers())) {
                ++levels;
            }
        }

        final int nesting = levels;

        /*
         * http://bugs.sun.com/view_bug.do?bug_id=5087240:
         *
         * Inner class constructor generic types array does not contain the enclosing classes and the closure context.
         * The enclosing classes are on the beginning of the params array while the closure context are on the end.
         *
         * TODO: test this thoroughly on various JVMs
         */
        if (hidden > 0) {
            for (int i = nesting; i < params.length - hidden; ++i) {
                if (Generics.rawType(types[i - nesting]) != params[i]) {
                    throw new IllegalStateException(String.format("Could not match parameter types of %s constructor: classes: %s, types: %s on %s %s version %s virtual machine for %s Java %s",
                                                                  type,
                                                                  Arrays.toString(params),
                                                                  Arrays.toString(types),
                                                                  System.getProperty("java.vm.vendor"),
                                                                  System.getProperty("java.vm.name"),
                                                                  System.getProperty("java.vm.version"),
                                                                  System.getProperty("java.vendor"),
                                                                  System.getProperty("java.version")));
                }
            }
        }

        final Annotation[][] parameterAnnotations = constructor.getParameterAnnotations();

        return new Parameters() {

            public Type genericType(final int index) {
                final int nested = index - nesting;
                return hidden == 0 ? types[index] : nested < 0 || nested >= types.length ? params[index] : types[nested];
            }

            public Annotation[] getAnnotations(final int index) {
                final int nested = index - nesting;
                return nested < 0 || nested >= parameterAnnotations.length ? NO_ANNOTATION : parameterAnnotations[nested];
            }
        };
    }

    /**
     * Works around http://bugs.sun.com/view_bug.do?bug_id=5087240.
     *
     * @author Tibor Varga
     */
    public interface Parameters {

        /**
         * Returns the generic parameter type at the given index.
         *
         * @param index the parameter index.
         *
         * @return the generic parameter type at the given index.
         */
        Type genericType(int index);

        /**
         * Returns the list of annotations specified for the parameter at the given index.
         *
         * @param index the parameter index.
         *
         * @return the list of annotations specified for the parameter at the given index.
         */
        Annotation[] getAnnotations(int index);
    }
}

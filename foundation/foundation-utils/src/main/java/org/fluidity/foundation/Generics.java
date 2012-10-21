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
        return Lists.asArray(unresolved(reference, new ArrayList<Type>()), Type.class, false);
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
            final Lists.Delimited output = Lists.delimited(",");

            for (final Type parameter : ((ParameterizedType) type).getActualTypeArguments()) {
                output.append(identity(parameter));
            }

            return String.format("%s<%s>", identity(((ParameterizedType) type).getRawType()), output);
        } else if (type instanceof GenericArrayType) {
            return String.format("%s[]", identity(((GenericArrayType) type).getGenericComponentType()));
        } else if (type instanceof TypeVariable) {
            final Lists.Delimited output = Lists.delimited(",");

            for (final Type bound : ((TypeVariable) type).getBounds()) {
                output.append(identity(bound));
            }

            return output.toString();
        } else if (type instanceof WildcardType) {
            final Lists.Delimited lower = Lists.delimited(",");

            for (final Type bound : ((WildcardType) type).getLowerBounds()) {
                lower.append(identity(bound));
            }

            final Lists.Delimited upper = Lists.delimited(",");

            for (final Type bound : ((WildcardType) type).getUpperBounds()) {
                upper.append(identity(bound));
            }

            return String.format("%s:%s", lower, upper);
        }

        assert false : type;
        return null;
    }

    /**
     * Returns a textual representation of the given type with complete type information.
     *
     * @param argument the generic type to convert to String.
     * @param qualified if <code>true</code>, the type's fully qualified name is used, otherwise its simple name is used.
     *
     * @return a textual representation of the given type with complete type information.
     */
    public static String toString(final Type argument, final boolean qualified) {
        if (argument instanceof Class) {
            final Class type = (Class) argument;
            return Strings.printClass(false, qualified, type);
        } else if (argument instanceof ParameterizedType) {
            return String.format("%s<%s>",
                                 toString(Generics.rawType(argument), qualified),
                                 Generics.toString(", ", qualified, ((ParameterizedType) argument).getActualTypeArguments()));
        } else if (argument instanceof GenericArrayType) {
            return String.format("%s[]", toString(((GenericArrayType) argument).getGenericComponentType(), qualified));
        } else if (argument instanceof TypeVariable) {
            final TypeVariable variable = (TypeVariable) argument;
            final Type[] bounds = variable.getBounds();

            return bounds.length == 1 && bounds[0] == Object.class
                   ? variable.getName()
                   : String.format("%s extends %s", variable.getName(), Generics.toString(" & ", qualified, bounds));
        } else if (argument instanceof WildcardType) {
            final WildcardType wildcard = (WildcardType) argument;
            final Type[] upperBounds = wildcard.getUpperBounds();
            final Type[] lowerBounds = wildcard.getLowerBounds();

            final String upper = upperBounds.length == 1 && upperBounds[0] == Object.class
                                 ? ""
                                 : " extends ".concat(Generics.toString(" & ", qualified, upperBounds));

            final String lower = lowerBounds.length == 0 ? "" : " super ".concat(Generics.toString(" & ", qualified, lowerBounds));

            return String.format("?%s%s", upper, lower);
        } else {
            assert false : argument.getClass();
            return argument.toString();
        }
    }

    private static String toString(final String delimiter, final boolean qualified, final Type... argument) {
        final Lists.Delimited delimited = Lists.delimited(delimiter);

        for (final Type type : argument) {
            delimited.add(Generics.toString(type, qualified));
        }

        return delimited.toString();
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
                assert rawReference == rawType : String.format("%s != %s", toString(reference, false), toString(type, false));
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
            return String.format("%s[]", componentType.toString());
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
            final Lists.Delimited parameters = Lists.delimited();

            for (final Type argument : arguments) {
                parameters.add(argument.toString());
            }

            final Type rawType = getRawType();
            return String.format("%s<%s>", rawType instanceof Class ? ((Class) rawType).getName() : rawType.toString(), parameters);
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
        final Annotation[][] annotations = constructor.getParameterAnnotations();

        final Class<?> enclosing = type.getEnclosingClass();

        final int enclosingTypes = enclosing == null || Modifier.isStatic(type.getModifiers()) ? 0 : 1;
        final int missingTypes = params.length - types.length;

        /*
         * http://bugs.sun.com/view_bug.do?bug_id=5087240:
         *
         * Non-static inner class constructor generic parameter types array and parameter annotations array might not contain entry for the enclosing class
         * whereas the parameter types array always does. Furthermore, the closure variables might not be present in the generic parameter type and parameter
          * annotation arrays but are present in the parameter type array at the end.
         *
         * The enclosing class is assumed to be in the beginning of the arrays when present while closure variables are assumed to be at the end.
         */
        if (missingTypes > 0) {
            for (int i = enclosingTypes; i < params.length - missingTypes + enclosingTypes; ++i) {
                if (Generics.rawType(types[i - enclosingTypes]) != params[i]) {
                    throw new IllegalStateException(String.format("Could not match parameter types of %s constructor; classes: %s, types: %s, annotations: %s, on %s %s version %s virtual machine for %s Java %s",
                                                                  Strings.printObject(false, type),
                                                                  Strings.printObject(false, params),
                                                                  Strings.printObject(false, types),
                                                                  Strings.printObject(false, annotations),
                                                                  System.getProperty("java.vm.vendor"),
                                                                  System.getProperty("java.vm.name"),
                                                                  System.getProperty("java.vm.version"),
                                                                  System.getProperty("java.vendor"),
                                                                  System.getProperty("java.version")));
                }
            }
        }

        return new Parameters() {
            public int size() {
                return params.length;
            }

            public Class<?> type(final int index) {
                return params[index];
            }

            public Type genericType(final int index) {
                final int nested = missingTypes > 0 ? index - enclosingTypes : index;
                return nested < 0 || nested >= types.length ? params[index] : types[nested];
            }

            public Annotation[] annotations(final int index) {
                final int nested = index - enclosingTypes;
                return nested < 0 || nested >= annotations.length ? NO_ANNOTATION : annotations[nested];
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
         * Returns the number of parameters.
         *
         * @return the number of parameters.
         */
        int size();

        /**
         * Returns the parameter type at the given index.
         *
         * @param index the parameter index.
         *
         * @return the parameter type at the given index.
         */
        Class<?> type(int index);

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
        Annotation[] annotations(int index);
    }
}

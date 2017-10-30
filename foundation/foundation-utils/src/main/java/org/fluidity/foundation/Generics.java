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

package org.fluidity.foundation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Function;

import org.fluidity.foundation.security.Security;

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
        } else if (type == null) {
            return null;
        }

        assert false : type.getTypeName();
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
        return typeParameter(type, index, true);
    }

    private static Type typeParameter(final Type type, final int index, final boolean resolve) {
        if (type instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) type;
            final Type[] arguments = parameterizedType.getActualTypeArguments();

            if (arguments != null && arguments.length > index) {
                final Type argument = arguments[index];
                return resolve && argument instanceof WildcardType ? rawType(rawType(parameterizedType.getRawType()).getTypeParameters()[index]) : argument;
            }
        } else if (type instanceof Class) {
            final TypeVariable[] parameters = ((Class) type).getTypeParameters();

            if (parameters != null && parameters.length > index) {
                final TypeVariable parameter = parameters[index];

                if (resolve) {
                    final Type[] bounds = parameter.getBounds();
                    return bounds.length == 1 ? bounds[0] : Object.class;
                } else {
                    return parameter;
                }
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
     * Resolves a type variable using the given base type. The given type is expected to have all type parameters resolved.
     *
     * @param reference the base type to resolve the type variable against.
     * @param variable  the type variable to resolve.
     * @param force     if <code>true</code> wild cards and unresolved type variables are resolved to their bound, else they remain unresolved.
     *
     * @return the resolved type variable if resolution was possible, <code>null</code> otherwise.
     */
    static Type resolve(final Type reference, final TypeVariable variable, final boolean force) {
        if (reference instanceof ParameterizedType) {
            final TypeVariable[] parameters = rawType(reference).getTypeParameters();

            for (int i = 0, limit = parameters.length; i < limit; i++) {
                if (Objects.equals(variable, parameters[i])) {
                    return typeParameter(reference, i, force);
                }
            }
        } else if (reference instanceof Class) {
            for (final TypeVariable parameter : ((Class) reference).getTypeParameters()) {
                if (Objects.equals(variable, parameter)) {
                    return force ? rawType(parameter) : parameter;
                }
            }
        } else if (reference instanceof GenericArrayType) {
            return resolve(((GenericArrayType) reference).getGenericComponentType(), variable, force);
        } else {
            return null;
        }

        final Class<?> _type = rawType(reference);
        final GenericDeclaration declaration = variable.getGenericDeclaration();

        if (declaration instanceof Class) {
            final Class<?> _class = (Class<?>) declaration;

            if (_class.isAssignableFrom(_type)) {
                return abstractions(reference, type -> _class.isAssignableFrom(rawType(type))
                                                       ? resolve(propagate(reference, type, force), variable, force)
                                                       : null);
            } else if (reference instanceof ParameterizedType) {
                return infer((ParameterizedType) reference, variable);
            }
        }

        return null;
    }

    private static Type infer(final ParameterizedType reference, final TypeVariable variable) {
        final GenericDeclaration declaration = variable.getGenericDeclaration();

        if (!(declaration instanceof Class)) {
            return null;
        }

        final Class<?> root = rawType(reference);

        return abstractions((Class) declaration, type -> {
            final Class<?> _class = rawType(type);

            if (root.isAssignableFrom(_class) && type instanceof ParameterizedType) {
                final Type[] arguments = ((ParameterizedType) type).getActualTypeArguments();

                for (int i = 0, limit = arguments.length; i < limit; i++) {
                    if (arguments[i] == variable) {
                        return _class == root
                               ? reference.getActualTypeArguments()[i]
                               : infer(reference, _class.getTypeParameters()[i]);
                    }
                }
            }

            return null;
        });
    }

    private static Collection<Type> unresolved(final Type reference, final Collection<Type> list) {
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
        return Lists.asArray(Type.class, false, unresolved(reference, new ArrayList<>()));
    }

    /**
     * Inspects the superclass and interfaces of <code>reference</code> and invokes <code>inspector</code> with each until it returns a type.
     * <code>Object.class</code> is never passed to <code>inspector</code>. The <code>inspector</code> can invoke this method again to traverse the type's
     * class hierarchy.
     *
     * @param reference the type to inspect the super and interfaces of.
     * @param inspector the inspector to invoke at every abstraction found.
     *
     * @return whatever the <code>inspector</code> returns; may be <code>null</code>.
     */
    public static Type abstractions(final Type reference, final Function<Type, Type> inspector) {
        Type selected = null;

        final Class<?> referenceClass = rawType(reference);
        final Type superType = referenceClass.getGenericSuperclass();

        if (superType != null && superType != Object.class) {
            selected = inspector.apply(superType);
        }

        if (selected == null) {
            for (final Type interfaceType : referenceClass.getGenericInterfaces()) {
                selected = inspector.apply(interfaceType);

                if (selected != null) break;
            }
        }

        return selected;
    }

    /**
     * Resolves all type variables of the <code>outbound</code> parameter using the <code>inbound</code> type as base type.
     *
     * @param inbound  the base type to resolve type variables against.
     * @param outbound the outbound type whose type variables are to be resolved.
     * @param resolve  if <code>true</code> wild cards and unresolved type variables are resolved to their bound, else they remain unresolved.
     *
     * @return a type with all type variables resolved or <code>null</code> if no resolution was possible.
     */
    public static Type propagate(final Type inbound, final Type outbound, final boolean resolve) {
        if (outbound instanceof Class) {
            return outbound;
        } else if (outbound instanceof ParameterizedType) {
            final ParameterizedType original = (ParameterizedType) outbound;

            boolean modified = false;

            final Type[] arguments = original.getActualTypeArguments();
            for (int i = 0, limit = arguments.length; i < limit; i++) {
                final Type argument = arguments[i];
                final Type resolved = propagate(inbound, argument, resolve);

                if (resolved != null && resolved != argument) {
                    arguments[i] = resolved;
                    modified = true;
                }
            }

            return modified ? new ParameterizedTypeImpl(original, arguments) : outbound;
        } else if (outbound instanceof GenericArrayType) {
            return new GenericArrayTypeImpl(propagate(inbound, arrayComponentType(outbound), resolve));
        } else if (outbound instanceof TypeVariable) {
            return resolve(inbound, (TypeVariable) outbound, resolve);
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
            return String.format("%s@%d", Strings.formatClass(false, true, (Class) type), System.identityHashCode(type));
        } else if (type instanceof ParameterizedType) {
            final StringJoiner output = new StringJoiner(",", "<", ">");

            for (final Type parameter : ((ParameterizedType) type).getActualTypeArguments()) {
                output.add(identity(parameter));
            }

            return identity(((ParameterizedType) type).getRawType()) + output;
        } else if (type instanceof GenericArrayType) {
            return identity(((GenericArrayType) type).getGenericComponentType()) + "[]";
        } else if (type instanceof TypeVariable) {
            final StringJoiner output = new StringJoiner(",");

            for (final Type bound : ((TypeVariable) type).getBounds()) {
                output.add(identity(bound));
            }

            return output.toString();
        } else if (type instanceof WildcardType) {
            final StringJoiner lower = new StringJoiner(",");

            for (final Type bound : ((WildcardType) type).getLowerBounds()) {
                lower.add(identity(bound));
            }

            final StringJoiner upper = new StringJoiner(",");

            for (final Type bound : ((WildcardType) type).getUpperBounds()) {
                upper.add(identity(bound));
            }

            return lower + ":" + upper;
        }

        assert false : type;
        return null;
    }

    /**
     * Returns a textual representation of the given type with complete type information.
     *
     * @param qualified if <code>true</code>, the type's fully qualified name is used, otherwise its simple name is used.
     * @param argument  the generic type to convert to String.
     *
     * @return a textual representation of the given type with complete type information.
     */
    public static String toString(final boolean qualified, final Type argument) {
        if (argument instanceof Class) {
            final Class type = (Class) argument;
            return Strings.formatClass(false, qualified, type);
        } else if (argument instanceof ParameterizedType) {
            return String.format("%s<%s>",
                                 Generics.toString(qualified, Generics.rawType(argument)),
                                 Generics.toString(", ", qualified, ((ParameterizedType) argument).getActualTypeArguments()));
        } else if (argument instanceof GenericArrayType) {
            return String.format("%s[]", Generics.toString(qualified, ((GenericArrayType) argument).getGenericComponentType()));
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
        final StringJoiner delimited = new StringJoiner(delimiter);

        for (final Type type : argument) {
            delimited.add(Generics.toString(qualified, type));
        }

        return delimited.toString();
    }

    /**
     * Returns the generic type the given <code>specific</code> class represents as the specialization of the <code>generic</code> type. If <code>generic</code>
     * is a parameterized type, the returned type will have the corresponding type <em>arguments</em> present. The {@linkplain #rawType(Type) class} of the
     * returned type will be <code>generic</code>.
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

    @SuppressWarnings("unchecked")
    private static Type specializedType(final Type specific, final Type generic, final Class<?> rawSpecific, final Class rawGeneric) {
        if (rawGeneric == rawSpecific) {
            return specific;
        } else if (rawGeneric.isAssignableFrom(rawSpecific)) {
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
                    final Type type = types[i];

                    if (rawGeneric == api) {
                        return type;
                    } else if (rawGeneric.isAssignableFrom(api)) {
                        return specializedType(type, generic, rawType(type), rawGeneric);
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
                    final Type resolved = resolve(type, variable, true);
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
                assert rawReference == rawType : String.format("%s != %s", toString(false, reference), toString(false, type));
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

    public static Type canonicalType(final Type type) {
        if (type instanceof Class) {
            return type;
        } else if (type instanceof ParameterizedType) {
            final Type[] arguments = ((ParameterizedType) type).getActualTypeArguments();

            int unresolved = 0;
            boolean changed = false;
            for (int i = 0, ii = arguments.length; i < ii; i++) {
                final Type argument = arguments[i];

                if (argument instanceof WildcardType || argument instanceof TypeVariable) {
                    arguments[i] = rawType(argument);
                    ++unresolved;
                } else {
                    arguments[i] = canonicalType(argument);

                    if (arguments[i] != argument) {
                        changed = true;
                    }
                }
            }

            if (unresolved == arguments.length) {
                return rawType(type);
            } else if (unresolved > 0 || changed) {
                return new ParameterizedTypeImpl((ParameterizedType) type, arguments);
            } else {
                return type;
            }
        } else if (type instanceof TypeVariable || type instanceof WildcardType) {
            return rawType(type);
        } else if (type instanceof GenericArrayType) {
            final Type componentType = ((GenericArrayType) type).getGenericComponentType();

            if (componentType instanceof WildcardType || componentType instanceof TypeVariable) {
                return new GenericArrayTypeImpl(rawType(componentType));
            } else {
                final Type canonicalType = canonicalType(componentType);

                if (canonicalType != componentType) {
                    return new GenericArrayTypeImpl(canonicalType);
                }
            }

            return type;
        } else {
            return type;
        }
    }

    private static class GenericArrayTypeImpl implements GenericArrayType {
        private final Type componentType;

        GenericArrayTypeImpl(final Type componentType) {
            this.componentType = componentType;
        }

        @Override
        public Type getGenericComponentType() {
            return componentType;
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || !(other instanceof GenericArrayType)) {
                return false;
            }

            final GenericArrayType that = (GenericArrayType) other;
            return Objects.equals(componentType, that.getGenericComponentType());
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

        @Override
        public Type[] getActualTypeArguments() {
            return arguments;
        }

        @Override
        public Type getRawType() {
            return original.getRawType();
        }

        @Override
        public Type getOwnerType() {
            return original.getOwnerType();
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other) {
                return true;
            }

            if (other == null || !(other instanceof ParameterizedType)) {
                return false;
            }

            final ParameterizedType that = (ParameterizedType) other;
            return Objects.equals(this.getRawType(), that.getRawType()) &&
                   Objects.equals(this.getOwnerType(), that.getOwnerType()) &&
                   Arrays.equals(arguments, that.getActualTypeArguments());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getRawType(), getOwnerType(), Arrays.hashCode(arguments));
        }

        @Override
        public String toString() {
            final StringJoiner parameters = new StringJoiner(", ", "<", ">");

            for (final Type argument : arguments) {
                parameters.add(argument.toString());
            }

            final Type rawType = getRawType();
            return (rawType instanceof Class ? ((Class) rawType).getName() : rawType.toString()) + parameters;
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
                    Security.invoke(() -> {
                        throw new IllegalStateException(String.format("Could not match parameter types of %s constructor; "
                                                                      + "classes: %s, types: %s, annotations: %s, "
                                                                      + "on %s %s version %s virtual machine for %s Java %s",
                                                                      Strings.formatObject(false, true, type),
                                                                      Strings.formatObject(false, true, params),
                                                                      Strings.formatObject(false, true, types),
                                                                      Strings.formatObject(false, true, annotations),
                                                                      System.getProperty("java.vm.vendor"),
                                                                      System.getProperty("java.vm.name"),
                                                                      System.getProperty("java.vm.version"),
                                                                      System.getProperty("java.vendor"),
                                                                      System.getProperty("java.version")));
                    });
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

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

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Objects;

import org.testng.annotations.Test;

import static org.fluidity.foundation.Generics.arrayComponentType;
import static org.fluidity.foundation.Generics.canonicalType;
import static org.fluidity.foundation.Generics.describe;
import static org.fluidity.foundation.Generics.isAssignable;
import static org.fluidity.foundation.Generics.propagate;
import static org.fluidity.foundation.Generics.rawType;
import static org.fluidity.foundation.Generics.resolve;
import static org.fluidity.foundation.Generics.specializedType;
import static org.fluidity.foundation.Generics.typeParameter;

/**
 * @author Tibor Varga
 */
public class GenericsTest {

    @Test
    public void testRawType() throws Exception {
        checkRawType(GenericsTest.class, GenericsTest.class);
        checkRawType(SomeClass.class.getGenericInterfaces()[0], Comparable.class);
        checkRawType(SomeClass.class.getConstructor(Comparable[].class).getGenericParameterTypes()[0], Comparable[].class);
    }

    @Test
    public void testParameters() throws Exception {
        checkRawType(typeParameter(SomeClass.class.getGenericInterfaces()[0], 0), SomeClass.class);
        checkRawType(typeParameter(GenericsTest.class, 0), null);
        checkRawType(typeParameter(F1.class, 0), Object.class);
        checkRawType(typeParameter(I.class, 1), Object.class);
        checkRawType(typeParameter(A.class, 2), Q.class);
    }

    @Test
    public void testArrays() throws Exception {
        checkRawType(arrayComponentType(SomeClass[].class), SomeClass.class);
        checkRawType(arrayComponentType(SomeClass.class.getConstructor(Comparable[].class).getGenericParameterTypes()[0]), Comparable.class);
    }

    @Test
    public void testDirectVariables() throws Exception {

        class References {
            public final A<I1, I2, I3> reference = null;
        }

        final Type reference = References.class.getField("reference").getGenericType();

        final Constructor<A> constructorA = A.class.getConstructor(B.class, B.class, B[].class);
        final ParameterizedType paramA0 = (ParameterizedType) constructorA.getGenericParameterTypes()[0];
        final ParameterizedType paramA1 = (ParameterizedType) constructorA.getGenericParameterTypes()[1];
        final GenericArrayType paramA2 = (GenericArrayType) constructorA.getGenericParameterTypes()[2];

        final Constructor<B> constructorB = B.class.getConstructor(C.class, C.class);
        final ParameterizedType paramB0 = (ParameterizedType) constructorB.getGenericParameterTypes()[0];
        final ParameterizedType paramB1 = (ParameterizedType) constructorB.getGenericParameterTypes()[1];

        assert B.class == rawType(propagate(reference, paramA0, true));
        assert B.class == rawType(propagate(reference, paramA1, true));
        assert B[].class == rawType(propagate(reference, paramA2, true));

        assert I1.class == resolve(reference, (TypeVariable) paramA0.getActualTypeArguments()[0], false);
        assert I2.class == resolve(reference, (TypeVariable) paramA0.getActualTypeArguments()[1], false);
        assert I2.class == resolve(reference, (TypeVariable) paramA1.getActualTypeArguments()[0], false);
        assert I3.class == resolve(reference, (TypeVariable) paramA1.getActualTypeArguments()[1], false);
        assert I1.class == resolve(reference, (TypeVariable) ((ParameterizedType) arrayComponentType(paramA2)).getActualTypeArguments()[0], true);
        assert I3.class == resolve(reference, (TypeVariable) ((ParameterizedType) arrayComponentType(paramA2)).getActualTypeArguments()[1], true);

        final Type inheritedA0B0 = propagate(propagate(reference, paramA0, true), paramB0, true);
        final Type inheritedA0B1 = propagate(propagate(reference, paramA0, true), paramB1, true);
        final Type inheritedA1B0 = propagate(propagate(reference, paramA1, true), paramB0, true);
        final Type inheritedA1B1 = propagate(propagate(reference, paramA1, true), paramB1, true);
        final Type inheritedA2B0 = propagate(propagate(reference, paramA2, true), paramB0, true);
        final Type inheritedA2B1 = propagate(propagate(reference, paramA2, true), paramB1, true);

        assert rawType(inheritedA0B0) == C.class;
        assert rawType(inheritedA0B1) == C.class;
        assert rawType(inheritedA1B0) == C.class;
        assert rawType(inheritedA1B1) == C.class;
        assert rawType(inheritedA2B0) == C.class;
        assert rawType(inheritedA2B1) == C.class;

        assert I1.class == typeParameter(inheritedA0B0, 0);
        assert I2.class == typeParameter(inheritedA0B1, 0);
        assert I2.class == typeParameter(inheritedA1B0, 0);
        assert I3.class == typeParameter(inheritedA1B1, 0);
        assert I1.class == typeParameter(inheritedA2B0, 0);
        assert I3.class == typeParameter(inheritedA2B1, 0);
    }

    @Test
    public void testExtendedVariables() throws Exception {

        class References {
            public final D reference = null;
        }

        final Type reference = References.class.getField("reference").getGenericType();

        final Constructor<D> constructorA = D.class.getConstructor(B.class, B.class, B[].class);
        final ParameterizedType paramA0 = (ParameterizedType) constructorA.getGenericParameterTypes()[0];
        final ParameterizedType paramA1 = (ParameterizedType) constructorA.getGenericParameterTypes()[1];
        final GenericArrayType paramA2 = (GenericArrayType) constructorA.getGenericParameterTypes()[2];

        final Constructor<B> constructorB = B.class.getConstructor(C.class, C.class);
        final ParameterizedType paramB0 = (ParameterizedType) constructorB.getGenericParameterTypes()[0];
        final ParameterizedType paramB1 = (ParameterizedType) constructorB.getGenericParameterTypes()[1];

        assert B.class == rawType(propagate(reference, paramA0, true));
        assert B.class == rawType(propagate(reference, paramA1, true));
        assert B[].class == rawType(propagate(reference, paramA2, true));

        assert I1.class == paramA0.getActualTypeArguments()[0];
        assert I2.class == paramA0.getActualTypeArguments()[1];
        assert I2.class == paramA1.getActualTypeArguments()[0];
        assert I3.class == paramA1.getActualTypeArguments()[1];
        assert I1.class == ((ParameterizedType) arrayComponentType(paramA2)).getActualTypeArguments()[0];
        assert I3.class == ((ParameterizedType) arrayComponentType(paramA2)).getActualTypeArguments()[1];

        final Type inheritedA0B0 = propagate(propagate(reference, paramA0, true), paramB0, true);
        final Type inheritedA0B1 = propagate(propagate(reference, paramA0, true), paramB1, true);
        final Type inheritedA1B0 = propagate(propagate(reference, paramA1, true), paramB0, true);
        final Type inheritedA1B1 = propagate(propagate(reference, paramA1, true), paramB1, true);
        final Type inheritedA2B0 = propagate(propagate(reference, paramA2, true), paramB0, true);
        final Type inheritedA2B1 = propagate(propagate(reference, paramA2, true), paramB1, true);

        assert rawType(inheritedA0B0) == C.class;
        assert rawType(inheritedA0B1) == C.class;
        assert rawType(inheritedA1B0) == C.class;
        assert rawType(inheritedA1B1) == C.class;
        assert rawType(inheritedA2B0) == C.class;
        assert rawType(inheritedA2B1) == C.class;

        assert I1.class == typeParameter(inheritedA0B0, 0);
        assert I2.class == typeParameter(inheritedA0B1, 0);
        assert I2.class == typeParameter(inheritedA1B0, 0);
        assert I3.class == typeParameter(inheritedA1B1, 0);
        assert I1.class == typeParameter(inheritedA2B0, 0);
        assert I3.class == typeParameter(inheritedA2B1, 0);
    }

    @Test
    public void testImplementedVariables() throws Exception {

        class References {
            public final E<I1> reference = null;
        }

        final Type reference = References.class.getField("reference").getGenericType();

        final Constructor<E> constructorA = E.class.getConstructor(B.class, B.class, B[].class);
        final ParameterizedType paramA0 = (ParameterizedType) constructorA.getGenericParameterTypes()[0];
        final ParameterizedType paramA1 = (ParameterizedType) constructorA.getGenericParameterTypes()[1];
        final GenericArrayType paramA2 = (GenericArrayType) constructorA.getGenericParameterTypes()[2];

        final Constructor<B> constructorB = B.class.getConstructor(C.class, C.class);
        final ParameterizedType paramB0 = (ParameterizedType) constructorB.getGenericParameterTypes()[0];
        final ParameterizedType paramB1 = (ParameterizedType) constructorB.getGenericParameterTypes()[1];

        assert B.class == rawType(propagate(reference, paramA0, true));
        assert B.class == rawType(propagate(reference, paramA1, true));
        assert B[].class == rawType(propagate(reference, paramA2, true));

        assert I1.class == resolve(reference, (TypeVariable) paramA0.getActualTypeArguments()[0], false);
        assert I2.class == paramA0.getActualTypeArguments()[1];
        assert I2.class == paramA1.getActualTypeArguments()[0];
        assert I3.class == paramA1.getActualTypeArguments()[1];
        assert I1.class == resolve(reference, (TypeVariable) ((ParameterizedType) arrayComponentType(paramA2)).getActualTypeArguments()[0], true);
        assert I3.class == ((ParameterizedType) arrayComponentType(paramA2)).getActualTypeArguments()[1];

        final Type inheritedA0B0 = propagate(propagate(reference, paramA0, true), paramB0, true);
        final Type inheritedA0B1 = propagate(propagate(reference, paramA0, true), paramB1, true);
        final Type inheritedA1B0 = propagate(propagate(reference, paramA1, true), paramB0, true);
        final Type inheritedA1B1 = propagate(propagate(reference, paramA1, true), paramB1, true);
        final Type inheritedA2B0 = propagate(propagate(reference, paramA2, true), paramB0, true);
        final Type inheritedA2B1 = propagate(propagate(reference, paramA2, true), paramB1, true);

        assert rawType(inheritedA0B0) == C.class;
        assert rawType(inheritedA0B1) == C.class;
        assert rawType(inheritedA1B0) == C.class;
        assert rawType(inheritedA1B1) == C.class;
        assert rawType(inheritedA2B0) == C.class;
        assert rawType(inheritedA2B1) == C.class;

        assert I1.class == typeParameter(inheritedA0B0, 0);
        assert I2.class == typeParameter(inheritedA0B1, 0);
        assert I2.class == typeParameter(inheritedA1B0, 0);
        assert I3.class == typeParameter(inheritedA1B1, 0);
        assert I1.class == typeParameter(inheritedA2B0, 0);
        assert I3.class == typeParameter(inheritedA2B1, 0);
    }

    @Test
    public void testUnresolvedTypeVariables() throws Exception {

        class References {
            public final A reference = null;
        }

        final Type reference = References.class.getField("reference").getGenericType();

        final Constructor<A> constructorA = A.class.getConstructor(B.class, B.class, B[].class);
        final ParameterizedType paramA0 = (ParameterizedType) constructorA.getGenericParameterTypes()[0];
        final ParameterizedType paramA1 = (ParameterizedType) constructorA.getGenericParameterTypes()[1];
        final GenericArrayType paramA2 = (GenericArrayType) constructorA.getGenericParameterTypes()[2];

        final Constructor<B> constructorB = B.class.getConstructor(C.class, C.class);
        final ParameterizedType paramB0 = (ParameterizedType) constructorB.getGenericParameterTypes()[0];
        final ParameterizedType paramB1 = (ParameterizedType) constructorB.getGenericParameterTypes()[1];

        assert B.class == rawType(propagate(reference, paramA0, true));
        assert B.class == rawType(propagate(reference, paramA1, true));
        assert B[].class == rawType(propagate(reference, paramA2, true));

        assert Q.class == resolve(reference, (TypeVariable) paramA0.getActualTypeArguments()[0], true);
        assert Q.class == resolve(reference, (TypeVariable) paramA0.getActualTypeArguments()[1], true);
        assert Q.class == resolve(reference, (TypeVariable) paramA1.getActualTypeArguments()[0], true);
        assert Q.class == resolve(reference, (TypeVariable) paramA1.getActualTypeArguments()[1], true);
        assert Q.class == resolve(reference, (TypeVariable) ((ParameterizedType) arrayComponentType(paramA2)).getActualTypeArguments()[0], true);
        assert Q.class == resolve(reference, (TypeVariable) ((ParameterizedType) arrayComponentType(paramA2)).getActualTypeArguments()[1], true);

        final Type inheritedA0B0 = propagate(propagate(reference, paramA0, true), paramB0, true);
        final Type inheritedA0B1 = propagate(propagate(reference, paramA0, true), paramB1, true);
        final Type inheritedA1B0 = propagate(propagate(reference, paramA1, true), paramB0, true);
        final Type inheritedA1B1 = propagate(propagate(reference, paramA1, true), paramB1, true);
        final Type inheritedA2B0 = propagate(propagate(reference, paramA2, true), paramB0, true);
        final Type inheritedA2B1 = propagate(propagate(reference, paramA2, true), paramB1, true);

        assert rawType(inheritedA0B0) == C.class;
        assert rawType(inheritedA0B1) == C.class;
        assert rawType(inheritedA1B0) == C.class;
        assert rawType(inheritedA1B1) == C.class;
        assert rawType(inheritedA2B0) == C.class;
        assert rawType(inheritedA2B1) == C.class;

        assert Q.class == typeParameter(inheritedA0B0, 0);
        assert Q.class == typeParameter(inheritedA0B1, 0);
        assert Q.class == typeParameter(inheritedA1B0, 0);
        assert Q.class == typeParameter(inheritedA1B1, 0);
        assert Q.class == typeParameter(inheritedA2B0, 0);
        assert Q.class == typeParameter(inheritedA2B1, 0);
    }

    @Test
    public void testWildcardTypeVariables() throws Exception {

        class References {
            public final A<?, ?, ?> reference = null;
        }

        final Type reference = References.class.getField("reference").getGenericType();

        final Constructor<A> constructorA = A.class.getConstructor(B.class, B.class, B[].class);
        final ParameterizedType paramA0 = (ParameterizedType) constructorA.getGenericParameterTypes()[0];
        final ParameterizedType paramA1 = (ParameterizedType) constructorA.getGenericParameterTypes()[1];
        final GenericArrayType paramA2 = (GenericArrayType) constructorA.getGenericParameterTypes()[2];

        final Constructor<B> constructorB = B.class.getConstructor(C.class, C.class);
        final ParameterizedType paramB0 = (ParameterizedType) constructorB.getGenericParameterTypes()[0];
        final ParameterizedType paramB1 = (ParameterizedType) constructorB.getGenericParameterTypes()[1];

        assert B.class == rawType(propagate(reference, paramA0, true));
        assert B.class == rawType(propagate(reference, paramA1, true));
        assert B[].class == rawType(propagate(reference, paramA2, true));

        assert Q.class == resolve(reference, (TypeVariable) paramA0.getActualTypeArguments()[0], true);
        assert Q.class == resolve(reference, (TypeVariable) paramA0.getActualTypeArguments()[1], true);
        assert Q.class == resolve(reference, (TypeVariable) paramA1.getActualTypeArguments()[0], true);
        assert Q.class == resolve(reference, (TypeVariable) paramA1.getActualTypeArguments()[1], true);
        assert Q.class == resolve(reference, (TypeVariable) ((ParameterizedType) arrayComponentType(paramA2)).getActualTypeArguments()[0], true);
        assert Q.class == resolve(reference, (TypeVariable) ((ParameterizedType) arrayComponentType(paramA2)).getActualTypeArguments()[1], true);

        final Type inheritedA0B0 = propagate(propagate(reference, paramA0, true), paramB0, true);
        final Type inheritedA0B1 = propagate(propagate(reference, paramA0, true), paramB1, true);
        final Type inheritedA1B0 = propagate(propagate(reference, paramA1, true), paramB0, true);
        final Type inheritedA1B1 = propagate(propagate(reference, paramA1, true), paramB1, true);
        final Type inheritedA2B0 = propagate(propagate(reference, paramA2, true), paramB0, true);
        final Type inheritedA2B1 = propagate(propagate(reference, paramA2, true), paramB1, true);

        assert rawType(inheritedA0B0) == C.class;
        assert rawType(inheritedA0B1) == C.class;
        assert rawType(inheritedA1B0) == C.class;
        assert rawType(inheritedA1B1) == C.class;
        assert rawType(inheritedA2B0) == C.class;
        assert rawType(inheritedA2B1) == C.class;

        assert Q.class == typeParameter(inheritedA0B0, 0);
        assert Q.class == typeParameter(inheritedA0B1, 0);
        assert Q.class == typeParameter(inheritedA1B0, 0);
        assert Q.class == typeParameter(inheritedA1B1, 0);
        assert Q.class == typeParameter(inheritedA2B0, 0);
        assert Q.class == typeParameter(inheritedA2B1, 0);
    }

    @Test
    public void testTypeEquality() throws Exception {

        class References {
            public final A<I1, I2, I3> reference = null;
        }

        final Type reference = References.class.getField("reference").getGenericType();
        final Constructor<A> constructorA = A.class.getConstructor(B.class, B.class, B[].class);

        final ParameterizedType paramA0 = (ParameterizedType) constructorA.getGenericParameterTypes()[0];
        final ParameterizedType paramA1 = (ParameterizedType) constructorA.getGenericParameterTypes()[1];
        final GenericArrayType paramA2 = (GenericArrayType) constructorA.getGenericParameterTypes()[2];

        final Constructor<D> constructorD = D.class.getConstructor(B.class, B.class, B[].class);
        final ParameterizedType paramD0 = (ParameterizedType) constructorD.getGenericParameterTypes()[0];
        final ParameterizedType paramD1 = (ParameterizedType) constructorD.getGenericParameterTypes()[1];
        final GenericArrayType paramD2 = (GenericArrayType) constructorD.getGenericParameterTypes()[2];

        assert Objects.equals(propagate(reference, paramA0, true), propagate(reference, paramA0, true));
        assert Objects.equals(propagate(reference, paramA0, true), paramD0);
        assert Objects.equals(propagate(reference, paramA0, true), paramD0);

        assert Objects.equals(propagate(reference, paramA1, true), propagate(reference, paramA1, true));
        assert Objects.equals(propagate(reference, paramA1, true), paramD1);
        assert Objects.equals(propagate(reference, paramA1, true), paramD1);

        assert Objects.equals(propagate(reference, paramA2, true), propagate(reference, paramA2, true));
        assert Objects.equals(propagate(reference, paramA2, true), paramD2);
        assert Objects.equals(propagate(reference, paramA2, true), paramD2);
    }

    @Test
    public void testSpecialization() throws Exception {
        final Type type1 = specializedType(G1.class, F1.class);
        assert type1 == F1.class : type1;

        final Type type2 = specializedType(G1.class, I.class);
        assert type2 == I.class : type2;

        final Type type3 = specializedType(G3.class, F1.class);
        assert type3 == F1.class : type3;

        final Type type4 = specializedType(G3.class, I.class);
        assert type4 == I.class : type4;

        final Type type5 = specializedType(G3.class, Q.class);
        assert type5 == Q.class : type5;

        final Type type6 = specializedType(G2.class, F1.class);
        assert rawType(type6) == F1.class : type6;
        assert type6 instanceof ParameterizedType : type6;
        assert typeParameter(type6, 0) == I4.class : type6;

        final Type type7 = specializedType(G2.class, I.class);
        assert rawType(type7) == I.class : type7;
        assert type7 instanceof ParameterizedType : type7;
        assert typeParameter(type7, 0) == I1.class : type7;
        assert typeParameter(type7, 1) == I2.class : type7;
        assert typeParameter(type7, 2) == I3.class : type7;

        final Type type8 = specializedType(G4.class, F1.class);
        assert rawType(type8) == F1.class : type8;
        assert type8 instanceof ParameterizedType : type8;
        assert typeParameter(type8, 0) == I4.class : type8;

        final Type type9 = specializedType(G4.class, I.class);
        assert rawType(type9) == I.class : type9;
        assert type9 instanceof ParameterizedType : type9;
        assert typeParameter(type9, 0) == I1.class : type9;
        assert typeParameter(type9, 1) == I2.class : type9;
        assert typeParameter(type9, 2) == I3.class : type9;
    }

    @Test
    public void testAssignmentChecks() throws Exception {

        // generic parameter types: I, F, I<I1, I2, I3>, F<I4>, I[], I<? super I4, ?, ?>, P<T>, F1<I4>[][]
        final Type[] types = H.class.getDeclaredConstructors()[0].getGenericParameterTypes();

        assert isAssignable(types[0], G1.class);
        assert !isAssignable(G1.class, types[0]);
        assert isAssignable(types[1], G1.class);
        assert !isAssignable(G1.class, types[1]);

        assert isAssignable(types[0], G3.class);
        assert !isAssignable(G3.class, types[0]);
        assert isAssignable(types[1], G3.class);
        assert !isAssignable(G3.class, types[1]);

        assert !isAssignable(types[2], G1.class);
        assert !isAssignable(G1.class, types[2]);
        assert isAssignable(types[2], G2.class);
        assert !isAssignable(G2.class, types[2]);
        assert !isAssignable(types[3], G1.class);
        assert !isAssignable(G1.class, types[3]);
        assert isAssignable(types[3], G2.class);
        assert !isAssignable(G2.class, types[3]);

        assert !isAssignable(types[2], G3.class);
        assert !isAssignable(G3.class, types[2]);
        assert isAssignable(types[2], G4.class);
        assert !isAssignable(G4.class, types[2]);
        assert !isAssignable(types[3], G3.class);
        assert !isAssignable(G3.class, types[3]);
        assert isAssignable(types[3], G4.class);
        assert !isAssignable(G4.class, types[3]);

        assert isAssignable(types[4], G1[].class);
        assert !isAssignable(G1[].class, types[4]);
        assert isAssignable(types[4], G3[].class);
        assert !isAssignable(G3[].class, types[4]);

        assert isAssignable(types[5], G4.class);
        assert !isAssignable(G4.class, types[5]);
        assert !isAssignable(types[5], G5.class);
        assert !isAssignable(G5.class, types[5]);
        assert !isAssignable(types[5], G6.class);
        assert !isAssignable(G6.class, types[5]);
        assert !isAssignable(types[5], G7.class);
        assert !isAssignable(G7.class, types[5]);

        assert isAssignable(types[6], G8.class);
        assert !isAssignable(G8.class, types[6]);

        assert isAssignable(Object.class, types[0]);
        assert isAssignable(Object.class, types[1]);
        assert isAssignable(Object.class, types[2]);
        assert isAssignable(Object.class, types[3]);
        assert isAssignable(Object.class, types[4]);
        assert isAssignable(Object.class, types[5]);
        assert isAssignable(Object.class, types[6]);

        assert !isAssignable(types[0], Object.class);
        assert !isAssignable(types[1], Object.class);
        assert !isAssignable(types[2], Object.class);
        assert !isAssignable(types[3], Object.class);
        assert !isAssignable(types[4], Object.class);
        assert !isAssignable(types[5], Object.class);
        assert !isAssignable(types[6], Object.class);
    }

    @Test
    public void testNonStaticInnerClasses() throws Exception {
        checkParameters(GenericsTest.class, describe(X1.class.getConstructors()[0]), false, false);
        checkParameters(X1.class, describe(X1.X2.class.getConstructors()[0]), true, false);
        checkParameters(X1.X2.class, describe(X1.X2.X3.class.getConstructors()[0]), true, true);
        checkParameters(X1.X2.X3.class, describe(X1.X2.X3.X4.class.getConstructors()[0]), false, true);

        final @A2 P2<I> context1 = null;
        final @A3 P3<I> context2 = null;

        @SuppressWarnings("UnusedParameters")
        class Closure1 {
            public Closure1(final @A1 P1 p1) {
                assert context1 == null;
                assert context2 == null;
            }
        }

        @SuppressWarnings("UnusedParameters")
        class Closure2 {
            public Closure2(final @A1 P1<I> p1) {
                assert context1 == null;
                assert context2 == null;
            }
        }

        checkParameters(GenericsTest.class, describe(Closure1.class.getConstructors()[0]), false, false, false);
        checkParameters(GenericsTest.class, describe(Closure2.class.getConstructors()[0]), true, false, false);

        final Generics.Parameters params = describe(E1.class.getDeclaredConstructors()[0]);
        assert params.size() == 2 : params.size();
        assert params.genericType(0) == String.class : params.genericType(0);
        assert params.genericType(1) == int.class : params.genericType(1);
    }

    @Test
    public void testStringValues() throws Exception {
        checkText(Generics.toString(true, Q.class), Strings.formatClass(false, true, Q.class));
        checkText(Generics.toString(true, D.class.getGenericSuperclass()),
                  String.format("%s<%s, %s, %s>",
                                Strings.formatClass(false, true, A.class),
                                Strings.formatClass(false, true, I1.class),
                                Strings.formatClass(false, true, I2.class),
                                Strings.formatClass(false, true, I3.class)));

        // generic parameter types: I, F, I<I1, I2, I3>, F<I4>, I[], I<? super I4, ?, ?>, P<T>, F1<I4>[][]
        final Type[] types = H.class.getDeclaredConstructors()[0].getGenericParameterTypes();

        checkText(Generics.toString(false, types[5]),
                  String.format("%s<? super %s, ? extends %s, ?>",
                                Strings.formatClass(false, false, I.class),
                                Strings.formatClass(false, false, I1.class),
                                Strings.formatClass(false, false, I2.class)));

        checkText(Generics.toString(false, types[7]),
                  String.format("%s<%s>[][]", Strings.formatClass(false, false, F1.class), Strings.formatClass(false, false, I4.class)));
    }

    @Test
    public void testTypePropagationViaSuperClass() throws Exception {
        final Type specialized = specializedType(SubType4.class, I.class);
        final Type resolved = propagate(SubType4.class, specialized, true);

        assert I3.class == typeParameter(resolved, 0) : String.format("%s, %s", resolved, I3.class);
        assert I2.class == typeParameter(resolved, 1) : String.format("%s, %s", resolved, I2.class);
        assert I1.class == typeParameter(resolved, 2) : String.format("%s, %s", resolved, I1.class);
    }

    @Test
    public void testTypePropagationViaInterface() throws Exception {
        final Type specialized = specializedType(SubType5.class, I.class);
        final Type resolved = propagate(SubType5.class, specialized, true);

        assert I1.class == typeParameter(resolved, 0) : String.format("%s, %s", resolved, I1.class);
        assert I2.class == typeParameter(resolved, 1) : String.format("%s, %s", resolved, I2.class);
        assert I3.class == typeParameter(resolved, 2) : String.format("%s, %s", resolved, I3.class);
    }

    @Test
    public void testTypePropagationVariableHidingSuperClass() throws Exception {
        final Type specialized = specializedType(SubType3.class, I.class);
        final Type resolved = propagate(SubType3.class, specialized, true);

        assert I1.class == typeParameter(resolved, 0) : String.format("%s, %s", resolved, I1.class);
        assert I2.class == typeParameter(resolved, 1) : String.format("%s, %s", resolved, I2.class);
        assert I3.class == typeParameter(resolved, 2) : String.format("%s, %s", resolved, I3.class);
    }

    @Test
    public void testTypePropagationVariableHidingInterface() throws Exception {
        final Type specialized = specializedType(SubType6.class, I.class);
        final Type resolved = propagate(SubType6.class, specialized, true);

        assert I1.class == typeParameter(resolved, 0) : String.format("%s, %s", resolved, I1.class);
        assert I2.class == typeParameter(resolved, 1) : String.format("%s, %s", resolved, I2.class);
        assert I3.class == typeParameter(resolved, 2) : String.format("%s, %s", resolved, I3.class);
    }

    @Test
    public void testTypePropagationViaSuperClassHierarchy() throws Exception {
        final Type specialized = specializedType(SubType14.class, I.class);
        final Type resolved = propagate(SubType14.class, specialized, true);

        assert I3.class == typeParameter(resolved, 0) : String.format("%s, %s", resolved, I3.class);
        assert I2.class == typeParameter(resolved, 1) : String.format("%s, %s", resolved, I2.class);
        assert I1.class == typeParameter(resolved, 2) : String.format("%s, %s", resolved, I1.class);
    }

    @Test
    public void testTypePropagationViaInterfaceHierarchy() throws Exception {
        final Type specialized = specializedType(SubType24.class, I.class);
        final Type resolved = propagate(SubType24.class, specialized, true);

        assert I3.class == typeParameter(resolved, 0) : String.format("%s, %s", resolved, I3.class);
        assert I2.class == typeParameter(resolved, 1) : String.format("%s, %s", resolved, I2.class);
        assert I1.class == typeParameter(resolved, 2) : String.format("%s, %s", resolved, I1.class);
    }

    @Test
    public void testTypePropagationBackward1Level() throws Exception {
        final Type referenceType = VariableRoot.class.getDeclaredField("dependency").getGenericType();
        final Type dependencyType = VariableDependency.class.getDeclaredConstructor(Object.class).getGenericParameterTypes()[0];
        final Type resolved = propagate(referenceType, dependencyType, true);

        assert Serializable.class == resolved : String.format("%s != %s", resolved, Serializable.class);
    }

    @Test
    public void testTypePropagationBackward2Levels() throws Exception {
        final Type referenceType = VariableRoot.class.getDeclaredField("dependency").getGenericType();
        final Type dependencyType = VariableChildDependency.class.getDeclaredConstructor(Object.class).getGenericParameterTypes()[0];
        final Type resolved = propagate(referenceType, dependencyType, true);

        assert Serializable.class == resolved : String.format("%s != %s", resolved, Serializable.class);
    }

    @Test
    public void testTypePropagationBackward2Steps2Levels() throws Exception {
        final Type referenceType1 = VariableRootStep1.class.getDeclaredField("dependency").getGenericType();
        final Type dynamicType1 = VariableRootStep2.class.getDeclaredField("dependency").getGenericType();
        final Type referenceType2 = propagate(referenceType1, dynamicType1, true);

        final Type dependencyType = VariableChildDependency.class.getDeclaredConstructor(Object.class).getGenericParameterTypes()[0];
        final Type resolved = propagate(referenceType2, dependencyType, true);

        assert Serializable.class == resolved : String.format("%s != %s", resolved, Serializable.class);
    }

    @Test
    public void testCanonicalType() throws Exception {

        @SuppressWarnings({ "WeakerAccess", "unused" })
        class X<T extends I1 & I2> {

            public Q q;

            public F1 f1;
            public F1<?> f2;
            public F1<T> f3;
            public F1<I4> f4;

            public C<F1> c1;
            public C<F1<?>> c2;
            public C<F1<T>> c3;
            public C<F1<I4>> c4;

            public A a1;
            public A<?, Q, Q> a2;
            public A<?, ?, Q> a3;
            public A<?, ?, ?> a4;
            public A<Q, Q, ?> a5;
            public A<Q, Q, Q> a6;

            public I i1;
            public I<?, ?, ?> i2;
            public I<T, ?, ?> i3;
            public I<T, ?, T> i4;
            public I<T, T, T> i5;

            public I<A, A, A> ia1;
            public I<A, A<?, ?, ?>, A> ia2;
            public I<A, A<?, ?, ?>, ?> ia3;
            public I<A<?, ?, ?>, A<?, ?, ?>, A<?, ?, ?>> ia4;
            public I<A<T, ?, ?>, A<?, T, ?>, A<?, ?, T>> ia5;
            public I<A<T, T, T>, A<T, T, T>, A<T, T, T>> ia6;
            public I<A<Q, Q, Q>, A, A> ia7;

            public I<?, ?, ?>[] aa1;
            public I<A<Q, Q, Q>, A, A>[] aa2;
        }

        checkCanonical(Class.class, X.class.getField("q").getGenericType());

        checkCanonical(Class.class, X.class.getField("f1").getGenericType());
        checkCanonical(Class.class, X.class.getField("f2").getGenericType());
        checkCanonical(Class.class, X.class.getField("f3").getGenericType());

        checkParameters(X.class.getField("f4").getGenericType(), I4.class);

        checkParameters(X.class.getField("c1").getGenericType(), F1.class);
        checkParameters(X.class.getField("c2").getGenericType(), F1.class);
        checkParameters(X.class.getField("c3").getGenericType(), F1.class);
        checkParameters(X.class.getField("c4").getGenericType(), X.class.getField("f4").getGenericType());

        checkCanonical(Class.class, X.class.getField("a1").getGenericType());
        checkParameters(X.class.getField("a2").getGenericType(), Q.class, Q.class, Q.class);
        checkParameters(X.class.getField("a3").getGenericType(), Q.class, Q.class, Q.class);
        checkCanonical(Class.class, X.class.getField("a4").getGenericType());
        checkParameters(X.class.getField("a5").getGenericType(), Q.class, Q.class, Q.class);
        checkParameters(X.class.getField("a6").getGenericType(), Q.class, Q.class, Q.class);

        checkCanonical(Class.class, X.class.getField("i1").getGenericType());
        checkCanonical(Class.class, X.class.getField("i2").getGenericType());
        checkCanonical(Class.class, X.class.getField("i3").getGenericType());
        checkCanonical(Class.class, X.class.getField("i4").getGenericType());
        checkCanonical(Class.class, X.class.getField("i5").getGenericType());

        checkParameters(X.class.getField("ia1").getGenericType(), A.class, A.class, A.class);
        checkParameters(X.class.getField("ia2").getGenericType(), A.class, A.class, A.class);
        checkParameters(X.class.getField("ia3").getGenericType(), A.class, A.class, A.class);
        checkParameters(X.class.getField("ia4").getGenericType(), A.class, A.class, A.class);
        checkParameters(X.class.getField("ia5").getGenericType(), A.class, A.class, A.class);
        checkParameters(X.class.getField("ia6").getGenericType(), A.class, A.class, A.class);

        checkParameters(X.class.getField("ia7").getGenericType(), X.class.getField("a6").getGenericType(), ParameterizedType.class, ParameterizedType.class);

        checkCanonical(Class.class, X.class.getField("aa1").getGenericType());
        checkParameters(X.class.getField("aa2").getGenericType(), X.class.getField("a6").getGenericType(), ParameterizedType.class, ParameterizedType.class);
    }

    private void checkCanonical(final Class<?> expected, final Type type) {
        final Type canonicalType = canonicalType(type);
        final Class<?> canonicalClass = canonicalType instanceof GenericArrayType
                                        ? ((GenericArrayType) canonicalType).getGenericComponentType().getClass()
                                        : canonicalType.getClass();

        assert expected.isAssignableFrom(canonicalClass) : canonicalClass;
    }

    private void checkParameters(final Type type, final Type... expected) {
        checkCanonical(ParameterizedType.class, type);

        checkParameters(expected, type instanceof GenericArrayType
                        ? (ParameterizedType) ((GenericArrayType) type).getGenericComponentType()
                        : (ParameterizedType) type);
    }

    private void checkParameters(final Type[] expected, final ParameterizedType type) {
        final Type[] arguments = type.getActualTypeArguments();
        assert arguments.length == expected.length : arguments.length;

        for (int i = 0; i < expected.length; i++) {
            checkCanonical(expected[i].getClass(), arguments[i]);
        }
    }

    private void checkText(final String actual, final String expected) {
        assert Objects.equals(expected, actual) : String.format("Expected %s, got %s", expected, actual);
    }

    private void checkParameters(final Class<?> enclosing, final Generics.Parameters params, final boolean generic1, final boolean generic2) {
        assert params.size() == 3 : params.size();
        assert params.genericType(0) == enclosing : params.genericType(0);

        assert rawType(params.genericType(1)) == P1.class : params.genericType(1);
        assert params.type(1) == P1.class : params.type(1);
        assert !generic1 || typeParameter(params.genericType(1), 0) == I.class : params.genericType(1);

        assert rawType(params.genericType(2)) == P2.class : params.genericType(2);
        assert params.type(2) == P2.class : params.type(2);
        assert !generic2 || typeParameter(params.genericType(2), 0) == I.class : params.genericType(2);

        assert params.annotations(0).length == 0 : Strings.formatObject(false, true, params.annotations(0));
        assert params.annotations(1)[0].annotationType() == A1.class : params.annotations(1)[0].annotationType();
        assert params.annotations(2)[0].annotationType() == A2.class : params.annotations(2)[0].annotationType();
    }

    private void checkParameters(final Class<?> enclosing,
                                 final Generics.Parameters params,
                                 final boolean generic1,
                                 final boolean generic2,
                                 final boolean generic3) {
        assert params.size() == 4 : params.size();
        assert params.genericType(0) == enclosing : params.genericType(0);

        assert rawType(params.genericType(1)) == P1.class : params.genericType(1);
        assert params.type(1) == P1.class : params.type(1);
        assert !generic1 || typeParameter(params.genericType(1), 0) == I.class : params.genericType(1);

        assert rawType(params.genericType(2)) == P2.class : params.genericType(2);
        assert params.type(2) == P2.class : params.type(2);
        assert !generic2 || typeParameter(params.genericType(2), 0) == I.class : params.genericType(2);

        assert rawType(params.genericType(3)) == P3.class : params.genericType(3);
        assert params.type(3) == P3.class : params.type(3);
        assert !generic3 || typeParameter(params.genericType(3), 0) == I.class : params.genericType(3);

        assert params.annotations(0).length == 0 : Strings.formatObject(false, true, params.annotations(0));

        // annotations on closure variables may be lost in some JVMs...
        assert params.annotations(1).length == 0 || params.annotations(1)[0].annotationType() == A1.class : Strings.formatObject(false,
                                                                                                                                 true,
                                                                                                                                 params.annotations(1));
        assert params.annotations(2).length == 0 || params.annotations(2)[0].annotationType() == A2.class : Strings.formatObject(false,
                                                                                                                                 true,
                                                                                                                                 params.annotations(2));
        assert params.annotations(3).length == 0 || params.annotations(3)[0].annotationType() == A3.class : Strings.formatObject(false,
                                                                                                                                 true,
                                                                                                                                 params.annotations(3));
    }

    private void checkRawType(final Type type, final Class<?> expected) {
        assert rawType(type) == expected : rawType(type);
    }

    private static class SomeClass implements Comparable<SomeClass> {

        @SuppressWarnings("UnusedParameters")
        public SomeClass(final Comparable<Serializable>[] sorters) { }

        public int compareTo(final SomeClass that) {
            throw new UnsupportedOperationException();
        }
    }

    private interface Q { }

    private interface I1 extends Q { }
    private interface I2 extends Q { }
    private interface I3 extends Q { }

    private static class A<R extends Q, S extends Q, T extends Q> {

        @SuppressWarnings("UnusedParameters")
        public A(final B<R, S> p1, final B<S, T>p2, final B<R, T>[] p3) { }
    }

    private static class B<A, B> {

        @SuppressWarnings("UnusedParameters")
        public B(final C<A> p1, final C<B> p2) { }
    }

    @SuppressWarnings({ "UnusedParameters", "unused" })
    private static class C<N> { }

    private static class D extends A<I1, I2, I3> {

        public D(final B<I1, I2> p1, final B<I2, I3> p2, final B<I1, I3>[] p3) {
            super(p1, p2, p3);
        }
    }

    @SuppressWarnings({ "UnusedParameters", "unused" })
    private interface I<R, S, T> { }

    private static class E<T> implements I<T, I2, I3> {

        @SuppressWarnings("UnusedParameters")
        public E(final B<T, I2> p1, final B<I2, I3> p2, final B<T, I3>[] p3) { }
    }

    @SuppressWarnings({ "UnusedParameters", "unused" })
    private interface F1<T extends I1 & I2> { }

    private static class G1 implements F1, I { }

    interface I4 extends I1, I2 { }

    private static class G2 implements F1<I4>, I<I1, I2, I3> { }

    private static class G3 extends G1 implements Q { }

    private static class G4 extends G2 { }

    private static class G5 implements I<I4, I4, Object> { }

    private static class G6<T> implements I<I4, I4, T> { }

    private static class G7<T> implements I<I1, I2, T> { }

    private static class G8<T> implements P<T> { }

    @SuppressWarnings({ "UnusedDeclaration" })
    private interface P<T> { }

    @SuppressWarnings({ "UnusedDeclaration" })
    private static class H {

        private <X> H(final I p1, final F1 p2, final I<I1, I2, I3> p3, final F1<I4> p4, final I[] p5, final I<? super I1, ? extends I2, ?> p6, final P<X> p7, final F1<I4>[][] p8) { }
    }

    @SuppressWarnings("UnusedDeclaration")
    private interface P1<T> { }

    @SuppressWarnings("UnusedDeclaration")
    private interface P2<T> { }

    @SuppressWarnings("UnusedDeclaration")
    private interface P3<T> { }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface A1 { }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface A2 { }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface A3 { }

    @SuppressWarnings("UnusedDeclaration")
    private class X1 {

        public X1(final @A1 P1 p1, final @A2 P2 p2) { }

        private class X2 {

            public X2(final @A1 P1<I> p1, final @A2 P2 p2) { }

            private class X3 {

                public X3(final @A1 P1<I> p1, final @A2 P2<I> p2) { }

                private class X4 {

                    public X4(final @A1 P1 p1, final @A2 P2<I> p2) { }
                }
            }
        }
    }

    @SuppressWarnings("unused")
    private enum E1 { A, B, C }

    private interface Decoy1<T> {}
    private interface Decoy2<T> {}
    private interface Decoy3<T> {}
    private interface Decoy4<T> {}

    private static class SuperType1<R, S, T> implements I<R, S, T> { }

    private static class SubType11<R, S> extends SuperType1<R, S, I1> implements Decoy4<I1> { }

    private static class SubType12<R> extends SubType11<R, I2> implements Decoy3<I2> { }

    private static class SubType13 extends SubType12<I3> implements Decoy2<I3> { }

    private static class SubType14 extends SubType13 implements Decoy1<Serializable> { }

    private interface SuperType2<R, S, T> extends I<R, S, T> { }

    private interface SubType21<R, S> extends Decoy4<I1>, SuperType2<R, S, I1> { }

    private interface SubType22<R> extends Decoy3<I2>, SubType21<R, I2> { }

    private interface SubType23 extends Decoy2<I3>, SubType22<I3> { }

    private static class SubType24 implements Decoy1<Serializable>, SubType23 { }

    private static class SuperType3<R, S, T> implements I<I1, I2, I3> { }

    private static class SubType3 extends SuperType3<I3, I2, I1> { }

    private static class SuperType4<R, S, T> implements I<R, S, T> { }

    private static class SubType4 extends SuperType4<I3, I2, I1> { }

    private interface SuperType5<R, S, T> extends I<I1, I2, I3> { }

    private static class SubType5 implements SuperType5<I3, I2, I1> { }

    private interface SuperType6<R, S, T> extends I<R, S, T> { }

    private static class SubType6 implements SuperType6<I1, I2, I3> { }

    private interface Reference<T> { }

    private static class VariableDependency<T> implements Reference<T> {

        private VariableDependency(final T dependency) {
            assert dependency != null;
        }
    }

    private static class VariableChildDependency<T> extends VariableDependency<T> {

        private VariableChildDependency(final T dependency) {
            super(dependency);
        }
    }

    private static class VariableRoot {

        final Reference<Serializable> dependency = null;
    }

    private static class VariableRootStep1 {

        final VariableRootStep2<Serializable> dependency = null;
    }

    private static class VariableRootStep2<T> {

        final Reference<T> dependency = null;
    }
}

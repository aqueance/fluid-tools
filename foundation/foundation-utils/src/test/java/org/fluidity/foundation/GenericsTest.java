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

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import org.testng.annotations.Test;

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
        checkRawType(Generics.typeParameter(SomeClass.class.getGenericInterfaces()[0], 0), SomeClass.class);
        checkRawType(Generics.typeParameter(GenericsTest.class, 0), null);
        checkRawType(Generics.typeParameter(F1.class, 0), Object.class);
        checkRawType(Generics.typeParameter(I.class, 1), Object.class);
        checkRawType(Generics.typeParameter(A.class, 2), Q.class);
    }

    @Test
    public void testArrays() throws Exception {
        checkRawType(Generics.arrayComponentType(SomeClass[].class), SomeClass.class);
        checkRawType(Generics.arrayComponentType(SomeClass.class.getConstructor(Comparable[].class).getGenericParameterTypes()[0]), Comparable.class);
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

        assert B.class == Generics.rawType(Generics.propagate(reference, paramA0));
        assert B.class == Generics.rawType(Generics.propagate(reference, paramA1));
        assert B[].class == Generics.rawType(Generics.propagate(reference, paramA2));

        assert I1.class == Generics.resolve(reference, (TypeVariable) paramA0.getActualTypeArguments()[0]);
        assert I2.class == Generics.resolve(reference, (TypeVariable) paramA0.getActualTypeArguments()[1]);
        assert I2.class == Generics.resolve(reference, (TypeVariable) paramA1.getActualTypeArguments()[0]);
        assert I3.class == Generics.resolve(reference, (TypeVariable) paramA1.getActualTypeArguments()[1]);
        assert I1.class == Generics.resolve(reference, (TypeVariable) ((ParameterizedType) Generics.arrayComponentType(paramA2)).getActualTypeArguments()[0]);
        assert I3.class == Generics.resolve(reference, (TypeVariable) ((ParameterizedType) Generics.arrayComponentType(paramA2)).getActualTypeArguments()[1]);

        final Type inheritedA0B0 = Generics.propagate(Generics.propagate(reference, paramA0), paramB0);
        final Type inheritedA0B1 = Generics.propagate(Generics.propagate(reference, paramA0), paramB1);
        final Type inheritedA1B0 = Generics.propagate(Generics.propagate(reference, paramA1), paramB0);
        final Type inheritedA1B1 = Generics.propagate(Generics.propagate(reference, paramA1), paramB1);
        final Type inheritedA2B0 = Generics.propagate(Generics.propagate(reference, paramA2), paramB0);
        final Type inheritedA2B1 = Generics.propagate(Generics.propagate(reference, paramA2), paramB1);

        assert Generics.rawType(inheritedA0B0) == C.class;
        assert Generics.rawType(inheritedA0B1) == C.class;
        assert Generics.rawType(inheritedA1B0) == C.class;
        assert Generics.rawType(inheritedA1B1) == C.class;
        assert Generics.rawType(inheritedA2B0) == C.class;
        assert Generics.rawType(inheritedA2B1) == C.class;

        assert I1.class == Generics.typeParameter(inheritedA0B0, 0);
        assert I2.class == Generics.typeParameter(inheritedA0B1, 0);
        assert I2.class == Generics.typeParameter(inheritedA1B0, 0);
        assert I3.class == Generics.typeParameter(inheritedA1B1, 0);
        assert I1.class == Generics.typeParameter(inheritedA2B0, 0);
        assert I3.class == Generics.typeParameter(inheritedA2B1, 0);
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

        assert B.class == Generics.rawType(Generics.propagate(reference, paramA0));
        assert B.class == Generics.rawType(Generics.propagate(reference, paramA1));
        assert B[].class == Generics.rawType(Generics.propagate(reference, paramA2));

        assert I1.class == paramA0.getActualTypeArguments()[0];
        assert I2.class == paramA0.getActualTypeArguments()[1];
        assert I2.class == paramA1.getActualTypeArguments()[0];
        assert I3.class == paramA1.getActualTypeArguments()[1];
        assert I1.class == ((ParameterizedType) Generics.arrayComponentType(paramA2)).getActualTypeArguments()[0];
        assert I3.class == ((ParameterizedType) Generics.arrayComponentType(paramA2)).getActualTypeArguments()[1];

        final Type inheritedA0B0 = Generics.propagate(Generics.propagate(reference, paramA0), paramB0);
        final Type inheritedA0B1 = Generics.propagate(Generics.propagate(reference, paramA0), paramB1);
        final Type inheritedA1B0 = Generics.propagate(Generics.propagate(reference, paramA1), paramB0);
        final Type inheritedA1B1 = Generics.propagate(Generics.propagate(reference, paramA1), paramB1);
        final Type inheritedA2B0 = Generics.propagate(Generics.propagate(reference, paramA2), paramB0);
        final Type inheritedA2B1 = Generics.propagate(Generics.propagate(reference, paramA2), paramB1);

        assert Generics.rawType(inheritedA0B0) == C.class;
        assert Generics.rawType(inheritedA0B1) == C.class;
        assert Generics.rawType(inheritedA1B0) == C.class;
        assert Generics.rawType(inheritedA1B1) == C.class;
        assert Generics.rawType(inheritedA2B0) == C.class;
        assert Generics.rawType(inheritedA2B1) == C.class;

        assert I1.class == Generics.typeParameter(inheritedA0B0, 0);
        assert I2.class == Generics.typeParameter(inheritedA0B1, 0);
        assert I2.class == Generics.typeParameter(inheritedA1B0, 0);
        assert I3.class == Generics.typeParameter(inheritedA1B1, 0);
        assert I1.class == Generics.typeParameter(inheritedA2B0, 0);
        assert I3.class == Generics.typeParameter(inheritedA2B1, 0);
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

        assert B.class == Generics.rawType(Generics.propagate(reference, paramA0));
        assert B.class == Generics.rawType(Generics.propagate(reference, paramA1));
        assert B[].class == Generics.rawType(Generics.propagate(reference, paramA2));

        assert I1.class == Generics.resolve(reference, (TypeVariable) paramA0.getActualTypeArguments()[0]);
        assert I2.class == paramA0.getActualTypeArguments()[1];
        assert I2.class == paramA1.getActualTypeArguments()[0];
        assert I3.class == paramA1.getActualTypeArguments()[1];
        assert I1.class == Generics.resolve(reference, (TypeVariable) ((ParameterizedType) Generics.arrayComponentType(paramA2)).getActualTypeArguments()[0]);
        assert I3.class == ((ParameterizedType) Generics.arrayComponentType(paramA2)).getActualTypeArguments()[1];

        final Type inheritedA0B0 = Generics.propagate(Generics.propagate(reference, paramA0), paramB0);
        final Type inheritedA0B1 = Generics.propagate(Generics.propagate(reference, paramA0), paramB1);
        final Type inheritedA1B0 = Generics.propagate(Generics.propagate(reference, paramA1), paramB0);
        final Type inheritedA1B1 = Generics.propagate(Generics.propagate(reference, paramA1), paramB1);
        final Type inheritedA2B0 = Generics.propagate(Generics.propagate(reference, paramA2), paramB0);
        final Type inheritedA2B1 = Generics.propagate(Generics.propagate(reference, paramA2), paramB1);

        assert Generics.rawType(inheritedA0B0) == C.class;
        assert Generics.rawType(inheritedA0B1) == C.class;
        assert Generics.rawType(inheritedA1B0) == C.class;
        assert Generics.rawType(inheritedA1B1) == C.class;
        assert Generics.rawType(inheritedA2B0) == C.class;
        assert Generics.rawType(inheritedA2B1) == C.class;

        assert I1.class == Generics.typeParameter(inheritedA0B0, 0);
        assert I2.class == Generics.typeParameter(inheritedA0B1, 0);
        assert I2.class == Generics.typeParameter(inheritedA1B0, 0);
        assert I3.class == Generics.typeParameter(inheritedA1B1, 0);
        assert I1.class == Generics.typeParameter(inheritedA2B0, 0);
        assert I3.class == Generics.typeParameter(inheritedA2B1, 0);
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

        assert B.class == Generics.rawType(Generics.propagate(reference, paramA0));
        assert B.class == Generics.rawType(Generics.propagate(reference, paramA1));
        assert B[].class == Generics.rawType(Generics.propagate(reference, paramA2));

        assert Q.class == Generics.resolve(reference, (TypeVariable) paramA0.getActualTypeArguments()[0]);
        assert Q.class == Generics.resolve(reference, (TypeVariable) paramA0.getActualTypeArguments()[1]);
        assert Q.class == Generics.resolve(reference, (TypeVariable) paramA1.getActualTypeArguments()[0]);
        assert Q.class == Generics.resolve(reference, (TypeVariable) paramA1.getActualTypeArguments()[1]);
        assert Q.class == Generics.resolve(reference, (TypeVariable) ((ParameterizedType) Generics.arrayComponentType(paramA2)).getActualTypeArguments()[0]);
        assert Q.class == Generics.resolve(reference, (TypeVariable) ((ParameterizedType) Generics.arrayComponentType(paramA2)).getActualTypeArguments()[1]);

        final Type inheritedA0B0 = Generics.propagate(Generics.propagate(reference, paramA0), paramB0);
        final Type inheritedA0B1 = Generics.propagate(Generics.propagate(reference, paramA0), paramB1);
        final Type inheritedA1B0 = Generics.propagate(Generics.propagate(reference, paramA1), paramB0);
        final Type inheritedA1B1 = Generics.propagate(Generics.propagate(reference, paramA1), paramB1);
        final Type inheritedA2B0 = Generics.propagate(Generics.propagate(reference, paramA2), paramB0);
        final Type inheritedA2B1 = Generics.propagate(Generics.propagate(reference, paramA2), paramB1);

        assert Generics.rawType(inheritedA0B0) == C.class;
        assert Generics.rawType(inheritedA0B1) == C.class;
        assert Generics.rawType(inheritedA1B0) == C.class;
        assert Generics.rawType(inheritedA1B1) == C.class;
        assert Generics.rawType(inheritedA2B0) == C.class;
        assert Generics.rawType(inheritedA2B1) == C.class;

        assert Q.class == Generics.typeParameter(inheritedA0B0, 0);
        assert Q.class == Generics.typeParameter(inheritedA0B1, 0);
        assert Q.class == Generics.typeParameter(inheritedA1B0, 0);
        assert Q.class == Generics.typeParameter(inheritedA1B1, 0);
        assert Q.class == Generics.typeParameter(inheritedA2B0, 0);
        assert Q.class == Generics.typeParameter(inheritedA2B1, 0);
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

        assert B.class == Generics.rawType(Generics.propagate(reference, paramA0));
        assert B.class == Generics.rawType(Generics.propagate(reference, paramA1));
        assert B[].class == Generics.rawType(Generics.propagate(reference, paramA2));

        assert Q.class == Generics.resolve(reference, (TypeVariable) paramA0.getActualTypeArguments()[0]);
        assert Q.class == Generics.resolve(reference, (TypeVariable) paramA0.getActualTypeArguments()[1]);
        assert Q.class == Generics.resolve(reference, (TypeVariable) paramA1.getActualTypeArguments()[0]);
        assert Q.class == Generics.resolve(reference, (TypeVariable) paramA1.getActualTypeArguments()[1]);
        assert Q.class == Generics.resolve(reference, (TypeVariable) ((ParameterizedType) Generics.arrayComponentType(paramA2)).getActualTypeArguments()[0]);
        assert Q.class == Generics.resolve(reference, (TypeVariable) ((ParameterizedType) Generics.arrayComponentType(paramA2)).getActualTypeArguments()[1]);

        final Type inheritedA0B0 = Generics.propagate(Generics.propagate(reference, paramA0), paramB0);
        final Type inheritedA0B1 = Generics.propagate(Generics.propagate(reference, paramA0), paramB1);
        final Type inheritedA1B0 = Generics.propagate(Generics.propagate(reference, paramA1), paramB0);
        final Type inheritedA1B1 = Generics.propagate(Generics.propagate(reference, paramA1), paramB1);
        final Type inheritedA2B0 = Generics.propagate(Generics.propagate(reference, paramA2), paramB0);
        final Type inheritedA2B1 = Generics.propagate(Generics.propagate(reference, paramA2), paramB1);

        assert Generics.rawType(inheritedA0B0) == C.class;
        assert Generics.rawType(inheritedA0B1) == C.class;
        assert Generics.rawType(inheritedA1B0) == C.class;
        assert Generics.rawType(inheritedA1B1) == C.class;
        assert Generics.rawType(inheritedA2B0) == C.class;
        assert Generics.rawType(inheritedA2B1) == C.class;

        assert Q.class == Generics.typeParameter(inheritedA0B0, 0);
        assert Q.class == Generics.typeParameter(inheritedA0B1, 0);
        assert Q.class == Generics.typeParameter(inheritedA1B0, 0);
        assert Q.class == Generics.typeParameter(inheritedA1B1, 0);
        assert Q.class == Generics.typeParameter(inheritedA2B0, 0);
        assert Q.class == Generics.typeParameter(inheritedA2B1, 0);
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

        assert Generics.propagate(reference, paramA0).equals(Generics.propagate(reference, paramA0));
        assert Generics.propagate(reference, paramA0).equals(paramD0);
        assert paramD0.equals(Generics.propagate(reference, paramA0));

        assert Generics.propagate(reference, paramA1).equals(Generics.propagate(reference, paramA1));
        assert Generics.propagate(reference, paramA1).equals(paramD1);
        assert paramD1.equals(Generics.propagate(reference, paramA1));

        assert Generics.propagate(reference, paramA2).equals(Generics.propagate(reference, paramA2));
        assert Generics.propagate(reference, paramA2).equals(paramD2);
        assert paramD2.equals(Generics.propagate(reference, paramA2));
    }

    @Test
    public void testSpecialization() throws Exception {
        final Type type1 = Generics.specializedType(G1.class, F1.class);
        assert type1 == F1.class : type1;

        final Type type2 = Generics.specializedType(G1.class, I.class);
        assert type2 == I.class : type2;

        final Type type3 = Generics.specializedType(G3.class, F1.class);
        assert type3 == F1.class : type3;

        final Type type4 = Generics.specializedType(G3.class, I.class);
        assert type4 == I.class : type4;

        final Type type5 = Generics.specializedType(G3.class, Q.class);
        assert type5 == Q.class : type5;

        final Type type6 = Generics.specializedType(G2.class, F1.class);
        assert Generics.rawType(type6) == F1.class : type6;
        assert type6 instanceof ParameterizedType : type6;
        assert Generics.typeParameter(type6, 0) == I4.class : type6;

        final Type type7 = Generics.specializedType(G2.class, I.class);
        assert Generics.rawType(type7) == I.class : type7;
        assert type7 instanceof ParameterizedType : type7;
        assert Generics.typeParameter(type7, 0) == I1.class : type7;
        assert Generics.typeParameter(type7, 1) == I2.class : type7;
        assert Generics.typeParameter(type7, 2) == I3.class : type7;

        final Type type8 = Generics.specializedType(G4.class, F1.class);
        assert Generics.rawType(type8) == F1.class : type8;
        assert type8 instanceof ParameterizedType : type8;
        assert Generics.typeParameter(type8, 0) == I4.class : type8;

        final Type type9 = Generics.specializedType(G4.class, I.class);
        assert Generics.rawType(type9) == I.class : type9;
        assert type9 instanceof ParameterizedType : type9;
        assert Generics.typeParameter(type9, 0) == I1.class : type9;
        assert Generics.typeParameter(type9, 1) == I2.class : type9;
        assert Generics.typeParameter(type9, 2) == I3.class : type9;
    }

    @Test
    public void testAssignmentChecks() throws Exception {

        // generic parameter types: I, F, I<I1, I2, I3>, F<I4>, I[], I<? super I4, ?, ?>, P<T>, F1<I4>[][]
        final Type[] types = H.class.getDeclaredConstructors()[0].getGenericParameterTypes();

        assert Generics.isAssignable(types[0], G1.class);
        assert !Generics.isAssignable(G1.class, types[0]);
        assert Generics.isAssignable(types[1], G1.class);
        assert !Generics.isAssignable(G1.class, types[1]);

        assert Generics.isAssignable(types[0], G3.class);
        assert !Generics.isAssignable(G3.class, types[0]);
        assert Generics.isAssignable(types[1], G3.class);
        assert !Generics.isAssignable(G3.class, types[1]);

        assert !Generics.isAssignable(types[2], G1.class);
        assert !Generics.isAssignable(G1.class, types[2]);
        assert Generics.isAssignable(types[2], G2.class);
        assert !Generics.isAssignable(G2.class, types[2]);
        assert !Generics.isAssignable(types[3], G1.class);
        assert !Generics.isAssignable(G1.class, types[3]);
        assert Generics.isAssignable(types[3], G2.class);
        assert !Generics.isAssignable(G2.class, types[3]);

        assert !Generics.isAssignable(types[2], G3.class);
        assert !Generics.isAssignable(G3.class, types[2]);
        assert Generics.isAssignable(types[2], G4.class);
        assert !Generics.isAssignable(G4.class, types[2]);
        assert !Generics.isAssignable(types[3], G3.class);
        assert !Generics.isAssignable(G3.class, types[3]);
        assert Generics.isAssignable(types[3], G4.class);
        assert !Generics.isAssignable(G4.class, types[3]);

        assert Generics.isAssignable(types[4], G1[].class);
        assert !Generics.isAssignable(G1[].class, types[4]);
        assert Generics.isAssignable(types[4], G3[].class);
        assert !Generics.isAssignable(G3[].class, types[4]);

        assert Generics.isAssignable(types[5], G4.class);
        assert !Generics.isAssignable(G4.class, types[5]);
        assert !Generics.isAssignable(types[5], G5.class);
        assert !Generics.isAssignable(G5.class, types[5]);
        assert !Generics.isAssignable(types[5], G6.class);
        assert !Generics.isAssignable(G6.class, types[5]);
        assert !Generics.isAssignable(types[5], G7.class);
        assert !Generics.isAssignable(G7.class, types[5]);

        assert Generics.isAssignable(types[6], G8.class);
        assert !Generics.isAssignable(G8.class, types[6]);

        assert Generics.isAssignable(Object.class, types[0]);
        assert Generics.isAssignable(Object.class, types[1]);
        assert Generics.isAssignable(Object.class, types[2]);
        assert Generics.isAssignable(Object.class, types[3]);
        assert Generics.isAssignable(Object.class, types[4]);
        assert Generics.isAssignable(Object.class, types[5]);
        assert Generics.isAssignable(Object.class, types[6]);

        assert !Generics.isAssignable(types[0], Object.class);
        assert !Generics.isAssignable(types[1], Object.class);
        assert !Generics.isAssignable(types[2], Object.class);
        assert !Generics.isAssignable(types[3], Object.class);
        assert !Generics.isAssignable(types[4], Object.class);
        assert !Generics.isAssignable(types[5], Object.class);
        assert !Generics.isAssignable(types[6], Object.class);
    }

    @Test
    public void testNonStaticInnerClasses() throws Exception {
        checkParameters(GenericsTest.class, Generics.describe(X1.class.getConstructors()[0]), false, false);
        checkParameters(X1.class, Generics.describe(X1.X2.class.getConstructors()[0]), true, false);
        checkParameters(X1.X2.class, Generics.describe(X1.X2.X3.class.getConstructors()[0]), true, true);
        checkParameters(X1.X2.X3.class, Generics.describe(X1.X2.X3.X4.class.getConstructors()[0]), false, true);

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

        checkParameters(GenericsTest.class, Generics.describe(Closure1.class.getConstructors()[0]), false, false, false);
        checkParameters(GenericsTest.class, Generics.describe(Closure2.class.getConstructors()[0]), true, false, false);

        final Generics.Parameters params = Generics.describe(E1.class.getDeclaredConstructors()[0]);
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
    public void testTypePropagation() throws Exception {
        final Type specialized = Generics.specializedType(SubType4.class, I.class);
        final Type resolved = Generics.propagate(SubType4.class, specialized);

        assert I3.class == Generics.typeParameter(resolved, 0) : String.format("%s, %s", resolved, I3.class);
        assert I2.class == Generics.typeParameter(resolved, 1) : String.format("%s, %s", resolved, I2.class);
        assert I1.class == Generics.typeParameter(resolved, 2) : String.format("%s, %s", resolved, I1.class);
    }

    private void checkText(final String actual, final String expected) {
        assert expected.equals(actual) : String.format("Expected %s, got %s", expected, actual);
    }

    private void checkParameters(final Class<?> enclosing, final Generics.Parameters params, final boolean generic1, final boolean generic2) {
        assert params.size() == 3 : params.size();
        assert params.genericType(0) == enclosing : params.genericType(0);

        assert Generics.rawType(params.genericType(1)) == P1.class : params.genericType(1);
        assert params.type(1) == P1.class : params.type(1);
        assert !generic1 || Generics.typeParameter(params.genericType(1), 0) == I.class : params.genericType(1);

        assert Generics.rawType(params.genericType(2)) == P2.class : params.genericType(2);
        assert params.type(2) == P2.class : params.type(2);
        assert !generic2 || Generics.typeParameter(params.genericType(2), 0) == I.class : params.genericType(2);

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

        assert Generics.rawType(params.genericType(1)) == P1.class : params.genericType(1);
        assert params.type(1) == P1.class : params.type(1);
        assert !generic1 || Generics.typeParameter(params.genericType(1), 0) == I.class : params.genericType(1);

        assert Generics.rawType(params.genericType(2)) == P2.class : params.genericType(2);
        assert params.type(2) == P2.class : params.type(2);
        assert !generic2 || Generics.typeParameter(params.genericType(2), 0) == I.class : params.genericType(2);

        assert Generics.rawType(params.genericType(3)) == P3.class : params.genericType(3);
        assert params.type(3) == P3.class : params.type(3);
        assert !generic3 || Generics.typeParameter(params.genericType(3), 0) == I.class : params.genericType(3);

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
        assert Generics.rawType(type) == expected : Generics.rawType(type);
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

    @SuppressWarnings("UnusedParameters")
    private static class C<N> { }

    private static class D extends A<I1, I2, I3> {

        public D(final B<I1, I2> p1, final B<I2, I3> p2, final B<I1, I3>[] p3) {
            super(p1, p2, p3);
        }
    }

    @SuppressWarnings("UnusedParameters")
    private interface I<R, S, T> { }

    private static class E<T> implements I<T, I2, I3> {

        @SuppressWarnings("UnusedParameters")
        public E(final B<T, I2> p1, final B<I2, I3> p2, final B<T, I3>[] p3) { }
    }

    @SuppressWarnings("UnusedParameters")
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

    private enum E1 { A, B, C }

    private static class SuperType<R, S, T> implements I<R, S, T> { }

    private static class SubType1<R, S> extends SuperType<R, S, I1> { }

    private static class SubType2<R> extends SubType1<R, I2> { }

    private static class SubType3 extends SubType2<I3> { }

    private static class SubType4 extends SubType3 { }
}

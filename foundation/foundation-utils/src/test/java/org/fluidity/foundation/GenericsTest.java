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
        assert C.class == Generics.rawType(Generics.propagate(reference, paramB0));
        assert C.class == Generics.rawType(Generics.propagate(reference, paramB1));

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
        assert C.class == Generics.rawType(Generics.propagate(reference, paramB0));
        assert C.class == Generics.rawType(Generics.propagate(reference, paramB1));

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
        assert C.class == Generics.rawType(Generics.propagate(reference, paramB0));
        assert C.class == Generics.rawType(Generics.propagate(reference, paramB1));

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
        assert C.class == Generics.rawType(Generics.propagate(reference, paramB0));
        assert C.class == Generics.rawType(Generics.propagate(reference, paramB1));

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
        assert C.class == Generics.rawType(Generics.propagate(reference, paramB0));
        assert C.class == Generics.rawType(Generics.propagate(reference, paramB1));

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

    @SuppressWarnings({ "UnusedDeclaration" })
    private static class C<N> { }

    @SuppressWarnings({ "UnusedDeclaration" })
    private static class D extends A<I1, I2, I3> {

        public D(final B<I1, I2> p1, final B<I2, I3> p2, final B<I1, I3>[] p3) {
            super(p1, p2, p3);
        }
    }

    @SuppressWarnings({ "UnusedDeclaration" })
    private interface I<R, S, T> { }

    @SuppressWarnings({ "UnusedDeclaration" })
    private static class E<T> implements I<T, I2, I3> {

        public E(final B<T, I2> p1, final B<I2, I3> p2, final B<T, I3>[] p3) { }
    }
}

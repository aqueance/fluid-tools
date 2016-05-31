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

package org.fluidity.composition.container.tests;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.Inject;
import org.fluidity.composition.Optional;
import org.fluidity.composition.Qualifier;
import org.fluidity.composition.spi.ComponentFactory;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
public final class CircularReferencesTests extends AbstractContainerTests {

    private static final AtomicBoolean loopback = new AtomicBoolean();

    public CircularReferencesTests(final ArtifactFactory factory) {
        super(factory);
    }

    @BeforeMethod
    public void setUp() throws Exception {
        loopback.set(false);
    }

    @Test(expectedExceptions = ComponentContainer.CircularReferencesException.class)
    public void circularTwoWayReferences() throws Exception {
        registry.bindComponent(Circular2Dependent1Class.class);
        registry.bindComponent(Circular2Dependent2Class.class);

        container.getComponent(Circular2Dependent1Class.class);
    }

    @Test(expectedExceptions = ComponentContainer.CircularReferencesException.class, expectedExceptionsMessageRegExp = ".*Circular2Dependent1.*Circular2Dependent1.*")
    public void circularTwoWayCalls() throws Exception {
        registry.bindComponent(Circular2Dependent1Impl.class);
        registry.bindComponent(Circular2Dependent2Impl.class);

        container.getComponent(Circular2Dependent1.class);
    }

    @Test(expectedExceptions = ComponentContainer.CircularReferencesException.class)
    public void circularThreeWayReferences() throws Exception {
        registry.bindComponent(Circular3Dependent1Class.class);
        registry.bindComponent(Circular3Dependent2Class.class);
        registry.bindComponent(Circular3Dependent3Class.class);

        container.getComponent(Circular3Dependent1Class.class);
    }

    @Test(expectedExceptions = ComponentContainer.CircularReferencesException.class, expectedExceptionsMessageRegExp = ".*Circular3Dependent1.*Circular3Dependent1.*")
    public void circularThreeWayCalls() throws Exception {
        registry.bindComponent(Circular3Dependent1Impl.class);
        registry.bindComponent(Circular3Dependent2Impl.class);
        registry.bindComponent(Circular3Dependent3Impl.class);

        container.getComponent(Circular3Dependent1.class);
    }

    @Test(expectedExceptions = ComponentContainer.CircularReferencesException.class, expectedExceptionsMessageRegExp = ".*Circular3IntermediateDependent1Class.*Circular3IntermediateDependent1Class.*")
    public void circularThreeWayIntermediateInstantiation() throws Exception {
        registry.bindComponent(Circular3IntermediateDependent1Class.class);
        registry.bindComponent(Circular3IntermediateDependent2Class.class);
        registry.bindComponent(Circular3IntermediateDependent3Class.class);

        container.getComponent(Circular3IntermediateDependent1Class.class);
    }

    @Test(expectedExceptions = ComponentContainer.CircularReferencesException.class, expectedExceptionsMessageRegExp = ".*CircularConstructor1.*CircularConstructor1.*")
    public void circularInstantiationAndConstructorCalls0() throws Exception {
        registry.bindComponent(CircularConstructor1Impl.class);
        registry.bindComponent(CircularConstructor2Impl.class);
        registry.bindComponent(CircularConstructor3Impl.class);
        container.getComponent(CircularConstructor1.class);
    }

    @Test(expectedExceptions = ComponentContainer.CircularReferencesException.class, expectedExceptionsMessageRegExp = ".*CircularConstructor1.*CircularConstructor1.*")
    public void circularInstantiationAndConstructorCalls1() throws Exception {
        registry.bindComponent(CircularConstructor1Impl.class);
        registry.bindComponent(CircularConstructor2Impl.class);
        registry.bindComponent(CircularConstructor3Impl.class);
        container.getComponent(CircularConstructor2.class);
    }

    @Test(expectedExceptions = ComponentContainer.CircularReferencesException.class, expectedExceptionsMessageRegExp = ".*CircularConstructor1.*CircularConstructor1.*")
    public void circularInstantiationAndConstructorCalls2() throws Exception {
        registry.bindComponent(CircularConstructor1Impl.class);
        registry.bindComponent(CircularConstructor2Impl.class);
        registry.bindComponent(CircularConstructor3Impl.class);
        container.getComponent(CircularConstructor3.class);
    }

    @DataProvider(name = "group-references")
    public Object[][] groupReferences() {
        return new Object[][] {
                new Object[] { MemberDependent.class },
                new Object[] { GroupDependent.class },
        };
    }

    @Test(dataProvider = "group-references", expectedExceptions = ComponentContainer.CircularReferencesException.class, expectedExceptionsMessageRegExp = ".*RecursiveGroup\\[\\].*RecursiveGroup\\[\\].*")
    public void testCircularGroupReferenceDifferentContexts(final Class<? extends HeadInterface> reference) throws Exception {
        registry.bindComponent(HeadClass.class);
        registry.bindComponent(reference);
        registry.bindComponent(GroupMember1.class);

        container.getComponent(HeadClass.class);
    }

    @Test(dataProvider = "group-references", expectedExceptions = ComponentContainer.CircularReferencesException.class, expectedExceptionsMessageRegExp = ".*RecursiveGroup\\[\\].*RecursiveGroup\\[\\].*")
    public void testCircularGroupReferenceSameContext(final Class<? extends HeadInterface> reference) throws Exception {
        registry.bindComponent(HeadClass.class);
        registry.bindComponent(reference);
        registry.bindComponent(GroupMember2.class);

        container.getComponent(HeadClass.class);
    }

    @Test(dataProvider = "group-references", expectedExceptions = ComponentContainer.CircularReferencesException.class, expectedExceptionsMessageRegExp = ".*RecursiveGroup\\[\\].*RecursiveGroup\\[\\].*")
    public void testDualCircularGroupReferenceDifferentContexts(final Class<? extends HeadInterface> reference) throws Exception {
        registry.bindComponent(HeadClass.class);
        registry.bindComponent(reference);
        registry.bindComponent(GroupMember1.class);
        registry.bindComponent(GroupMember2.class);

        container.getComponent(HeadClass.class);
    }

    @Test(dataProvider = "group-references", expectedExceptions = ComponentContainer.CircularReferencesException.class, expectedExceptionsMessageRegExp = ".*RecursiveGroup\\[\\].*RecursiveGroup\\[\\].*")
    public void testDualCircularGroupReferenceSameContexts(final Class<? extends HeadInterface> reference) throws Exception {
        registry.bindComponent(HeadClass.class);
        registry.bindComponent(reference);
        registry.bindComponent(GroupMember1.class);
        registry.bindComponent(GroupMember3.class);

        container.getComponent(HeadClass.class);
    }

    @Test(dataProvider = "group-references", expectedExceptions = ComponentContainer.CircularReferencesException.class, expectedExceptionsMessageRegExp = ".*RecursiveGroup\\[\\].*RecursiveGroup\\[\\].*")
    public void testCircularGroupInstantiation(final Class<? extends HeadInterface> reference) throws Exception {
        registry.bindComponent(HeadClass.class);
        registry.bindComponent(reference);
        registry.bindComponent(GroupMember1.class);

        container.getComponent(HeadClass.class);
    }

    @Test
    public void testContextAwareFactory0() throws Exception {
        registry.bindComponent(A.class);
        registry.bindComponent(BF.class);
        registry.bindComponent(C0.class);

        final A a = container.getComponent(A.class);

        assert a != null : A.class;
        assert a.b != null : B.class;
        assert a.b.dependency != null : C0.class;
        assert ((C0) a.b.dependency).b != null : C0.class;
        assert ((C0) a.b.dependency).b != a.b : B.class;
        assert ((C0) a.b.dependency).b.dependency == null : B.class;
    }

    @Test(expectedExceptions = ComponentContainer.CircularReferencesException.class, expectedExceptionsMessageRegExp = ".*CircularReferencesTests\\.B.*CircularReferencesTests\\.B.*")
    public void testContextAwareFactory1() throws Exception {
        registry.bindComponent(A.class);
        registry.bindComponent(BF.class);
        registry.bindComponent(C1.class);

        container.getComponent(A.class);
    }

    @Test(expectedExceptions = ComponentContainer.CircularReferencesException.class, expectedExceptionsMessageRegExp = ".*CircularReferencesTests\\.A.*CircularReferencesTests\\.A.*")
    public void testContextAwareFactory2() throws Exception {
        registry.bindComponent(A.class);
        registry.bindComponent(BF.class);
        registry.bindComponent(C2.class);

        container.getComponent(A.class);
    }

    private interface Circular2Dependent1 { }

    private interface Circular2Dependent2 { }

    @Component(automatic = false)
    private static class Circular2Dependent1Impl implements Circular2Dependent1 {

        @SuppressWarnings("UnusedParameters")
        public Circular2Dependent1Impl(final Circular2Dependent2 dependency) { }
    }

    @Component(automatic = false)
    private static class Circular2Dependent2Impl implements Circular2Dependent2 {

        @SuppressWarnings("UnusedParameters")
        public Circular2Dependent2Impl(final Circular2Dependent1 dependency) { }
    }

    private interface Circular3Dependent1 { }

    private interface Circular3Dependent2 { }

    private interface Circular3Dependent3 { }

    @Component(automatic = false)
    private static class Circular3Dependent1Impl implements Circular3Dependent1 {

        @SuppressWarnings("UnusedParameters")
        public Circular3Dependent1Impl(final Circular3Dependent2 dependency) {
            assert false;
        }
    }

    @Component(automatic = false)
    private static class Circular3Dependent2Impl implements Circular3Dependent2 {

        @SuppressWarnings("UnusedParameters")
        public Circular3Dependent2Impl(final Circular3Dependent3 dependency) {
            assert false;
        }
    }

    @Component(automatic = false)
    private static class Circular3Dependent3Impl implements Circular3Dependent3 {

        @SuppressWarnings("UnusedParameters")
        public Circular3Dependent3Impl(final Circular3Dependent1 dependency) {
            assert false;
        }
    }

    @Component(automatic = false)
    private static class Circular2Dependent1Class  {

        @SuppressWarnings("UnusedParameters")
        public Circular2Dependent1Class(final Circular2Dependent2Class dependency) { }
    }

    @Component(automatic = false)
    private static class Circular2Dependent2Class {

        @SuppressWarnings("UnusedParameters")
        public Circular2Dependent2Class(final Circular2Dependent1Class dependency) { }
    }

    @Component(api = Circular3Dependent1Class.class, automatic = false)
    private static class Circular3Dependent1Class {

        @SuppressWarnings("UnusedParameters")
        public Circular3Dependent1Class(final Circular3Dependent2Class dependency) {
            assert false;
        }
    }

    @Component(api = Circular3Dependent2Class.class, automatic = false)
    private static class Circular3Dependent2Class {

        @SuppressWarnings("UnusedParameters")
        public Circular3Dependent2Class(final Circular3Dependent3Class dependency) {
            assert false;
        }
    }

    @Component(api = Circular3Dependent3Class.class, automatic = false)
    private static class Circular3Dependent3Class {

        @SuppressWarnings("UnusedParameters")
        public Circular3Dependent3Class(final Circular3Dependent1Class dependency) {
            assert false;
        }
    }

    @Component(api = Circular3IntermediateDependent1Class.class, automatic = false)
    private static class Circular3IntermediateDependent1Class {

        @SuppressWarnings("UnusedParameters")
        public Circular3IntermediateDependent1Class(final Circular3IntermediateDependent2Class dependency) {
            assert false;
        }
    }

    @Component(api = Circular3IntermediateDependent2Class.class, automatic = false)
    private static class Circular3IntermediateDependent2Class {

        @SuppressWarnings("UnusedParameters")
        public Circular3IntermediateDependent2Class(final Circular3IntermediateDependent3 dependency) {
            assert false;
        }
    }

    @Component(automatic = false)
    private interface Circular3IntermediateDependent3 { }

    @Component(automatic = false)
    private static class Circular3IntermediateDependent3Class implements Circular3IntermediateDependent3 {

        @SuppressWarnings("UnusedParameters")
        public Circular3IntermediateDependent3Class(final Circular3IntermediateDependent1Class dependency) {
            assert false;
        }
    }

    @Component(automatic = false)
    private interface CircularConstructor1 { }

    @Component(automatic = false)
    private interface CircularConstructor2 { }

    @Component(automatic = false)
    private interface CircularConstructor3 { }

    // this will be instantiated once
    @Component(automatic = false)
    private static class CircularConstructor1Impl implements CircularConstructor1 {

        @SuppressWarnings("UnusedParameters")
        public CircularConstructor1Impl(final CircularConstructor2 dependency) {
            assert false;
        }
    }

    // this will be instantiated twice because it forces in its constructor the instantiation of its dependency
    @Component(automatic = false)
    private static class CircularConstructor2Impl implements CircularConstructor2 {

        @SuppressWarnings("UnusedParameters")
        public CircularConstructor2Impl(final CircularConstructor3 dependency) {
            assert false;
        }
    }

    // this will be instantiated thrice because both it and CircularConstructor2Impl force in their constructor the instantiation of their dependency
    @Component(automatic = false)
    private static class CircularConstructor3Impl implements CircularConstructor3 {

        @SuppressWarnings("UnusedParameters")
        public CircularConstructor3Impl(final CircularConstructor1 dependency) {
            assert false;
        }
    }

    /*
     * head -> head -> member -> group -> member
     *              -> group -> member -> group
     */

    @Component(automatic = false, api = HeadClass.class)
    @Context1
    private static class HeadClass {

        @SuppressWarnings("UnusedParameters")
        public HeadClass(final HeadInterface next) {
            assert false;
        }
    }

    private interface HeadInterface { }

    @Component(automatic = false)
    private static class MemberDependent implements HeadInterface {

        @SuppressWarnings("UnusedParameters")
        public MemberDependent(final @Optional GroupMember1 next1, final @Optional GroupMember2 next2, final @Optional GroupMember3 next3) {
            assert false;
        }
    }

    @Component(automatic = false)
    private static class GroupDependent implements HeadInterface {

        @SuppressWarnings("UnusedParameters")
        public GroupDependent(final @ComponentGroup RecursiveGroup[] group) {
            assert false;
        }
    }

    @ComponentGroup
    private interface RecursiveGroup { }

    private static abstract class GroupMember implements RecursiveGroup {

        @SuppressWarnings("UnusedParameters")
        public GroupMember(final RecursiveGroup[] group) {
            assert false;
        }
    }


    @Component(automatic = false)
    @Component.Qualifiers({ Context1.class, Context2.class })
    private static class GroupMember1 extends GroupMember {

        @SuppressWarnings("UnusedParameters")
        public GroupMember1(final @Context2 @ComponentGroup RecursiveGroup[] group, final ComponentContext context) {
            super(group);
        }
    }

    @Component(automatic = false)
    @Component.Qualifiers({ Context1.class, Context2.class })
    private static class GroupMember2 extends GroupMember {

        @SuppressWarnings("UnusedParameters")
        public GroupMember2(final @ComponentGroup RecursiveGroup[] group, final ComponentContext context) {
            super(group);
        }
    }

    @Component(automatic = false)
    @Component.Qualifiers(Context1.class)
    private static class GroupMember3 extends GroupMember {

        @SuppressWarnings("UnusedParameters")
        public GroupMember3(final @ComponentGroup RecursiveGroup[] group, final ComponentContext context) {
            super(group);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER })
    @Qualifier(Qualifier.Composition.IMMEDIATE)
    public @interface Context1 { }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER })
    @Qualifier(Qualifier.Composition.IMMEDIATE)
    public @interface Context2 { }

    @Component(automatic = false)
    private static final class A {

        @Inject @Context1 B b;  // B -> C
    }

    @Component(automatic = false)
    @SuppressWarnings("unused")
    private static final class B {

        final Object dependency;

        B() {            // no context
            dependency = null;
        }

        B(C c) {         // Context1
            dependency = c;
        }

        B(A a) {        // Context2
            dependency = a;
        }
    }

    private interface C { }

    @Component(automatic = false)
    private static final class C0 implements C {

        @Inject B b;
    }

    @Component(automatic = false)
    private static final class C1 implements C {

        @Inject @Context1 B b;
    }

    @Component(automatic = false)
    private static final class C2 implements C {

        @Inject @Context2 B b;
    }

    /*
     * For @Context1, declare and use B(A)
     * For @Context2, declare and use B(C)
     * For no context, declare and use B()
     */
    @Component(api = B.class, automatic = false)
    @Component.Qualifiers({ Context1.class, Context2.class })
    private static final class BF implements ComponentFactory {

        @Override
        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws Exception {
            final Context1[] context1 = context.qualifiers(Context1.class);
            final Context2[] context2 = context.qualifiers(Context2.class);

            final Constructor<B> constructor;
            final Dependency<?>[] resolve;

            if (context1 != null) {
                resolve = dependencies.resolve(constructor = B.class.getDeclaredConstructor(C.class));
            } else if (context2 != null) {
                resolve = dependencies.resolve(constructor = B.class.getDeclaredConstructor(A.class));
            } else {
                resolve = dependencies.resolve(constructor = B.class.getDeclaredConstructor());
            }

            return new Instance() {
                @Override
                public void bind(final Registry registry) throws Exception {
                    final Object[] dependencies = new Object[resolve.length];

                    // TODO: simplify this
                    for (int i = 0; i < resolve.length; i++) {
                        dependencies[i] = resolve[i].instance();
                    }

                    registry.bindInstance(constructor.newInstance(dependencies));
                }
            };
        }
    }
}

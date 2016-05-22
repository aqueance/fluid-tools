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

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.Optional;
import org.fluidity.foundation.Lists;

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

    @Test(expectedExceptions = ComponentContainer.CircularReferencesException.class)
    public void circularThreeWayReferences() throws Exception {
        registry.bindComponent(Circular3Dependent1Class.class);
        registry.bindComponent(Circular3Dependent2Class.class);
        registry.bindComponent(Circular3Dependent3Class.class);

        container.getComponent(Circular3Dependent1Class.class);
    }

    @Test
    public void circularTwoWayCalls() throws Exception {
        registry.bindComponent(Circular2Dependent1Impl.class);
        registry.bindComponent(Circular2Dependent2Impl.class);

        ping(Circular2Dependent1.class);

        assert loopback.get();
    }

    @Test
    public void circularThreeWayCalls() throws Exception {
        registry.bindComponent(Circular3Dependent1Impl.class);
        registry.bindComponent(Circular3Dependent2Impl.class);
        registry.bindComponent(Circular3Dependent3Impl.class);

        ping(Circular3Dependent1.class);

        assert loopback.get();
    }

    @Test
    public void circularThreeWayIntermediateInstantiation() throws Exception {
        registry.bindComponent(Circular3IntermediateDependent1Class.class);
        registry.bindComponent(Circular3IntermediateDependent2Class.class);
        registry.bindComponent(Circular3IntermediateDependent3Class.class);

        ping(Circular3IntermediateDependent1Class.class);

        assert loopback.get();
    }

    @Test(expectedExceptions = ComponentContainer.CircularReferencesException.class)
    public void circularInstantiationAndConstructorCalls0() throws Exception {
        circularConstruction(true, true, true);
    }

    @Test
    public void circularInstantiationAndConstructorCalls1() throws Exception {
        circularConstruction(false, true, true);
    }

    @Test
    public void circularInstantiationAndConstructorCalls2() throws Exception {
        circularConstruction(true, false, true);
    }

    @Test
    public void circularInstantiationAndConstructorCalls3() throws Exception {
        circularConstruction(true, true, false);
    }

    @Test
    public void circularInstantiationAndConstructorCalls4() throws Exception {
        circularConstruction(true, false, false);
    }

    @Test
    public void circularInstantiationAndConstructorCalls5() throws Exception {
        circularConstruction(false, true, false);
    }

    @Test
    public void circularInstantiationAndConstructorCalls6() throws Exception {
        circularConstruction(false, false, true);
    }

    @Test
    public void circularInstantiationWithNoConstructorCalls() throws Exception {
        circularConstruction(false, false, false);
    }

    @DataProvider(name = "group-references")
    public Object[][] groupReferences() {
        return new Object[][] {
                new Object[] { HeadInterfaceImpl1.class, false },
                new Object[] { HeadInterfaceImpl2.class, true },
        };
    }

    @Test(dataProvider = "group-references")
    @SuppressWarnings("UnusedParameters")
    public void testCircularGroupReferenceDifferentContexts(final Class<? extends HeadInterface> reference, final boolean indirect) throws Exception {
        registry.bindComponent(HeadClass.class);
        registry.bindComponent(reference);
        registry.bindComponent(GroupMember1.class);

        GroupMember1.call = false;

        final List<Annotation[]> instances = GroupMember1.contexts;
        instances.clear();

        // GroupMember1 does not try to instantiate itself: no method called on proxy
        final HeadClass head = container.getComponent(HeadClass.class);

        // no circular method invocation
        assert !loopback.get();

        // proxy instantiated once
        assert instances.size() == 1 : String.format("%s: %d", reference, instances.size());
        assert instances.get(0).length == 1 : String.format("%s: %s", reference, Arrays.toString(instances.get(0)));

        // method invoked
        head.ping();

        // circular method invocation
        assert loopback.get();

        // proxy instantiated for a different context
        assert instances.size() == 2 : String.format("%s: %d", reference, instances.size());

        assert instances.get(0).length == 1 : String.format("%s: %s", reference, Arrays.toString(instances.get(0)));

        // new instance has a different context
        assert instances.get(1).length == 2 : String.format("%s: %s", reference, Arrays.toString(instances.get(1)));
    }

    @Test(dataProvider = "group-references")
    public void testCircularGroupReferenceSameContext(final Class<? extends HeadInterface> reference, final boolean indirect) throws Exception {
        registry.bindComponent(HeadClass.class);
        registry.bindComponent(reference);
        registry.bindComponent(GroupMember2.class);

        GroupMember2.call = false;

        final List<Annotation[]> instances = GroupMember2.contexts;
        instances.clear();

        // GroupMember2 does not try to instantiate itself: no method called on proxy
        final HeadClass head = container.getComponent(HeadClass.class);

        // no circular method invocation
        assert !loopback.get();

        if (indirect) {

            // proxy instantiated once
            assert instances.size() == 0 : String.format("%s: %d", reference, instances.size());

            // method invoked
            head.ping();

            // circular method invocation
            assert loopback.get();
        }

        // proxy instantiated for the same context
        assert instances.size() == 1 : String.format("%s: %d", reference, instances.size());
        assert instances.get(0).length == 1 : String.format("%s: %s", reference, Arrays.toString(instances.get(0)));
    }

    @Test(dataProvider = "group-references")
    public void testDualCircularGroupReferenceDifferentContexts(final Class<? extends HeadInterface> reference, final boolean indirect) throws Exception {
        registry.bindComponent(HeadClass.class);
        registry.bindComponent(reference);
        registry.bindComponent(GroupMember1.class);
        registry.bindComponent(GroupMember2.class);

        GroupMember1.call = false;
        GroupMember2.call = false;

        final List<Annotation[]> member1 = GroupMember1.contexts;
        final List<Annotation[]> member2 = GroupMember2.contexts;

        member1.clear();
        member2.clear();

        // GroupMembers do not try to instantiate itself: no method called on proxy
        final HeadClass head = container.getComponent(HeadClass.class);

        // no circular method invocation
        assert !loopback.get();

        /*
         * Direct:   (1)head -> (1)interface1 -> (1)member1 -> (1,2)group[] -!> (1,2)member1 -x> (1,2)group[]
         *                                                                  -!> (1,2)member2 -x> (1,2)group[]
         *                                       (1)member2 -> (1)group[] -@> (1)member1
         *                                                                -!> (1)member2
         *
         * Indirect: (1)head -> (1)interface2 -> (1)group[] -> (1)member1 -> (1,2)group[] -!> (1,2)member1 -x> (1,2)group[]
         *                                                  -!> (1)member2 -x> (1)group[]
         */

        // proxy1 instantiated for initial context
        assert member1.size() == 1 : String.format("%s: %d", reference, member1.size());
        assert member1.get(0).length == 1 : String.format("%s: %s", reference, Arrays.toString(member1.get(0)));

        if (indirect) {

            // proxy2 not yet instantiated: creates circular reference to its enclosing group
            assert member2.size() == 0 : String.format("%s: %d", reference, member2.size());
        } else {

            // proxy2 instantiated for initial context
            assert member2.size() == 1 : String.format("%s: %d", reference, member2.size());
            assert member2.get(0).length == 1 : String.format("%s: %s", reference, Arrays.toString(member2.get(0)));
        }

        // method invoked
        head.ping();

        // circular method invocation
        assert loopback.get();

        // proxy1 instantiated for both contexts
        assert member1.size() == 2 : String.format("%s: %d", reference, member1.size());

        assert member1.get(0).length == 1 : String.format("%s: %s", reference, Arrays.toString(member1.get(0)));
        assert member1.get(1).length == 2 : String.format("%s: %s", reference, Arrays.toString(member1.get(1)));

        // proxy2 instantiated for both contexts
        assert member2.size() == 2 : String.format("%s: %d", reference, member2.size());

        if (indirect) {

            // both instances are proxied, instantiation order is unknown:
            if (member2.get(0).length == 1) {
                assert member2.get(1).length == 2 : String.format("%s: %s", reference, Arrays.toString(member2.get(1)));
            } else if (member2.get(1).length == 1) {
                assert member2.get(0).length == 2 : String.format("%s: %s", reference, Arrays.toString(member2.get(0)));
            } else {
                assert false : String.format("%s: %s, %s", reference, Arrays.toString(member2.get(0)), Arrays.toString(member2.get(1)));
            }
        } else {
            assert member2.get(0).length == 1 : String.format("%s: %s", reference, Arrays.toString(member2.get(0)));
            assert member2.get(1).length == 2 : String.format("%s: %s", reference, Arrays.toString(member2.get(1)));
        }
    }

    @Test(dataProvider = "group-references")
    public void testDualCircularGroupReferenceSameContexts(final Class<? extends HeadInterface> reference, final boolean indirect) throws Exception {
        registry.bindComponent(HeadClass.class);
        registry.bindComponent(reference);
        registry.bindComponent(GroupMember1.class);
        registry.bindComponent(GroupMember3.class);

        GroupMember1.call = false;
        GroupMember3.call = false;

        final List<Annotation[]> member1 = GroupMember1.contexts;
        final List<Annotation[]> member3 = GroupMember3.contexts;

        member1.clear();
        member3.clear();

        // GroupMember1 does not try to instantiate itself: no method called on proxy
        final HeadClass head = container.getComponent(HeadClass.class);

        // no circular method invocation
        assert !loopback.get();

        /*
        * Direct:   (1)head -> (1)interface1 -> (1)member1 -> (1,2)group[] -!> (1,2)member1 -x> (1,2)group[]
        *                                                                  -!> (1)member3 -x> (1)group[]
        *                                    -> (1)member3 -x> (1)group[] -@> (1)member1
        *                                                                 -!> (1)member3
        *
        * Indirect: (1)head -> (1)interface2 -> (1)group[] -> (1)member1 -> (1,2)group[] -!> (1,2)member1 -x> (1,2)group[]
        *                                                                                -!> (1)member3 -x> (1)group[]
        *                                                  -!> (1)member3 -x> (1)group[]
        */

        // proxy1 instantiated once
        assert member1.size() == 1 : String.format("%s: %d", reference, member1.size());
        assert member1.get(0).length == 1 : String.format("%s: %s", reference, Arrays.toString(member1.get(0)));

        if (indirect) {

            // proxy3 not yet instantiated
            assert member3.size() == 0 : String.format("%s: %d", reference, member3.size());
        } else {

            // proxy3 instantiated once
            assert member3.size() == 1 : String.format("%s: %d", reference, member3.size());
            assert member3.get(0).length == 1 : String.format("%s: %s", reference, Arrays.toString(member3.get(0)));
        }

        // method invoked
        head.ping();

        // circular method invocation
        assert loopback.get();

        // proxy1 instantiated for two different contexts
        assert member1.size() == 2 : String.format("%s: %d", reference, member1.size());

        assert member1.get(0).length == 1 : String.format("%s: %s", reference, Arrays.toString(member1.get(0)));
        assert member1.get(1).length == 2 : String.format("%s: %s", reference, Arrays.toString(member1.get(1)));

        // proxy3 not instantiated again
        assert member3.size() == 1 : String.format("%s: %d", reference, member3.size());
        assert member3.get(0).length == 1 : String.format("%s: %s", reference, Arrays.toString(member3.get(0)));
    }

    @Test(dataProvider = "group-references", expectedExceptions = ComponentContainer.CircularReferencesException.class)
    public void testCircularGroupInstantiation(final Class<? extends HeadInterface> reference, final boolean ping) throws Exception {
        registry.bindComponent(HeadClass.class);
        registry.bindComponent(reference);
        registry.bindComponent(GroupMember1.class);

        GroupMember1.call = true;
        GroupMember1.contexts.clear();

        // GroupMember1 tries to instantiate itself: circular reference
        final HeadClass head = container.getComponent(HeadClass.class);

        if (ping) {
            head.ping();
        }
    }

    @Test
    public void testContextUnawareDuplicate() throws Exception {
        registry.bindComponent(AImpl.class);
        registry.bindComponent(BImpl.class);
        registry.bindComponent(CImpl.class);
        registry.bindComponent(DImpl.class);

        AImpl.count = BImpl.count = CImpl.count = DImpl.count = 0;

        /*
         * A -> B -> C(1) -> [1]B -!> C
         *        -> D            -> [1]D
         */

        final AImpl a = container.getComponent(AImpl.class);

        a.ping();       // make sure all proxies are de-referenced

        assert AImpl.count == 1 : AImpl.count;  // single instance of A
        assert BImpl.count == 2 : BImpl.count;  // two instances of B; one for the empty context and one for the context defined at C
        assert CImpl.count == 1 : CImpl.count;  // single instance of C
        assert DImpl.count == 2 : DImpl.count;  // two instances of D; one for the empty context and one for the context defined at C

        assert a.b.self() != a.b.c().b().self();
        assert a.b.c().self() == a.b.c().b().c().self();
        assert a.b.d().self() != a.b.c().b().d().self();
    }

    private void circularConstruction(final boolean call1, final boolean call2, final boolean call3) {
        CircularConstructor1Impl.call = call1;
        CircularConstructor2Impl.call = call2;
        CircularConstructor3Impl.call = call3;

        registry.bindComponent(CircularConstructor1Impl.class);
        registry.bindComponent(CircularConstructor2Impl.class);
        registry.bindComponent(CircularConstructor3Impl.class);

        ping(CircularConstructor1.class);
        ping(CircularConstructor2.class);
        ping(CircularConstructor3.class);
    }

    private void ping(final Class<? extends Pingable> componentClass) {
        final Pingable component = container.getComponent(componentClass);
        assert component != null : "Circular resolution failed";
        component.ping();
    }

    public interface Pingable {

        void ping();
    }

    private static class PingableImpl implements Pingable {

        private final AtomicBoolean visited = new AtomicBoolean();
        private final Pingable[] delegates;

        protected PingableImpl(final Pingable... delegates) {
            assert delegates != null;
            this.delegates = delegates;
        }

        public final void ping() {
            if (visited.compareAndSet(false, true)) {
                for (final Pingable delegate : delegates) {
                    if (delegate != null) {
                        delegate.ping();
                    }
                }
            } else {
                loopback.set(true);
            }
        }
    }

    private interface Circular2Dependent1 extends Pingable { }

    private interface Circular2Dependent2 extends Pingable { }

    @Component(automatic = false)
    private static class Circular2Dependent1Impl extends PingableImpl implements Circular2Dependent1 {

        public Circular2Dependent1Impl(final Circular2Dependent2 dependency) {
            super(dependency);
        }
    }

    @Component(automatic = false)
    private static class Circular2Dependent2Impl extends PingableImpl implements Circular2Dependent2 {

        public Circular2Dependent2Impl(final Circular2Dependent1 dependency) {
            super(dependency);
        }
    }

    private interface Circular3Dependent1 extends Pingable { }

    private interface Circular3Dependent2 extends Pingable { }

    private interface Circular3Dependent3 extends Pingable { }

    @Component(automatic = false)
    private static class Circular3Dependent1Impl extends PingableImpl implements Circular3Dependent1 {

        public Circular3Dependent1Impl(final Circular3Dependent2 dependency) {
            super(dependency);
        }
    }

    @Component(automatic = false)
    private static class Circular3Dependent2Impl extends PingableImpl implements Circular3Dependent2 {

        public Circular3Dependent2Impl(final Circular3Dependent3 dependency) {
            super(dependency);
        }
    }

    @Component(automatic = false)
    private static class Circular3Dependent3Impl extends PingableImpl implements Circular3Dependent3 {

        public Circular3Dependent3Impl(final Circular3Dependent1 dependency) {
            super(dependency);
        }
    }

    @Component(api = Circular2Dependent1Class.class, automatic = false)
    private static class Circular2Dependent1Class extends PingableImpl implements Pingable {

        public Circular2Dependent1Class(final Circular2Dependent2Class dependency) {
            super(dependency);
        }
    }

    @Component(api = Circular2Dependent2Class.class, automatic = false)
    private static class Circular2Dependent2Class extends PingableImpl implements Pingable {

        public Circular2Dependent2Class(final Circular2Dependent1Class dependency) {
            super(dependency);
        }
    }

    @Component(api = Circular3Dependent1Class.class, automatic = false)
    private static class Circular3Dependent1Class extends PingableImpl implements Pingable {

        public Circular3Dependent1Class(final Circular3Dependent2Class dependency) {
            super(dependency);
        }
    }

    @Component(api = Circular3Dependent2Class.class, automatic = false)
    private static class Circular3Dependent2Class extends PingableImpl implements Pingable {

        public Circular3Dependent2Class(final Circular3Dependent3Class dependency) {
            super(dependency);
        }
    }

    @Component(api = Circular3Dependent3Class.class, automatic = false)
    private static class Circular3Dependent3Class extends PingableImpl implements Pingable {

        public Circular3Dependent3Class(final Circular3Dependent1Class dependency) {
            super(dependency);
        }
    }

    @Component(api = Circular3IntermediateDependent1Class.class, automatic = false)
    private static class Circular3IntermediateDependent1Class extends PingableImpl implements Pingable {

        public Circular3IntermediateDependent1Class(final Circular3IntermediateDependent2Class dependency) {
            super(dependency);
        }
    }

    @Component(api = Circular3IntermediateDependent2Class.class, automatic = false)
    private static class Circular3IntermediateDependent2Class extends PingableImpl implements Pingable {

        public Circular3IntermediateDependent2Class(final Circular3IntermediateDependent3 dependency) {
            super(dependency);
        }
    }

    @Component(automatic = false)
    private interface Circular3IntermediateDependent3 extends Pingable { }

    @Component(automatic = false)
    private static class Circular3IntermediateDependent3Class extends PingableImpl implements Circular3IntermediateDependent3 {

        public Circular3IntermediateDependent3Class(final Circular3IntermediateDependent1Class dependency) {
            super(dependency);
        }
    }

    @Component(automatic = false)
    private interface CircularConstructor1 extends Pingable { }

    @Component(automatic = false)
    private interface CircularConstructor2 extends Pingable { }

    @Component(automatic = false)
    private interface CircularConstructor3 extends Pingable { }

    // this will be instantiated once
    @Component(automatic = false)
    private static class CircularConstructor1Impl implements CircularConstructor1 {

        public static boolean call = false;

        public CircularConstructor1Impl(final CircularConstructor2 dependency) {
            if (call) dependency.ping();
        }

        public void ping() {
            // empty
        }
    }

    // this will be instantiated twice because it forces in its constructor the instantiation of its dependency
    @Component(automatic = false)
    private static class CircularConstructor2Impl implements CircularConstructor2 {

        public static boolean call = false;

        public CircularConstructor2Impl(final CircularConstructor3 dependency) {
            if (call) dependency.ping();
        }

        public void ping() {
            // empty
        }
    }

    // this will be instantiated thrice because both it and CircularConstructor2Impl force in their constructor the instantiation of their dependency
    @Component(automatic = false)
    private static class CircularConstructor3Impl implements CircularConstructor3 {

        public static boolean call = false;

        public CircularConstructor3Impl(final CircularConstructor1 dependency) {
            if (call) dependency.ping();
        }

        public void ping() {
            // empty
        }
    }

    /*
     * head -> head -> member -> group -> member
     *              -> group -> member -> group
     */

    @Component(automatic = false, api = HeadClass.class)
    @Context1
    private static class HeadClass extends PingableImpl {

        public HeadClass(final HeadInterface next) {
            super(next);
        }
    }

    private interface HeadInterface extends Pingable { }

    @Component(automatic = false)
    private static class HeadInterfaceImpl1 extends PingableImpl implements HeadInterface {

        public HeadInterfaceImpl1(final @Optional GroupMember1 next1, final @Optional GroupMember2 next2, final @Optional GroupMember3 next3) {
            super(next1, next2, next3);
        }
    }

    @Component(automatic = false)
    private static class HeadInterfaceImpl2 extends PingableImpl implements HeadInterface {

        public HeadInterfaceImpl2(final @ComponentGroup RecursiveGroup[] group) {
            super(group);
        }
    }

    @ComponentGroup
    private interface RecursiveGroup extends Pingable { }

    private static abstract class GroupMember extends PingableImpl implements RecursiveGroup {

        public GroupMember(final RecursiveGroup[] group, final List<Annotation[]> contexts, final boolean call, final ComponentContext context) {
            super(group);

            final List<Annotation> qualifiers = new ArrayList<Annotation>();

            for (final Class<? extends Annotation> type : context.types()) {
                qualifiers.add(context.qualifier(type, null));
            }

            contexts.add(Lists.asArray(Annotation.class, qualifiers));

            if (call) ping();
        }
    }


    @Component(automatic = false)
    @Component.Qualifiers({ Context1.class, Context2.class })
    private static class GroupMember1 extends GroupMember {

        public static boolean call;
        public static final List<Annotation[]> contexts  = new ArrayList<Annotation[]>();

        public GroupMember1(final @Context2 @ComponentGroup RecursiveGroup[] group, final ComponentContext context) {
            super(group, contexts, call, context);
        }
    }

    @Component(automatic = false)
    @Component.Qualifiers({ Context1.class, Context2.class })
    private static class GroupMember2 extends GroupMember {

        public static boolean call;
        public static final List<Annotation[]> contexts  = new ArrayList<Annotation[]>();

        public GroupMember2(final @ComponentGroup RecursiveGroup[] group, final ComponentContext context) {
            super(group, contexts, call, context);
        }
    }

    @Component(automatic = false)
    @Component.Qualifiers(Context1.class)
    private static class GroupMember3 extends GroupMember {

        public static boolean call;
        public static final List<Annotation[]> contexts  = new ArrayList<Annotation[]>();

        public GroupMember3(final @ComponentGroup RecursiveGroup[] group, final ComponentContext context) {
            super(group, contexts, call, context);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Context1 { }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface Context2 { }

    public interface Self {

        Self self();
    }

    @Component(automatic = false, api = AImpl.class)
    private static class AImpl extends PingableImpl implements Self {

        public static int count;

        public final B b;

        public AImpl(final B b) {
            super(b);
            this.b = b;
            ++count;
        }

        public Self self() {
            return this;
        }
    }

    public interface B extends Pingable, Self {

        C c();

        D d();
    }

    public interface C extends Pingable, Self {

        B b();
    }

    public interface D extends Pingable, Self { }

    @Component(automatic = false)
    private static class BImpl extends PingableImpl implements B {

        public static int count;

        private final C c;
        private final D d;

        public BImpl(final C c, final D d) {
            super(c, d);
            this.c = c;
            this.d = d;

            ++count;
        }

        public Self self() {
            return this;
        }

        public C c() {
            return c;
        }

        public D d() {
            return d;
        }
    }

    @Component(automatic = false)
    private static class CImpl extends PingableImpl implements C {

        public static int count;

        public final B b;

        public CImpl(@Context2 final B b) {
            super(b);
            this.b = b;
            ++count;
        }

        public Self self() {
            return this;
        }

        public B b() {
            return b;
        }
    }

    @Component(automatic = false)
    @Component.Qualifiers(Context2.class)
    private static class DImpl implements D {

        public static int count;

        public DImpl() {
            ++count;
        }

        public void ping() {
            // empty
        }

        public Self self() {
            return this;
        }
    }
}

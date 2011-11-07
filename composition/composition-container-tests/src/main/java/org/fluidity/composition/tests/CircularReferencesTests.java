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

package org.fluidity.composition.tests;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
public final class CircularReferencesTests extends AbstractContainerTests {

    public CircularReferencesTests(final ArtifactFactory factory) {
        super(factory);
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

    @Test(expectedExceptions = ComponentContainer.CircularInvocationException.class)
    public void circularTwoWayCalls() throws Exception {
        registry.bindComponent(Circular2Dependent1Impl.class);
        registry.bindComponent(Circular2Dependent2Impl.class);

        ping(Circular2Dependent1.class);
        ping(Circular2Dependent2.class);
    }

    @Test(expectedExceptions = ComponentContainer.CircularInvocationException.class)
    public void circularThreeWayCalls() throws Exception {
        registry.bindComponent(Circular3Dependent1Impl.class);
        registry.bindComponent(Circular3Dependent2Impl.class);
        registry.bindComponent(Circular3Dependent3Impl.class);

        ping(Circular3Dependent1.class);
        ping(Circular3Dependent2.class);
        ping(Circular3Dependent2.class);
    }

    @Test(expectedExceptions = ComponentContainer.CircularInvocationException.class)
    public void circularThreeWayIntermediateInstantiation() throws Exception {
        registry.bindComponent(Circular3IntermediateDependent1Class.class);
        registry.bindComponent(Circular3IntermediateDependent2Class.class);
        registry.bindComponent(Circular3IntermediateDependent3Class.class);

        ping(Circular3IntermediateDependent1Class.class);
        ping(Circular3IntermediateDependent2Class.class);
        ping(Circular3IntermediateDependent3.class);
    }

    @Test(expectedExceptions = ComponentContainer.CircularReferencesException.class)
    public void circularReferences() throws Exception {
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

        private final Pingable delegate;

        protected PingableImpl(final Pingable delegate) {
            assert delegate != null;
            this.delegate = delegate;
        }

        public final void ping() {
            delegate.ping();
        }
    }

    private interface Circular2Dependent1 extends Pingable {

    }

    private interface Circular2Dependent2 extends Pingable {

    }

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

    private interface Circular3Dependent1 extends Pingable {

    }

    private interface Circular3Dependent2 extends Pingable {

    }

    private interface Circular3Dependent3 extends Pingable {

    }

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
    private interface Circular3IntermediateDependent3 extends Pingable {

    }

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
}

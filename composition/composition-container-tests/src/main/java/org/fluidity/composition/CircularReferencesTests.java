/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.fluidity.composition;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public final class CircularReferencesTests extends AbstractContainerTests {

    public CircularReferencesTests(final ContainerFactory factory) {
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

    @Test
    public void circularTwoWayInstantiation() throws Exception {
        registry.bindComponent(Circular2Dependent1Impl.class);
        registry.bindComponent(Circular2Dependent2Impl.class);

        ping(Circular2Dependent1.class);
        ping(Circular2Dependent2.class);
    }

    @Test
    public void circularThreeWayInstantiation() throws Exception {
        registry.bindComponent(Circular3Dependent1Impl.class);
        registry.bindComponent(Circular3Dependent2Impl.class);
        registry.bindComponent(Circular3Dependent3Impl.class);

        ping(Circular3Dependent1.class);
        ping(Circular3Dependent2.class);
        ping(Circular3Dependent2.class);
    }

    @Test
    public void circularThreeWayIntermediateInstantiation() throws Exception {
        registry.bindComponent(Circular3IntermediateDependent1Class.class);
        registry.bindComponent(Circular3IntermediateDependent2Class.class);
        registry.bindComponent(Circular3IntermediateDependent3Class.class);

        ping(Circular3IntermediateDependent1Class.class);
        ping(Circular3IntermediateDependent2Class.class);
        ping(Circular3IntermediateDependent3.class);
    }

    @Test(expectedExceptions = ComponentContainer.CircularReferencesException.class)
    public void circularConstructorCalls() throws Exception {
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

    private void circularConstruction(final boolean call1, final boolean call2, final boolean call3) {
        CircularNasty1Impl.call = call1;
        CircularNasty2Impl.call = call2;
        CircularNasty3Impl.call = call3;

        registry.bindComponent(CircularNasty1Impl.class);
        registry.bindComponent(CircularNasty2Impl.class);
        registry.bindComponent(CircularNasty3Impl.class);

        ping(CircularNasty1.class);
        ping(CircularNasty2.class);
        ping(CircularNasty3.class);
    }

    private void ping(final Class<? extends Pingable> componentClass) {
        final Pingable component = container.getComponent(componentClass);
        assert component != null : "Circular resolution failed";
        component.ping();
    }

    private static interface Pingable {

        void ping();
    }

    private static class PingableImpl implements Pingable {

        private boolean visited;

        private final Pingable delegate;

        protected PingableImpl(final Pingable delegate) {
            assert delegate != null;
            this.delegate = delegate;
        }

        public final void ping() {
            if (!visited) {
                visited = true;
                try {
                    delegate.ping();
                } finally {
                    visited = false;
                }
            }
        }
    }

    private static interface Circular2Dependent1 extends Pingable {

    }

    private static interface Circular2Dependent2 extends Pingable {

    }

    private static class Circular2Dependent1Impl extends PingableImpl implements Circular2Dependent1 {

        public Circular2Dependent1Impl(final Circular2Dependent2 dependency) {
            super(dependency);
        }
    }

    private static class Circular2Dependent2Impl extends PingableImpl implements Circular2Dependent2 {

        public Circular2Dependent2Impl(final Circular2Dependent1 dependency) {
            super(dependency);
        }
    }

    private static interface Circular3Dependent1 extends Pingable {

    }

    private static interface Circular3Dependent2 extends Pingable {

    }

    private static interface Circular3Dependent3 extends Pingable {

    }

    private static class Circular3Dependent1Impl extends PingableImpl implements Circular3Dependent1 {

        public Circular3Dependent1Impl(final Circular3Dependent2 dependency) {
            super(dependency);
        }
    }

    private static class Circular3Dependent2Impl extends PingableImpl implements Circular3Dependent2 {

        public Circular3Dependent2Impl(final Circular3Dependent3 dependency) {
            super(dependency);
        }
    }

    private static class Circular3Dependent3Impl extends PingableImpl implements Circular3Dependent3 {

        public Circular3Dependent3Impl(final Circular3Dependent1 dependency) {
            super(dependency);
        }
    }

    @Component(api = Circular2Dependent1Class.class)
    private static class Circular2Dependent1Class extends PingableImpl implements Pingable {

        public Circular2Dependent1Class(final Circular2Dependent2Class dependency) {
            super(dependency);
        }
    }

    @Component(api = Circular2Dependent2Class.class)
    private static class Circular2Dependent2Class extends PingableImpl implements Pingable {

        public Circular2Dependent2Class(final Circular2Dependent1Class dependency) {
            super(dependency);
        }
    }

    @Component(api = Circular3Dependent1Class.class)
    private static class Circular3Dependent1Class extends PingableImpl implements Pingable {

        public Circular3Dependent1Class(final Circular3Dependent2Class dependency) {
            super(dependency);
        }
    }

    @Component(api = Circular3Dependent2Class.class)
    private static class Circular3Dependent2Class extends PingableImpl implements Pingable {

        public Circular3Dependent2Class(final Circular3Dependent3Class dependency) {
            super(dependency);
        }
    }

    @Component(api = Circular3Dependent3Class.class)
    private static class Circular3Dependent3Class extends PingableImpl implements Pingable {

        public Circular3Dependent3Class(final Circular3Dependent1Class dependency) {
            super(dependency);
        }
    }

    @Component(api = Circular3IntermediateDependent1Class.class)
    private static class Circular3IntermediateDependent1Class extends PingableImpl implements Pingable {

        public Circular3IntermediateDependent1Class(final Circular3IntermediateDependent2Class dependency) {
            super(dependency);
        }
    }

    @Component(api = Circular3IntermediateDependent2Class.class)
    private static class Circular3IntermediateDependent2Class extends PingableImpl implements Pingable {

        public Circular3IntermediateDependent2Class(final Circular3IntermediateDependent3 dependency) {
            super(dependency);
        }
    }

    private static interface Circular3IntermediateDependent3 extends Pingable {

    }

    private static class Circular3IntermediateDependent3Class extends PingableImpl implements Circular3IntermediateDependent3 {

        public Circular3IntermediateDependent3Class(final Circular3IntermediateDependent1Class dependency) {
            super(dependency);
        }
    }

    private static interface CircularNasty1 extends Pingable {}

    private static interface CircularNasty2 extends Pingable {}

    private static interface CircularNasty3 extends Pingable {}

    // this will be instantiated once
    @Component
    private static class CircularNasty1Impl implements CircularNasty1 {
        public static boolean call = false;

        public CircularNasty1Impl(final CircularNasty2 dependency) {
            if (call) dependency.ping();
        }

        public void ping() {
            // empty
        }
    }

    // this will be instantiated twice because it forces in its constructor the instantiation of its dependency
    @Component
    private static class CircularNasty2Impl implements CircularNasty2 {
        public static boolean call = false;

        public CircularNasty2Impl(final CircularNasty3 dependency) {
            if (call) dependency.ping();
        }

        public void ping() {
            // empty
        }
    }

    // this will be instantiated thrice because both it and CircularNasty2Impl force in their constructor the instantiation of their dependency
    @Component
    private static class CircularNasty3Impl implements CircularNasty3 {
        public static boolean call = false;

        public CircularNasty3Impl(final CircularNasty1 dependency) {
            if (call) dependency.ping();
        }

        public void ping() {
            // empty
        }
    }
}

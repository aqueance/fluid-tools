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
@SuppressWarnings("unchecked")
public class ConstructorDiscoveryTests extends AbstractContainerTests {

    public ConstructorDiscoveryTests(final ContainerFactory factory) {
        super(factory);
    }

    @Test
    public void testSingleConstructor() throws Exception {
        registry.bindComponent(SingleConstructor.class);
        assert container.getComponent(SingleConstructor.class) != null : "Single constructor not accepted";
    }

    @Test
    public void testTwoConstructors0() throws Exception {
        registry.bindInstance("string", String.class);
        registry.bindComponent(TwoConstructors0.class);
        assert container.getComponent(TwoConstructors0.class) != null : "Non-default constructor not accepted";
    }

    @Test
    public void testSingleComponentConstructor() throws Exception {
        registry.bindInstance("string", String.class);
        registry.bindComponent(SingleComponentConstructor.class);
        assert container.getComponent(SingleComponentConstructor.class) != null : "Single annotated constructor not accepted";
    }

    @Test
    public void testHiddenComponentConstructor() throws Exception {
        registry.bindInstance(1, Integer.TYPE);
        registry.bindComponent(HiddenComponentConstructor.class);
        assert container.getComponent(HiddenComponentConstructor.class) != null : "Private annotated constructor not accepted";
    }

    @Test(expectedExceptions = ComponentContainer.ResolutionException.class, expectedExceptionsMessageRegExp = ".*constructor.*")
    public void testTwoConstructor1() throws Exception {
        registry.bindComponent(TwoConstructors1.class);
        assert container.getComponent(TwoConstructors1.class) == null : "Ambiguous constructor accepted";
    }

    @Test(expectedExceptions = ComponentContainer.ResolutionException.class, expectedExceptionsMessageRegExp = ".*constructor.*")
    public void testThreeConstructors() throws Exception {
        registry.bindComponent(ThreeConstructors.class);
        assert container.getComponent(ThreeConstructors.class) == null : "Ambiguous constructor accepted";
    }

    @Test(expectedExceptions = ComponentContainer.ResolutionException.class, expectedExceptionsMessageRegExp = ".*constructor.*")
    public void testMultipleComponentConstructors() throws Exception {
        registry.bindComponent(MultipleComponentConstructors.class);
        assert container.getComponent(MultipleComponentConstructors.class) == null : "Ambiguous constructor accepted";
    }

    @Test(expectedExceptions = ComponentContainer.ResolutionException.class, expectedExceptionsMessageRegExp = ".*constructor.*")
    public void testMixedComponentConstructors() throws Exception {
        registry.bindInstance(1, Integer.TYPE);
        registry.bindComponent(MixedComponentConstructors.class);
        assert container.getComponent(MixedComponentConstructors.class) == null : "Ambiguous constructor accepted";
    }

    private static class SingleConstructor {

    }

    @SuppressWarnings("UnusedDeclaration")
    private static class TwoConstructors0 {

        private TwoConstructors0() {
        }

        private TwoConstructors0(final String ignore) {
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class TwoConstructors1 {

        private TwoConstructors1(final String ignore) {
        }

        private TwoConstructors1(final int ignore) {
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class ThreeConstructors {

        private ThreeConstructors() {
        }

        private ThreeConstructors(final String ignore) {
        }

        private ThreeConstructors(final int ignore) {
        }
    }
    
    @SuppressWarnings("UnusedDeclaration")
    private static class SingleComponentConstructor {

        private SingleComponentConstructor() {
        }

        private SingleComponentConstructor(final int ignore) {
        }

        @Component
        private SingleComponentConstructor(final String ignore) {
        }
    }
    
    @SuppressWarnings("UnusedDeclaration")
    private static class MultipleComponentConstructors {

        private MultipleComponentConstructors() {
        }

        @Component
        private MultipleComponentConstructors(final int ignore) {
        }

        @Component
        private MultipleComponentConstructors(final String ignore) {
        }
    }

    private static class HiddenComponentConstructor {

        private HiddenComponentConstructor() {
        }

        public HiddenComponentConstructor(final String ignore) {
        }

        @Component
        private HiddenComponentConstructor(final int ignore) {
        }
    }

    private static class MixedComponentConstructors {

        private MixedComponentConstructors() {
        }

        @Component
        public MixedComponentConstructors(final int ignore) {
        }

        @Component
        private MixedComponentConstructors(final String ignore) {
        }
    }
}

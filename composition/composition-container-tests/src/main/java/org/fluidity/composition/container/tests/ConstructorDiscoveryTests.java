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

package org.fluidity.composition.container.tests;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.Inject;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
public class ConstructorDiscoveryTests extends AbstractContainerTests {

    ConstructorDiscoveryTests(final ArtifactFactory factory) {
        super(factory);
    }

    @Test
    public void testSingleConstructor() throws Exception {
        registry.bindComponent(SingleConstructor.class);
        assert container.getComponent(SingleConstructor.class) != null : "Single constructor not accepted";
    }

    @Test
    public void testTwoConstructors0() throws Exception {
        registry.bindInstance("string");
        registry.bindComponent(TwoConstructors0.class);
        assert container.getComponent(TwoConstructors0.class) != null : "Non-default constructor not accepted";
    }

    @Test
    public void testSingleComponentConstructor() throws Exception {
        registry.bindInstance("string");
        registry.bindComponent(SingleComponentConstructor.class);
        assert container.getComponent(SingleComponentConstructor.class) != null : "Single annotated constructor not accepted";
    }

    @Test
    public void testHiddenComponentConstructor() throws Exception {
        registry.bindInstance(1);
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
        registry.bindInstance(1);
        registry.bindComponent(MixedComponentConstructors.class);
        assert container.getComponent(MixedComponentConstructors.class) == null : "Ambiguous constructor accepted";
    }

    private static class SingleConstructor { }

    @SuppressWarnings("UnusedDeclaration")
    private static class TwoConstructors0 {

        TwoConstructors0() { }

        TwoConstructors0(final String ignore) { }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class TwoConstructors1 {

        TwoConstructors1(final String ignore) { }

        TwoConstructors1(final int ignore) { }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class ThreeConstructors {

        ThreeConstructors() { }

        ThreeConstructors(final String ignore) { }

        ThreeConstructors(final int ignore) { }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class SingleComponentConstructor {

        SingleComponentConstructor() { }

        SingleComponentConstructor(final int ignore) { }

        @Inject
        SingleComponentConstructor(final String ignore) { }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class MultipleComponentConstructors {

        MultipleComponentConstructors() { }

        @Inject
        MultipleComponentConstructors(final int ignore) { }

        @Inject
        MultipleComponentConstructors(final String ignore) { }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class HiddenComponentConstructor {

        HiddenComponentConstructor() { }

        public HiddenComponentConstructor(final String ignore) { }

        @Inject
        HiddenComponentConstructor(final int ignore) { }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class MixedComponentConstructors {

        MixedComponentConstructors() { }

        @Inject
        public MixedComponentConstructors(final int ignore) { }

        @Inject
        MixedComponentConstructors(final String ignore) { }
    }
}

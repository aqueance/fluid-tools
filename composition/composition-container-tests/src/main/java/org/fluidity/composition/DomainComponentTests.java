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

package org.fluidity.composition;

import java.util.Arrays;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
public final class DomainComponentTests extends AbstractContainerTests {

    public DomainComponentTests(final ContainerFactory factory) {
        super(factory);
    }

    @Test
    public void testComponentResolution() throws Exception {
        registry.bindComponent(Component.class);

        final OpenComponentContainer domain = container.makeDomainContainer();
        domain.getRegistry().bindComponent(Dependency.class);

        try {
            container.getComponent(Component.class);
            assert false : "Should have thrown exception";
        } catch (final ComponentContainer.InstantiationException e) {
            // expected
        }

        assert domain.getComponent(Component.class) != null;
    }

    @Test
    public void testDomainIsolation() throws Exception {
        registry.bindComponent(Component.class);

        final OpenComponentContainer domain1 = container.makeDomainContainer();
        domain1.getRegistry().bindComponent(Dependency.class);

        final OpenComponentContainer domain2 = container.makeDomainContainer();
        domain2.getRegistry().bindComponent(Dependency.class);

        try {
            container.getComponent(Component.class);
            assert false : "Should have thrown exception";
        } catch (final ComponentContainer.InstantiationException e) {
            // expected
        }

        final Component instance1 = domain1.getComponent(Component.class);
        final Component instance2 = domain2.getComponent(Component.class);

        assert instance1 != null;
        assert instance2 != null;
        assert instance1 != instance2;

        assert instance1 == domain1.getComponent(Component.class);
        assert instance2 == domain2.getComponent(Component.class);
    }

    @Test
    public void testDependencyChain() throws Exception {
        registry.bindComponent(Root.class);
        registry.bindComponent(Component.class);

        final OpenComponentContainer domain = container.makeDomainContainer();
        domain.getRegistry().bindComponent(Head.class);
        domain.getRegistry().bindComponent(Dependency.class);

        assert domain.getComponent(Root.class) != null;
    }

    @Test(expectedExceptions = ComponentContainer.ResolutionException.class, expectedExceptionsMessageRegExp = ".*Dependency.*")
    public void testMissingDependency() throws Exception {
        registry.bindComponent(Component.class);

        final OpenComponentContainer domain = container.makeDomainContainer();

        try {
            domain.getComponent(Component.class);
        } catch (final ComponentContainer.InstantiationException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test
    public void testComponentGroups() throws Exception {
        registry.bindComponent(GroupMember1.class);

        final OpenComponentContainer domain1 = container.makeDomainContainer();
        domain1.getRegistry().bindComponent(Dependency.class);
        domain1.getRegistry().bindComponent(GroupMember2.class);

        final OpenComponentContainer domain2 = container.makeDomainContainer();
        domain2.getRegistry().bindComponent(Dependency.class);
        domain2.getRegistry().bindComponent(GroupMember3.class);

        try {
            container.getComponentGroup(GroupApi.class);
            assert false : "Should have thrown exception";
        } catch (final ComponentContainer.InstantiationException e) {
            // expected
        }

        final GroupApi[] members1 = domain1.getComponentGroup(GroupApi.class);
        final GroupApi[] members2 = domain2.getComponentGroup(GroupApi.class);

        assert members1 != null;
        assert members2 != null;
        assert !Arrays.equals(members1, members2);

        assert Arrays.equals(members1, domain1.getComponentGroup(GroupApi.class));
        assert Arrays.equals(members2, domain2.getComponentGroup(GroupApi.class));
    }

    private static class Root {

        private Root(final Component dependency) {
            assert dependency != null;
        }
    }

    private static class Head {

        private Head(final Component dependency) {
            assert dependency != null;
        }
    }

    private static class Component {

        private Component(final Dependency dependency) {
            assert dependency != null;
        }
    }

    private static class Dependency { }

    @ComponentGroup
    public static interface GroupApi {}

    public static class GroupMember1 implements GroupApi {

        public GroupMember1(final Dependency dependency) {
            assert dependency != null;
        }
    }

    public static class GroupMember2 implements GroupApi {

        public GroupMember2(final Dependency dependency) {
            assert dependency != null;
        }
    }

    public static class GroupMember3 implements GroupApi {

        public GroupMember3(final Dependency dependency) {
            assert dependency != null;
        }
    }
}

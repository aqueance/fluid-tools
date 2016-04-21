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

import java.util.Arrays;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.OpenContainer;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
public final class DomainComponentTests extends AbstractContainerTests {

    DomainComponentTests(final ArtifactFactory factory) {
        super(factory);
    }

    @Test
    public void testComponentResolution() throws Exception {
        registry.bindComponent(Component.class);

        final OpenContainer domain = container.makeDomainContainer(registry -> registry.bindComponent(Dependency.class));

        try {
            container.getComponent(Component.class);
            assert false : "Should have thrown exception";
        } catch (final ComponentContainer.InjectionException e) {
            // expected
        }

        assert domain.getComponent(Component.class) != null;
    }

    @Test
    public void testDomainIsolation() throws Exception {
        registry.bindComponent(Component.class);

        final OpenContainer domain1 = container.makeDomainContainer(registry -> registry.bindComponent(Dependency.class));

        final OpenContainer domain2 = container.makeDomainContainer(registry -> registry.bindComponent(Dependency.class));

        try {
            container.getComponent(Component.class);
            assert false : "Should have thrown exception";
        } catch (final ComponentContainer.InjectionException e) {
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
        registry.bindComponent(Dependency.class);

        final OpenContainer domain = container.makeDomainContainer(registry -> registry.bindComponent(Dependency.class));

        final Root component = domain.getComponent(Root.class);
        assert component != null;

        final Root root = container.getComponent(Root.class);
        assert component != root;
        assert root.dependency != component.dependency;
        assert root.dependency.dependency != component.dependency.dependency;

        assert root.dependency.dependency == container.getComponent(Dependency.class);
        assert component.dependency.dependency == domain.getComponent(Dependency.class);
    }

    @Test
    public void testDependencyCrossChain() throws Exception {
        registry.bindComponent(Root.class);
        registry.bindComponent(Component.class);
        registry.bindComponent(Dependency.class);

        final OpenContainer domain = container.makeDomainContainer(registry -> registry.bindComponent(Component.class));

        final Root component = domain.getComponent(Root.class);
        assert component != null;

        final Root root = container.getComponent(Root.class);
        assert component != root;
        assert root.dependency != component.dependency;
        assert root.dependency.dependency != component.dependency.dependency;

        assert root.dependency == container.getComponent(Component.class);
        assert component.dependency == domain.getComponent(Component.class);
    }

    @Test
    public void testDependencyPriority() throws Exception {
        registry.bindComponent(Root.class);
        registry.bindComponent(Component.class);
        registry.bindComponent(Dependency.class);

        final Component fallback = container.getComponent(Component.class);     // acquire this and its dependency before the domain container can say a word

        final OpenContainer domain = container.makeDomainContainer(registry -> registry.bindComponent(Component.class));

        final Root component = domain.getComponent(Root.class);
        assert component != null;

        final Root root = container.getComponent(Root.class);
        assert component != root;
        assert root.dependency != component.dependency;
        assert root.dependency.dependency != component.dependency.dependency;

        assert root.dependency == fallback;
        assert component.dependency == domain.getComponent(Component.class);
    }

    @Test(expectedExceptions = ComponentContainer.ResolutionException.class, expectedExceptionsMessageRegExp = ".*Dependency.*")
    public void testMissingDependency() throws Exception {
        registry.bindComponent(Component.class);

        final OpenContainer domain = container.makeDomainContainer();

        try {
            domain.getComponent(Component.class);
        } catch (final ComponentContainer.InstantiationException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test
    public void testComponentGroups() throws Exception {
        registry.bindComponent(GroupMember1.class);

        final OpenContainer domain1 = container.makeDomainContainer(registry -> {
            registry.bindComponent(Dependency.class);
            registry.bindComponent(GroupMember2.class);
        });

        final OpenContainer domain2 = container.makeDomainContainer(registry -> {
            registry.bindComponent(Dependency.class);
            registry.bindComponent(GroupMember3.class);
        });

        try {
            container.getComponentGroup(GroupApi.class);
            assert false : "Should have thrown exception";
        } catch (final ComponentContainer.InjectionException e) {
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

        public final Component dependency;

        @SuppressWarnings("UnusedDeclaration")
        private Root(final Component dependency) {
            this.dependency = dependency;
            assert dependency != null;
        }
    }

    private static class Component {

        public final Dependency dependency;

        @SuppressWarnings("UnusedDeclaration")
        private Component(final Dependency dependency) {
            this.dependency = dependency;
            assert dependency != null;
        }
    }

    private static class Dependency { }

    @ComponentGroup
    interface GroupApi { }

    private static class GroupMember1 implements GroupApi {

        public GroupMember1(final Dependency dependency) {
            assert dependency != null;
        }
    }

    private static class GroupMember2 implements GroupApi {

        public GroupMember2(final Dependency dependency) {
            assert dependency != null;
        }
    }

    private static class GroupMember3 implements GroupApi {

        public GroupMember3(final Dependency dependency) {
            assert dependency != null;
        }
    }
}

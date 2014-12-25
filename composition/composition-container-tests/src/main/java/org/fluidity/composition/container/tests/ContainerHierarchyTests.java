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

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.OpenContainer;
import org.fluidity.composition.ServiceProvider;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
public final class ContainerHierarchyTests extends AbstractContainerTests {

    public ContainerHierarchyTests(final ArtifactFactory factory) {
        super(factory);
    }

    @Test
    public void linkingComponentDependencyResolvesInParent() throws Exception {
        registry.bindComponent(DependentValue.class);
        registry.isolateComponent(Value.class);

        verifyComponent(container);
    }

    @Test
    public void linkingComponentDependencyResolvesInChild() throws Exception {
        registry.isolateComponent(Value.class).bindComponent(DependentValue.class);

        verifyComponent(container);
    }

    @Test
    public void dependencyFromChildResolvesInParent() throws Exception {
        registry.bindComponent(DependentValue.class);

        final OpenContainer child = container.makeChildContainer(new ComponentContainer.Bindings() {
            public void bindComponents(final ComponentContainer.Registry registry) {
                registry.bindComponent(Value.class);
            }
        });

        verifyComponent(child);
    }

    @Test
    public void linkingComponentDependencyResolvesToOtherLinkingComponentAtSameLevel() throws Exception {
        registry.isolateComponent(DependentValue.class);
        registry.isolateComponent(Value.class);

        verifyComponent(container);
    }

    @Test
    public void linkingComponentDependencyResolvesToOtherLinkingComponentAtHigherLevel() throws Exception {
        registry.isolateComponent(DependentValue.class);

        final OpenContainer child = container.makeChildContainer(new ComponentContainer.Bindings() {
            public void bindComponents(final ComponentContainer.Registry registry) {
                registry.isolateComponent(Value.class);
            }
        });

        verifyComponent(child);
    }

    @Test
    public void childDefaultsToLocalBindingsIndependentOfParentBindings() throws Exception {
        registry.bindComponent(OtherDependentValue.class);
        registry.bindComponent(OtherValue.class);

        final OpenContainer child = container.makeChildContainer(new ComponentContainer.Bindings() {
            public void bindComponents(final ComponentContainer.Registry registry) {
                registry.bindComponent(DependentValue.class);
                registry.bindComponent(Value.class);
            }
        });

        verifyComponent(child);

        assert child.getComponent(Key.class) instanceof Value;
        assert child.getComponent(DependentKey.class) instanceof DependentValue;

        assert container.getComponent(Key.class) instanceof OtherValue;
        assert container.getComponent(DependentKey.class) instanceof OtherDependentValue;
    }

    @Test
    public void childContainerContainsItself() throws Exception {
        final OpenContainer childContainer = container.makeChildContainer(new ComponentContainer.Bindings() {
            public void bindComponents(final ComponentContainer.Registry registry) {
                registry.bindComponent(ContainerDependent.class);
            }
        });

        final ContainerDependent component = childContainer.getComponent(ContainerDependent.class);
        assert component != null;

        final ComponentContainer container = component.container();

        // verify that we have the same container in our hands
        final KeyCheck check = new KeyCheck();

        assert container.initialize(check).key == null;
        registry.bindComponent(Value.class);
        registry.bindComponent(DependentValue.class);
        assert container.initialize(check).key != null;
    }

    @Test
    public void testPrivateContainers() throws Exception {
        registry.bindComponent(PublicComponent.class);

        final RootComponent root1 = new RootComponent();
        final RootComponent root2 = new RootComponent();

        final OpenContainer domain1 = container.makePrivateContainer(RootComponent.Private.class, new ComponentContainer.Bindings() {
            public void bindComponents(final ComponentContainer.Registry registry) {
                registry.bindInstance(root1);
            }
        });

        final OpenContainer domain2 = container.makePrivateContainer(RootComponent.Private.class, new ComponentContainer.Bindings() {
            public void bindComponents(final ComponentContainer.Registry registry) {
                registry.bindInstance(root2);
            }
        });

        final PrivateComponent private1 = domain1.getComponent(PrivateComponent.class);
        final PrivateComponent private2 = domain2.getComponent(PrivateComponent.class);

        assert private1 != null;
        assert private2 != null;
        assert private1 != private2;

        assert private1.root == root1;
        assert private2.root == root2;

        assert private1.dependency == private2.dependency;
    }

    /**
     * This is intentionally private - makes sure the container is able to instantiate non-public classes
     */
    @Component(automatic = false)
    private static class OtherValue implements Key {

        public OtherValue(final DependentKey dependent) {
            assert dependent != null;
        }

        public String key() {
            return null;
        }
    }

    /**
     * This is intentionally private - makes sure the container is able to instantiate non-public classes
     */
    @Component(automatic = false)
    private static class OtherDependentValue extends DependentValue {

        public OtherDependentValue() {
            // empty
        }
    }

    @Component(automatic = false)
    private static class RootComponent {

        @ServiceProvider(type = "private")
        private static interface Private {}
    }

    @Component(api = PrivateComponent.class, automatic = false)
    private static class PrivateComponent implements RootComponent.Private {

        public final RootComponent root;
        public final PublicComponent dependency;

        @SuppressWarnings("UnusedParameters")
        public PrivateComponent(final RootComponent root, final PublicComponent dependency) {
            this.root = root;
            this.dependency = dependency;
        }
    }

    @Component(automatic = false)
    private static class PublicComponent { }
}

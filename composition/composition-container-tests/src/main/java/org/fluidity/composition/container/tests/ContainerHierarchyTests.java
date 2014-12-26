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
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.OpenContainer;
import org.fluidity.composition.spi.ComponentFactory;

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
    public void testManualPrivateContainers() throws Exception {
        registry.bindComponent(PublicComponent.class);

        final RootComponent root1 = new RootComponent();
        final RootComponent root2 = new RootComponent();

        final OpenContainer domain1 = container.makePrivateContainer(RootComponent.class, new ComponentContainer.Bindings() {
            public void bindComponents(final ComponentContainer.Registry registry) {
                registry.bindInstance(root1);
            }
        });

        final OpenContainer domain2 = container.makePrivateContainer(RootComponent.class, new ComponentContainer.Bindings() {
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

    @Test
    public void testAutomaticPrivateContainers() throws Exception {
        registry.bindComponent(PublicComponent.class);
        registry.bindComponent(RootInstanceFactory.class);

        final RootInstance root1 = container.getComponent(RootInstance.class);
        final RootInstance root2 = container.getComponent(RootInstance.class);

        assert root1 != null;
        assert root2 != null;
        assert root1 != root2;

        assert root1.dependency != root2.dependency;
        assert root1.dependency.dependency == root2.dependency.dependency;
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
    private static class RootComponent { }

    @Component(root = RootComponent.class)
    private static class PrivateComponent {

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

    @Component(api = RootInstance.class, stateful = true, automatic = false)
    private static class RootInstanceFactory implements ComponentFactory {

        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws Exception {
            dependencies.discover(RootInstance.class);

            return new Instance() {
                public void bind(final Registry registry) throws Exception {
                    registry.bindComponent(RootInstance.class);
                }
            };
        }
    }

    @Component(automatic = false)
    private static class RootInstance {

        public final PrivateInstance dependency;

        @SuppressWarnings("UnusedDeclaration")
        private RootInstance(final PrivateInstance dependency) {
            this.dependency = dependency;
        }
    }

    @Component(root = RootInstance.class)
    private static class PrivateInstance {

        public final PublicComponent dependency;

        @SuppressWarnings("UnusedParameters")
        public PrivateInstance(final PublicComponent dependency) {
            this.dependency = dependency;
        }
    }
}

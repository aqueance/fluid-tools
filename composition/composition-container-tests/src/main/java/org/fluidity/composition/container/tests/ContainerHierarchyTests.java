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

    ContainerHierarchyTests(final ArtifactFactory factory) {
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

        final OpenContainer child = container.makeChildContainer(registry -> registry.bindComponent(Value.class));

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

        final OpenContainer child = container.makeChildContainer(registry -> registry.isolateComponent(Value.class));

        verifyComponent(child);
    }

    @Test
    public void childDefaultsToLocalBindingsIndependentOfParentBindings() throws Exception {
        registry.bindComponent(OtherDependentValue.class);
        registry.bindComponent(OtherValue.class);

        final OpenContainer child = container.makeChildContainer(registry -> {
            registry.bindComponent(DependentValue.class);
            registry.bindComponent(Value.class);
        });

        verifyComponent(child);

        assert child.getComponent(Key.class) instanceof Value;
        assert child.getComponent(DependentKey.class) instanceof DependentValue;

        assert container.getComponent(Key.class) instanceof OtherValue;
        assert container.getComponent(DependentKey.class) instanceof OtherDependentValue;
    }

    @Test
    public void childContainerContainsItself() throws Exception {
        final OpenContainer childContainer = container.makeChildContainer(registry -> registry.bindComponent(ContainerDependent.class));

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
    public void testExplicitScopedComponents() throws Exception {
        registry.bindComponent(PublicComponent.class);

        final RootComponent root1 = container.instantiate(RootComponent.class);
        final RootComponent root2 = container.instantiate(RootComponent.class);

        assert root1 != null;
        assert root2 != null;
        assert root1 != root2;

        assert root1.dependency != root2.dependency;
        assert root1.dependency.dependency == root2.dependency.dependency;
    }

    @Test
    public void testImplicitScopedComponents() throws Exception {
        registry.bindComponent(PublicComponent.class);
        registry.bindComponent(RootComponentFactory.class);

        final RootComponent root1 = container.getComponent(RootComponent.class);
        final RootComponent root2 = container.getComponent(RootComponent.class);

        assert root1 != null;
        assert root2 != null;
        assert root1 != root2;

        assert root1.dependency != root2.dependency;
        assert root1.dependency.dependency == root2.dependency.dependency;
    }

    @Test
    public void testNestedScopedComponents() throws Exception {
        registry.bindComponent(MyRootComponent.class);

        assert container.getComponent(MyRootComponent.class) != null;
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
    private static class PublicComponent { }

    @Component(api = RootComponent.class, stateful = true, automatic = false)
    private static class RootComponentFactory implements ComponentFactory {

        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws Exception {
            dependencies.discover(RootComponent.class);

            return registry -> registry.bindComponent(RootComponent.class);
        }
    }

    @Component(scope = RootComponent.class)
    private static class RootComponent {

        public final PrivateComponent dependency;

        @SuppressWarnings("UnusedDeclaration")
        private RootComponent(final PrivateComponent dependency) {
            this.dependency = dependency;
        }
    }

    @Component(scope = RootComponent.class)
    private static class PrivateComponent {

        public final PublicComponent dependency;

        @SuppressWarnings("UnusedParameters")
        public PrivateComponent(final PublicComponent dependency) {
            this.dependency = dependency;
        }
    }

    private interface MyRootComponent { }

    private interface MyScopedRootComponent { }

    private interface MyNestedScopedComponent { }

    @Component(scope = MyRootComponent.class)
    private static final class MyRootComponentImpl implements MyRootComponent {

        @SuppressWarnings("UnusedParameters")
        MyRootComponentImpl(final MyScopedRootComponent dependency) { }
    }

    @Component(api = MyScopedRootComponent.class, scope = MyRootComponent.class)
    private static final class MyScopedRootComponentFactory implements ComponentFactory {

        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws Exception {
            dependencies.discover(MyScopedRootComponentImpl.class);

            return registry -> registry.bindComponent(MyScopedRootComponentImpl.class);
        }
    }

    @Component(scope = MyScopedRootComponent.class)
    private static final class MyScopedRootComponentImpl implements MyScopedRootComponent {

        @SuppressWarnings("UnusedParameters")
        MyScopedRootComponentImpl(final MyNestedScopedComponent dependency) { }
    }

    @Component(scope = MyScopedRootComponent.class)
    private static final class MyNestedScopedComponentImpl implements MyNestedScopedComponent {}
}

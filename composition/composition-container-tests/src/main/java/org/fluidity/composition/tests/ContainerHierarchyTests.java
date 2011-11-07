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
import org.fluidity.composition.OpenComponentContainer;

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
        registry.makeChildContainer(Value.class);

        verifyComponent(container);
    }

    @Test
    public void linkingComponentDependencyResolvesInChild() throws Exception {
        final OpenComponentContainer child = registry.makeChildContainer(Value.class);
        child.getRegistry().bindComponent(DependentValue.class);

        verifyComponent(container);
    }

    @Test
    public void dependencyFromChildResolvesInParent() throws Exception {
        registry.bindComponent(DependentValue.class);

        final OpenComponentContainer child = container.makeChildContainer();
        child.getRegistry().bindComponent(Value.class);

        verifyComponent(child);
    }

    @Test
    public void linkingComponentDependencyResolvesOnOtherLinkingComponentAtSameLevel() throws Exception {
        registry.makeChildContainer(DependentValue.class);
        registry.makeChildContainer(Value.class);

        verifyComponent(container);
    }

    @Test
    public void linkingComponentDependencyResolvesOnOtherLinkingComponentAtHigherLevel() throws Exception {
        registry.makeChildContainer(DependentValue.class);
        final OpenComponentContainer child = registry.makeChildContainer().getRegistry().makeChildContainer(Value.class);

        verifyComponent(child);
    }

    @Test
    public void childDefaultsToLocalBindingsIndependentOfParentBindings() throws Exception {
        registry.bindComponent(OtherDependentValue.class);
        registry.bindComponent(OtherValue.class);

        final OpenComponentContainer child = container.makeChildContainer();
        final OpenComponentContainer.Registry childRegistry = child.getRegistry();
        childRegistry.bindComponent(DependentValue.class);
        childRegistry.bindComponent(Value.class);

        verifyComponent(child);

        assert child.getComponent(Key.class) instanceof Value;
        assert child.getComponent(DependentKey.class) instanceof DependentValue;

        assert container.getComponent(Key.class) instanceof OtherValue;
        assert container.getComponent(DependentKey.class) instanceof OtherDependentValue;
    }

    @Test
    public void childContainerContainsItself() throws Exception {
        final OpenComponentContainer childContainer = registry.makeChildContainer();
        final OpenComponentContainer.Registry childRegistry = childContainer.getRegistry();

        childRegistry.bindComponent(ContainerDependent.class);

        final ContainerDependent component = childContainer.getComponent(ContainerDependent.class);
        assert component != null;

        final ComponentContainer container = component.container();

        // verify that we have the same container in our hands
        assert container.getComponent(Key.class) == null;
        registry.bindComponent(Value.class);
        registry.bindComponent(DependentValue.class);
        assert container.getComponent(Key.class) != null;
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
}

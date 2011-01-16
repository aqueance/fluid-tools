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
public final class ContainerHierarchyTests extends AbstractContainerTests {

    public ContainerHierarchyTests(final ContainerFactory factory) {
        super(factory);
    }

    @Test
    public void linkingComponentDependencyResolvesInParent() throws Exception {
        registry.bindComponent(DependentKey.class, DependentValue.class);
        registry.makeNestedContainer(Key.class, Value.class);

        verifyComponent(Value.instanceCount, 1, container);
    }

    @Test
    public void linkingComponentDependencyResolvesInChild() throws Exception {
        final OpenComponentContainer nested = registry.makeNestedContainer(Key.class, Value.class);
        nested.getRegistry().bindComponent(DependentKey.class, DependentValue.class);

        verifyComponent(Value.instanceCount, 1, container);
    }

    @Test
    public void dependencyFromChildResolvesInParent() throws Exception {
        registry.bindComponent(DependentKey.class, DependentValue.class);

        final OpenComponentContainer nested = container.makeNestedContainer();
        nested.getRegistry().bindComponent(Key.class, Value.class);

        verifyComponent(Value.instanceCount, 1, nested);
    }

    @Test
    public void linkingComponentDependencyResolvesOnOther0LinkingComponentAtSameLevel() throws Exception {
        registry.makeNestedContainer(DependentKey.class, DependentValue.class);
        registry.makeNestedContainer(Key.class, Value.class);

        verifyComponent(Value.instanceCount, 1, container);
    }

    @Test
    public void linkingComponentDependencyResolvesOnOther0LinkingComponentAtHigherLevel() throws Exception {
        registry.makeNestedContainer(DependentKey.class, DependentValue.class);
        final OpenComponentContainer nested = registry.makeNestedContainer().getRegistry().makeNestedContainer(Key.class, Value.class);

        verifyComponent(Value.instanceCount, 1, nested);
    }

    @Test
    public void childDefaultsToLocalBindingsIndependentOfParentBindings() throws Exception {
        registry.bindComponent(DependentKey.class, OtherDependentValue.class);
        registry.bindComponent(Key.class, OtherValue.class);

        final OpenComponentContainer nested = container.makeNestedContainer();
        final ComponentContainer.Registry nestedRegistry = nested.getRegistry();
        nestedRegistry.bindComponent(DependentKey.class, DependentValue.class);
        nestedRegistry.bindComponent(Key.class, Value.class);

        final int originalCount = Value.instanceCount;

        assert nested.getComponent(Key.class) instanceof Value;
        assert nested.getComponent(DependentKey.class) instanceof DependentValue;

        assert container.getComponent(Key.class) instanceof OtherValue;
        assert container.getComponent(DependentKey.class) instanceof OtherDependentValue;

        verifyComponent(originalCount, 1, nested);
    }

    @Test
    public void nestedContainerContainsItself() throws Exception {
        final OpenComponentContainer nestedContainer = registry.makeNestedContainer();
        final ComponentContainer.Registry nestedRegistry = nestedContainer.getRegistry();

        nestedRegistry.bindDefault(ContainerDependent.class);

        final ContainerDependent component = nestedContainer.getComponent(ContainerDependent.class);
        assert component != null;

        final ComponentContainer container = component.container();

        // verify that we have the same container in our hands
        assert container.getComponent(Key.class) == null;
        registry.bindComponent(Key.class, Value.class);
        registry.bindComponent(DependentKey.class, DependentValue.class);
        assert container.getComponent(Key.class) != null;
    }

    /**
     * This is intentionally private - makes sure the container is able to instantiate non-public classes
     */
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
    private static class OtherDependentValue extends DependentValue {

        public OtherDependentValue() {
            // empty
        }
    }
}

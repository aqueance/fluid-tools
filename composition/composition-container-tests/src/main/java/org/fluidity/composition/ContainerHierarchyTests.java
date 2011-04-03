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
public final class ContainerHierarchyTests extends AbstractContainerTests {

    public ContainerHierarchyTests(final ContainerFactory factory) {
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
        final ComponentContainer.Registry childRegistry = child.getRegistry();
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
        final ComponentContainer.Registry childRegistry = childContainer.getRegistry();

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

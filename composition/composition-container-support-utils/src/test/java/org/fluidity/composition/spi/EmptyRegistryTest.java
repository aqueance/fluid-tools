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

package org.fluidity.composition.spi;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.Components;
import org.fluidity.composition.OpenComponentContainer;
import org.fluidity.tests.MockGroupAbstractTest;

import org.easymock.EasyMock;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class EmptyRegistryTest extends MockGroupAbstractTest {

    private final ComponentRegistry mock = addControl(ComponentRegistry.class);
    private final OpenComponentContainer container = addControl(OpenComponentContainer.class);
    private final ComponentContainer.Registry registry = new EmptyRegistry(mock);

    @Test
    public void childContainer() throws Exception {
        EasyMock.expect(mock.makeChildContainer()).andReturn(container);

        replay();
        final OpenComponentContainer container = registry.makeChildContainer();
        verify();

        assert container == this.container;
    }

    @Test
    public void linkingContainer() throws Exception {
        EasyMock.expect(mock.makeChildContainer(Components.inspect(MarkedGroupComponent.class))).andReturn(container);

        replay();
        final OpenComponentContainer container = registry.makeChildContainer(MarkedGroupComponent.class);
        verify();

        assert container == this.container;
    }

    @Test
    public void subclassWithComponentAnnotation() throws Exception {
        mock.bindComponent(Components.inspect(UnmarkedComponent.class));

        replay();
        registry.bindComponent(UnmarkedComponent.class);
        verify();

        final Object component = new UnmarkedComponent();

        mock.bindInstance(component, Components.inspect(UnmarkedComponent.class));

        replay();
        registry.bindInstance(component);
        verify();
    }

    private static interface Interface1 {

    }

    private static interface Interface2 {

    }

    private static interface Interface3 {

    }

    @ComponentGroup
    private static interface GroupInterface1 {

    }

    @ComponentGroup
    private static interface GroupInterface2 {

    }

    private static interface GroupInterface3 extends Interface2 {

    }

    private static interface InheritingInterface1 extends GroupInterface1 {

    }

    private static interface InheritingInterface2 extends GroupInterface2 {

    }

    @Component(api = Interface1.class)
    private static class InvalidComponent {

    }

    private static class UnmarkedComponent implements Interface1, Interface2 {

    }

    @Component
    private static class ComponentSubclass extends UnmarkedComponent implements Interface3 {

    }

    @Component(api = { Interface1.class, Interface2.class })
    private static class ComponentImplementation extends UnmarkedComponent implements Interface3 {

    }

    @Component(api = { Interface1.class, Interface2.class })
    private static class UnmarkedGroupComponent implements Interface1, Interface2, InheritingInterface1, InheritingInterface2, GroupInterface3 {

    }

    @Component(api = { Interface1.class, Interface2.class })
    @ComponentGroup(api = GroupInterface3.class)
    private static class MarkedGroupComponent implements Interface1, Interface2, InheritingInterface1, InheritingInterface2, GroupInterface3 {

    }

    @Component(api = Interface1.class)
    private static class MarkedFactory implements ComponentFactory {

        public void newComponent(final OpenComponentContainer container, final ComponentContext context) {
            throw new UnsupportedOperationException();
        }
    }
}

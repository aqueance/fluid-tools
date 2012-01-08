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
@SuppressWarnings("unchecked")
public class EmptyRegistryTest extends MockGroupAbstractTest {

    private final ComponentRegistry mock = mock(ComponentRegistry.class);
    private final OpenComponentContainer container = mock(OpenComponentContainer.class);
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

    private interface Interface1 {

    }

    private interface Interface2 {

    }

    private interface Interface3 {

    }

    @ComponentGroup
    private interface GroupInterface1 {

    }

    @ComponentGroup
    private interface GroupInterface2 {

    }

    private interface GroupInterface3 extends Interface2 {

    }

    private interface InheritingInterface1 extends GroupInterface1 {

    }

    private interface InheritingInterface2 extends GroupInterface2 {

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
    private static class MarkedFactory implements CustomComponentFactory {

        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws ComponentContainer.ResolutionException {
            throw new UnsupportedOperationException();
        }
    }
}

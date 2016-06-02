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

package org.fluidity.composition.container.spi;

import java.util.Collection;
import java.util.Collections;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.Components;
import org.fluidity.composition.MutableContainer;
import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.testing.Simulator;

import org.easymock.EasyMock;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings({ "unchecked", "UnusedDeclaration" })
public class EmptyRegistryTest extends Simulator {

    private final MockObjects dependencies = dependencies();

    private final MutableContainer container = dependencies.normal(MutableContainer.class);
    private final ComponentRegistry delegate = dependencies.normal(ComponentRegistry.class)
            ;
    private final ComponentContainer.Registry registry = new EmptyRegistry(delegate);

    @Test
    public void linkingContainer() throws Exception {
        EasyMock.expect(delegate.makeChildContainer(Components.inspect(MarkedGroupComponent.class))).andReturn(container);
        EasyMock.expect(container.getRegistry()).andReturn(registry);

        final ComponentContainer.Registry registry = verify(() -> EmptyRegistryTest.this.registry.isolateComponent(MarkedGroupComponent.class));

        assert registry == this.registry;
    }

    @Test
    public void subclassWithComponentAnnotation() throws Exception {
        test(() -> {
            delegate.bindComponent(Components.inspect(UnmarkedComponent.class));

            verify(() -> registry.bindComponent(UnmarkedComponent.class));
        });

        test(() -> {
            final Object component = new UnmarkedComponent();

            delegate.bindInstance(component, Components.inspect(UnmarkedComponent.class));

            verify(() -> registry.bindInstance(component));
        });
    }

    private interface Interface1 { }

    private interface Interface2 { }

    private interface Interface3 { }

    @ComponentGroup
    private interface GroupInterface1 { }
    @ComponentGroup
    private interface GroupInterface2 { }

    private interface GroupInterface3 extends Interface2 { }

    private interface InheritingInterface1 extends GroupInterface1 { }

    private interface InheritingInterface2 extends GroupInterface2 { }

    @Component(api = Interface1.class)
    private static class InvalidComponent { }

    private static class UnmarkedComponent implements Interface1, Interface2 { }

    @Component
    private static class ComponentSubclass extends UnmarkedComponent implements Interface3 { }

    @Component(api = { Interface1.class, Interface2.class })
    private static class ComponentImplementation extends UnmarkedComponent implements Interface3 { }

    @Component(api = { Interface1.class, Interface2.class })
    private static class UnmarkedGroupComponent implements Interface1, Interface2, InheritingInterface1, InheritingInterface2, GroupInterface3 { }

    @Component(api = { Interface1.class, Interface2.class })
    @ComponentGroup(api = GroupInterface3.class)
    private static class MarkedGroupComponent implements Interface1, Interface2, InheritingInterface1, InheritingInterface2, GroupInterface3 { }

    @Component(api = Interface1.class)
    private static class MarkedFactory implements ComponentFactory {

        public Instance resolve(final ComponentContext context, final Container dependencies) throws Exception {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void bindsComponentGroup() throws Exception {
        final Class<?>[] elements = { GroupComponent1.class, GroupComponent2.class, GroupComponent3.class };

        final Collection<Class<?>> groups = Collections.singletonList(GroupComponent.class);
        for (final Class type : elements) {
            delegate.bindComponent(new Components.Interfaces(type, new Components.Specification[] {
                    new Components.Specification(type, groups)
            }));
        }

        verify(() -> registry.bindComponentGroup(GroupComponent.class, (Class<GroupComponent>[]) elements));
    }

    interface GroupComponent { }

    private static class GroupComponent1 implements GroupComponent { }

    private static class GroupComponent2 implements GroupComponent { }

    private static class GroupComponent3 implements GroupComponent { }
}

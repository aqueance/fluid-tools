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

import java.lang.reflect.Type;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.composition.spi.CustomComponentFactory;
import org.fluidity.foundation.Generics;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
public final class CustomFactoryTests extends AbstractContainerTests {

    @SuppressWarnings("unchecked")
    private final CustomComponentFactory factory = mock(CustomComponentFactory.class);
    private final ComponentFactory.Instance instance = mock(ComponentFactory.Instance.class);

    public CustomFactoryTests(final ArtifactFactory factory) {
        super(factory);
    }

    @BeforeMethod
    public void setMockFactory() {
        DependentFactory.delegate = this.factory;
        GroupMember1Factory.delegate = this.factory;
        GroupMember2Factory.delegate = this.factory;
    }

    @Test
    public void invokesStandaloneFactoryClassOnce() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(DependentFactory.class);
        registry.bindComponent(FactoryDependency.class);

        final Check check = new Check();

        registry.bindInstance(check);

        EasyMock.expect(factory.resolve(EasyMock.<ComponentContext>notNull(), EasyMock.<ComponentFactory.Resolver>notNull()))
                .andAnswer(new FactoryInvocation(Check.class, check, instance))
                .anyTimes();
        instance.bind(EasyMock.<ComponentFactory.Registry>notNull());
        EasyMock.expectLastCall().anyTimes();

        replay();
        verifyComponent(container);
        verify();
    }

    @Test
    public void invokesStandaloneFactoryClassOnceInLinkedContainer() throws Exception {
        registry.bindComponent(Value.class);
        final ComponentContainer.Registry child = registry.isolateComponent(DependentFactory.class);

        final Check check = new Check();

        child.bindComponent(FactoryDependency.class);
        child.bindInstance(check);

        EasyMock.expect(factory.resolve(EasyMock.<ComponentContext>notNull(), EasyMock.<ComponentFactory.Resolver>notNull()))
                .andAnswer(new FactoryInvocation(Check.class, check, instance))
                .anyTimes();
        instance.bind(EasyMock.<ComponentFactory.Registry>notNull());
        EasyMock.expectLastCall().anyTimes();

        replay();
        verifyComponent(container);
        verify();
    }

    @Test
    public void circularFactoryInvocation() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(DependentFactory.class);
        registry.bindComponent(FactoryDependency.class);

        final Check check = new Check();

        registry.bindInstance(check);

        EasyMock.expect(factory.resolve(EasyMock.<ComponentContext>notNull(), EasyMock.<ComponentFactory.Resolver>notNull())).andReturn(instance).anyTimes();
        instance.bind(EasyMock.<ComponentFactory.Registry>notNull());
        EasyMock.expectLastCall().anyTimes();

        replay();
        verifyComponent(container);
        verify();
    }

    @Test
    public void groupMemberFactoryOrder1() throws Exception {
        registry.bindComponent(GroupMember2.class);
        registry.bindComponent(GroupMember1Factory.class);
        groupMemberChecks(1);
    }

    @Test
    public void groupMemberFactoryOrder2() throws Exception {
        registry.bindComponent(GroupMember1Factory.class);
        registry.bindComponent(GroupMember2.class);
        groupMemberChecks(1);
    }

    private void groupMemberChecks(final int factories) {
        EasyMock.expect(factory.resolve(EasyMock.<ComponentContext>notNull(), EasyMock.<ComponentFactory.Resolver>notNull())).andReturn(instance).anyTimes();
        instance.bind(EasyMock.<ComponentFactory.Registry>notNull());
        EasyMock.expectLastCall().times(factories);

        replay();
        final GroupApi[] group = container.getComponentGroup(GroupApi.class);
        verify();

        assert group != null : GroupApi.class;
        assert group.length == 2 : group.length;
        assert group[0] != group[1];
    }

    @Test(expectedExceptions = ComponentContainer.ResolutionException.class, expectedExceptionsMessageRegExp = ".*[Dd]ynamic.*Factory.*")
    public void testRestrictedContainer1() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(DependentValue.class);
        registry.bindComponent(DynamicFactory1.class);

        replay();
        container.getComponent(DynamicComponent1.class);
        verify();

        assert false : "Dynamic dependency should have been prevented";
    }

    @Test
    public void testRestrictedContainer2() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(DependentValue.class);
        registry.bindComponent(DynamicFactory2.class);

        replay();
        final DynamicComponent2 component = container.getComponent(DynamicComponent2.class);
        verify();

        assert component.key() != null;
    }

    @Test
    public void testTypeReference() throws Exception {
        registry.bindComponent(CustomDependent.class);
        registry.bindComponent(CustomDependencyFactory.class);

        replay();
        assert container.getComponent(CustomDependent.class) != null;
        verify();
    }

    @Component(api = DependentKey.class, automatic = false)
    private static class DependentFactory implements CustomComponentFactory {

        public static CustomComponentFactory delegate;

        public DependentFactory(final FactoryDependency dependency) {
            assert dependency != null;
        }

        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws ComponentContainer.ResolutionException {
            assert delegate != null;
            final Instance instance = delegate.resolve(context, dependencies);

            return new Instance() {
                public void bind(final Registry registry) throws ComponentContainer.BindingException {
                    registry.bindComponent(DependentValue.class);

                    assert instance != null;
                    instance.bind(registry);
                }
            };
        }
    }

    @ComponentGroup
    private interface GroupApi { }

    private interface GroupMember2Api extends GroupApi { }

    @Component(automatic = false)
    private static class GroupMember1 implements GroupApi { }

    @Component(automatic = false)
    private static class GroupMember2 implements GroupMember2Api { }

    @Component(api = GroupMember1.class, automatic = false)
    private static class GroupMember1Factory implements CustomComponentFactory {

        public static CustomComponentFactory delegate;

        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws ComponentContainer.ResolutionException {
            assert delegate != null;
            final Instance instance = delegate.resolve(context, dependencies);

            return new Instance() {
                public void bind(final Registry registry) throws ComponentContainer.BindingException {
                    registry.bindComponent(GroupMember1.class);

                    assert instance != null;
                    instance.bind(registry);
                }
            };
        }
    }

    @Component(api = GroupMember2Api.class, automatic = false)
    private static class GroupMember2Factory implements CustomComponentFactory {

        public static CustomComponentFactory delegate;

        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws ComponentContainer.ResolutionException {
            assert delegate != null;
            final Instance instance = delegate.resolve(context, dependencies);

            return new Instance() {
                public void bind(final Registry registry) throws ComponentContainer.BindingException {
                    registry.bindComponent(GroupMember2.class);

                    assert instance != null;
                    instance.bind(registry);
                }
            };
        }
    }

    private static class FactoryInvocation implements IAnswer<ComponentFactory.Instance> {

        private final Class<?> checkKey;
        private final Object checkValue;
        private final ComponentFactory.Instance instance;

        public FactoryInvocation(final Class<?> checkKey, final Object checkValue, final ComponentFactory.Instance instance) {
            this.checkKey = checkKey;
            this.checkValue = checkValue;
            this.instance = instance;
        }

        public ComponentFactory.Instance answer() throws Throwable {
            final ComponentFactory.Resolver resolver = (ComponentFactory.Resolver) EasyMock.getCurrentArguments()[1];
            assert resolver != null : "Received no resolver";

            final ComponentFactory.Dependency<?> dependency = resolver.resolve(checkKey);
            assert dependency != null && dependency.instance() == checkValue : "Container does not check up";

            return instance;
        }
    }

    @Component(automatic = false)
    private static class DynamicComponent1 {

        private DynamicComponent1(final ComponentContainer container) {
            assert container != null : ComponentContainer.class;
            container.getComponent(Key.class);
        }
    }

    @Component(api = DynamicComponent1.class)
    private static class DynamicFactory1 implements CustomComponentFactory {

        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws ComponentContainer.ResolutionException {
            final Dependency<?>[] args = dependencies.discover(DynamicComponent1.class);

            return new Instance() {
                public void bind(final Registry registry) throws ComponentContainer.BindingException {
                    registry.bindInstance(new DynamicComponent1((ComponentContainer) args[0].instance()));
                }
            };
        }
    }

    @Component(automatic = false)
    private static class DynamicComponent2 {

        private final ComponentContainer container;

        private DynamicComponent2(final ComponentContainer container) {
            assert container != null : ComponentContainer.class;
            this.container = container;
        }

        public Key key() {
            return container.getComponent(Key.class);
        }
    }

    @Component(api = DynamicComponent2.class)
    private static class DynamicFactory2 implements CustomComponentFactory {

        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws ComponentContainer.ResolutionException {
            final Dependency<?>[] args = dependencies.discover(DynamicComponent1.class);

            return new Instance() {
                public void bind(final Registry registry) throws ComponentContainer.BindingException {
                    registry.bindInstance(new DynamicComponent2((ComponentContainer) args[0].instance()));
                }
            };
        }
    }

    private interface CustomParameter { }

    private interface CustomParameter1 extends CustomParameter { }

    private interface CustomParameter2 extends CustomParameter { }

    @SuppressWarnings("UnusedDeclaration")
    private static class CustomDependency<T> { }

    @Component(api = CustomDependency.class)
    @Component.Context(Component.Reference.class)
    private static class CustomDependencyFactory implements CustomComponentFactory {

        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws ComponentContainer.ResolutionException {
            assert context.defines(Component.Reference.class);
            assert context.annotations(Component.Reference.class) != null;

            final Component.Reference annotation = context.annotation(Component.Reference.class, getClass());
            final Type reference = annotation.type();
            final Class<?> parameter = annotation.parameter(0);

            assert parameter != null : reference;
            assert Generics.rawType(reference) == CustomDependency.class : Generics.rawType(reference);
            assert CustomParameter.class.isAssignableFrom(parameter) : Generics.typeParameter(reference, 0);

            return new Instance() {
                public void bind(final Registry registry) throws ComponentContainer.BindingException {
                    registry.bindInstance(new CustomDependency(), CustomDependency.class);
                }
            };
        }
    }

    private static class CustomDependent {

        public CustomDependent(final ComponentContext context,
                               final CustomDependency<CustomParameter1> dependency1,
                               final CustomDependency<CustomParameter2> dependency2,
                               final CustomDependency<CustomParameter1> dependency3) {
            assert !context.defines(Component.Reference.class) : context;

            assert dependency1 != null;
            assert dependency2 != null;
            assert dependency3 != null;
            assert (CustomDependency) dependency1 != dependency2 : "Type parameter did not contribute to component context";
            assert (CustomDependency) dependency1 == dependency3 : "Type parameter did not contribute to component context";
        }
    }
}

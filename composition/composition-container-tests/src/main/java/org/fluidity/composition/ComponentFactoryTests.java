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

package org.fluidity.composition;

import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.composition.spi.Factory;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
public final class ComponentFactoryTests extends AbstractContainerTests {

    @SuppressWarnings("unchecked")
    private final ComponentFactory factory = addControl(ComponentFactory.class);
    private final Factory.Instance instance = addControl(Factory.Instance.class);

    public ComponentFactoryTests(final ContainerFactory factory) {
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

        EasyMock.expect(factory.resolve(EasyMock.<Factory.Resolver>notNull(), EasyMock.<ComponentContext>notNull())).andAnswer(new FactoryInvocation(Check.class, check, instance)).anyTimes();
        instance.bind(EasyMock.<Factory.Registry>notNull());
        EasyMock.expectLastCall().anyTimes();

        replay();
        verifyComponent(container);
        verify();
    }

    @Test
    public void invokesStandaloneFactoryClassOnceInChildContainer() throws Exception {
        registry.bindComponent(Value.class);

        final OpenComponentContainer child = registry.makeChildContainer(DependentFactory.class);

        final Check check = new Check();
        final ComponentContainer.Registry childRegistry = child.getRegistry();

        childRegistry.bindComponent(FactoryDependency.class);
        childRegistry.bindInstance(check);

        EasyMock.expect(factory.resolve(EasyMock.<Factory.Resolver>notNull(), EasyMock.<ComponentContext>notNull())).andAnswer(new FactoryInvocation(Check.class, check, instance)).anyTimes();
        instance.bind(EasyMock.<Factory.Registry>notNull());
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

        EasyMock.expect(factory.resolve(EasyMock.<Factory.Resolver>notNull(), EasyMock.<ComponentContext>notNull())).andReturn(instance).anyTimes();
        instance.bind(EasyMock.<Factory.Registry>notNull());
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
        EasyMock.expect(factory.resolve(EasyMock.<Factory.Resolver>notNull(), EasyMock.<ComponentContext>notNull())).andReturn(instance).anyTimes();
        instance.bind(EasyMock.<Factory.Registry>notNull());
        EasyMock.expectLastCall().times(factories);

        replay();
        final GroupApi[] group = container.getComponentGroup(GroupApi.class);
        verify();

        assert group != null : GroupApi.class;
        assert group.length == 2 : group.length;
        assert group[0] != group[1];
    }

    @Test(enabled = false)  // TODO
    public void testContainerDependentInstantiatingFactory() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(ContainerDependentInstantiatingFactory.class);

        replay();
        container.getComponent(ContainerDependentInstantiatingComponent.class);
        verify();

        assert false : "Dynamic dependency should have been prevented";
    }

    @Component(api = DependentKey.class, automatic = false)
    private static class DependentFactory implements ComponentFactory {

        public static ComponentFactory delegate;

        public DependentFactory(final FactoryDependency dependency) {
            assert dependency != null;
        }

        public Instance resolve(final Resolver dependencies, final ComponentContext context) throws ComponentContainer.ResolutionException {
            assert delegate != null;
            final Instance instance = delegate.resolve(dependencies, context);

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
    private static interface GroupApi { }

    private static interface GroupMember2Api extends GroupApi { }

    @Component(automatic = false)
    private static class GroupMember1 implements GroupApi { }

    @Component(automatic = false)
    private static class GroupMember2 implements GroupMember2Api { }

    @Component(api = GroupMember1.class, automatic = false)
    private static class GroupMember1Factory implements ComponentFactory {

        public static ComponentFactory delegate;

        public Instance resolve(final Resolver dependencies, final ComponentContext context) throws ComponentContainer.ResolutionException {
            assert delegate != null;
            final Instance instance = delegate.resolve(dependencies, context);

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
    private static class GroupMember2Factory implements ComponentFactory {

        public static ComponentFactory delegate;

        public Instance resolve(final Resolver dependencies, final ComponentContext context) throws ComponentContainer.ResolutionException {
            assert delegate != null;
            final Instance instance = delegate.resolve(dependencies, context);

            return new Instance() {
                public void bind(final Registry registry) throws ComponentContainer.BindingException {
                    registry.bindComponent(GroupMember2.class);

                    assert instance != null;
                    instance.bind(registry);
                }
            };
        }

    }

    private static class FactoryInvocation implements IAnswer<Factory.Instance> {

        private final Class<?> checkKey;
        private final Object checkValue;
        private final Factory.Instance instance;

        public FactoryInvocation(final Class<?> checkKey, final Object checkValue, final Factory.Instance instance) {
            this.checkKey = checkKey;
            this.checkValue = checkValue;
            this.instance = instance;
        }

        public Factory.Instance answer() throws Throwable {
            final Factory.Resolver resolver = (Factory.Resolver) EasyMock.getCurrentArguments()[0];
            assert resolver != null : "Received no resolver";

            final Factory.Dependency<?> dependency = resolver.resolve(checkKey);
            assert dependency != null && dependency.instance() == checkValue : "Container does not check up";

            return instance;
        }
    }

    @Component(automatic = false)
    private static class ContainerDependentInstantiatingComponent {

        private ContainerDependentInstantiatingComponent(final ComponentContainer container) {
            assert container != null : ComponentContainer.class;
            container.getComponent(Key.class);
        }
    }

    @Component(api = ContainerDependentInstantiatingComponent.class)
    private static class ContainerDependentInstantiatingFactory implements ComponentFactory {

        public Instance resolve(final Resolver dependencies, final ComponentContext context) throws ComponentContainer.ResolutionException {
            final Dependency<?>[] args = dependencies.discover(ContainerDependentInstantiatingComponent.class);

            return new Instance() {
                public void bind(final Registry registry) throws ComponentContainer.BindingException {
                    registry.bindInstance(new ContainerDependentInstantiatingComponent((ComponentContainer) args[0].instance()));
                }
            };
        }
    }
}

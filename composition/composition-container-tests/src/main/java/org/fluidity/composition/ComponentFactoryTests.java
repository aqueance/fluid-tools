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

import java.io.Serializable;

import org.fluidity.composition.spi.ComponentFactory;

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
    private ComponentFactory factory = addControl(ComponentFactory.class);

    public ComponentFactoryTests(final ContainerFactory factory) {
        super(factory);
    }

    @BeforeMethod
    public void setMockFactory() {
        Factory.delegate = this.factory;
        GroupMember1Factory.delegate = this.factory;
        GroupMember2Factory.delegate = this.factory;
    }

    @Test
    public void invokesStandaloneFactoryClassOnce() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(Factory.class);
        registry.bindComponent(FactoryDependency.class);

        final String check = "check";

        registry.bindInstance(check, Serializable.class);

        factory.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull());
        EasyMock.expectLastCall().andAnswer(new FactoryInvocation(Serializable.class, check)).anyTimes();

        replay();
        verifyComponent(container);
        verify();
    }

    @Test
    public void invokesStandaloneFactoryClassOnceInChildContainer() throws Exception {
        registry.bindComponent(Value.class);

        final OpenComponentContainer child = registry.makeChildContainer(Factory.class);

        final String check = "check";
        final ComponentContainer.Registry childRegistry = child.getRegistry();

        childRegistry.bindComponent(FactoryDependency.class);
        childRegistry.bindInstance(check, Serializable.class);

        factory.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull());
        EasyMock.expectLastCall().andAnswer(new FactoryInvocation(Serializable.class, check)).anyTimes();

        replay();
        verifyComponent(container);
        verify();
    }

    @Test
    public void circularFactoryInvocation() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(Factory.class);
        registry.bindComponent(FactoryDependency.class);

        final String check = "check";

        registry.bindInstance(check, Serializable.class);

        factory.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull());
        EasyMock.expectLastCall().andAnswer(new CircularFactoryInvocation()).anyTimes();

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

    @Test
    public void groupMemberComponentFactoryOrder1() throws Exception {
        registry.bindComponent(GroupMember1Factory.class);
        registry.bindComponent(GroupMember2Factory.class);
        registry.bindComponent(GroupMember1.class);
        registry.bindComponent(GroupMember2.class);
        groupMemberChecks(2);
    }

    @Test
    public void groupMemberComponentFactoryOrder2() throws Exception {
        registry.bindComponent(GroupMember1.class);
        registry.bindComponent(GroupMember2.class);
        registry.bindComponent(GroupMember1Factory.class);
        registry.bindComponent(GroupMember2Factory.class);
        groupMemberChecks(2);
    }

    private void groupMemberChecks(final int factories) {
        factory.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull());
        EasyMock.expectLastCall().times(factories);

        replay();
        final GroupApi[] group = container.getComponentGroup(GroupApi.class);
        verify();

        assert group != null : GroupApi.class;
        assert group.length == 2 : group.length;
        assert group[0] != group[1];
    }

    @Component(api = DependentKey.class)
    private static class Factory implements ComponentFactory {

        public static ComponentFactory delegate;

        public Factory(final FactoryDependency dependency) {
            assert dependency != null;
        }

        public void newComponent(final OpenComponentContainer container, final ComponentContext context) {
            final ComponentContainer.Registry registry = container.getRegistry();
            registry.bindComponent(DependentValue.class);

            assert delegate != null;
            delegate.newComponent(container, context);
        }
    }

    @ComponentGroup
    private static interface GroupApi { }

    private static interface GroupMember2Api extends GroupApi { }

    private static class GroupMember1 implements GroupApi { }

    @Component
    private static class GroupMember2 implements GroupMember2Api { }

    @Component(api = GroupMember1.class)
    private static class GroupMember1Factory implements ComponentFactory {

        public static ComponentFactory delegate;

        public void newComponent(final OpenComponentContainer container, final ComponentContext context) {
            final ComponentContainer.Registry registry = container.getRegistry();
            registry.bindComponent(GroupMember1.class);

            assert delegate != null;
            delegate.newComponent(container, context);
        }
    }

    @Component(api = {GroupMember2Api.class })
    private static class GroupMember2Factory implements ComponentFactory {

        public static ComponentFactory delegate;

        public void newComponent(final OpenComponentContainer container, final ComponentContext context) {
            final ComponentContainer.Registry registry = container.getRegistry();
            registry.bindComponent(GroupMember2.class);

            assert delegate != null;
            delegate.newComponent(container, context);
        }
    }

    private static class FactoryInvocation implements IAnswer<Void> {

        private final Class<?> checkKey;
        private final Object checkValue;

        public FactoryInvocation(final Class<?> checkKey, final Object checkValue) {
            this.checkKey = checkKey;
            this.checkValue = checkValue;
        }

        public Void answer() throws Throwable {
            final OpenComponentContainer container = (OpenComponentContainer) EasyMock.getCurrentArguments()[0];

            assert container != null : "Received no container";
            assert container.getComponent(checkKey) == checkValue : "Container does not check up";

            return null;
        }
    }

    private static class CircularFactoryInvocation implements IAnswer<Void> {

        public Void answer() throws Throwable {
            final OpenComponentContainer container = (OpenComponentContainer) EasyMock.getCurrentArguments()[0];
            assert container != null;
            return null;
        }
    }
}

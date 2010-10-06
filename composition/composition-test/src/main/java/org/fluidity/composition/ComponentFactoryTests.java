/*
 * Copyright (c) 2006-2010 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public final class ComponentFactoryTests extends AbstractContainerTests {

    @SuppressWarnings("unchecked")
    private ComponentFactory<DependentKey> factory = addControl(ComponentFactory.class);

    public ComponentFactoryTests(final ContainerFactory factory) {
        super(factory);
    }

    @BeforeMethod
    public void setMockFactory() {
        Factory.delegate = this.factory;
    }

    @Test(expectedExceptions = ComponentContainer.BindingException.class)
    public void reportsBrokenFactory() throws Exception {
        registry.bindComponent(BrokenFactory.class);
    }

    @Test(expectedExceptions = ComponentContainer.BindingException.class)
    public void reportsNestedBrokenFactory() throws Exception {
        registry.makeNestedContainer(BrokenFactory.class, BrokenFactory.class);
    }

    @Test
    public void invokesStandaloneFactoryClassOnce() throws Exception {
        registry.bindComponent(Key.class, Value.class);
        registry.bindComponent(Factory.class);
        registry.bindComponent(FactoryDependency.class);

        final String check = "check";

        registry.bindInstance(Serializable.class, check);

        EasyMock.expect(factory.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull()))
                .andAnswer(new FactoryInvocation(Serializable.class, check));

        replay();
        verifyComponent(Value.instanceCount, 1, container);
        verify();
    }

    @Test
    public void invokesNestedStandaloneFactoryClassOnce() throws Exception {
        registry.bindComponent(Key.class, Value.class);

        final OpenComponentContainer nested = registry.makeNestedContainer(Factory.class, Factory.class);

        final String check = "check";
        final ComponentContainer.Registry nestedRegistry = nested.getRegistry();

        nestedRegistry.bindComponent(FactoryDependency.class);
        nestedRegistry.bindInstance(Serializable.class, check);

        EasyMock.expect(factory.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull()))
                .andAnswer(new FactoryInvocation(Serializable.class, check));

        replay();
        verifyComponent(Value.instanceCount, 1, container);
        verify();
    }

    @Test
    public void circularFactoryInvocation() throws Exception {
        registry.bindComponent(Key.class, Value.class);
        registry.bindComponent(Factory.class);
        registry.bindComponent(FactoryDependency.class);

        final String check = "check";

        registry.bindInstance(Serializable.class, check);

        EasyMock.expect(factory.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull()))
                .andAnswer(new CircularFactoryInvocation(container));

        replay();
        verifyComponent(Value.instanceCount, 1, container);
        verify();
    }

    @Component(api = DependentKey.class, type = DependentValue.class)
    private static class Factory implements ComponentFactory<DependentKey> {

        public static ComponentFactory<DependentKey> delegate;

        public Factory(final FactoryDependency dependency) {
            assert dependency != null;
        }

        public DependentKey newComponent(final OpenComponentContainer container, final ComponentContext context) {
            final ComponentContainer.Registry registry = container.getRegistry();
            registry.bindComponent(DependentKey.class, DependentValue.class);

            assert delegate != null;
            return delegate.newComponent(container, context);
        }
    }

    // faulty: has no @Component(type = <component class>) annotation

    @Component(api = DependentKey.class)
    private static class BrokenFactory implements ComponentFactory<DependentKey> {

        public BrokenFactory() {
            assert false : "Should not have been instantiated";
        }

        public DependentKey newComponent(final OpenComponentContainer container, final ComponentContext context) {
            assert false : "Should not have been invoked";
            return null;
        }
    }

    private static class FactoryInvocation implements IAnswer<DependentKey> {

        private final Class<?> checkKey;
        private final Object checkValue;

        public FactoryInvocation(final Class<?> checkKey, final Object checkValue) {
            this.checkKey = checkKey;
            this.checkValue = checkValue;
        }

        public DependentKey answer() throws Throwable {
            final OpenComponentContainer received = (OpenComponentContainer) EasyMock.getCurrentArguments()[0];

            assert received != null : "Received no container";
            assert received.getComponent(checkKey) == checkValue : "Container does not check up";

            return received.getComponent(DependentKey.class);
        }
    }

    private static class CircularFactoryInvocation implements IAnswer<DependentKey> {

        private final OpenComponentContainer container;

        public CircularFactoryInvocation(final OpenComponentContainer container) {
            this.container = container;
        }

        public DependentKey answer() throws Throwable {
            return container.getComponent(DependentKey.class);
        }
    }
}

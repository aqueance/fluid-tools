/*
 * Copyright (c) 2006-2008 Tibor Adam Varga (tibor.adam.varga@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Softweare"), to deal
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */
package org.fluidity.composition.web;

import java.util.List;
import java.util.Map;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.OpenComponentContainer;
import org.fluidity.tests.MockGroupAbstractTest;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class DependencyResolverImplTest extends MockGroupAbstractTest {

    private static OpenComponentContainer container;

    private final ComponentContainer.Registry registry = addControl(ComponentContainer.Registry.class);

    private DependencyResolver resolver = new DependencyResolverImpl();

    public DependencyResolverImplTest() {
        container = addControl(OpenComponentContainer.class);
    }

    @Test
    public void componentAcquisitionByClass() throws Exception {
        Component component = new Component();

        EasyMock.expect(container.getComponent(Component.class)).andReturn(component);

        replay();
        assert resolver.findComponent(MockComponentContainer.class.getName(), Component.class.getName()) == component;
        verify();
    }

    @Test
    public void usesChildContainerWhenComponentNotFound() throws Exception {
        final Component component = new Component();

        EasyMock.expect(container.getComponent(Component.class)).andReturn(null);
        EasyMock.expect(
            container.getComponent(EasyMock.same(Component.class), (ComponentContainer.Bindings) EasyMock.notNull()))
            .andAnswer(new IAnswer<Component>() {
                public Component answer() throws Throwable {
                    ComponentContainer.Bindings bindings =
                        (ComponentContainer.Bindings) EasyMock.getCurrentArguments()[1];

                    // invoke testee supplied parameter
                    bindings.registerComponents(registry);

                    // return to testee
                    return component;
                }
            });

        // this is what the testee should do when invoke above from the inner class
        registry.bind(Component.class);

        replay();
        assert resolver.findComponent(MockComponentContainer.class.getName(), Component.class.getName()) == component;
        verify();
    }

    public static class MockComponentContainer implements OpenComponentContainer {

        public ComponentContainer getContainer() {
            return DependencyResolverImplTest.container;
        }

        public <T> T getComponent(Class<T> componentClass) {
            return DependencyResolverImplTest.container.getComponent(componentClass);
        }

        public <T> T getComponent(Class<T> componentClass, Bindings bindings) {
            return DependencyResolverImplTest.container.getComponent(componentClass, bindings);
        }

        public Registry getRegistry() {
            return DependencyResolverImplTest.container.getRegistry();
        }

        public Map<Class, List<Class>> getUnresolvedDependencies() {
            return DependencyResolverImplTest.container.getUnresolvedDependencies();
        }

        public OpenComponentContainer makeNestedContainer() {
            return DependencyResolverImplTest.container.makeNestedContainer();
        }
    }

    public static class Component {

    }
}

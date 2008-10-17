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
package org.fluidity.composition.pico;

import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMock;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.OpenComponentContainer;
import org.fluidity.tests.MockGroupAbstractTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class PicoComponentContainerTest extends MockGroupAbstractTest {

    private OpenComponentContainer container;

    private ComponentContainer.Registry registry;

    @SuppressWarnings({ "unchecked" })
    private ComponentContainer.ComponentFactory<DependentKey> factory =
        addControl(ComponentContainer.ComponentFactory.class);

    public PicoComponentContainerTest() {
        Factory.delegate = factory;
    }

    @BeforeMethod
    public void setup() throws Exception {
        super.setup();
        
        container = new PicoComponentContainer();
        registry = container.getRegistry();

        // clean up
        Value.dependent = null;
    }

    @Test
    public void singletonComponentRegistration() throws Exception {
        int originalCount = Value.instanceCount;

        registry.bind(Key.class, Value.class, true, false, false);
        registry.bind(DependentKey.class, DependentValue.class, true, false, false);

        verifyComponent(container);

        Key component = container.getComponent(Key.class);

        // the key tells us how many times the class was instantiated
        assert container.getComponent(Key.class).key().equals(component.key());
        assert Value.instanceCount == originalCount + 1;
    }

    @Test
    public void nonSingletonComponentRegistration() throws Exception {
        registry.bind(Key.class, Value.class, false, false, false);
        registry.bind(DependentKey.class, DependentValue.class, true, false, false);

        Key component = container.getComponent(Key.class);
        assert component != null;
        assert component instanceof Value;

        // the key tells us how many times the class was instantiated
        assert !container.getComponent(Key.class).key().equals(component.key());
    }

    @Test
    public void threadLocalComponentRegistration() throws Exception {
        registry.bind(Key.class, Value.class, true, true, false);
        registry.bind(DependentKey.class, DependentValue.class, true, false, false);

        final Key component = container.getComponent(Key.class);
        assert component != null;

        // the same the second time as well
        assert container.getComponent(Key.class) == component;

        Thread thread = new Thread() {
            public void run() {
                Key localComponent = container.getComponent(Key.class);
                assert localComponent != null;

                // the same the second time as well
                assert localComponent == container.getComponent(Key.class);

                // not the same as for the other thread
                // the key tells us how many times the class was instantiated
                assert !component.key().equals(localComponent.key());
            }
        };

        thread.start();
        thread.join();
    }

    @Test
    public void deferredComponentRegistration() throws Exception {
        int originalCount = Value.instanceCount;

        registry.bind(Key.class, Value.class, true, false, true);
        registry.bind(DependentKey.class, DependentValue.class, true, false, false);

        Key component = container.getComponent(Key.class);
        assert component != null;

        assert Value.instanceCount == originalCount;
        component.key();
        assert Value.instanceCount == originalCount + 1;
    }

    @Test
    public void findsSuitableImplementation() throws Exception {
        registry.bind(Key.class, Value.class, true, false, false);

        // register by class, not interface
        registry.bind(DependentValue.class, DependentValue.class, true, false, false);

        verifyComponent(container);

        // registration by class was used
        assert Value.dependent instanceof DependentValue;
    }

    @Test
    public void defaultImplementation() throws Exception {
        registry.bind(Key.class, Value.class, true, false, false);

        // this should not be found by interface
        registry.bind(DependentValue.class, DependentValue.class, true, false, false);

        // this should be found by interface
        registry.bind(DependentKey.class, FakeDependentValue.class, true, false, false);

        verifyComponent(container);

        // registration by interface took precedence
        assert Value.dependent instanceof FakeDependentValue;
    }

    @Test
    public void instanceRegistration() throws Exception {
        registry.bind(Key.class, Value.class, true, false, false);
        registry.bind(DependentKey.class, new DependentValue());

        verifyComponent(container);
    }

    @Test
    public void nestedContainerDefaultsToParent() throws Exception {
        registry.bind(DependentKey.class, DependentValue.class, true, false, false);

        // one variant of the makeNestedContainer method
        registry.makeNestedContainer(Key.class, Value.class, true, false, false);
        verifyComponent(container);
    }

    @Test
    public void nestedContainerResolvesLocally() throws Exception {

        // another variant of the makeNestedContainer method
        OpenComponentContainer nested = registry.makeNestedContainer(Key.class, Value.class, true, false, false,
            registry.component(DependentKey.class));

        nested.getRegistry().bind(DependentKey.class, DependentValue.class, true, false, false);

        verifyComponent(container);
    }

    @Test
    public void nestedContainerInteraction() throws Exception {
        registry.makeNestedContainer(DependentKey.class, DependentValue.class, true, false, false);
        registry.makeNestedContainer(Key.class, Value.class, true, false, false);
        verifyComponent(container);
    }

    @Test
    public void componentsWithForcedComponentParameter() throws Exception {
        registry.bind(Key.class, Value.class, true, false, false, registry.component(DependentKey.class));

        registry.bind(DependentKey.class, DependentValue.class, true, false, false);

        verifyComponent(container);
    }

    @Test
    public void componentsWithForcedConstantParameter() throws Exception {
        registry.bind(Key.class, Value.class, true, false, false, registry.constant(new DependentValue()));

        verifyComponent(container);
    }

    @Test
    public void doesNotResolveJdkFoundationClasses() throws Exception {
        registry.bind(ArrayList.class, ArrayList.class, true, false, false);
        assert container.getComponent(List.class) == null;
        assert container.getComponent(ArrayList.class) != null;
    }

    @Test
    public void childRegistryOfContainerUsesContainerForParent() throws Exception {
        registry.bind(DependentKey.class, DependentValue.class, true, false, false);

        OpenComponentContainer nested = container.makeNestedContainer();
        nested.getRegistry().bind(Key.class, Value.class, true, false, false);

        verifyComponent(nested);
    }

    @Test
    public void childRegistryReallyNested() throws Exception {
        registry.bind(DependentKey.class, FakeDependentValue.class, true, false, false);
        registry.bind(Key.class, FakeValue.class, true, false, false);

        OpenComponentContainer nestedContainer = container.makeNestedContainer();
        ComponentContainer.Registry nestedRegistry = nestedContainer.getRegistry();
        nestedRegistry.bind(DependentKey.class, DependentValue.class, true, false, false);
        nestedRegistry.bind(Key.class, Value.class, true, false, false);

        assert nestedContainer.getComponent(Key.class) instanceof Value;
        assert nestedContainer.getComponent(DependentKey.class) instanceof DependentValue;

        assert container.getComponent(Key.class) instanceof FakeValue;
        assert container.getComponent(DependentKey.class) instanceof FakeDependentValue;

        verifyComponent(nestedContainer);
    }

    @Test
    public void containerContainsItself() throws Exception {

        // make sure we have the container inside the container without asserting to have received the same instance
        ComponentContainer containerComponent = container.getComponent(ComponentContainer.class);
        assert containerComponent != null;

        assert containerComponent.getComponent(Key.class) == null;
        registry.bind(Key.class, Value.class);
        registry.bind(DependentKey.class, DependentValue.class);
        assert containerComponent.getComponent(Key.class) != null;
    }

    @Test
    public void nestedContainerContainsItself() throws Exception {
        final OpenComponentContainer nestedContainer = registry.makeNestedContainer();
        final ComponentContainer.Registry nestedRegistry = nestedContainer.getRegistry();

        // make sure we have the container inside the container without asserting to have received the same instance
        ComponentContainer containerComponent = nestedContainer.getComponent(ComponentContainer.class);
        assert containerComponent != null;

        assert containerComponent.getComponent(Key.class) == null;
        nestedRegistry.bind(Key.class, Value.class);
        nestedRegistry.bind(DependentKey.class, DependentValue.class);
        assert containerComponent.getComponent(Key.class) != null;
    }

    @Test
    public void invokesFactoryInstanceOnce() throws Exception {
        int originalCount = Value.instanceCount;
        final DependentValue value = new DependentValue();

        registry.bind(Key.class, Value.class, true, false, false);
        registry.bind(DependentKey.class, DependentValue.class, true, false, false, factory);
        EasyMock.expect(factory.makeComponent(container)).andReturn(value);

        replay();
        Key component = container.getComponent(Key.class);

        // the key tells us how many times the class was instantiated
        assert container.getComponent(Key.class).key().equals(component.key());
        assert Value.instanceCount == originalCount + 1;
        assert container.getComponent(DependentKey.class) == value;
        verify();
    }

    @Test
    public void invokesNestedFactoryInstanceOnce() throws Exception {
        int originalCount = Value.instanceCount;
        final DependentValue value = new DependentValue();

        registry.bind(Key.class, Value.class, true, false, false);
        OpenComponentContainer nested =
            registry.makeNestedContainer(DependentKey.class, DependentValue.class, true, false, false, factory);
        EasyMock.expect(factory.makeComponent(nested)).andReturn(value);

        replay();
        Key component = container.getComponent(Key.class);

        // the key tells us how many times the class was instantiated
        assert container.getComponent(Key.class).key().equals(component.key());
        assert Value.instanceCount == originalCount + 1;
        assert container.getComponent(DependentKey.class) == value;
        verify();
    }

    @Test
    public void invokesFactoryClassOnce() throws Exception {
        int originalCount = Value.instanceCount;
        final DependentValue value = new DependentValue();

        registry.bind(Key.class, Value.class, true, false, false);
        registry.bind(Factory.class, Factory.class);
        registry.bind(FactoryDependency.class, FactoryDependency.class);
        registry.bind(DependentKey.class, DependentValue.class, true, false, false, Factory.class);
        EasyMock.expect(factory.makeComponent(container)).andReturn(value);

        replay();
        Key component = container.getComponent(Key.class);

        // the key tells us how many times the class was instantiated
        assert container.getComponent(Key.class).key().equals(component.key());
        assert Value.instanceCount == originalCount + 1;
        assert container.getComponent(DependentKey.class) == value;
        verify();
    }

    @Test
    public void invokesNestedFactoryClassOnce() throws Exception {
        int originalCount = Value.instanceCount;
        final DependentValue value = new DependentValue();

        registry.bind(Key.class, Value.class, true, false, false);
        registry.bind(Factory.class, Factory.class);
        registry.bind(FactoryDependency.class, FactoryDependency.class);
        OpenComponentContainer nested =
            registry.makeNestedContainer(DependentKey.class, DependentValue.class, true, false, false, Factory.class);
        EasyMock.expect(factory.makeComponent(nested)).andReturn(value);

        replay();
        Key component = container.getComponent(Key.class);

        // the key tells us how many times the class was instantiated
        assert container.getComponent(Key.class).key().equals(component.key());
        assert Value.instanceCount == originalCount + 1;
        assert container.getComponent(DependentKey.class) == value;
        verify();
    }

    @Test
    public void identifiesContainerChain() throws Exception {
        replay();
        OpenComponentContainer nested = container.makeNestedContainer();

        String topString = container.toString();
        String nestedString = nested.toString();
        assert topString.startsWith("container ");

        int prefix = "container ".length();
        String topId = topString.substring(prefix);
        assert nestedString.startsWith("container ");
        String nestedSuffix = " > " + topId;
        assert nestedString.endsWith(nestedSuffix);

        String nestedId = nestedString.substring(prefix, nestedString.length() - nestedSuffix.length());
        assert !topId.equals(nestedId);
        verify();
    }

    @Test
    public void transientComponentInstantiation() throws Exception {
        replay();
        Key value = container.getComponent(Key.class, new ComponentContainer.Bindings() {

            public void registerComponents(ComponentContainer.Registry registry) {
                registry.bind(Key.class, Value.class);
                registry.bind(DependentKey.class, DependentValue.class);
            }
        });

        assert value != null;
        assert container.getComponent(Key.class) == null;
        assert container.getComponent(DependentKey.class) == null;
        verify();
    }

    private void verifyComponent(ComponentContainer container) {
        Object component = container.getComponent(Key.class);
        assert component != null;
        assert component instanceof Value;
        assert container.getComponent(Value.class) == component;
    }

    public static interface Keyed {

        String key();
    }

    public static interface Key extends Keyed {

    }

    public static interface DependentKey {

    }

    /**
     * This is intentionally private - makes sure the container is able to instantiate non-public classes
     */
    private static class Value implements Key {

        public static DependentKey dependent;

        public static int instanceCount = 0;

        private static int counter = 0;

        private final String key;

        public Value(DependentKey dependent) {
            assert dependent != null;
            Value.dependent = dependent;

            synchronized (Value.class) {
                key = String.valueOf(counter++);
                instanceCount++;
            }
        }

        public String key() {
            return key;
        }
    }

    /**
     * This is intentionally private - makes sure the container is able to instantiate non-public classes
     */
    private static class FakeValue implements Key {

        public FakeValue(DependentKey dependent) {
            assert dependent != null;
        }

        public String key() {
            return null;
        }
    }

    /**
     * This is intentionally private - makes sure the container is able to instantiate non-public classes
     */
    private static class DependentValue implements DependentKey {

    }

    /**
     * This is intentionally private - makes sure the container is able to instantiate non-public classes
     */
    private static class FakeDependentValue implements DependentKey {

    }

    /**
     * Something for the Factory to depend on, tests dependency resolution on factories.
     */
    public static class FactoryDependency {
        // empty
    }

    private static class Factory implements ComponentContainer.ComponentFactory<DependentKey> {

        public static ComponentContainer.ComponentFactory<DependentKey> delegate;

        public Factory(FactoryDependency dependent) {
            assert dependent != null;
        }

        public DependentKey makeComponent(OpenComponentContainer container) {
            assert delegate != null;
            return delegate.makeComponent(container);
        }
    }
}

package org.fluidity.composition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.fluidity.tests.MockGroupAbstractTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @authot Tibor Varga
 */
public abstract class ComponentContainerAbstractTest extends MockGroupAbstractTest {

    private OpenComponentContainer container;
    private ComponentContainer.Registry registry;

    @SuppressWarnings({ "unchecked" })
    protected ComponentContainer.ComponentFactory<DependentKey> factory =
        addControl(ComponentContainer.ComponentFactory.class);

    public ComponentContainerAbstractTest() {
        Factory.delegate = factory;
    }

    protected abstract OpenComponentContainer newContainer();

    @BeforeMethod
    public void setup() throws Exception {
        super.setup();

        container = newContainer();
        registry = container.getRegistry();

        // clean up
        Value.dependent = null;
    }

    @Test
    public void singletonComponentRegistration() throws Exception {
        final int originalCount = Value.instanceCount;

        registry.bind(Key.class, Value.class, true, false, false);
        registry.bind(DependentKey.class, DependentValue.class, true, false, false);

        verifyComponent(container);

        final Key component = container.getComponent(Key.class);

        // the key tells us how many times the class was instantiated
        assert container.getComponent(Key.class).key().equals(component.key());
        assert Value.instanceCount == originalCount + 1;
    }

    @Test
    public void nonSingletonComponentRegistration() throws Exception {
        registry.bind(Key.class, Value.class, false, false, false);
        registry.bind(DependentKey.class, DependentValue.class, true, false, false);

        final Key component = container.getComponent(Key.class);
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

        final Thread thread = new Thread() {
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
        final int originalCount = Value.instanceCount;

        registry.bind(Key.class, Value.class, true, false, true);
        registry.bind(DependentKey.class, DependentValue.class, true, false, false);

        final Key component = container.getComponent(Key.class);
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
        final OpenComponentContainer nested = registry.makeNestedContainer(Key.class, Value.class, true, false, false,
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

        final OpenComponentContainer nested = container.makeNestedContainer();
        nested.getRegistry().bind(Key.class, Value.class, true, false, false);

        verifyComponent(nested);
    }

    @Test
    public void childRegistryReallyNested() throws Exception {
        registry.bind(DependentKey.class, FakeDependentValue.class, true, false, false);
        registry.bind(Key.class, FakeValue.class, true, false, false);

        final OpenComponentContainer nestedContainer = container.makeNestedContainer();
        final ComponentContainer.Registry nestedRegistry = nestedContainer.getRegistry();
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
        final ComponentContainer containerComponent = nestedContainer.getComponent(ComponentContainer.class);
        assert containerComponent != null;

        assert containerComponent.getComponent(Key.class) == null;
        nestedRegistry.bind(Key.class, Value.class);
        nestedRegistry.bind(DependentKey.class, DependentValue.class);
        assert containerComponent.getComponent(Key.class) != null;
    }

    @Test
    public void invokesFactoryInstanceOnce() throws Exception {
        final int originalCount = Value.instanceCount;
        final DependentValue value = new DependentValue();

        registry.bind(Key.class, Value.class, true, false, false);
        registry.bind(DependentKey.class, DependentValue.class, true, false, false, factory);

        final String check = "check";
        registry.bind(Serializable.class, check);
        EasyMock.expect(factory.makeComponent((ComponentContainer) EasyMock.notNull()))
            .andAnswer(new ContainerCheck<DependentKey, DependentValue>(container, value, Serializable.class, check));

        replay();
        final Key component = container.getComponent(Key.class);

        // the key tells us how many times the class was instantiated
        assert container.getComponent(Key.class).key().equals(component.key());
        assert Value.instanceCount == originalCount + 1;
        assert container.getComponent(DependentKey.class) == value;
        verify();
    }

    @Test
    public void invokesNestedFactoryInstanceOnce() throws Exception {
        final int originalCount = Value.instanceCount;
        final DependentValue value = new DependentValue();

        registry.bind(Key.class, Value.class, true, false, false);
        OpenComponentContainer nested =
            registry.makeNestedContainer(DependentKey.class, DependentValue.class, true, false, false, factory);
        final String check = "check";
        nested.getRegistry().bind(Serializable.class, check);
        EasyMock.expect(factory.makeComponent((ComponentContainer) EasyMock.notNull()))
            .andAnswer(new ContainerCheck<DependentKey, DependentValue>(nested, value, Serializable.class, check));

        replay();
        final Key component = container.getComponent(Key.class);

        // the key tells us how many times the class was instantiated
        assert container.getComponent(Key.class).key().equals(component.key());
        assert Value.instanceCount == originalCount + 1;
        assert container.getComponent(DependentKey.class) == value;
        verify();
    }

    @Test
    public void invokesFactoryClassOnce() throws Exception {
        final int originalCount = Value.instanceCount;
        final DependentValue value = new DependentValue();

        registry.bind(Key.class, Value.class, true, false, false);
        registry.bind(Factory.class, Factory.class);
        registry.bind(FactoryDependency.class, FactoryDependency.class);
        registry.bind(DependentKey.class, DependentValue.class, true, false, false, Factory.class);

        final String check = "check";
        registry.bind(Serializable.class, check);
        EasyMock.expect(factory.makeComponent((ComponentContainer) EasyMock.notNull()))
            .andAnswer(new ContainerCheck<DependentKey, DependentValue>(container, value, Serializable.class, check));

        replay();
        final Key component = container.getComponent(Key.class);

        // the key tells us how many times the class was instantiated
        assert container.getComponent(Key.class).key().equals(component.key());
        assert Value.instanceCount == originalCount + 1;
        assert container.getComponent(DependentKey.class) == value;
        verify();
    }

    @Test
    public void invokesNestedFactoryClassOnce() throws Exception {
        final int originalCount = Value.instanceCount;
        final DependentValue value = new DependentValue();

        registry.bind(Key.class, Value.class, true, false, false);
        registry.bind(Factory.class, Factory.class);
        registry.bind(FactoryDependency.class, FactoryDependency.class);
        final OpenComponentContainer nested =
            registry.makeNestedContainer(DependentKey.class, DependentValue.class, true, false, false, Factory.class);

        final String check = "check";
        nested.getRegistry().bind(Serializable.class, check);
        EasyMock.expect(factory.makeComponent((ComponentContainer) EasyMock.notNull()))
            .andAnswer(new ContainerCheck<DependentKey, DependentValue>(nested, value, Serializable.class, check));

        replay();
        final Key component = container.getComponent(Key.class);

        // the key tells us how many times the class was instantiated
        assert container.getComponent(Key.class).key().equals(component.key());
        assert Value.instanceCount == originalCount + 1;
        assert container.getComponent(DependentKey.class) == value;
        verify();
    }

    @Test
    public void identifiesContainerChain() throws Exception {
        replay();
        final OpenComponentContainer nested = container.makeNestedContainer();

        final String topString = container.toString();
        final String nestedString = nested.toString();
        assert topString.startsWith("container ");

        final int prefix = "container ".length();
        final String topId = topString.substring(prefix);
        assert nestedString.startsWith("container ");
        final String nestedSuffix = " > " + topId;
        assert nestedString.endsWith(nestedSuffix);

        final String nestedId = nestedString.substring(prefix, nestedString.length() - nestedSuffix.length());
        assert !topId.equals(nestedId);
        verify();
    }

    @Test
    public void transientComponentInstantiation() throws Exception {
        replay();
        final Key value = container.getComponent(Key.class, new ComponentContainer.Bindings() {

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
        final Object component = container.getComponent(Key.class);
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

        public Factory(final FactoryDependency dependent) {
            assert dependent != null;
        }

        public DependentKey makeComponent(final ComponentContainer container) {
            assert delegate != null;
            return delegate.makeComponent(container);
        }
    }

    private static class ContainerCheck<K, V extends K> implements IAnswer<K> {

        private final OpenComponentContainer container;
        private final Class<?> checkKey;
        private final Object checkValue;
        private final V value;

        public ContainerCheck(final OpenComponentContainer container,
                               final V value, final Class<?> checkKey,
                               Object checkValue
        ) {
            this.container = container;
            this.checkKey = checkKey;
            this.checkValue = checkValue;
            this.value = value;
        }

        public K answer() throws Throwable {
            final ComponentContainer received = (ComponentContainer) EasyMock.getCurrentArguments()[0];

            assert received != null : "Received no container";
            assert received.getComponent(checkKey) == checkValue
                : "Expected " + container.toString() + ", got " + received.toString();

            return value;
        }
    }
}

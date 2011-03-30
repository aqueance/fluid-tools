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

import org.fluidity.tests.MockGroupAbstractTest;

import org.testng.annotations.BeforeMethod;

/**
 * Abstract test for container related test cases. Used by {@link ComponentContainerAbstractTest}.
 *
 * @author Tibor Varga
 */
public abstract class AbstractContainerTests extends MockGroupAbstractTest {

    protected final ContainerFactory factory;

    protected OpenComponentContainer container;
    protected ComponentContainer.Registry registry;

    public AbstractContainerTests(final ContainerFactory factory) {
        this.factory = factory;
    }

    @BeforeMethod
    public void createContainer() throws Exception {
        container = factory.createContainer();
        registry = container.getRegistry();
        Value.dependent = null;
    }

    /**
     * Verifies that the given container contains a Key component whose class is Value.
     *
     * @param container the container to verify.
     */
    protected void verifyComponent(final ComponentContainer container) {
        final int originalCount = Value.instanceCount;

        final Key component = container.getComponent(Key.class);
        assert component != null : "Test component not found in container";
        assert component instanceof Value : String.format("Test component is not of correct type: %s", component);

        // ask for the component again and compare with the last result
        assert container.getComponent(Key.class).key().equals(component.key()) : "Multiple component queries created multiple instances";

        // this tells us how many times the class was instantiated in this method
        assert Value.instanceCount == originalCount + 1 : String.format("Expected only 1 Key object, got %d", Value.instanceCount - originalCount);
    }

    public static interface Key {

        String key();
    }

    /**
     * This is intentionally protected - makes sure the container is able to instantiate non-public classes
     */
    @Component(automatic = false)
    protected static class Value implements Key {

        public static DependentKey dependent;

        public static int instanceCount = 0;        // counts the number of instances created
        private static int counter = 0;

        private final String key;

        // requires an instance of DependentKey

        public Value(final DependentKey dependent) {
            assert dependent != null;
            Value.dependent = dependent;

            synchronized (Value.class) {
                key = String.valueOf(counter++);
                instanceCount++;
            }
        }

        // the number of instances up to this one is the key

        public String key() {
            return key;
        }
    }

    public static interface DependentKey {

        ComponentContext context();
    }

    /**
     * This is intentionally protected - makes sure the container is able to instantiate non-public classes
     */
    @Component(automatic = false)
    protected static class DependentValue implements DependentKey {

        private final ComponentContext context;

        public DependentValue() {
            this(null);
        }

        public DependentValue(final ComponentContext context) {
            this.context = context;
        }

        public ComponentContext context() {
            return context;
        }
    }

    /**
     * This is intentionally protected - makes sure the container is able to instantiate non-public classes
     */
    @Component(primary = false, automatic = false)
    protected static class FallbackDependentValue implements DependentKey {

        private final ComponentContext context;

        public FallbackDependentValue() {
            this(null);
        }

        public FallbackDependentValue(final ComponentContext context) {
            this.context = context;
        }

        public ComponentContext context() {
            return context;
        }
    }

    /**
     * Depends on the enclosing container.
     */
    @Component(automatic = false)
    protected static class ContainerDependent {

        private final ComponentContainer container;

        public ContainerDependent(final ComponentContainer container) {
            assert container != null;
            this.container = container;
        }

        public ComponentContainer container() {
            return container;
        }
    }

    /**
     * Something for a factory to depend on, tests dependency resolution on factories.
     */
    @Component(automatic = false)
    public static class FactoryDependency {
        // empty
    }

    /**
     * Just a class with no dependencies, intended to be registered to a container and then checked if the registration there in a container received from the
     * system.
     */
    public static class Check {

    }
}

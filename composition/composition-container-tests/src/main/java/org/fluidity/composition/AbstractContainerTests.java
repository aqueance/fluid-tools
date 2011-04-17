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
    @SuppressWarnings("UnusedDeclaration")
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

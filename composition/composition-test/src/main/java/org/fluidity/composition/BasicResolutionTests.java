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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public final class BasicResolutionTests extends AbstractContainerTests {

    public BasicResolutionTests(final ContainerFactory factory) {
        super(factory);
    }

    private boolean collected;

    @Test
    public void transientContainersGetGarbageCollected() throws Exception {
        collected = false;      // just in case...

        class FinalizationAware {

            @Override
            protected void finalize() throws Throwable {
                collected = true;
                super.finalize();
            }
        }

        for (int i = 0; i < 100000 && !collected; ++i) {
            container.makeNestedContainer().getRegistry().bindInstance(Object.class, new FinalizationAware());
            Runtime.getRuntime().gc();
        }

        assert collected : "No component has been garbage collected";
    }

    @Test
    public void singletonComponentRegistration() throws Exception {
        registry.bindComponent(Value.class); // Value depends on DependentKey
        registry.bindComponent(DependentValue.class);

        verifyComponent(Value.instanceCount, 1, container);
    }

    @Test
    public void findsDefaultImplementation() throws Exception {
        registry.bindComponent(Value.class);

        // register by class, not interface
        registry.bindDefault(DependentValue.class);

        verifyComponent(Value.instanceCount, 1, container);

        // registration by class was used
        assert Value.dependent instanceof DependentValue;
    }

    @Test
    public void specificImplementationTakesPrecedenceOverDefault() throws Exception {
        registry.bindComponent(Value.class);

        // this should not be found by interface
        registry.bindComponent(DefaultDependentValue.class);

        // this should be found by interface
        registry.bindComponent(DependentValue.class);

        verifyComponent(Value.instanceCount, 1, container);

        // registration by interface took precedence
        assert Value.dependent instanceof DependentValue;
    }

    @Test
    public void instanceRegistration() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindInstance(DependentKey.class, new DependentValue());

        verifyComponent(Value.instanceCount, 1, container);
    }

    @Test
    public void dependencyOnContainer() throws Exception {
        registry.bindDefault(ContainerDependent.class);

        final ContainerDependent component = container.getComponent(ContainerDependent.class);
        assert component != null;

        final ComponentContainer container = component.container();

        // verify that we have the same container in our hands
        assert container.getComponent(Key.class) == null;
        registry.bindComponent(Key.class, Value.class);
        registry.bindComponent(DependentKey.class, DependentValue.class);
        assert container.getComponent(Key.class) != null;
    }

    @Test
    public void identifiesContainerChain() throws Exception {
        final OpenComponentContainer nested = container.makeNestedContainer();

        final String topString = container.toString();
        final String nestedString = nested.toString();
        assert topString.startsWith("container ") : String.format("Wrong container ID: %s", topString);

        final int prefix = "container ".length();
        final String topId = topString.substring(prefix);
        assert nestedString.startsWith("container ") : String.format("Wrong container ID: %s", nestedString);
        final String nestedSuffix = " > " + topId;
        assert nestedString.endsWith(nestedSuffix) : String.format("Wrong container ID: %s", nestedString);

        final String nestedId = nestedString.substring(prefix, nestedString.length() - nestedSuffix.length());
        assert !topId.equals(nestedId) : "Nested container has the same ID as its parent";
    }

    @Test
    public void transientComponentInstantiation() throws Exception {
        final Key value = container.getComponent(Key.class, new ComponentContainer.Bindings() {

            public void bindComponents(ComponentContainer.Registry registry) {
                registry.bindComponent(Key.class, Value.class);
                registry.bindComponent(DependentKey.class, DependentValue.class);
            }
        });

        assert value != null;
        assert container.getComponent(Key.class) == null;
        assert container.getComponent(DependentKey.class) == null;
    }

    @Test
    public void checkSerialization() throws Exception {
        SerializableComponent.container = container;

        registry.bindComponent(Value.class); // Value depends on DependentKey
        registry.bindComponent(DependentValue.class);

        final SerializableComponent component = new SerializableComponent();

        component.verify("original object");

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        new ObjectOutputStream(out).writeObject(component);

        final SerializableComponent clone = (SerializableComponent) new ObjectInputStream(new ByteArrayInputStream(out.toByteArray())).readObject();

        clone.verify("cloned object");
    }

    // this is how to inject dependencies into a serializable component
    public static class SerializableComponent implements Serializable {

        /* in real life you'd use new ComponentContainerAccess() here */
        private static ComponentContainer container;

        @Component
        @SuppressWarnings({ "UnusedDeclaration" })
        private transient Key dependency;

        public SerializableComponent() {
            assert container != null;
            container.initialize(this);
        }

        private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            assert container != null;
            container.initialize(this);
        }

        public void verify(final String context) {
            assert dependency != null: String.format("Dependency not set in %s", context);
        }
    }

    /**
     * This is intentionally private - makes sure the container is able to instantiate non-public classes
     */
    @Component(fallback = true)
    private static class DefaultDependentValue extends DependentValue {

    }
}

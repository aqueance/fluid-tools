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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
            container.makeChildContainer().getRegistry().bindInstance(Object.class, new FinalizationAware());
            Runtime.getRuntime().gc();
        }

        assert collected : "No component has been garbage collected";
    }

    @Test
    public void singletonComponentRegistration() throws Exception {
        registry.bindComponent(Key.class, Value.class); // Value depends on DependentKey
        registry.bindComponent(DependentKey.class, DependentValue.class);

        verifyComponent(Value.instanceCount, 1, container);
    }

    @Test
    public void findsDefaultImplementation() throws Exception {
        registry.bindComponent(Key.class, Value.class);
        registry.bindComponent(DependentKey.class, DefaultDependentValue.class);

        verifyComponent(Value.instanceCount, 1, container);

        assert Value.dependent instanceof DefaultDependentValue;
    }

    @Test
    public void primaryImplementationTakesPrecedenceOverFallback() throws Exception {
        registry.bindComponent(Key.class, Value.class);

        // this is the fallback
        registry.bindComponent(DependentKey.class, DefaultDependentValue.class);

        // this is the primary
        registry.bindComponent(DependentKey.class, DependentValue.class);

        verifyComponent(Value.instanceCount, 1, container);

        // registration by interface took precedence
        assert Value.dependent instanceof DependentValue : Value.dependent;
    }

    @Test
    public void instanceRegistration() throws Exception {
        registry.bindComponent(Key.class, Value.class);
        registry.bindInstance(DependentKey.class, new DependentValue());

        verifyComponent(Value.instanceCount, 1, container);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void arrayRegistration() throws Exception {
        registry.bindComponent(ArrayDependent.class, ArrayDependent.class);
        final Object[] array = new Object[0];
        registry.bindInstance((Class<Object[]>) array.getClass(), array);

        assert container.getComponent(ArrayDependent.class) != null;
    }

    @Test
    public void dependencyOnContainer() throws Exception {
        registry.bindComponent(ContainerDependent.class, ContainerDependent.class);

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
        final OpenComponentContainer child = container.makeChildContainer();

        final String topString = container.toString();
        final String childString = child.toString();
        assert topString.startsWith("container ") : String.format("Wrong container ID: %s", topString);

        final int prefix = "container ".length();
        final String topId = topString.substring(prefix);
        assert childString.startsWith("container ") : String.format("Wrong container ID: %s", childString);
        final String childSuffix = " > " + topId;
        assert childString.endsWith(childSuffix) : String.format("Wrong container ID: %s", childString);

        final String childId = childString.substring(prefix, childString.length() - childSuffix.length());
        assert !topId.equals(childId) : "Child container has the same ID as its parent";
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
    public void allComponents() throws Exception {
        registry.bindComponent(Service1.class, Service1.class);
        registry.bindComponent(Service2.class, Service2.class);
        registry.bindComponent(Service3.class, Service3.class);
        registry.bindComponent(Service4.class, Service4.class);
        registry.bindComponent(Intermediate.class, Intermediate.class);

        AbstractService.createList.clear();
        AbstractService.callList.clear();

        for (final AbstractService service : container.getAllComponents(AbstractService.class)) {
            service.call();
        }

        assert AbstractService.createList.size() == 4 : AbstractService.createList;
        assert AbstractService.callList.equals(AbstractService.createList) : String.format("%n%s%n%s", AbstractService.createList, AbstractService.callList);
    }

    @Test
    public void checkSerialization() throws Exception {
        SerializableComponent.container = container;

        registry.bindComponent(Key.class, Value.class); // Value depends on DependentKey
        registry.bindComponent(DependentKey.class, DependentValue.class);

        final SerializableComponent component = new SerializableComponent();

        component.verify("original object");

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        new ObjectOutputStream(out).writeObject(component);

        final SerializableComponent clone = (SerializableComponent) new ObjectInputStream(new ByteArrayInputStream(out.toByteArray())).readObject();

        clone.verify("cloned object");
    }

    @Test()
    public void checkLocalClass() throws Exception {
        class Local { }

        registry.bindInstance(BasicResolutionTests.class, this);
        registry.bindComponent(Local.class, Local.class);

        assert container.getComponent(Local.class) != null : "Local class was not instantiated";
    }

    @Test
    public void checkNonStaticInnerClass() throws Exception {
        registry.bindComponent(OuterClass.class, OuterClass.class);
        registry.bindComponent(OuterClass.InnerClass.class, OuterClass.InnerClass.class);

        assert container.getComponent(OuterClass.InnerClass.class) != null : "Non-static inner class was not instantiated";
    }

    @Test(expectedExceptions = ComponentContainer.BindingException.class, expectedExceptionsMessageRegExp = ".*anonymous.*")
    public void checkAnonymousClass() throws Exception {
        registry.bindInstance(BasicResolutionTests.class, this);
        registry.bindComponent(Serializable.class, new Serializable() { }.getClass());

        assert container.getComponent(Serializable.class) == null : "Anonymous inner class should not be instantiated";
    }

    // this is how to inject dependencies into a serializable component
    public static class SerializableComponent implements Serializable {

        /* in real life you'd use new ContainerBoundary() here */
        private static ComponentContainer container;

        @Component(automatic = false)
        @SuppressWarnings("UnusedDeclaration")
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

    private static class ArrayDependent {

        public ArrayDependent(final Object[] array) {
            assert array != null;
        }
    }

    private static abstract class AbstractService {

        public static final List<AbstractService> createList = new ArrayList<AbstractService>();
        public static final List<AbstractService> callList = new ArrayList<AbstractService>();

        protected AbstractService() {
            createList.add(this);
        }

        public void call() {
            callList.add(this);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class Service1 extends AbstractService {

        private Service1(final Service2 dependency2, final Service4 dependency4) {
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class Service2 extends AbstractService {

        private Service2(final Intermediate dependency) {
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class Intermediate {

        private Intermediate(final Service3 dependency) {
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class Service3 extends AbstractService {

        private Service3(final Service4 dependency) {
        }
    }

    private static class Service4 extends AbstractService {}

    public static class OuterClass {

        @Component(automatic = false)
        public class InnerClass {

        }
    }
}

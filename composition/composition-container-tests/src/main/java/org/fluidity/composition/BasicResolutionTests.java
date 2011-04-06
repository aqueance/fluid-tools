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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fluidity.composition.spi.ComponentResolutionObserver;
import org.fluidity.composition.spi.DependencyPath;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
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
            container.makeChildContainer().getRegistry().bindInstance(new FinalizationAware());
            Runtime.getRuntime().gc();
        }

        assert collected : "No component has been garbage collected";
    }

    @Test
    public void singletonComponentRegistration() throws Exception {
        registry.bindComponent(Value.class);            // depends on DependentKey
        registry.bindComponent(DependentValue.class);   // implements DependentKey

        verifyComponent(container);
    }

    @Test
    public void findsFallbackImplementation() throws Exception {
        registry.bindComponent(Value.class);                    // depends on DependentKey
        registry.bindComponent(FallbackDependentValue.class);   // implements DependentKey

        verifyComponent(container);

        assert Value.dependent instanceof FallbackDependentValue;
    }

    @Test
    public void primaryImplementationTakesPrecedenceOverFallback() throws Exception {
        registry.bindComponent(Value.class);

        // this is the fallback
        registry.bindComponent(FallbackDependentValue.class);

        // this is the primary
        registry.bindComponent(DependentValue.class);

        verifyComponent(container);

        // registration by interface took precedence
        assert Value.dependent instanceof DependentValue : Value.dependent;
    }

    @Test
    public void instanceRegistration() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindInstance(new DependentValue());

        verifyComponent(container);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void arrayRegistration() throws Exception {
        registry.bindComponent(ArrayDependent.class);
        registry.bindInstance(new Object[0]);

        assert container.getComponent(ArrayDependent.class) != null;
    }

    @Test
    public void dependencyOnContainer() throws Exception {
        registry.bindComponent(ContainerDependent.class);

        final ContainerDependent component = container.getComponent(ContainerDependent.class);
        assert component != null;

        final ComponentContainer container = component.container();

        // verify that we have the same container in our hands
        assert container.getComponent(Key.class) == null;
        registry.bindComponent(Value.class);
        registry.bindComponent(DependentValue.class);
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
                registry.bindComponent(Value.class);
                registry.bindComponent(DependentValue.class);
            }
        });

        assert value != null;
        assert container.getComponent(Key.class) == null;
        assert container.getComponent(DependentKey.class) == null;
    }

    @Test
    public void multipleInterfaces() throws Exception {
        registry.bindComponent(MultipleInterfaces.class);

        final Interface1 component1 = container.getComponent(Interface1.class);
        final Interface2 component2 = container.getComponent(Interface2.class);
        final Interface3 component3 = container.getComponent(Interface3.class);

        assert component1 == component2;
        assert component2 == component3;
    }

    @Test
    public void serialization() throws Exception {
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

    @Test()
    public void localClass() throws Exception {
        class Local { }

        registry.bindInstance(this);
        registry.bindComponent(Local.class);

        assert container.getComponent(Local.class) != null : "Local class was not instantiated";
    }

    @Test
    public void nonStaticInnerClass() throws Exception {
        registry.bindComponent(OuterClass.class);
        registry.bindComponent(OuterClass.InnerClass.class);

        assert container.getComponent(OuterClass.InnerClass.class) != null : "Non-static inner class was not instantiated";
    }

    @Test
    public void testObservation() throws Exception {
        registry.bindComponent(MultipleInterfaces.class);

        final Map<Class<?>, Class<?>> resolved = new HashMap<Class<?>, Class<?>>();
        final List<Class<?>> instantiated = new ArrayList<Class<?>>();

        final ObservedComponentContainer observed = container.observed(new ComponentResolutionObserver() {
            public void resolved(final DependencyPath path, final Class<?> type) {
                assert path != null;
                resolved.put(path.head(true), type);
            }

            public void instantiated(final DependencyPath path) {
                instantiated.add(path.head(false));
            }
        });

        observed.resolveComponent(Interface1.class);
        observed.resolveComponent(Interface2.class);
        observed.resolveComponent(Interface3.class);

        assert resolved.size() == 3 : resolved;
        assert resolved.get(Interface1.class) == MultipleInterfaces.class;
        assert resolved.get(Interface2.class) == MultipleInterfaces.class;
        assert resolved.get(Interface3.class) == MultipleInterfaces.class;

        assert instantiated.size() == 0 : instantiated;

        observed.getComponent(Interface1.class);
        observed.getComponent(Interface2.class);
        observed.getComponent(Interface3.class);

        assert instantiated.size() == 1 : instantiated;
        assert instantiated.contains(MultipleInterfaces.class);
    }

    @Test(expectedExceptions = ComponentContainer.BindingException.class, expectedExceptionsMessageRegExp = ".*anonymous.*")
    public void anonymousClass() throws Exception {
        registry.bindInstance(this);
        registry.bindComponent(new Serializable() { }.getClass());

        assert container.getComponent(Serializable.class) == null : "Anonymous inner class should not be instantiated";
    }

    @Test(expectedExceptions = ComponentContainer.InstantiationException.class, expectedExceptionsMessageRegExp = ".*DependencyChain1.*DependencyChain2.*DependencyChain3.*")
    public void testInstantiationPath() throws Exception {
        registry.bindComponent(DependencyChain1.class);
        registry.bindComponent(DependencyChain2.class);
        registry.bindComponent(DependencyChain3.class);

        container.getComponent(DependencyChain1.class);
    }

    // this is how to inject dependencies into a Serializable component
    public static class SerializableComponent implements Serializable {

        /* in real life you'd use new ContainerBoundary() here */
        private static ComponentContainer container;

        @Inject
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

    public static class OuterClass {

        @Component(automatic = false)
        public class InnerClass {

        }
    }

    private static interface Interface1 { }

    private static interface Interface2 { }

    private static interface Interface3 { }

    @Component(automatic = false)
    private static class MultipleInterfaces implements Interface1, Interface2, Interface3 {}

    @Component(automatic = false)
    private static class DependencyChain1 {

        @SuppressWarnings("UnusedDeclaration")
        private DependencyChain1(final DependencyChain2 dependency) {
        }
    }

    @Component(automatic = false)
    private static class DependencyChain2 {

        @SuppressWarnings("UnusedDeclaration")
        private DependencyChain2(final DependencyChain3 dependency) {
        }
    }

    @Component(automatic = false)
    private static class DependencyChain3 {

        private DependencyChain3() {
            throw new UnsupportedOperationException();
        }
    }
}

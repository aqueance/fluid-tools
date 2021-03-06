/*
 * Copyright (c) 2006-2018 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.composition.container.tests;

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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.DependencyPath;
import org.fluidity.composition.Inject;
import org.fluidity.composition.ObservedContainer;
import org.fluidity.composition.OpenContainer;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
public final class BasicResolutionTests extends AbstractContainerTests {

    BasicResolutionTests(final ArtifactFactory factory) {
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
            container.makeChildContainer(registry -> registry.bindInstance(new FinalizationAware()));
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
        final KeyCheck check = new KeyCheck();

        assert container.initialize(check).key == null;
        registry.bindComponent(Value.class);
        registry.bindComponent(DependentValue.class);
        assert container.initialize(check).key != null;
    }

    @Test(expectedExceptions = ComponentContainer.ResolutionException.class, expectedExceptionsMessageRegExp = ".*[Dd]ynamic.*Factory.*")
    public void dynamicDependencies() throws Exception {
        registry.bindComponent(ContainerDependent.class);
        registry.bindComponent(DynamicDependent.class);

        try {
            container.getComponent(DynamicDependent.class);
            assert false : "Dynamic dependency should have been prevented";
        } catch (final ComponentContainer.InstantiationException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test
    public void identifiesContainerChain() throws Exception {
        final ComponentContainer child = container.makeChildContainer();

        final String topString = container.toString();
        final String childString = child.toString();
        assert topString.startsWith("container ") : String.format("Wrong container ID: %s", topString);

        final int prefix = "container ".length();
        final String topId = topString.substring(prefix);
        assert childString.startsWith("container ") : String.format("Wrong container ID: %s", childString);

        final String childSuffix = childString.substring(prefix);
        final String childPrefix = topId.concat(" > ");
        assert childSuffix.startsWith(childPrefix) : String.format("Wrong container ID: %s", childString);

        final String childId = childSuffix.substring(childSuffix.length() - childPrefix.length());
        assert !Objects.equals(topId, childId) : "Child container has the same ID as its parent";
    }

    @Test
    public void transientComponentInstantiation() throws Exception {
        assert container.instantiate(Check.class) != null;
    }

    @Test
    public void transientDependentComponentInstantiation() throws Exception {
        final Key value = container.instantiate(Value.class, (ComponentContainer.Bindings) registry -> registry.bindComponent(DependentValue.class));

        assert value != null;
        assert container.getComponent(Key.class) == null;
        assert container.getComponent(Value.class) == null;
        assert container.getComponent(DependentKey.class) == null;
    }

    @Test
    public void transientBindings() throws Exception {
        final OpenContainer child = container.makeChildContainer(registry -> {
            registry.bindComponent(Value.class);
            registry.bindComponent(DependentValue.class);
        });

        final Key value = child.getComponent(Key.class);

        assert value != null;
        assert container.getComponent(Key.class) == null;
        assert container.getComponent(Value.class) == null;
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

        final SerializableComponent component = container.instantiate(SerializableComponent.class);

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

        final Map<Class<?>, Class<?>> resolved = new HashMap<>();
        final List<Class<?>> instantiated = new ArrayList<>();

        final ObservedContainer observed = container.observed(new ComponentContainer.ObserverSupport() {
            public void resolved(final DependencyPath path, final Class<?> type) {
                assert path != null;
                resolved.put(path.tail().api(), type);
            }

            public void instantiated(final DependencyPath path, final AtomicReference<?> ignored) {
                instantiated.add(path.tail().type());
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

    private static class SerializableComponent implements Serializable {

        /* in real life you'd use <code>Containers.global()</code> here */
        private static ComponentContainer container;

        @Inject
        private transient Key dependency;

        public SerializableComponent() {
            assert container != null : "Container not set by the caller";
        }

        private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            assert container != null;
            container.initialize(this);
        }

        void verify(final String context) {
            assert dependency != null: String.format("Dependency not set in %s", context);
        }
    }

    private static class ArrayDependent {

        public ArrayDependent(final Object[] array) {
            assert array != null;
        }
    }

    private static class OuterClass {

        @Component(automatic = false)
        public class InnerClass {

        }
    }

    private interface Interface1 { }

    private interface Interface2 { }

    private interface Interface3 { }

    @Component(automatic = false)
    private static class MultipleInterfaces implements Interface1, Interface2, Interface3 { }

    @Component(automatic = false)
    @SuppressWarnings("UnusedDeclaration")
    private static class DependencyChain1 {

        DependencyChain1(final DependencyChain2 dependency) { }
    }

    @Component(automatic = false)
    @SuppressWarnings("UnusedDeclaration")
    private static class DependencyChain2 {

        DependencyChain2(final DependencyChain3 dependency) { }
    }

    @Component(automatic = false)
    private static class DependencyChain3 {

        DependencyChain3() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Attempts dynamic component resolution. The attempt is expected to fail.
     */
    @Component(automatic = false)
    protected static class DynamicDependent {

        public DynamicDependent(final ComponentContainer container) {
            container.instantiate(ContainerDependent.class);
        }
    }
}

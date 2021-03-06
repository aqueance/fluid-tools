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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.Inject;
import org.fluidity.foundation.Generics;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
public class TypeParameterTests extends AbstractContainerTests {

    TypeParameterTests(final ArtifactFactory artifacts) {
        super(artifacts);
    }

    @Test
    public void testEmbeddedContainer() throws Exception {
        registry.bindComponent(TypedComponent1a.class);
        registry.bindComponent(TypedComponent1b.class);
        registry.bindComponent(TypedComponent2.class);
        registry.bindComponent(TypedComponent3.class);
        registry.bindComponent(TypedComponent4.class);
        registry.bindComponent(RootComponent.class);
        registry.bindComponent(SerializableImpl.class);

        final RootComponent rootComponent = container.getComponent(RootComponent.class);
        assert rootComponent != null;

        assert rootComponent.p3 != null;
        assert rootComponent.p3.p2 != null;

        rootComponent.p3.container.invoke(rootComponent.p3, TypedComponent3.class.getMethod("method", TypedComponent1a.class));
        rootComponent.p4.container.invoke(rootComponent.p4, TypedComponent4.class.getMethod("method", TypedComponent1b.class));
    }

    @Test(expectedExceptions = ComponentContainer.ResolutionException.class)
    public void testRootContainer1a() throws Exception {
        registry.bindComponent(TypedComponent1a.class);
        registry.bindComponent(TypedComponent1b.class);
        registry.bindComponent(TypedComponent2.class);
        registry.bindComponent(TypedComponent3.class);
        registry.bindComponent(TypedComponent4.class);
        registry.bindComponent(RootComponent.class);
        registry.bindComponent(SerializableImpl.class);

        final RootComponent rootComponent = container.getComponent(RootComponent.class);
        assert rootComponent != null;

        assert rootComponent.p3 != null;
        assert rootComponent.p3.p2 != null;

        container.invoke(rootComponent.p3, TypedComponent3.class.getMethod("method", TypedComponent1a.class));
    }

    @Test(expectedExceptions = ComponentContainer.ResolutionException.class, expectedExceptionsMessageRegExp = ".*[Uu]nresolved.*\\[[T\\]].*")
    public void testRootContainer1b() throws Exception {
        registry.bindComponent(TypedComponent1a.class);
        registry.bindComponent(TypedComponent1b.class);
        registry.bindComponent(TypedComponent2.class);
        registry.bindComponent(TypedComponent3.class);
        registry.bindComponent(TypedComponent4.class);
        registry.bindComponent(RootComponent.class);
        registry.bindComponent(SerializableImpl.class);

        final RootComponent rootComponent = container.getComponent(RootComponent.class);
        assert rootComponent != null;

        assert rootComponent.p3 != null;
        assert rootComponent.p3.p2 != null;

        container.invoke(rootComponent.p4, TypedComponent4.class.getMethod("method", TypedComponent1b.class));
    }

    @Test
    public void testTypedContextAware() throws Exception {
        registry.bindComponent(SerializableImpl.class);
        registry.bindComponent(ExternalizableImpl.class);
        registry.bindComponent(TypedContextAware.class);
        registry.bindComponent(TypedRootComponent1.class);
        registry.bindComponent(TypedRootComponent2.class);

        final TypedRootComponent1 root1 = container.getComponent(TypedRootComponent1.class);
        final TypedRootComponent2 root2 = container.getComponent(TypedRootComponent2.class);

        assert root1 != null;
        assert root2 != null;

        final TypedComponent dependency1 = root1.dependency;
        final TypedComponent dependency2 = root2.dependency;
        assert dependency1 != dependency2;
    }

    @Test
    public void testTypedContextUnaware() throws Exception {
        registry.bindComponent(SerializableImpl.class);
        registry.bindComponent(ExternalizableImpl.class);
        registry.bindComponent(TypedContextUnaware.class);
        registry.bindComponent(TypedRootComponent1.class);
        registry.bindComponent(TypedRootComponent2.class);

        final TypedRootComponent1 root1 = container.getComponent(TypedRootComponent1.class);
        final TypedRootComponent2 root2 = container.getComponent(TypedRootComponent2.class);

        assert root1 != null;
        assert root2 != null;

        final TypedComponent dependency1 = root1.dependency;
        final TypedComponent dependency2 = root2.dependency;
        assert dependency1 == dependency2;      // counter-intuitive, but this is the expected behavior
    }

    @Test
    public void testTypeResolved1() throws Exception {
        registry.bindComponent(TypedResolved1.class);

        assert container.getComponent(TypedComponent.class) != null;
    }

    @Test
    public void testTypeResolved2() throws Exception {
        registry.bindComponent(TypedResolved2.class);

        assert container.getComponent(TypedComponent.class) != null;
    }

    @Component(automatic = false)
    @Component.Qualifiers(Component.Reference.class)
    @SuppressWarnings("UnusedDeclaration")
    private static class TypedComponent1a<T> {

        private TypedComponent1a(final ComponentContext context) {
            final Component.Reference reference = context.qualifier(Component.Reference.class, getClass());
            assert reference != null;

            final Type type = reference.type();
            assert type instanceof ParameterizedType : type;
            assert Generics.rawType(type) == getClass() : type;

            final Type parameter = Generics.typeParameter(type, 0);
            assert !(parameter instanceof TypeVariable) : parameter;
        }
    }

    @Component(automatic = false)
    @Component.Qualifiers(Component.Reference.class)
    @SuppressWarnings("UnusedDeclaration")
    private static class TypedComponent1b<T> {

        private TypedComponent1b(final ComponentContext context, final T serializable) {
            final Component.Reference reference = context.qualifier(Component.Reference.class, getClass());
            assert reference != null;

            final Type type = reference.type();
            assert type instanceof ParameterizedType : type;
            assert Generics.rawType(type) == getClass() : type;

            final Type parameter = Generics.typeParameter(type, 0);
            assert !(parameter instanceof TypeVariable) : parameter;
        }
    }

    @Component(automatic = false)
    private static class TypedComponent2<T> {

        @Inject
        public TypedComponent1b<T> component1;
    }

    @Component(automatic = false)
    @SuppressWarnings("UnusedDeclaration")
    private static class TypedComponent3<T> {

        public final ComponentContainer container;
        private final TypedComponent2<T> p2;

        private TypedComponent3(final ComponentContainer container, final TypedComponent2<T> p1) {
            this.container = container;
            this.p2 = p1;
        }

        public void method(final TypedComponent1a<T> p1) {
            assert p1 != null;
        }
    }

    @Component(automatic = false)
    private static class TypedComponent4<T> {

        @Inject
        public ComponentContainer container;

        @Inject
        private TypedComponent2<T> p2;

        public void method(final TypedComponent1b<T> p1) {
            assert p1 != null;
        }
    }

    @Component(automatic = false)
    private static class RootComponent {

        final TypedComponent3<Serializable> p3;
        final TypedComponent4<Serializable> p4;

        @SuppressWarnings("UnusedDeclaration")
        private RootComponent(final TypedComponent3<Serializable> p1, final TypedComponent4<Serializable> p2) {
            this.p3 = p1;
            this.p4 = p2;
        }
    }

    @Component
    private static class SerializableImpl implements Serializable { }

    @Component
    private static class ExternalizableImpl implements Externalizable {

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException { }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException { }
    }

    @SuppressWarnings("UnusedDeclaration")
    interface TypedComponent<T> { }

    @Component
    @Component.Qualifiers(Component.Reference.class)
    private static class TypedContextAware<T> implements TypedComponent<T> {

        public TypedContextAware(final T dependency) {
            assert dependency != null;
        }
    }

    @Component
    private static class TypedContextUnaware<T> implements TypedComponent<T> {

        final T dependency;

        public TypedContextUnaware(final T dependency) {
            assert dependency != null;
            this.dependency = dependency;
        }
    }

    @Component
    @Component.Qualifiers(Component.Reference.class)
    private static class TypedResolved1 implements TypedComponent<Serializable> { }

    @Component
    private static class TypedResolved2 implements TypedComponent<Serializable> { }

    @Component
    private static class TypedRootComponent1 {

        final TypedComponent<Serializable> dependency;

        @SuppressWarnings("UnusedDeclaration")
        private TypedRootComponent1(final TypedComponent<Serializable> dependency) {
            assert dependency != null;
            this.dependency = dependency;
        }
    }

    @Component
    private static class TypedRootComponent2 {

        final TypedComponent<Externalizable> dependency;

        @SuppressWarnings("UnusedDeclaration")
        private TypedRootComponent2(final TypedComponent<Externalizable> dependency) {
            assert dependency != null;
            this.dependency = dependency;
        }
    }
}

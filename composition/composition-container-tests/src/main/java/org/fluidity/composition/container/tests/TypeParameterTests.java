/*
 * Copyright (c) 2006-2012 Tibor Adam Varga (tibor.adam.varga on gmail)
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
public class TypeParameterTests extends AbstractContainerTests {

    public TypeParameterTests(final ArtifactFactory artifacts) {
        super(artifacts);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testTypes() throws Exception {
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

    @Test
    @SuppressWarnings("unchecked")
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
    @SuppressWarnings("unchecked")
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
    @SuppressWarnings("unchecked")
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
    @SuppressWarnings("unchecked")
    public void testTypedContextAware() throws Exception {
        registry.bindComponent(SerializableImpl.class);
        registry.bindComponent(TypedContextAware.class);
        registry.bindComponent(TypedRootComponent.class);

        assert container.getComponent(TypedRootComponent.class) != null;
    }

    @Test(expectedExceptions = ComponentContainer.ResolutionException.class)
    @SuppressWarnings("unchecked")
    public void testTypedContextUnaware() throws Exception {
        registry.bindComponent(SerializableImpl.class);
        registry.bindComponent(TypedContextUnaware.class);
        registry.bindComponent(TypedRootComponent.class);

        assert container.getComponent(TypedRootComponent.class) != null;
    }

    @Test(expectedExceptions = ComponentContainer.BindingException.class)
    @SuppressWarnings("unchecked")
    public void testTypeResolved1() throws Exception {
        registry.bindComponent(TypedResolved1.class);
    }

    @Test
    @SuppressWarnings("unchecked")
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

        public final TypedComponent3<Serializable> p3;
        public final TypedComponent4<Serializable> p4;

        @SuppressWarnings("UnusedDeclaration")
        private RootComponent(final TypedComponent3<Serializable> p1, final TypedComponent4<Serializable> p2) {
            this.p3 = p1;
            this.p4 = p2;
        }
    }

    @Component
    private static class SerializableImpl implements Serializable { }

    @SuppressWarnings("UnusedDeclaration")
    public interface TypedComponent<T> { }

    @Component
    @Component.Qualifiers(Component.Reference.class)
    private static class TypedContextAware<T> implements TypedComponent<T> {

        public TypedContextAware(final T dependency) {
            assert dependency != null;
        }
    }

    @Component
    private static class TypedContextUnaware<T> implements TypedComponent<T> {

        public TypedContextUnaware(final T dependency) {
            assert dependency != null;
        }
    }

    @Component
    @Component.Qualifiers(Component.Reference.class)
    private static class TypedResolved1 implements TypedComponent<Serializable> { }

    @Component
    private static class TypedResolved2 implements TypedComponent<Serializable> { }

    @Component
    private static class TypedRootComponent {

        @SuppressWarnings("UnusedDeclaration")
        private TypedRootComponent(final TypedComponent<Serializable> dependency) {
            assert dependency != null;
        }
    }
}

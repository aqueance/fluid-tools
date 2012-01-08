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

package org.fluidity.composition.tests;

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
        registry.bindComponent(TypedComponent1.class);
        registry.bindComponent(TypedComponent2.class);
        registry.bindComponent(TypedComponent3.class);
        registry.bindComponent(TypedComponent4.class);
        registry.bindComponent(RootComponent.class);

        final RootComponent rootComponent = container.getComponent(RootComponent.class);
        assert rootComponent != null;

        assert rootComponent.p3 != null;
        assert rootComponent.p3.p2 != null;

        rootComponent.p3.container.invoke(rootComponent.p3, true, TypedComponent3.class.getMethod("method", TypedComponent1.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEmbeddedContainer() throws Exception {
        registry.bindComponent(TypedComponent1.class);
        registry.bindComponent(TypedComponent2.class);
        registry.bindComponent(TypedComponent3.class);
        registry.bindComponent(TypedComponent4.class);
        registry.bindComponent(RootComponent.class);

        final RootComponent rootComponent = container.getComponent(RootComponent.class);
        assert rootComponent != null;

        assert rootComponent.p3 != null;
        assert rootComponent.p3.p2 != null;

        rootComponent.p3.container.invoke(rootComponent.p3, true, TypedComponent3.class.getMethod("method", TypedComponent1.class));
    }

    @Test(expectedExceptions = ComponentContainer.ResolutionException.class)
    @SuppressWarnings("unchecked")
    public void testRootContainer() throws Exception {
        registry.bindComponent(TypedComponent1.class);
        registry.bindComponent(TypedComponent2.class);
        registry.bindComponent(TypedComponent3.class);
        registry.bindComponent(TypedComponent4.class);
        registry.bindComponent(RootComponent.class);

        final RootComponent rootComponent = container.getComponent(RootComponent.class);
        assert rootComponent != null;

        assert rootComponent.p3 != null;
        assert rootComponent.p3.p2 != null;

        container.invoke(rootComponent.p3, true, TypedComponent3.class.getMethod("method", TypedComponent1.class));
    }

    @Component(automatic = false)
    @Component.Context(typed = true)
    @SuppressWarnings("UnusedDeclaration")
    private static class TypedComponent1<T> {

        private TypedComponent1(final ComponentContext context) {
            final Component.Reference reference = context.annotation(Component.Reference.class, getClass());
            assert reference != null;

            final Type type = reference.type();
            assert type instanceof ParameterizedType : type;
            assert Generics.rawType(type) == getClass() : type;

            final Type parameter = Generics.typeParameter(type, 0);
            assert !(parameter instanceof TypeVariable) : parameter;
        }
    }

    @Component(automatic = false)
    @Component.Context(typed = true)
    private static class TypedComponent2<T> {

        @Inject
        @SuppressWarnings("UnusedDeclaration")
        public TypedComponent1<T> component1;
    }

    @Component(automatic = false)
    @Component.Context(typed = true)
    @SuppressWarnings("UnusedDeclaration")
    private static class TypedComponent3<T> {

        public final ComponentContainer container;
        private final TypedComponent2<T> p2;

        private TypedComponent3(final ComponentContainer container, final TypedComponent2<T> p1) {
            this.container = container;
            this.p2 = p1;
        }

        public void method(final TypedComponent1<T> p1) {
            assert p1 != null;
        }
    }

    @Component(automatic = false)
    @Component.Context(typed = true)
    private static class TypedComponent4<T> {

        @Inject
        @SuppressWarnings("UnusedDeclaration")
        private TypedComponent2<T> component2;
    }

    @Component(automatic = false)
    private static class RootComponent {

        public final TypedComponent3<Serializable> p3;

        @SuppressWarnings("UnusedDeclaration")
        private RootComponent(final TypedComponent3<Serializable> p1, final TypedComponent4<Serializable> p2) {
            this.p3 = p1;
        }
    }
}

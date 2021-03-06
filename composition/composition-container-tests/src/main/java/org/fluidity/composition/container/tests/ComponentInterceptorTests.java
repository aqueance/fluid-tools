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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.Inject;
import org.fluidity.composition.OpenContainer;
import org.fluidity.composition.Optional;
import org.fluidity.composition.Qualifier;
import org.fluidity.composition.spi.ComponentInterceptor;
import org.fluidity.composition.spi.Dependency;
import org.fluidity.foundation.Generics;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings({ "unchecked", "WeakerAccess", "Duplicates" })
public final class ComponentInterceptorTests extends AbstractContainerTests {

    @BeforeMethod
    public void setUp() throws Exception {
        Interceptor.list.clear();
        Interceptor2.instances.set(0);
        Interceptor6.instances.set(0);
    }

    private OpenContainer child(final ComponentContainer container, final ComponentContainer.Bindings... bindings) {
        return (container == null ? this.container : container).makeDomainContainer(bindings);
    }

    ComponentInterceptorTests(final ArtifactFactory artifacts) {
        super(artifacts);
    }

    private void check(final Class<?> type, final Class<? extends ComponentInterceptor>... interceptors) {
        final Collection<Class<?>> list = Interceptor.list.get(type);

        if (interceptors.length == 0) {
            assert list == null : String.format("Interceptors applied to %s", type);
        } else {
            assert list != null : String.format("No interceptors applied to %s", type);

            final List<Class<? extends ComponentInterceptor>> expected = Arrays.asList(interceptors);
            assert new ArrayList<>(list).equals(expected) : String.format("For %s%nexpected %s%n     got %s", type, expected, list);
        }
    }

    @Test
    public void testSingleContainer() throws Exception {
        final ComponentContainer.Bindings interceptors = registry -> {
            registry.bindInstance(ComponentInterceptorTests.this);
            registry.bindComponent(InterceptorDependency.class);
            registry.bindComponent(Interceptor0.class);
            registry.bindComponent(Interceptor1.class);
            registry.bindComponent(Interceptor2.class);
            registry.bindComponent(Interceptor3.class);
            registry.bindInstance(new Interceptor4());
            registry.bindComponent(Interceptor5.class);
            registry.bindComponent(Interceptor6.class);
        };

        final OpenContainer container = child(null, interceptors, registry -> {
            registry.bindComponent(RootComponent.class);
            registry.bindComponent(Dependency1.class);
            registry.bindComponent(Dependency2.class);
            registry.bindComponent(Dependency11.class);
            registry.bindComponent(Dependency12.class);
            registry.bindComponent(Dependency13.class);
            registry.bindComponent(Dependency21.class);
        });

        Interceptor.list.clear();

        verify((Task) () -> container.getComponent(RootComponent.class));

        check(InterceptorDependency.class);
        check(RootComponent.class);
        check(Dependency1.class, Interceptor2.class, Interceptor1.class, Interceptor6.class);
        check(Dependency11.class, Interceptor4.class, Interceptor3.class, Interceptor6.class);
        check(Dependency12.class, Interceptor5.class, Interceptor4.class, Interceptor6.class);
        check(Dependency13.class, Interceptor3.class, Interceptor4.class, Interceptor6.class);
        check(Dependency2.class, Interceptor2.class, Interceptor6.class);
        check(Dependency21.class, Interceptor3.class, Interceptor4.class, Interceptor5.class, Interceptor6.class);

        assert Interceptor2.instances.get() == 1;
        assert Interceptor6.instances.get() > 1;
    }

    @Test
    public void testContainerHierarchy() throws Exception {
        final ComponentContainer.Bindings rootInterceptors = registry -> {
            registry.bindInstance(ComponentInterceptorTests.this);
            registry.bindComponent(InterceptorDependency.class);
            registry.bindComponent(Interceptor0.class);
            registry.bindComponent(Interceptor1.class);
            registry.bindComponent(Interceptor2.class);
        };

        final ComponentContainer.Bindings parentInterceptors = registry -> {
            registry.bindComponent(Interceptor5.class);
            registry.bindComponent(Interceptor6.class);
        };

        final ComponentContainer.Bindings childInterceptors = registry -> {
            registry.bindComponent(Interceptor3.class);
            registry.bindInstance(new Interceptor4());
        };

        final OpenContainer container = child(child(child(null, rootInterceptors), parentInterceptors), childInterceptors, registry -> {
            registry.bindComponent(RootComponent.class);
            registry.bindComponent(Dependency1.class);
            registry.bindComponent(Dependency2.class);
            registry.bindComponent(Dependency11.class);
            registry.bindComponent(Dependency12.class);
            registry.bindComponent(Dependency13.class);
            registry.bindComponent(Dependency21.class);
        });

        Interceptor.list.clear();

        verify((Task) () -> container.getComponent(RootComponent.class));

        check(InterceptorDependency.class);
        check(RootComponent.class);
        check(Dependency1.class, Interceptor2.class, Interceptor1.class, Interceptor6.class);
        check(Dependency11.class, Interceptor4.class, Interceptor3.class, Interceptor6.class);
        check(Dependency12.class, Interceptor5.class, Interceptor4.class, Interceptor6.class);
        check(Dependency13.class, Interceptor3.class, Interceptor4.class, Interceptor6.class);
        check(Dependency2.class, Interceptor2.class, Interceptor6.class);
        check(Dependency21.class, Interceptor3.class, Interceptor4.class, Interceptor5.class, Interceptor6.class);

        assert Interceptor2.instances.get() == 1;
        assert Interceptor6.instances.get() > 1;
    }

    @Test
    public void testConvenience() throws Exception {
        final ComponentContainer.Bindings bindings = registry -> {
            registry.bindInstance(ComponentInterceptorTests.this);
            registry.bindComponent(RootComponent.class);
            registry.bindComponent(Dependency1.class);
            registry.bindComponent(Dependency2.class);
            registry.bindComponent(Dependency11.class);
            registry.bindComponent(Dependency12.class);
            registry.bindComponent(Dependency13.class);
            registry.bindComponent(Dependency21.class);
        };

        final OpenContainer container = child(this.container
                                                  .intercepting(new Interceptor0(), new Interceptor1(), new Interceptor2())
                                                  .intercepting(new Interceptor5(new InterceptorDependency()), new Interceptor6())
                                                  .intercepting(new Interceptor3(), new Interceptor4()),
                                              bindings);

        Interceptor.list.clear();

        verify((Task) () -> container.getComponent(RootComponent.class));

        check(InterceptorDependency.class);
        check(RootComponent.class);
        check(Dependency1.class, Interceptor2.class, Interceptor1.class, Interceptor6.class);
        check(Dependency11.class, Interceptor4.class, Interceptor3.class, Interceptor6.class);
        check(Dependency12.class, Interceptor5.class, Interceptor4.class, Interceptor6.class);
        check(Dependency13.class, Interceptor3.class, Interceptor4.class, Interceptor6.class);
        check(Dependency2.class, Interceptor2.class, Interceptor6.class);
        check(Dependency21.class, Interceptor3.class, Interceptor4.class, Interceptor5.class, Interceptor6.class);

        assert Interceptor2.instances.get() == 1;
        assert Interceptor6.instances.get() == 1;
    }

    @Test
    public void testMissingDependency() throws Exception {
        final ComponentContainer.Bindings interceptors = registry -> {
            registry.bindInstance(ComponentInterceptorTests.this);
            registry.bindComponent(InterceptorDependency.class);
            registry.bindComponent(Interceptor0.class);
            registry.bindComponent(Interceptor1.class);
            registry.bindComponent(Interceptor2.class);
            registry.bindComponent(Interceptor3.class);
            registry.bindInstance(new Interceptor4());
            registry.bindComponent(Interceptor5.class);
            registry.bindComponent(Interceptor6.class);
            registry.bindComponent(RemovingInterceptor.class);
        };

        final OpenContainer container = child(null, interceptors, registry -> {
            registry.bindComponent(RootComponent.class);
            registry.bindComponent(Dependency1.class);
            registry.bindComponent(Dependency2.class);
            registry.bindComponent(Dependency21.class);
        });

        Interceptor.list.clear();

        verify((Task) () -> container.getComponent(RootComponent.class));

        check(InterceptorDependency.class);
        check(RootComponent.class);
        check(Dependency1.class, Interceptor2.class, Interceptor1.class, Interceptor6.class);
        check(Dependency2.class, Interceptor2.class, Interceptor6.class);
        check(Dependency11.class);
        check(Dependency12.class);
        check(Dependency13.class);
        check(Dependency21.class, Interceptor3.class, Interceptor4.class, RemovingInterceptor.class);

        assert Interceptor2.instances.get() == 1;
        assert Interceptor6.instances.get() > 1;
    }

    @Annotation01
    @Component(automatic = false)
    @SuppressWarnings({ "UnusedParameters", "UnusedDeclaration" })
    private static class RootComponent {

        @Inject @Annotation2
        private Dependency2 field;

        private RootComponent(final @Annotation1 @Annotation2 Dependency1 dependency) { }
    }

    // non-static for testing purposes
    @Annotation02
    @SuppressWarnings("UnusedDeclaration")
    private class Dependency1 {

        Dependency1(final @Optional @Annotation3 @Annotation4 Dependency11 dependency1,
                    final @Optional @Annotation4 @Annotation5 Dependency12 dependency2,
                    final @Optional @Annotation4 @Annotation3 Dependency13 dependency3) { }
    }

    // non-static for testing purposes
    @Annotation02
    @SuppressWarnings("UnusedDeclaration")
    private class Dependency2 {

        Dependency2(final @Optional @Annotation5 @Remove @Annotation4 @Annotation3 Dependency21 dependency) { }
    }

    @Component(automatic = false)
    private static class Dependency11 { }

    @Component(automatic = false)
    private static class Dependency12 { }

    @Component(automatic = false)
    private static class Dependency13 { }

    @Component(automatic = false)
    private static class Dependency21 { }

    private static abstract class Interceptor implements ComponentInterceptor {

        public static Map<Class<?>, Collection<Class<?>>> list = new HashMap<>();

        public Dependency intercept(final Type reference, final ComponentContext context, final Dependency dependency) {
            Interceptor.list.computeIfAbsent(Generics.rawType(reference), _key -> new ArrayList<>())
                            .add(getClass());

            return dependency;
        }
    }

    @Component.Qualifiers({ Annotation01.class, Annotation02.class, Annotation1.class })   // will never match in this test
    private static class Interceptor0 extends Interceptor { }

    @Component.Qualifiers({ Annotation01.class, Annotation1.class })
    private static class Interceptor1 extends Interceptor { }

    @Component.Qualifiers({ Annotation01.class, Annotation2.class })
    private static class Interceptor2 extends Interceptor {

        public static final AtomicInteger instances = new AtomicInteger(0);

        private Interceptor2() {
            Interceptor2.instances.incrementAndGet();
        }
    }

    @Component.Qualifiers({ Annotation02.class, Annotation3.class })
    private static class Interceptor3 extends Interceptor { }

    // non-static for testing purposes
    @Component.Qualifiers(Annotation4.class)
    private class Interceptor4 extends Interceptor { }

    @Component.Qualifiers(Annotation5.class)
    @SuppressWarnings("UnusedParameters")
    private static class Interceptor5 extends Interceptor {

        private Interceptor5(final @Annotation5 InterceptorDependency dependency) { }
    }

    @Component(stateful = true)
    private static class Interceptor6 extends Interceptor {

        public static final AtomicInteger instances = new AtomicInteger(0);

        private Interceptor6() {
            Interceptor6.instances.incrementAndGet();
        }
    }

    @Component.Qualifiers(Remove.class)
    private static class RemovingInterceptor extends Interceptor {

        @Override
        public Dependency intercept(final Type reference, final ComponentContext context, final Dependency dependency) {
            super.intercept(reference, context, dependency);
            return null;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.PARAMETER, ElementType.FIELD, ElementType.TYPE })
    @Qualifier(Qualifier.Composition.IMMEDIATE)
    @interface Annotation01 { }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.PARAMETER, ElementType.FIELD, ElementType.TYPE })
    @Qualifier(Qualifier.Composition.IMMEDIATE)
    @interface Annotation02 { }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER, ElementType.FIELD})
    @Qualifier(Qualifier.Composition.IMMEDIATE)
    @interface Annotation1 { }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER, ElementType.FIELD})
    @Qualifier(Qualifier.Composition.IMMEDIATE)
    @interface Annotation2 { }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER, ElementType.FIELD})
    @Qualifier(Qualifier.Composition.IMMEDIATE)
    @interface Annotation3 { }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER, ElementType.FIELD})
    @Qualifier(Qualifier.Composition.IMMEDIATE)
    @interface Annotation4 { }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER, ElementType.FIELD})
    @Qualifier(Qualifier.Composition.IMMEDIATE)
    @interface Annotation5 { }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER, ElementType.FIELD})
    @Qualifier(Qualifier.Composition.IMMEDIATE)
    @interface Remove { }

    private static class InterceptorDependency {}
}

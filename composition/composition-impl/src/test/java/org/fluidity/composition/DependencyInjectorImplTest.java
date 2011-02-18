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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.fluidity.composition.network.ContextDefinition;
import org.fluidity.composition.network.Graph;
import org.fluidity.composition.spi.ComponentMapping;
import org.fluidity.composition.spi.DependencyResolver;
import org.fluidity.tests.MockGroupAbstractTest;

import org.easymock.EasyMock;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class DependencyInjectorImplTest extends MockGroupAbstractTest {

    private final Graph.Traversal traversal = addControl(Graph.Traversal.class);
    private final DependencyResolver resolver = addControl(DependencyResolver.class);
    private final ComponentMapping mapping = addControl(ComponentMapping.class);

    private final ContextDefinition context = addControl(ContextDefinition.class);
    private final ComponentContainer container = addControl(ComponentContainer.class);

    private final DependencyInjector injector = new DependencyInjectorImpl();

    private final ComponentMapping dummyMapping = new ComponentMapping() {

        public <T extends Annotation> T contextSpecification(Class<T> type) {
            return null;
        }

        public Annotation[] providedContext() {
            return null;
        }
    };

    private static Annotation[] neverNull(final Annotation[] array) {
        return array == null ? new Annotation[0] : array;
    }

    private ContextDefinition setupFieldResolution(final Class<?> componentType,
                                                   final String fieldName,
                                                   final Object component,
                                                   final Context contextAnnotations,
                                                   final ComponentContext createdContext,
                                                   final Annotation[] containerAnnotations) throws Exception {
        assert fieldName != null;
        final Field field = componentType.getDeclaredField(fieldName);
        assert field != null : String.format("%s.%s", componentType.getClass(), fieldName);

        return setupDependencyResolution(componentType,
                                         field.getType(),
                                         field.getAnnotations(),
                                         contextAnnotations,
                                         createdContext,
                                         containerAnnotations,
                                         component);
    }

    private ContextDefinition[] setupConstructorResolution(final Class<?> componentType,
                                                           final Constructor<?> constructor,
                                                           final Context contextAnnotations,
                                                           final ComponentContext createdContext,
                                                           final Annotation[] containerAnnotations,
                                                           final Object... components) throws Exception {
        assert constructor != null : String.format("%s(?)", componentType.getClass());

        final List<ContextDefinition> copies = new ArrayList<ContextDefinition>();

        int i = 0;
        for (final Class<?> dependencyType : constructor.getParameterTypes()) {
            copies.add(setupDependencyResolution(componentType,
                                                 dependencyType,
                                                 dependencyType.getAnnotations(),
                                                 contextAnnotations,
                                                 createdContext,
                                                 containerAnnotations,
                                                 components[i++]));
        }

        return copies.toArray(new ContextDefinition[copies.size()]);
    }

    private ContextDefinition setupDependencyResolution(final Class<?> componentType,
                                                        final Class<?> dependencyType,
                                                        final Annotation[] dependencyAnnotations,
                                                        final Context contextAnnotations,
                                                        final ComponentContext createdContext,
                                                        final Annotation[] containerAnnotations,
                                                        final Object component) {
        final ContextDefinition copy = addLocalControl(ContextDefinition.class);

        EasyMock.expect(context.copy()).andReturn(copy);

        if (dependencyType.isArray()) {
            assert component == null || component.getClass().isArray() : component.getClass();
            final Object[] services = (Object[]) component;

            EasyMock.expect(resolver.resolveComponent(dependencyType.getComponentType(), copy, traversal)).andReturn(new Graph.Node.Constant(services));
        } else if (dependencyType == ComponentContext.class) {
            EasyMock.expect(mapping.contextSpecification(Context.class)).andReturn(contextAnnotations);
            EasyMock.expect(copy.reduce(contextAnnotations)).andReturn(copy);

            EasyMock.expect(copy.create()).andReturn(createdContext);
        } else {
            final Annotation[] componentContext = neverNull(componentType.getAnnotations());
            final Annotation[] dependencyContext = neverNull(dependencyAnnotations);

            final Context acceptedContext = dependencyType.getAnnotation(Context.class);

            final Annotation[] definitions = new Annotation[componentContext.length + dependencyContext.length];
            System.arraycopy(componentContext, 0, definitions, 0, componentContext.length);
            System.arraycopy(dependencyContext, 0, definitions, componentContext.length, dependencyContext.length);

            EasyMock.expect(copy.expand(EasyMock.aryEq(definitions))).andReturn(copy);

            if (dependencyType == ComponentContainer.class) {
                EasyMock.expect(mapping.providedContext()).andReturn(containerAnnotations).anyTimes();
                EasyMock.expect(resolver.container(copy)).andReturn(container);
            } else {
                final ComponentMapping mapping = addLocalControl(ComponentMapping.class);

                EasyMock.expect(resolver.mapping(dependencyType)).andReturn(mapping);
                EasyMock.expect(mapping.contextSpecification(Context.class)).andReturn(acceptedContext);
                EasyMock.expect(copy.reduce(acceptedContext)).andReturn(copy);
                EasyMock.expect(resolver.resolveComponent(dependencyType, copy, traversal)).andReturn(new Graph.Node.Constant(component));
            }
        }
        return copy;
    }

    private void setupCollection(final ContextDefinition context, final ContextDefinition... copies) {
        EasyMock.expect(context.collect(Arrays.asList(copies))).andReturn(context);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void injectsFields() throws Exception {
        final FieldInjected component = new FieldInjected();

        final Dependency dependency = new DependencyImpl();
        final ContextDefinition copy1 = setupFieldResolution(FieldInjected.class, "dependency", dependency, null, null, null);

        final ContextDefinition copy2 = addLocalControl(ContextDefinition.class);
        EasyMock.expect(context.copy()).andReturn(copy2);

        final ServiceImpl1 service1 = new ServiceImpl1();
        final ServiceImpl2 service2 = new ServiceImpl2();

        EasyMock.expect(resolver.resolveComponent(Service.class, copy2, traversal)).andReturn(new Graph.Node.Constant(new Service[] { service1, service2 }));

        setupCollection(context, copy1, copy2);

        replay();
        assert component == injector.fields(traversal, resolver, dummyMapping, context, component);
        verify();

        assert component.dependency == dependency : component.dependency;

        assert component.services != null;
        assert component.services.length == 2;
        assert component.services[0] == service1;
        assert component.services[1] == service2;
    }

    @Test(expectedExceptions = ComponentContainer.ResolutionException.class, expectedExceptionsMessageRegExp = ".*Dependency.*")
    public void complainsAboutMissingFields() throws Exception {
        final FieldInjected component = new FieldInjected();

        setupFieldResolution(FieldInjected.class, "dependency", null, null, null, null);

        replay();
        assert component == injector.fields(traversal, resolver, dummyMapping, context, component);
        verify();
    }

    @Test
    public void injectsNullForOptionalFields() throws Exception {
        final OptionalFieldInjected component = new OptionalFieldInjected();

        setupCollection(context, setupFieldResolution(OptionalFieldInjected.class, "dependency", null, null, null, null));

        replay();
        assert component == injector.fields(traversal, resolver, dummyMapping, context, component);
        verify();

        assert component.dependency == null : component.dependency;
    }

    @Test
    @SuppressWarnings( { "unchecked" })
    public void injectsConstructor() throws Exception {
        final Dependency dependency = new DependencyImpl();
        final ServiceImpl1 service1 = new ServiceImpl1();
        final ServiceImpl2 service2 = new ServiceImpl2();

        final Service[] services = { service1, service2 };

        final Constructor<ConstructorInjected> constructor = ConstructorInjected.class.getDeclaredConstructor(Dependency.class, Service[].class);
        setupCollection(context, setupConstructorResolution(ConstructorInjected.class, constructor, null, null, null, dependency, services));
        setupCollection(context);   // contexts for an empty field list

        ConstructorInjected.expectedGroupSize = services.length;

        replay();
        assert injector.constructor(traversal, resolver, dummyMapping, context, constructor) != null;
        verify();
    }

    @Test
    public void handlesSpecialDependencies() throws Exception {
        final SpecialDependent component = new SpecialDependent();

        final ComponentContext created = addLocalControl(ComponentContext.class);

        setupCollection(context,
                        setupFieldResolution(component.getClass(), "container", container, null, null, null),
                        setupFieldResolution(component.getClass(), "context", container, null, created, null));

        replay();
        assert component == injector.fields(traversal, resolver, mapping, context, component);
        verify();

        assert component.container == container : component.container;
        assert component.context == created : component.context;
    }

    @Test
    public void neverInjectsNullForGroup() throws Exception {
        final Constructor<MissingGroupConsumer> constructor = MissingGroupConsumer.class.getDeclaredConstructor(MissingService[].class);
        setupCollection(context, setupConstructorResolution(MissingGroupConsumer.class, constructor, null, null, null, (Object) null));
        setupCollection(context);   // contexts for an empty field list

        replay();
        assert injector.constructor(traversal, resolver, dummyMapping, context, constructor) != null;
        verify();
    }

    private final class FieldInjected {

        @Component
        public Dependency dependency;

        @ComponentGroup
        public Service[] services;
    }

    private final class OptionalFieldInjected {

        @Optional
        @Component
        public Dependency dependency;
    }

    private static final class ConstructorInjected {

        public static int expectedGroupSize;

        @SuppressWarnings("UnusedParameters")
        public ConstructorInjected(final Dependency dependency, final @ComponentGroup Service[] services) {
            assert dependency != null;

            assert services != null;
            assert services.length == expectedGroupSize;

            for (final Service service : services) {
                assert service != null;
            }
        }
    }

    private static final class MissingGroupConsumer {

        @SuppressWarnings("UnusedParameters")
        public MissingGroupConsumer(final @ComponentGroup MissingService[] services) {
            assert services != null;
        }
    }

    public static interface Dependency {

    }

    public static class DependencyImpl implements Dependency {

    }

    @ComponentGroup
    public static interface Service {

    }

    public static class ServiceImpl1 implements Service {

    }

    public static class ServiceImpl2 implements Service {

    }

    public static class SpecialDependent {

        @Component
        public ComponentContainer container;

        @Component
        public ComponentContext context;
    }

    @ComponentGroup
    public static interface MissingService {

    }
}

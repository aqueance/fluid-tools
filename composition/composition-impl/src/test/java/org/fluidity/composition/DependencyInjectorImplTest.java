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

import org.fluidity.composition.spi.ComponentMapping;
import org.fluidity.composition.spi.DependencyResolver;
import org.fluidity.tests.MockGroupAbstractTest;

import org.easymock.EasyMock;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class DependencyInjectorImplTest extends MockGroupAbstractTest {

    private final DependencyResolver resolver = addControl(DependencyResolver.class);
    private final ClassDiscovery discovery = addControl(ClassDiscovery.class);
    private final ComponentMapping mapping = addControl(ComponentMapping.class);

    private final ContextDefinition context = addControl(ContextDefinition.class);
    private final ComponentContainer container = addControl(ComponentContainer.class);

    private final DependencyInjector injector = new DependencyInjectorImpl(discovery);

    private final ComponentMapping dummyMapping = new ComponentMapping() {
        public boolean isFactoryMapping() {
            return false;
        }

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
                                                           final int index,
                                                           final Context contextAnnotations,
                                                           final ComponentContext createdContext,
                                                           final Annotation[] containerAnnotations,
                                                           final Object... components) throws Exception {
        final Constructor constructor = componentType.getDeclaredConstructors()[index];
        assert constructor != null : String.format("%s.%d", componentType.getClass(), index);

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
            assert component.getClass().isArray() : component.getClass();
            final Object[] services = (Object[]) component;

            for (final Object service : services) {
                EasyMock.expect(resolver.resolve(service.getClass(), copy)).andReturn(service);
            }
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
                EasyMock.expect(resolver.resolve(dependencyType, copy)).andReturn(component);
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
        EasyMock.expect(discovery.findComponentClasses(EasyMock.same(Service.class), EasyMock.<ClassLoader>notNull(), EasyMock.eq(false)))
                .andReturn(new Class[] { ServiceImpl1.class, ServiceImpl2.class });

        final ServiceImpl1 service1 = new ServiceImpl1();
        final ServiceImpl2 service2 = new ServiceImpl2();

        EasyMock.expect(resolver.resolve(ServiceImpl1.class, copy2)).andReturn(service1);
        EasyMock.expect(resolver.resolve(ServiceImpl2.class, copy2)).andReturn(null);
        EasyMock.expect(resolver.create(ServiceImpl2.class, copy2)).andReturn(service2);

        setupCollection(context, copy1, copy2);

        replay();
        assert component == injector.injectFields(resolver, dummyMapping, context, component);
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
        assert component == injector.injectFields(resolver, dummyMapping, context, component);
        verify();
    }

    @Test
    public void injectsNullForOptionalFields() throws Exception {
        final OptionalFieldInjected component = new OptionalFieldInjected();

        setupCollection(context, setupFieldResolution(OptionalFieldInjected.class, "dependency", null, null, null, null));

        replay();
        assert component == injector.injectFields(resolver, dummyMapping, context, component);
        verify();

        assert component.dependency == null : component.dependency;
    }

    @Test
    @SuppressWarnings( { "unchecked" })
    public void injectsConstructor() throws Exception {
        final FieldInjected component = new FieldInjected();

        final Dependency dependency = new DependencyImpl();
        final ServiceImpl1 service1 = new ServiceImpl1();
        final ServiceImpl2 service2 = new ServiceImpl2();

        EasyMock.expect(discovery.findComponentClasses(EasyMock.same(Service.class), EasyMock.<ClassLoader>notNull(), EasyMock.eq(false)))
                .andReturn(new Class[] { ServiceImpl1.class, ServiceImpl2.class });

        setupCollection(context, setupConstructorResolution(ConstructorInjected.class, 0, null, null, null, dependency, new Object[] { service1, service2 }));

        replay();
        Object[] arguments = injector.injectConstructor(resolver,
                                                        dummyMapping,
                                                        context,
                                                        ConstructorInjected.class.getDeclaredConstructor(Dependency.class, Object[].class));
        verify();

        assert arguments[0] == dependency : component.dependency;

        assert arguments[1] != null;
        assert ((Object[]) arguments[1]).length == 2;
        assert ((Object[]) arguments[1])[0] == service1;
        assert ((Object[]) arguments[1])[1] == service2;
    }

    @Test
    public void handlesSpecialDependencies() throws Exception {
        final SpecialDependent component = new SpecialDependent();

        final ComponentContext created = addLocalControl(ComponentContext.class);

        setupCollection(context,
                        setupFieldResolution(component.getClass(), "container", container, null, null, null),
                        setupFieldResolution(component.getClass(), "context", container, null, created, null));

        replay();
        assert component == injector.injectFields(resolver, mapping, context, component);
        verify();

        assert component.container == container : component.container;
        assert component.context == created : component.context;
    }

    private final class FieldInjected {

        @Component
        public Dependency dependency;

        @ServiceProvider
        public Service[] services;
    }

    private final class OptionalFieldInjected {

        @Optional
        @Component
        public Dependency dependency;
    }

    private static final class ConstructorInjected {

        public ConstructorInjected(final Dependency dependency, @ServiceProvider(api = Service.class) final Object[] services) {
            assert dependency != null;
        }
    }

    public static interface Dependency {

    }

    public static class DependencyImpl implements Dependency {

    }

    @ServiceProvider
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
}

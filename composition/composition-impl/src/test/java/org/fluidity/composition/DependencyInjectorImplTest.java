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
    private final ReferenceChain referenceChain = addControl(ReferenceChain.class);
    private final ReferenceChain.Link link = addControl(ReferenceChain.Link.class);
    private final ComponentMapping mapping = addControl(ComponentMapping.class);

    private final ComponentContext context = addControl(ComponentContext.class);
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

    private ComponentContext setupFieldResolution(final Class<?> componentType, final String fieldName, final Class<?> dependencyType, final Object component)
            throws Exception {
        final ComponentContext copy = addLocalControl(ComponentContext.class);

        EasyMock.expect(context.copy()).andReturn(copy);

        assert fieldName != null;
        final Field field = componentType.getDeclaredField(fieldName);
        assert field != null : String.format("%s.%s", componentType.getClass(), fieldName);

        final Annotation[] componentContext = neverNull(componentType.getAnnotations());
        final Annotation[] dependencyContext = neverNull(field.getAnnotations());

        final Context acceptedContext = dependencyType.getAnnotation(Context.class);

        final Annotation[] definitions = new Annotation[componentContext.length + dependencyContext.length];
        System.arraycopy(componentContext, 0, definitions, 0, componentContext.length);
        System.arraycopy(dependencyContext, 0, definitions, componentContext.length, dependencyContext.length);

        EasyMock.expect(copy.expand(EasyMock.aryEq(definitions))).andReturn(copy);

        EasyMock.expect(resolver.mapping(dependencyType)).andReturn(mapping);
        EasyMock.expect(mapping.contextSpecification(Context.class)).andReturn(acceptedContext);
        EasyMock.expect(copy.reduce(acceptedContext)).andReturn(copy);
        EasyMock.expect(resolver.resolve(dependencyType, copy)).andReturn(component);

        return copy;
    }

    private ComponentContext[] setupConstructorResolution(final Class<?> componentType, final int index, final Object... components) throws Exception {
        final Constructor constructor = componentType.getDeclaredConstructors()[index];
        assert constructor != null : String.format("%s.%d", componentType.getClass(), index);

        final List<ComponentContext> copies = new ArrayList<ComponentContext>();

        int i = 0;
        for (final Class<?> dependencyType : constructor.getParameterTypes()) {
            final ComponentContext copy = addLocalControl(ComponentContext.class);

            copies.add(copy);

            EasyMock.expect(context.copy()).andReturn(copy);

            if (dependencyType.isArray()) {
                assert components[i].getClass().isArray() : i;
                final Object[] services = (Object[]) components[i++];

                for (final Object service : services) {
                    EasyMock.expect(resolver.resolve(service.getClass(), copy)).andReturn(service);
                }
            } else {
                final ComponentMapping mapping = addLocalControl(ComponentMapping.class);

                final Annotation[] componentContext = neverNull(componentType.getAnnotations());
                final Annotation[] dependencyContext = neverNull(dependencyType.getAnnotations());

                final Context acceptedContext = dependencyType.getAnnotation(Context.class);

                final Annotation[] definitions = new Annotation[componentContext.length + dependencyContext.length];
                System.arraycopy(componentContext, 0, definitions, 0, componentContext.length);
                System.arraycopy(dependencyContext, 0, definitions, componentContext.length, dependencyContext.length);

                EasyMock.expect(copy.expand(EasyMock.aryEq(definitions))).andReturn(copy);

                EasyMock.expect(resolver.mapping(dependencyType)).andReturn(mapping);
                EasyMock.expect(mapping.contextSpecification(Context.class)).andReturn(acceptedContext);
                EasyMock.expect(copy.reduce(acceptedContext)).andReturn(copy);
                EasyMock.expect(resolver.resolve(dependencyType, copy)).andReturn(components[i++]);
            }
        }

        return copies.toArray(new ComponentContext[copies.size()]);
    }

    private void setupCollection(final ComponentContext context, final ComponentContext... copies) {
        EasyMock.expect(context.collect(Arrays.asList(copies))).andReturn(context);
    }

    @SuppressWarnings( { "unchecked" })
    @Test
    public void injectsFields() throws Exception {
        final FieldInjected component = new FieldInjected();

        final Dependency dependency = new DependencyImpl();
        final ComponentContext copy1 = setupFieldResolution(FieldInjected.class, "dependency", Dependency.class, dependency);

        final ComponentContext copy2 = addLocalControl(ComponentContext.class);

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
        assert component == injector.injectFields(resolver, dummyMapping, FieldInjected.class, context, component);
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

        setupFieldResolution(FieldInjected.class, "dependency", Dependency.class, null);

        replay();
        assert component == injector.injectFields(resolver, dummyMapping, FieldInjected.class, context, component);
        verify();
    }

    @Test
    public void injectsNullForOptionalFields() throws Exception {
        final OptionalFieldInjected component = new OptionalFieldInjected();

        setupCollection(context, setupFieldResolution(OptionalFieldInjected.class, "dependency", Dependency.class, null));

        replay();
        assert component == injector.injectFields(resolver, dummyMapping, OptionalFieldInjected.class, context, component);
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

        setupCollection(context, setupConstructorResolution(ConstructorInjected.class, 0, dependency, new Object[] { service1, service2 }));

        replay();
        Object[] arguments = injector.injectConstructor(resolver,
                                                        dummyMapping,
                                                        ConstructorInjected.class,
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

        final ComponentContext copy1 = addLocalControl(ComponentContext.class);
        EasyMock.expect(context.copy()).andReturn(copy1);

        EasyMock.expect(mapping.providedContext()).andReturn(null).anyTimes();
        EasyMock.expect(resolver.container(copy1)).andReturn(container);

        final ComponentContext copy2 = addLocalControl(ComponentContext.class);
        EasyMock.expect(context.copy()).andReturn(copy2);

        EasyMock.expect(mapping.contextSpecification(Context.class)).andReturn(null);
        EasyMock.expect(copy2.reduce(null)).andReturn(copy2);

        EasyMock.expect(context.collect(Arrays.asList(copy1, copy2))).andReturn(context);

        replay();
        assert component == injector.injectFields(resolver, mapping, SpecialDependent.class, context, component);
        verify();

        assert component.container == container : component.container;
        assert component.context == copy2 : component.context;
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

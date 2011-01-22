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
    private final ContextChain contextChain = addControl(ContextChain.class);
    private final ReferenceChain referenceChain = addControl(ReferenceChain.class);
    private final ContextFactory contextFactory = addControl(ContextFactory.class);

    private final ComponentContext context = addControl(ComponentContext.class);
    private final ComponentContext context2= addControl(ComponentContext.class);
    private final ComponentContainer container = addControl(ComponentContainer.class);

    private final DependencyInjector injector = new DependencyInjectorImpl(discovery);

    @SuppressWarnings({ "unchecked" })
    @Test
    public void injectsFields() throws Exception {
        final FieldInjected component = new FieldInjected();

        EasyMock.expect(resolver.contextChain()).andReturn(contextChain).anyTimes();
        EasyMock.expect(resolver.referenceChain()).andReturn(referenceChain).anyTimes();
        EasyMock.expect(resolver.contextFactory()).andReturn(contextFactory).anyTimes();
        EasyMock.expect(contextFactory.extractContext(EasyMock.<Annotation[]>anyObject())).andReturn(null);

        final Dependency dependency = new DependencyImpl();
        EasyMock.expect(resolver.resolve(Dependency.class, context)).andReturn(dependency);

        EasyMock.expect(discovery.findComponentClasses(EasyMock.same(Service.class), EasyMock.<ClassLoader>notNull(), EasyMock.eq(false)))
                .andReturn(new Class[] { ServiceImpl1.class, ServiceImpl2.class });

        final ServiceImpl1 service1 = new ServiceImpl1();
        final ServiceImpl2 service2 = new ServiceImpl2();
        EasyMock.expect(resolver.resolve(ServiceImpl1.class, context)).andReturn(service1);
        EasyMock.expect(resolver.resolve(ServiceImpl2.class, context)).andReturn(null);
        EasyMock.expect(resolver.create(ServiceImpl2.class, context)).andReturn(service2);

        replay();
        assert component == injector.injectFields(resolver, FieldInjected.class, context, component);
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

        EasyMock.expect(resolver.contextChain()).andReturn(contextChain).anyTimes();
        EasyMock.expect(resolver.referenceChain()).andReturn(referenceChain).anyTimes();
        EasyMock.expect(resolver.contextFactory()).andReturn(contextFactory).anyTimes();
        EasyMock.expect(contextFactory.extractContext(EasyMock.<Annotation[]>anyObject())).andReturn(null);

        EasyMock.expect(resolver.resolve(Dependency.class, context)).andReturn(null);

        replay();
        assert component == injector.injectFields(resolver, FieldInjected.class, context, component);
        verify();
    }

    @Test
    public void injectsNullForOptionalFields() throws Exception {
        final OptionalFieldInjected component = new OptionalFieldInjected();

        EasyMock.expect(resolver.contextChain()).andReturn(contextChain).anyTimes();
        EasyMock.expect(resolver.referenceChain()).andReturn(referenceChain).anyTimes();
        EasyMock.expect(resolver.contextFactory()).andReturn(contextFactory).anyTimes();
        EasyMock.expect(contextFactory.extractContext(EasyMock.<Annotation[]>anyObject())).andReturn(null);

        EasyMock.expect(resolver.resolve(Dependency.class, context)).andReturn(null);

        replay();
        assert component == injector.injectFields(resolver, OptionalFieldInjected.class, context, component);
        verify();

        assert component.dependency == null : component.dependency;
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void injectsConstructor() throws Exception {
        final FieldInjected component = new FieldInjected();

        EasyMock.expect(resolver.contextChain()).andReturn(contextChain).anyTimes();
        EasyMock.expect(resolver.referenceChain()).andReturn(referenceChain).anyTimes();
        EasyMock.expect(resolver.contextFactory()).andReturn(contextFactory).anyTimes();
        EasyMock.expect(contextFactory.extractContext(EasyMock.<Annotation[]>anyObject())).andReturn(null);

        final Dependency dependency = new DependencyImpl();
        EasyMock.expect(resolver.resolve(Dependency.class, context)).andReturn(dependency);

        EasyMock.expect(discovery.findComponentClasses(EasyMock.same(Service.class), EasyMock.<ClassLoader>notNull(), EasyMock.eq(false)))
                .andReturn(new Class[] { ServiceImpl1.class, ServiceImpl2.class });

        final ServiceImpl1 service1 = new ServiceImpl1();
        final ServiceImpl2 service2 = new ServiceImpl2();
        EasyMock.expect(resolver.resolve(ServiceImpl1.class, context)).andReturn(service1);
        EasyMock.expect(resolver.resolve(ServiceImpl2.class, context)).andReturn(null);
        EasyMock.expect(resolver.create(ServiceImpl2.class, context)).andReturn(service2);

        replay();
        Object[] arguments = injector.injectConstructor(resolver,
                                                        ConstructorInjected.class,
                                                        context, ConstructorInjected.class.getDeclaredConstructor(Dependency.class, Object[].class));
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

        EasyMock.expect(resolver.contextChain()).andReturn(contextChain).anyTimes();
        EasyMock.expect(resolver.referenceChain()).andReturn(referenceChain).anyTimes();
        EasyMock.expect(resolver.contextFactory()).andReturn(contextFactory).anyTimes();

        EasyMock.expect(resolver.container(context)).andReturn(container);
        EasyMock.expect(contextChain.consumedContext(SpecialDependent.class, SpecialDependent.class, context, referenceChain)).andReturn(context2);

        replay();
        assert component == injector.injectFields(resolver, SpecialDependent.class, context, component);
        verify();

        assert component.container == container : component.container;
        assert component.context == context2 : component.context;
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

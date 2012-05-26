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

package org.fluidity.composition.container.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.Inject;
import org.fluidity.composition.Optional;
import org.fluidity.composition.container.ContextDefinition;
import org.fluidity.composition.container.DependencyInjector;
import org.fluidity.composition.container.spi.ContextNode;
import org.fluidity.composition.container.spi.DependencyGraph;
import org.fluidity.composition.container.spi.DependencyResolver;
import org.fluidity.tests.MockGroupAbstractTest;

import org.easymock.EasyMock;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class DependencyInjectorImplTest extends MockGroupAbstractTest {

    private final DependencyGraph.Traversal traversal = mock(DependencyGraph.Traversal.class);
    private final DependencyResolver resolver = mock(DependencyResolver.class);
    private final ContextDefinition context = mock(ContextDefinition.class);
    private final ComponentContainer container = mock(ComponentContainer.class);
    private final Component.Reference reference = mock(Component.Reference.class);
    private final ContextNode contexts = mock(ContextNode.class);

    private final DependencyInjector injector = new DependencyInjectorImpl();

    private static Annotation[] neverNull(final Annotation[] array) {
        return array == null ? new Annotation[0] : array;
    }

    private ContextDefinition setupFieldResolution(final Class<?> componentType,
                                                   final String fieldName,
                                                   final Object component,
                                                   final ComponentContext createdContext) throws Exception {
        assert fieldName != null;
        final Field field = componentType.getDeclaredField(fieldName);
        assert field != null : String.format("%s.%s", componentType.getClass(), fieldName);

        return setupDependencyResolution(componentType, field.getType(), field.getAnnotations(), createdContext, component);
    }

    private ContextDefinition[] setupConstructorResolution(final Class<?> componentType,
                                                           final Constructor<?> constructor,
                                                           final ComponentContext createdContext,
                                                           final Object... components) throws Exception {
        assert constructor != null : String.format("%s(?)", componentType.getClass());

        final List<ContextDefinition> copies = new ArrayList<ContextDefinition>();

        final Class<?>[] parameterTypes = constructor.getParameterTypes();
        final Annotation[][] annotations = constructor.getParameterAnnotations();
        for (int i = 0, limit = parameterTypes.length; i < limit; i++) {
            final Class<?> dependencyType = parameterTypes[i];
            copies.add(setupDependencyResolution(componentType,
                                                 dependencyType,
                                                 annotations[i],
                                                 createdContext, components[i]));
        }

        return copies.toArray(new ContextDefinition[copies.size()]);
    }

    private ContextDefinition[] setupMethodResolution(final Class<?> componentType,
                                                      final Method method,
                                                      final ComponentContext createdContext,
                                                      final Object... components) throws Exception {
        assert method != null : String.format("%s(?)", componentType.getClass());

        final List<ContextDefinition> copies = new ArrayList<ContextDefinition>();

        final Class<?>[] parameterTypes = method.getParameterTypes();
        final Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0, limit = parameterTypes.length; i < limit; i++) {
            final Class<?> dependencyType = parameterTypes[i];
            copies.add(setupDependencyResolution(componentType,
                                                 dependencyType,
                                                 annotations[i],
                                                 createdContext, components[i]));
        }

        return copies.toArray(new ContextDefinition[copies.size()]);
    }

    @SuppressWarnings("unchecked")
    private ContextDefinition setupDependencyResolution(final Class<?> componentType,
                                                        final Class<?> dependencyType,
                                                        final Annotation[] dependencyAnnotations,
                                                        final ComponentContext createdContext,
                                                        final Object component) {
        EasyMock.expect(contexts.providedContext()).andReturn(null);

        final ContextDefinition copy = localMock(ContextDefinition.class);

        final Annotation[] componentContext = neverNull(componentType.getAnnotations());
        final Annotation[] dependencyContext = neverNull(dependencyAnnotations);

        final Annotation[] definitions = new Annotation[componentContext.length + dependencyContext.length];
        System.arraycopy(componentContext, 0, definitions, 0, componentContext.length);
        System.arraycopy(dependencyContext, 0, definitions, componentContext.length, dependencyContext.length);

        EasyMock.expect(context.reference()).andReturn(reference);
        EasyMock.expect(reference.type()).andReturn(dependencyType);

        EasyMock.expect(context.advance(dependencyType)).andReturn(copy);
        EasyMock.expect(copy.expand(EasyMock.aryEq(definitions))).andReturn(copy);

        if (dependencyType.isArray()) {
            assert component == null || component.getClass().isArray() : component.getClass();
            final Object[] services = (Object[]) component;

            EasyMock.expect(resolver.resolveGroup(EasyMock.same(dependencyType.getComponentType()),
                                                  EasyMock.same(copy),
                                                  EasyMock.same(traversal),
                                                  EasyMock.<Annotation[]>notNull(),
                                                  EasyMock.same(dependencyType))).andReturn(new DependencyGraph.Node.Constant(dependencyType, services, null));
        } else if (dependencyType == ComponentContext.class) {
            EasyMock.expect(contexts.contextConsumer()).andReturn((Class) componentType);
            EasyMock.expect(context.copy()).andReturn(copy);
            EasyMock.expect(copy.accept(componentType)).andReturn(copy);
            EasyMock.expect(copy.create()).andReturn(createdContext);
        } else {
            if (dependencyType == ComponentContainer.class) {
                EasyMock.expect(context.copy()).andReturn(copy);
                EasyMock.expect(copy.expand(EasyMock.aryEq(definitions))).andReturn(copy);
                EasyMock.expect(resolver.container(copy)).andReturn(container);
            } else {
                final DependencyGraph.Node.Constant node = new DependencyGraph.Node.Constant(dependencyType, component, null);
                EasyMock.expect(resolver.resolveComponent(dependencyType, copy, traversal, dependencyType)).andReturn(node);
                EasyMock.expect(resolver.replace(copy, traversal, dependencyType, node)).andReturn(node);
            }
        }

        return copy;
    }

    @SuppressWarnings("unchecked")
    private void setupCollection(final Class<?> accept, final ContextDefinition context, final ContextDefinition... copies) {
        if (accept != null) {
            EasyMock.expect(contexts.contextConsumer()).andReturn((Class) accept);
            EasyMock.expect(context.accept(accept)).andReturn(context);
        }

        EasyMock.expect(context.collect(Arrays.asList(copies))).andReturn(context);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void injectsFields() throws Exception {
        final FieldInjected component = new FieldInjected();

        final Dependency dependency = new DependencyImpl();
        final ContextDefinition copy1 = setupFieldResolution(FieldInjected.class, "dependency", dependency, null);

        final ServiceImpl1 service1 = new ServiceImpl1();
        final ServiceImpl2 service2 = new ServiceImpl2();
        final Service[] services = { service1, service2 };
        final ContextDefinition copy2 = setupFieldResolution(FieldInjected.class, "services", services, null);

        setupCollection(null, context, copy1, copy2);

        expectCallbacks();

        replay();
        assert component == injector.fields(component, traversal, resolver, contexts, context);
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

        setupCollection(null, context,
                        setupFieldResolution(FieldInjected.class, "dependency", null, null),
                        setupFieldResolution(FieldInjected.class, "services", null, null));

        expectCallbacks();

        replay();
        assert component == injector.fields(component, traversal, resolver, contexts, context);
        verify();
    }

    @Test
    public void injectsNullForOptionalFields() throws Exception {
        final OptionalFieldInjected component = new OptionalFieldInjected();

        setupCollection(null, context, setupFieldResolution(OptionalFieldInjected.class, "dependency", null, null));

        expectCallbacks();

        replay();
        assert component == injector.fields(component, traversal, resolver, contexts, context);
        verify();

        assert component.dependency == null : component.dependency;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void injectsConstructor() throws Exception {
        final Dependency dependency = new DependencyImpl();
        final ServiceImpl1 service1 = new ServiceImpl1();
        final ServiceImpl2 service2 = new ServiceImpl2();

        final Service[] services = { service1, service2 };

        final Constructor<ConstructorInjected> constructor = ConstructorInjected.class.getDeclaredConstructor(Dependency.class, Service[].class);

        setupCollection(ConstructorInjected.class, context, setupConstructorResolution(ConstructorInjected.class, constructor, null, dependency, services));

        EasyMock.expect(context.create()).andReturn(localMock(ComponentContext.class));

        ConstructorInjected.expectedGroupSize = services.length;

        expectCallbacks();

        replay();
        assert injector.constructor(ConstructorInjected.class, traversal, resolver, contexts, context, constructor) != null;
        verify();
    }

    @Test
    public void handlesSpecialDependencies() throws Exception {
        final SpecialDependent component = new SpecialDependent();

        final ComponentContext created = localMock(ComponentContext.class);

        setupCollection(null, context,
                        setupFieldResolution(component.getClass(), "container", container, null),
                        setupFieldResolution(component.getClass(), "context", context, created));

        expectCallbacks();

        EasyMock.expect(created.annotation(Component.Reference.class, null)).andReturn(null);

        replay();
        assert component == injector.fields(component, traversal, resolver, contexts, context);
        verify();

        assert component.context == created : component.context;
        assert component.container != null : component.container;

        EasyMock.expect(container.getComponent(ContextDefinition.class)).andReturn(context);

        expectCallbacks();

        replay();
        assert component.container.getComponent(ContextDefinition.class) == context;
        verify();
    }

    @Test
    public void neverInjectsNullForGroup() throws Exception {
        final Constructor<MissingGroupConsumer> constructor = MissingGroupConsumer.class.getDeclaredConstructor(MissingService[].class);
        setupCollection(MissingGroupConsumer.class, context, setupConstructorResolution(MissingGroupConsumer.class, constructor, null, (Object) null));
        EasyMock.expect(context.create()).andReturn(localMock(ComponentContext.class));

        expectCallbacks();

        replay();
        assert injector.constructor(MissingGroupConsumer.class, traversal, resolver, contexts, context, constructor) != null;
        verify();
    }

    @Test
    public void testInvokesMethod() throws Exception {
        final MethodInjected component = new MethodInjected();

        final Method method = MethodInjected.class.getDeclaredMethod("explicit", Dependency.class, Service[].class);
        final DependencyImpl dependency = new DependencyImpl();
        final Service[] services = new Service[0];

        setupMethodResolution(MissingGroupConsumer.class, method, null, dependency, services);

        replay();
        assert "value".equals(injector.invoke(component, method, null, traversal, resolver, contexts, context, true));
        verify();

        assert component.dependency == dependency;
        assert component.services == services;
    }

    @Test
    public void testInvokesMethodWithParameters() throws Exception {
        final MethodInjected component = new MethodInjected();

        final Method method = MethodInjected.class.getDeclaredMethod("implicit", Dependency.class, Service[].class);
        final DependencyImpl dependency = new DependencyImpl();

        replay();
        assert "value".equals(injector.invoke(component, method, new Object[] { dependency, null }, traversal, resolver, contexts, context, false));
        verify();

        assert component.dependency == dependency;
        assert component.services == null;
    }

    private void expectCallbacks() {
        traversal.descend(EasyMock.<Class<?>>notNull(), EasyMock.<Class<?>>notNull(), EasyMock.<Annotation[]>notNull(), EasyMock.<Annotation[]>notNull());
        EasyMock.expectLastCall().anyTimes();
        traversal.ascend(EasyMock.<Class<?>>notNull(), EasyMock.<Class<?>>notNull());
        EasyMock.expectLastCall().anyTimes();
    }

    private final class FieldInjected {

        @Inject
        public Dependency dependency;

        @Inject
        @ComponentGroup
        public Service[] services;
    }

    private final class OptionalFieldInjected {

        @Optional
        @Inject
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

    public interface Dependency { }

    public static class DependencyImpl implements Dependency { }

    @ComponentGroup
    public interface Service { }

    public static class ServiceImpl1 implements Service { }

    public static class ServiceImpl2 implements Service { }

    public static class SpecialDependent {

        @Inject
        public ComponentContainer container;

        @Inject
        public ComponentContext context;
    }

    @ComponentGroup
    public interface MissingService { }

    @SuppressWarnings("UnusedDeclaration")
    public static class MethodInjected {

        public Dependency dependency;
        public Service[] services;

        private String explicit(final Dependency dependency, final @ComponentGroup Service[] services) {
            this.dependency = dependency;
            this.services = services;
            return "value";
        }

        private String implicit(final @Inject Dependency dependency, final @ComponentGroup Service[] services) {
            this.dependency = dependency;
            this.services = services;
            return "value";
        }
    }
}

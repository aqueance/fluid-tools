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

package org.fluidity.composition;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.fluidity.composition.spi.ComponentMapping;
import org.fluidity.composition.spi.DependencyResolver;
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

    private final DependencyInjector injector = new DependencyInjectorImpl();

    private final ComponentMapping mapping = new ComponentMapping() {
        public Set<Class<? extends Annotation>> acceptedContext() {
            return null;
        }

        public Annotation[] annotations() {
            return null;
        }
    };

    private static Annotation[] neverNull(final Annotation[] array) {
        return array == null ? new Annotation[0] : array;
    }

    private ContextDefinition setupFieldResolution(final Class<?> componentType,
                                                   final String fieldName,
                                                   final Object component,
                                                   final ComponentContext createdContext,
                                                   final Annotation[] containerAnnotations) throws Exception {
        assert fieldName != null;
        final Field field = componentType.getDeclaredField(fieldName);
        assert field != null : String.format("%s.%s", componentType.getClass(), fieldName);

        return setupDependencyResolution(componentType,
                                         field.getType(),
                                         field.getAnnotations(),
                                         createdContext,
                                         containerAnnotations,
                                         component);
    }

    private ContextDefinition[] setupConstructorResolution(final Class<?> componentType,
                                                           final Constructor<?> constructor,
                                                           final ComponentContext createdContext,
                                                           final Annotation[] containerAnnotations,
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
                                                 createdContext,
                                                 containerAnnotations,
                                                 components[i]));
        }

        return copies.toArray(new ContextDefinition[copies.size()]);
    }

    private ContextDefinition[] setupMethodResolution(final Class<?> componentType,
                                                           final Method method,
                                                           final ComponentContext createdContext,
                                                           final Annotation[] containerAnnotations,
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
                                                 createdContext,
                                                 containerAnnotations,
                                                 components[i]));
        }

        return copies.toArray(new ContextDefinition[copies.size()]);
    }

    private ContextDefinition setupDependencyResolution(final Class<?> componentType,
                                                        final Class<?> dependencyType,
                                                        final Annotation[] dependencyAnnotations,
                                                        final ComponentContext createdContext,
                                                        final Annotation[] containerAnnotations,
                                                        final Object component) {
        final ContextDefinition copy = localMock(ContextDefinition.class);

        EasyMock.expect(context.copy()).andReturn(copy);

        final Annotation[] componentContext = neverNull(componentType.getAnnotations());
        final Annotation[] dependencyContext = neverNull(dependencyAnnotations);

        final Context annotation = dependencyType.getAnnotation(Context.class);
        final Set<Class<? extends Annotation>> acceptedContext = annotation == null ? null : new HashSet<Class<? extends Annotation>>(Arrays.asList(annotation.value()));

        final Annotation[] definitions = new Annotation[componentContext.length + dependencyContext.length];
        System.arraycopy(componentContext, 0, definitions, 0, componentContext.length);
        System.arraycopy(dependencyContext, 0, definitions, componentContext.length, dependencyContext.length);

        EasyMock.expect(copy.expand(EasyMock.aryEq(definitions))).andReturn(copy);

        if (dependencyType.isArray()) {
            assert component == null || component.getClass().isArray() : component.getClass();
            final Object[] services = (Object[]) component;

            EasyMock.expect(resolver.resolveGroup(EasyMock.same(dependencyType.getComponentType()),
                                                  EasyMock.same(copy),
                                                  EasyMock.same(traversal),
                                                  EasyMock.<Annotation[]>notNull()))
                    .andReturn(new DependencyGraph.Node.Constant(dependencyType, services, null));
        } else if (dependencyType == ComponentContext.class) {
            EasyMock.expect(copy.reduce(EasyMock.<Set<Class<? extends Annotation>>>isNull())).andReturn(copy);

            EasyMock.expect(copy.create()).andReturn(createdContext);
        } else {
            if (dependencyType == ComponentContainer.class) {
                EasyMock.expect(mapping.annotations()).andReturn(containerAnnotations).anyTimes();
                EasyMock.expect(resolver.container(copy)).andReturn(container);
            } else {
                final ComponentMapping mapping = localMock(ComponentMapping.class);

                EasyMock.expect(resolver.mapping(dependencyType, copy)).andReturn(mapping);
                EasyMock.expect(mapping.acceptedContext()).andReturn(acceptedContext);
                EasyMock.expect(copy.reduce(acceptedContext)).andReturn(copy);
                EasyMock.expect(resolver.resolveComponent(dependencyType, copy, traversal)).andReturn(new DependencyGraph.Node.Constant(dependencyType, component, null));
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
        final ContextDefinition copy1 = setupFieldResolution(FieldInjected.class, "dependency", dependency, null, null);

        final ServiceImpl1 service1 = new ServiceImpl1();
        final ServiceImpl2 service2 = new ServiceImpl2();
        final Service[] services = { service1, service2 };
        final ContextDefinition copy2 = setupFieldResolution(FieldInjected.class, "services", services, null, null);

        setupCollection(context, copy1, copy2);

        expectCallbacks();

        replay();
        assert component == injector.fields(component, traversal, resolver, mapping, context);
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

        setupCollection(context,
                        setupFieldResolution(FieldInjected.class, "dependency", null, null, null),
                        setupFieldResolution(FieldInjected.class, "services", null, null, null));

        expectCallbacks();

        replay();
        assert component == injector.fields(component, traversal, resolver, mapping, context);
        verify();
    }

    @Test
    public void injectsNullForOptionalFields() throws Exception {
        final OptionalFieldInjected component = new OptionalFieldInjected();

        setupCollection(context, setupFieldResolution(OptionalFieldInjected.class, "dependency", null, null, null));

        expectCallbacks();

        replay();
        assert component == injector.fields(component, traversal, resolver, mapping, context);
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
        setupCollection(context, setupConstructorResolution(ConstructorInjected.class, constructor, null, null, dependency, services));
        EasyMock.expect(context.create()).andReturn(localMock(ComponentContext.class));

        ConstructorInjected.expectedGroupSize = services.length;

        expectCallbacks();

        replay();
        assert injector.constructor(traversal, resolver, mapping, context, constructor) != null;
        verify();
    }

    @Test
    public void handlesSpecialDependencies() throws Exception {
        final SpecialDependent component = new SpecialDependent();

        final ComponentContext created = localMock(ComponentContext.class);

        setupCollection(context,
                        setupFieldResolution(component.getClass(), "container", container, null, null),
                        setupFieldResolution(component.getClass(), "context", context, created, null));

        expectCallbacks();

        replay();
        assert component == injector.fields(component, traversal, resolver, mapping, context);
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
        setupCollection(context, setupConstructorResolution(MissingGroupConsumer.class, constructor, null, null, (Object) null));
        EasyMock.expect(context.create()).andReturn(localMock(ComponentContext.class));

        expectCallbacks();

        replay();
        assert injector.constructor(traversal, resolver, mapping, context, constructor) != null;
        verify();
    }

    @Test
    public void testInvokesMethod() throws Exception {
        final MethodInjected component = new MethodInjected();

        final Method method = MethodInjected.class.getDeclaredMethod("inject", Dependency.class, Service[].class);
        final DependencyImpl dependency = new DependencyImpl();
        final Service[] services = new Service[0];

        setupMethodResolution(MissingGroupConsumer.class, method, null, null, dependency, services);

        replay();
        assert "value".equals(injector.invoke(component, method, traversal, resolver, mapping, context));
        verify();

        assert component.dependency == dependency;
        assert component.services == services;
    }

    private void expectCallbacks() {
        traversal.resolving(EasyMock.<Class<?>>notNull(), EasyMock.<Class<?>>notNull(), EasyMock.<Annotation[]>notNull(), EasyMock.<Annotation[]>notNull());
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

    public static class MethodInjected {

        public Dependency dependency;
        public Service[] services;

        private String inject(final Dependency dependency, final @ComponentGroup Service[] services) {
            this.dependency = dependency;
            this.services = services;
            return "value";
        }
    }
}

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

import java.io.Closeable;
import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.Inject;
import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.foundation.Exceptions;
import org.fluidity.foundation.Generics;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
public final class CustomFactoryTests extends AbstractContainerTests {

    @SuppressWarnings("unchecked")
    private final ComponentFactory factory = mock(ComponentFactory.class);
    private final ComponentFactory.Instance instance = mock(ComponentFactory.Instance.class);

    public CustomFactoryTests(final ArtifactFactory factory) {
        super(factory);
    }

    @BeforeMethod
    public void setMockFactory() {
        DependentFactory.delegate = this.factory;
        GroupMember1Factory.delegate = this.factory;
        GroupMember2Factory.delegate = this.factory;
    }

    @Test
    public void invokesStandaloneFactoryClassOnce() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(DependentFactory.class);
        registry.bindComponent(FactoryDependency.class);

        final Check check = new Check();

        registry.bindInstance(check);

        EasyMock.expect(factory.resolve(EasyMock.<ComponentContext>notNull(), EasyMock.<ComponentFactory.Resolver>notNull()))
                .andAnswer(new FactoryInvocation(Check.class, check, instance))
                .anyTimes();
        instance.bind(EasyMock.<ComponentFactory.Registry>notNull());
        EasyMock.expectLastCall().anyTimes();

        replay();
        verifyComponent(container);
        verify();
    }

    @Test
    public void invokesStandaloneFactoryClassOnceInLinkedContainer() throws Exception {
        registry.bindComponent(Value.class);
        final ComponentContainer.Registry child = registry.isolateComponent(DependentFactory.class);

        final Check check = new Check();

        child.bindComponent(FactoryDependency.class);
        child.bindInstance(check);

        EasyMock.expect(factory.resolve(EasyMock.<ComponentContext>notNull(), EasyMock.<ComponentFactory.Resolver>notNull()))
                .andAnswer(new FactoryInvocation(Check.class, check, instance))
                .anyTimes();
        instance.bind(EasyMock.<ComponentFactory.Registry>notNull());
        EasyMock.expectLastCall().anyTimes();

        replay();
        verifyComponent(container);
        verify();
    }

    @Test
    public void circularFactoryInvocation() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(DependentFactory.class);
        registry.bindComponent(FactoryDependency.class);

        final Check check = new Check();

        registry.bindInstance(check);

        EasyMock.expect(factory.resolve(EasyMock.<ComponentContext>notNull(), EasyMock.<ComponentFactory.Resolver>notNull())).andReturn(instance).anyTimes();
        instance.bind(EasyMock.<ComponentFactory.Registry>notNull());
        EasyMock.expectLastCall().anyTimes();

        replay();
        verifyComponent(container);
        verify();
    }

    @Test
    public void groupMemberFactoryOrder1() throws Exception {
        registry.bindComponent(GroupMember2.class);
        registry.bindComponent(GroupMember1Factory.class);
        groupMemberChecks(1);
    }

    @Test
    public void groupMemberFactoryOrder2() throws Exception {
        registry.bindComponent(GroupMember1Factory.class);
        registry.bindComponent(GroupMember2.class);
        groupMemberChecks(1);
    }

    private void groupMemberChecks(final int factories) {
        EasyMock.expect(factory.resolve(EasyMock.<ComponentContext>notNull(), EasyMock.<ComponentFactory.Resolver>notNull())).andReturn(instance).anyTimes();
        instance.bind(EasyMock.<ComponentFactory.Registry>notNull());
        EasyMock.expectLastCall().times(factories);

        replay();
        final GroupApi[] group = container.getComponentGroup(GroupApi.class);
        verify();

        assert group != null : GroupApi.class;
        assert group.length == 2 : group.length;
        assert group[0] != group[1];
    }

    @Test(expectedExceptions = ComponentContainer.ResolutionException.class, expectedExceptionsMessageRegExp = ".*[Dd]ynamic.*Factory.*")
    public void testRestrictedContainer1() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(DependentValue.class);
        registry.bindComponent(DynamicFactory1.class);

        replay();
        container.getComponent(DynamicComponent1.class);
        verify();

        assert false : "Dynamic dependency should have been prevented";
    }

    @Test
    public void testRestrictedContainer2() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(DependentValue.class);
        registry.bindComponent(DynamicFactory2.class);

        replay();
        final DynamicComponent2 component = container.getComponent(DynamicComponent2.class);
        verify();

        assert component.key() != null;
    }

    @Test
    public void testTypeReference() throws Exception {
        registry.bindComponent(CustomDependent.class);
        registry.bindComponent(CustomDependencyFactory.class);

        replay();
        assert container.getComponent(CustomDependent.class) != null;
        verify();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Component.Context(collect = Component.Context.Collection.IMMEDIATE)
    public @interface Name {

        String value();
    }

    @Component(automatic = false)
    @Name("name-1")
    private static class ContextProvider {

        private final NamedComponent dependency1;
        private final NamedComponent dependency2;
        private final NamedGroup[] group;

        @Inject
        @Name("name-4")
        private @ComponentGroup NamedGroup[] field;

        @Inject
        @Name("name-5")
        private NamedComponent dependency3;

        public ContextProvider(final NamedComponent dependency1,
                               final @Name("name-2") NamedComponent dependency2,
                               final @Name("name-3") @ComponentGroup NamedGroup[] group) {
            this.dependency1 = dependency1;
            this.dependency2 = dependency2;
            this.group = group;
        }

        public String name1() {
            return dependency1.name;
        }

        public String name2() {
            return dependency2.name;
        }

        public String name3() {
            return dependency3.name;
        }

        public NamedGroup[] field() {
            return field;
        }

        public NamedGroup[] group() {
            return group;
        }
    }

    @Component(automatic = false)
    @Component.Context(Name.class)
    private static class NamedComponent {

        public final String name;

        @SuppressWarnings("UnusedDeclaration")
        private NamedComponent(final ComponentContext context) {
            this.name = context.annotation(Name.class, NamedComponent.class).value();
        }
    }

    @ComponentGroup
    private interface NamedGroup {

        String name();
    }

    private static abstract class NamedGroupMember implements NamedGroup {

        private final String name;

        protected NamedGroupMember(final ComponentContext context) {
            this.name = context.annotation(Name.class, getClass()).value();
        }

        public String name() {
            return name;
        }
    }

    @Component.Context(Name.class)
    private static class NamedGroupMember1 extends NamedGroupMember {

        protected NamedGroupMember1(final ComponentContext context) {
            super(context);
        }
    }

    @Component.Context(Name.class)
    private static class NamedGroupMember2 extends NamedGroupMember {

        protected NamedGroupMember2(final ComponentContext context) {
            super(context);
        }
    }

    @Component(automatic = false)
    private static class ConstructorDelegatingFactory implements ComponentFactory {

        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws ComponentContainer.ResolutionException {
            final Constructor<?> constructor = dependencies.constructor(ContextProvider.class);

            dependencies.discover(constructor);

            final Container container = dependencies.local(ContextProvider.class, new Container.Bindings() {
                public void bindComponents(final Container.Registry registry) {
                    registry.bindComponent(NamedComponent.class);
                }
            });

            final Dependency<NamedComponent> dependency1 = container.resolve(constructor, 0, NamedComponent.class);
            final Dependency<NamedComponent> dependency2 = container.resolve(constructor, 1, NamedComponent.class);
            final Dependency<NamedGroup[]> dependency3 = container.resolve(constructor, 2, NamedGroup[].class);

            final Field field = Exceptions.wrap(new Exceptions.Command<Field>() {
                public Field run() throws Throwable {
                    return ContextProvider.class.getDeclaredField("dependency3");
                }
            });

            final Dependency<NamedComponent> dependency4 = container.resolve(field, NamedComponent.class);

            return new Instance() {
                public void bind(final Registry registry) throws ComponentContainer.BindingException {
                    final ContextProvider instance = new ContextProvider(dependency1.instance(), dependency2.instance(), dependency3.instance());

                    Exceptions.wrap(new Exceptions.Command<Void>() {
                        public Void run() throws Throwable {
                            field.setAccessible(true);
                            field.set(instance, dependency4.instance());
                            return null;
                        }
                    });

                    registry.bindInstance(instance);
                }
            };

        }
    }

    private static class ContextProviderFactory {

        @Name("name-1")
        public static ContextProvider factoryMethod(final NamedComponent dependency1,
                                                    final @Name("name-2") NamedComponent dependency2,
                                                    final @Name("name-3") @ComponentGroup NamedGroup[] group) {
            return new ContextProvider(dependency1, dependency2, group);
        }
    }

    @Component(automatic = false)
    private static class FactoryDelegatingFactory implements ComponentFactory {

        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws ComponentContainer.ResolutionException {
            final Method method = Exceptions.wrap(new Exceptions.Command<Method>() {
                public Method run() throws Throwable {
                    return ContextProviderFactory.class.getMethod("factoryMethod", NamedComponent.class, NamedComponent.class, NamedGroup[].class);
                }
            });

            dependencies.discover(method);

            final Container container = dependencies.local(ContextProvider.class, new Container.Bindings() {
                public void bindComponents(final Container.Registry registry) {
                    registry.bindComponent(NamedComponent.class);
                }
            });

            final Dependency<NamedComponent> dependency1 = container.resolve(method, 0, NamedComponent.class);
            final Dependency<NamedComponent> dependency2 = container.resolve(method, 1, NamedComponent.class);
            final Dependency<NamedGroup[]> dependency3 = container.resolve(method, 2, NamedGroup[].class);

            final Field field = Exceptions.wrap(new Exceptions.Command<Field>() {
                public Field run() throws Throwable {
                    return ContextProvider.class.getDeclaredField("dependency3");
                }
            });

            final Dependency<NamedComponent> dependency4 = container.resolve(field, NamedComponent.class);

            return new Instance() {
                public void bind(final Registry registry) throws ComponentContainer.BindingException {
                    final ContextProvider instance = ContextProviderFactory.factoryMethod(dependency1.instance(), dependency2.instance(), dependency3.instance());

                    Exceptions.wrap(new Exceptions.Command<Void>() {
                        public Void run() throws Throwable {
                            field.setAccessible(true);
                            field.set(instance, dependency4.instance());
                            return null;
                        }
                    });

                    registry.bindInstance(instance);
                }
            };

        }
    }

    @DataProvider(name = "delegating-factories")
    public Object[][] delegatingFactories() {
        return new Object[][] {
                new Object[] { new ConstructorDelegatingFactory() },
                new Object[] { new FactoryDelegatingFactory() },
        };
    }

    @Test(dataProvider = "delegating-factories")
    public void testDelegatingFactories(final ComponentFactory factory) throws Exception {
        registry.bindFactory(factory, ContextProvider.class);
        registry.bindComponent(NamedGroupMember1.class);
        registry.bindComponent(NamedGroupMember2.class);

        replay();
        final ContextProvider component = container.getComponent(ContextProvider.class);
        verify();

        assert component != null;
        assert "name-1".equals(component.name1()) : component.name1();
        assert "name-2".equals(component.name2()) : component.name2();
        assert "name-5".equals(component.name3()) : component.name3();

        final NamedGroup[] group = component.group();
        assert group != null;
        assert group.length == 2 : group.length;

        for (final NamedGroup member : group) {
            assert "name-3".equals(member.name()) : String.format("%s: %s", member.getClass(), member.name());
        }

        final NamedGroup[] field = component.field();
        assert field != null;
        assert field.length == 2 : field.length;

        for (final NamedGroup member : field) {
            assert "name-4".equals(member.name()) : String.format("%s: %s", member.getClass(), member.name());
        }
    }

    @DataProvider(name = "resolution-variants")
    public Object[][] resolutionTypes() {
        return new Object[][] {
                new Object[] { 0 },
                new Object[] { 1 },
                new Object[] { 2 },
                new Object[] { 3 },
                new Object[] { 4 },
        };
    }

    @Test(dataProvider = "resolution-variants")
    public void testResolver(final int variant) throws Exception {

        @Component(automatic = false)
        class Main { }

        @Component(api = Secondary.class, automatic = false)
        @Component.Context(Component.Reference.class)
        class Secondary implements Serializable {
            public Secondary(final ComponentContext context) {
                switch (variant) {
                case 0:
                case 1:
                case 2:
                    assert context.annotation(Component.Reference.class, null).type() == Secondary.class : context;
                    break;
                case 3:
                    assert context.annotation(Component.Reference.class, null).type() == Serializable.class : context;
                    break;
                default:
                    assert false : variant;
                }
            }
        }

        @Component(api = Main.class, automatic = false)
        class Factory implements ComponentFactory {
            public Instance resolve(final ComponentContext context, final Resolver dependencies) throws ComponentContainer.ResolutionException {
                switch (variant) {
                case 0: {
                    final Dependency<Secondary> dependency = dependencies.resolve(Secondary.class, null, null);
                    assert dependency != null : Secondary.class;

                    return new Instance() {
                        public void bind(final Registry registry) throws ComponentContainer.BindingException {
                            registry.bindInstance(dependency.instance());
                            registry.bindComponent(Main.class);
                        }
                    };
                }

                case 1: {
                    final Dependency<Secondary> dependency = dependencies.resolve(Secondary.class, Secondary.class, null);
                    assert dependency != null : Secondary.class;

                    return new Instance() {
                        public void bind(final Registry registry) throws ComponentContainer.BindingException {
                            registry.bindInstance(dependency.instance());
                            registry.bindComponent(Main.class);
                        }
                    };
                }

                case 2: {
                    final Dependency<Secondary> dependency = dependencies.resolve(null, Secondary.class, null);
                    assert dependency != null : Secondary.class;

                    return new Instance() {
                        public void bind(final Registry registry) throws ComponentContainer.BindingException {
                            registry.bindInstance(dependency.instance());
                            registry.bindComponent(Main.class);
                        }
                    };
                }

                case 3: {
                    final Dependency<Secondary> dependency = dependencies.resolve(Secondary.class, Serializable.class, null);
                    assert dependency != null : Secondary.class;

                    return new Instance() {
                        public void bind(final Registry registry) throws ComponentContainer.BindingException {
                            registry.bindInstance(dependency.instance());
                            registry.bindComponent(Main.class);
                        }
                    };
                }

                case 4: {
                    try {
                        dependencies.resolve(Secondary.class, Closeable.class, null);
                        assert false : "Should have thrown resolution exception";
                    } catch (final ComponentContainer.ResolutionException e) {
                        // that's fine
                    }

                    return null;
                }

                default:
                    assert false : variant;
                    return null;
                }
            }
        }


        registry.bindInstance(this, CustomFactoryTests.class);
        registry.bindInstance(variant);
        registry.bindComponent(Secondary.class);
        registry.bindComponent(Factory.class);

        replay();

        switch (variant) {
        case 0:
        case 1:
        case 2:
        case 3:
            assert container.getComponent(Main.class) != null;
            break;
        case 4:
            assert container.getComponent(Main.class) == null;
            break;
        default:
            assert false : variant;
            break;
        }

        verify();
    }

    @Component(api = DependentKey.class, automatic = false)
    private static class DependentFactory implements ComponentFactory {

        public static ComponentFactory delegate;

        public DependentFactory(final FactoryDependency dependency) {
            assert dependency != null;
        }

        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws ComponentContainer.ResolutionException {
            assert delegate != null;
            final Instance instance = delegate.resolve(context, dependencies);

            return new Instance() {
                public void bind(final Registry registry) throws ComponentContainer.BindingException {
                    registry.bindComponent(DependentValue.class);

                    assert instance != null;
                    instance.bind(registry);
                }
            };
        }
    }

    @ComponentGroup
    private interface GroupApi { }

    private interface GroupMember2Api extends GroupApi { }

    @Component(automatic = false)
    private static class GroupMember1 implements GroupApi { }

    @Component(automatic = false)
    private static class GroupMember2 implements GroupMember2Api { }

    @Component(api = GroupMember1.class, automatic = false)
    private static class GroupMember1Factory implements ComponentFactory {

        public static ComponentFactory delegate;

        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws ComponentContainer.ResolutionException {
            assert delegate != null;
            final Instance instance = delegate.resolve(context, dependencies);

            return new Instance() {
                public void bind(final Registry registry) throws ComponentContainer.BindingException {
                    registry.bindComponent(GroupMember1.class);

                    assert instance != null;
                    instance.bind(registry);
                }
            };
        }
    }

    @Component(api = GroupMember2Api.class, automatic = false)
    private static class GroupMember2Factory implements ComponentFactory {

        public static ComponentFactory delegate;

        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws ComponentContainer.ResolutionException {
            assert delegate != null;
            final Instance instance = delegate.resolve(context, dependencies);

            return new Instance() {
                public void bind(final Registry registry) throws ComponentContainer.BindingException {
                    registry.bindComponent(GroupMember2.class);

                    assert instance != null;
                    instance.bind(registry);
                }
            };
        }
    }

    private static class FactoryInvocation implements IAnswer<ComponentFactory.Instance> {

        private final Class<?> checkKey;
        private final Object checkValue;
        private final ComponentFactory.Instance instance;

        public FactoryInvocation(final Class<?> checkKey, final Object checkValue, final ComponentFactory.Instance instance) {
            this.checkKey = checkKey;
            this.checkValue = checkValue;
            this.instance = instance;
        }

        public ComponentFactory.Instance answer() throws Throwable {
            final ComponentFactory.Resolver resolver = (ComponentFactory.Resolver) EasyMock.getCurrentArguments()[1];
            assert resolver != null : "Received no resolver";

            final ComponentFactory.Dependency<?> dependency = resolver.resolve(checkKey, null, null);
            assert dependency != null && dependency.instance() == checkValue : "Container does not check up";

            return instance;
        }
    }

    @Component(automatic = false)
    private static class DynamicComponent1 {

        private DynamicComponent1(final ComponentContainer container) {
            assert container != null : ComponentContainer.class;
            container.getComponent(Key.class);
        }
    }

    @Component(api = DynamicComponent1.class)
    private static class DynamicFactory1 implements ComponentFactory {

        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws ComponentContainer.ResolutionException {
            final Dependency<?>[] args = dependencies.discover(DynamicComponent1.class);

            return new Instance() {
                public void bind(final Registry registry) throws ComponentContainer.BindingException {
                    registry.bindInstance(new DynamicComponent1((ComponentContainer) args[0].instance()));
                }
            };
        }
    }

    @Component(automatic = false)
    private static class DynamicComponent2 {

        private final ComponentContainer container;

        private DynamicComponent2(final ComponentContainer container) {
            assert container != null : ComponentContainer.class;
            this.container = container;
        }

        public Key key() {
            return container.getComponent(Key.class);
        }
    }

    @Component(api = DynamicComponent2.class)
    private static class DynamicFactory2 implements ComponentFactory {

        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws ComponentContainer.ResolutionException {
            final Dependency<?>[] args = dependencies.discover(DynamicComponent1.class);

            return new Instance() {
                public void bind(final Registry registry) throws ComponentContainer.BindingException {
                    registry.bindInstance(new DynamicComponent2((ComponentContainer) args[0].instance()));
                }
            };
        }
    }

    private interface CustomParameter { }

    private interface CustomParameter1 extends CustomParameter { }

    private interface CustomParameter2 extends CustomParameter { }

    @SuppressWarnings("UnusedDeclaration")
    private static class CustomDependency<T> { }

    @Component(api = CustomDependency.class)
    @Component.Context(Component.Reference.class)
    private static class CustomDependencyFactory implements ComponentFactory {

        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws ComponentContainer.ResolutionException {
            assert context.defines(Component.Reference.class);
            assert context.annotations(Component.Reference.class) != null;

            final Component.Reference annotation = context.annotation(Component.Reference.class, getClass());
            final Type reference = annotation.type();
            final Class<?> parameter = annotation.parameter(0);

            assert parameter != null : reference;
            assert Generics.rawType(reference) == CustomDependency.class : Generics.rawType(reference);
            assert CustomParameter.class.isAssignableFrom(parameter) : Generics.typeParameter(reference, 0);

            return new Instance() {
                public void bind(final Registry registry) throws ComponentContainer.BindingException {
                    registry.bindInstance(new CustomDependency(), CustomDependency.class);
                }
            };
        }
    }

    private static class CustomDependent {

        public CustomDependent(final ComponentContext context,
                               final CustomDependency<CustomParameter1> dependency1,
                               final CustomDependency<CustomParameter2> dependency2,
                               final CustomDependency<CustomParameter1> dependency3) {
            assert !context.defines(Component.Reference.class) : context;

            assert dependency1 != null;
            assert dependency2 != null;
            assert dependency3 != null;
            assert (CustomDependency) dependency1 != dependency2 : "Type parameter did not contribute to component context";
            assert (CustomDependency) dependency1 == dependency3 : "Type parameter did not contribute to component context";
        }
    }
}

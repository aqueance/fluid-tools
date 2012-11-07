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

    private final MockObjects dependencies = dependencies();

    @SuppressWarnings("unchecked")
    private final ComponentFactory factory = dependencies.normal(ComponentFactory.class);
    private final ComponentFactory.Instance instance = dependencies.normal(ComponentFactory.Instance.class);

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

        verify(new Task() {
            public void run() throws Exception {
                verifyComponent(container);
            }
        });
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

        verify(new Task() {
            public void run() throws Exception {
                verifyComponent(container);
            }
        });
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

        verify(new Task() {
            public void run() throws Exception {
                verifyComponent(container);
            }
        });
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

    private void groupMemberChecks(final int factories) throws Exception {
        EasyMock.expect(factory.resolve(EasyMock.<ComponentContext>notNull(), EasyMock.<ComponentFactory.Resolver>notNull())).andReturn(instance).anyTimes();
        instance.bind(EasyMock.<ComponentFactory.Registry>notNull());
        EasyMock.expectLastCall().times(factories);

        final GroupApi[] group = verify(new Work<GroupApi[]>() {
            public GroupApi[] run() throws Exception {
                return container.getComponentGroup(GroupApi.class);
            }
        });

        assert group != null : GroupApi.class;
        assert group.length == 2 : group.length;
        assert group[0] != group[1];
    }

    @Test(expectedExceptions = ComponentContainer.ResolutionException.class, expectedExceptionsMessageRegExp = ".*[Dd]ynamic.*Factory.*")
    public void testRestrictedContainer1() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(DependentValue.class);
        registry.bindComponent(DynamicFactory1.class);

        verify(new Task() {
            public void run() throws Exception {
                container.getComponent(DynamicComponent1.class);
            }
        });

        assert false : "Dynamic dependency should have been prevented";
    }

    @Test
    public void testRestrictedContainer2() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(DependentValue.class);
        registry.bindComponent(DynamicFactory2.class);

        final DynamicComponent2 component = verify(new Work<DynamicComponent2>() {
            public DynamicComponent2 run() throws Exception {
                return container.getComponent(DynamicComponent2.class);
            }
        });

        assert component.key() != null;
    }

    @Test
    public void testTypeReference() throws Exception {
        registry.bindComponent(CustomDependent.class);
        registry.bindComponent(CustomDependencyFactory.class);

        final CustomDependent component = verify(new Work<CustomDependent>() {
            public CustomDependent run() throws Exception {
                return container.getComponent(CustomDependent.class);
            }
        });

        assert component != null;
    }

    @DataProvider(name = "delegating-factories")
    public Object[][] delegatingFactories() {
        return new Object[][] {
                new Object[] { new ConstructorDelegatingFactory1() },
                new Object[] { new ConstructorDelegatingFactory2() },
                new Object[] { new FactoryDelegatingFactory1() },
                new Object[] { new FactoryDelegatingFactory2() },
        };
    }

    @Test(dataProvider = "delegating-factories")
    public void testDelegatingFactories(final ComponentFactory factory) throws Exception {
        registry.bindFactory(factory, ContextProvider.class);
        registry.bindComponent(NamedGroupMember1.class);
        registry.bindComponent(NamedGroupMember2.class);

        final ContextProvider component = verify(new Work<ContextProvider>() {
            public ContextProvider run() throws Exception {
                return container.getComponent(ContextProvider.class);
            }
        });

        assert component != null;
        assert "name-1".equals(component.name1()) : component.name1();
        assert "name-2".equals(component.name2()) : component.name2();
        assert "name-5".equals(component.name3()) : component.name3();

        final NamedGroup[] group = component.group();
        assert group != null;
        assert group.length == 3 : group.length;

        for (final NamedGroup member : group) {
            assert "name-3".equals(member.name()) : String.format("%s: %s", member.getClass(), member.name());
        }

        final NamedGroup[] field1 = component.field1();
        assert field1 != null;
        assert field1.length == 2 : field1.length;

        for (final NamedGroup member : field1) {
            assert "name-4".equals(member.name()) : String.format("%s: %s", member.getClass(), member.name());
        }

        final NamedGroup[] field2 = component.field2();
        assert field2 != null;
        assert field2.length == 2 : field2.length;

        for (final NamedGroup member : field2) {
            assert "name-6".equals(member.name()) : String.format("%s: %s", member.getClass(), member.name());
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
            public Instance resolve(final ComponentContext context, final Resolver dependencies) throws Exception {
                switch (variant) {
                case 0: {
                    final Dependency<Secondary> dependency = dependencies.resolve(Secondary.class, (Type) null, null);
                    assert dependency != null : Secondary.class;

                    return new Instance() {
                        public void bind(final Registry registry) throws Exception {
                            registry.bindInstance(dependency.instance());
                            registry.bindComponent(Main.class);
                        }
                    };
                }

                case 1: {
                    final Dependency<Secondary> dependency = dependencies.resolve(Secondary.class, (Type) Secondary.class, null);
                    assert dependency != null : Secondary.class;

                    return new Instance() {
                        public void bind(final Registry registry) throws Exception {
                            registry.bindInstance(dependency.instance());
                            registry.bindComponent(Main.class);
                        }
                    };
                }

                case 2: {
                    final Dependency<Secondary> dependency = dependencies.resolve(null, (Type) Secondary.class, null);
                    assert dependency != null : Secondary.class;

                    return new Instance() {
                        public void bind(final Registry registry) throws Exception {
                            registry.bindInstance(dependency.instance());
                            registry.bindComponent(Main.class);
                        }
                    };
                }

                case 3: {
                    final Dependency<Secondary> dependency = dependencies.resolve(Secondary.class, (Type) Serializable.class, null);
                    assert dependency != null : Secondary.class;

                    return new Instance() {
                        public void bind(final Registry registry) throws Exception {
                            registry.bindInstance(dependency.instance());
                            registry.bindComponent(Main.class);
                        }
                    };
                }

                case 4: {
                    try {
                        dependencies.resolve(Secondary.class, (Type) Closeable.class, null);
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

        verify(new Task() {
            public void run() throws Exception {
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
            }
        });
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Component.Context(collect = Component.Context.Collection.IMMEDIATE)
    public @interface Name {

        String value();
    }

    private static class SuperClass {

        @Inject
        @Name("name-4")
        protected  @ComponentGroup NamedGroup[] field1;

        @Inject
        @Name("name-6")
        protected @ComponentGroup NamedGroup[] field2;

        @Inject
        @Name("name-5")
        protected NamedComponent field3;
    }

    @Component(automatic = false)
    @Name("name-1")
    private static class ContextProvider extends SuperClass {

        private final NamedComponent dependency1;
        private final NamedComponent dependency2;
        private final NamedGroup[] group;

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
            return field3.name;
        }

        public NamedGroup[] field1() {
            return field1;
        }

        public NamedGroup[] field2() {
            return field2;
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
    private final static class NamedGroupMember1 extends NamedGroupMember {

        NamedGroupMember1(final ComponentContext context) {
            super(context);
        }
    }

    @Component.Context(Name.class)
    private final static class NamedGroupMember2 extends NamedGroupMember {

        NamedGroupMember2(final ComponentContext context) {
            super(context);
        }
    }

    @Component.Context(Name.class)
    private final static class NamedGroupMember3 extends NamedGroupMember {

        NamedGroupMember3(final ComponentContext context) {
            super(context);
        }
    }

    @Component(automatic = false)
    private static class ConstructorDelegatingFactory1 implements ComponentFactory {

        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws Exception {
            final Constructor<?> constructor = dependencies.constructor(ContextProvider.class);

            dependencies.discover(constructor);

            final Container container = dependencies.local(ContextProvider.class, new Container.Bindings() {
                public void bindComponents(final Container.Registry registry) {
                    registry.bindComponent(NamedGroupMember3.class);
                    registry.bindComponent(NamedComponent.class);
                }
            });

            final Dependency<?>[] resolved = container.resolve(constructor);

            final Field field1 = SuperClass.class.getDeclaredField("field3");
            final Field field2 = SuperClass.class.getDeclaredField("field2");

            final Dependency<NamedComponent> dependency4 = container.resolve(NamedComponent.class, ContextProvider.class, field1);
            final Dependency<NamedGroup[]> dependency5 = dependencies.resolve(NamedGroup[].class, ContextProvider.class, field2);

            return new Instance() {
                public void bind(final Registry registry) throws Exception {
                    final ContextProvider instance = (ContextProvider) constructor.newInstance(dependencies.instantiate(resolved));

                    final NamedComponent value1 = dependency4.instance();
                    assert value1 != null;

                    field1.setAccessible(true);
                    field1.set(instance, value1);

                    final NamedGroup[] value2 = dependency5.instance();
                    assert value2 != null;

                    field2.setAccessible(true);
                    field2.set(instance, value2);

                    registry.bindInstance(instance);
                }
            };

        }
    }

    @Component(automatic = false)
    private static class ConstructorDelegatingFactory2 implements ComponentFactory {

        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws Exception {
            final Constructor<?> constructor = dependencies.constructor(ContextProvider.class);

            dependencies.discover(constructor);

            final Container container = dependencies.local(ContextProvider.class, new Container.Bindings() {
                public void bindComponents(final Container.Registry registry) {
                    registry.bindComponent(NamedGroupMember3.class);
                    registry.bindComponent(NamedComponent.class);
                }
            });

            final Dependency<NamedComponent> dependency1 = container.resolve(NamedComponent.class, constructor, 0);
            final Dependency<NamedComponent> dependency2 = container.resolve(NamedComponent.class, constructor, 1);
            final Dependency<NamedGroup[]> dependency3 = container.resolve(NamedGroup[].class, constructor, 2);

            final Field field1 = SuperClass.class.getDeclaredField("field3");
            final Field field2 = SuperClass.class.getDeclaredField("field2");

            final Dependency<NamedComponent> dependency4 = container.resolve(NamedComponent.class, ContextProvider.class, field1);
            final Dependency<NamedGroup[]> dependency5 = dependencies.resolve(NamedGroup[].class, ContextProvider.class, field2);

            return new Instance() {
                public void bind(final Registry registry) throws Exception {
                    final ContextProvider instance = new ContextProvider(dependency1.instance(), dependency2.instance(), dependency3.instance());

                    final NamedComponent value1 = dependency4.instance();
                    assert value1 != null;

                    field1.setAccessible(true);
                    field1.set(instance, value1);

                    final NamedGroup[] value2 = dependency5.instance();
                    assert value2 != null;

                    field2.setAccessible(true);
                    field2.set(instance, value2);

                    registry.bindInstance(instance);
                }
            };

        }
    }

    private static class ContextProviderFactory {

        @Name("name-1")
        @SuppressWarnings("UnusedDeclaration")
        public static ContextProvider factoryMethod(final NamedComponent dependency1,
                                                    final @Name("name-2") NamedComponent dependency2,
                                                    final @Name("name-3") @ComponentGroup NamedGroup[] group) {
            return new ContextProvider(dependency1, dependency2, group);
        }
    }

    @Component(automatic = false)
    private static class FactoryDelegatingFactory1 implements ComponentFactory {

        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws Exception {
            final Method method = ContextProviderFactory.class.getMethod("factoryMethod", NamedComponent.class, NamedComponent.class, NamedGroup[].class);

            dependencies.discover(method);

            final Container container = dependencies.local(null, new Container.Bindings() {
                public void bindComponents(final Container.Registry registry) {
                    registry.bindComponent(NamedGroupMember3.class);
                    registry.bindComponent(NamedComponent.class);
                }
            });

            final Dependency<?>[] resolved = container.resolve(ContextProvider.class, method);

            final Field field1 = SuperClass.class.getDeclaredField("field3");
            final Field field2 = SuperClass.class.getDeclaredField("field2");

            final Dependency<NamedComponent> dependency4 = container.resolve(NamedComponent.class, ContextProvider.class, field1);
            final Dependency<NamedGroup[]> dependency5 = dependencies.resolve(NamedGroup[].class, ContextProvider.class, field2);

            return new Instance() {
                public void bind(final Registry registry) throws Exception {
                    final ContextProvider instance = (ContextProvider) method.invoke(null, dependencies.instantiate(resolved));

                    final NamedComponent value1 = dependency4.instance();
                    assert value1 != null;

                    field1.setAccessible(true);
                    field1.set(instance, value1);

                    final NamedGroup[] value2 = dependency5.instance();
                    assert value2 != null;

                    field2.setAccessible(true);
                    field2.set(instance, value2);

                    registry.bindInstance(instance);
                }
            };

        }
    }

    @Component(automatic = false)
    private static class FactoryDelegatingFactory2 implements ComponentFactory {

        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws Exception {
            final Method method = ContextProviderFactory.class.getMethod("factoryMethod", NamedComponent.class, NamedComponent.class, NamedGroup[].class);

            dependencies.discover(method);

            final Container container = dependencies.local(null, new Container.Bindings() {
                public void bindComponents(final Container.Registry registry) {
                    registry.bindComponent(NamedGroupMember3.class);
                    registry.bindComponent(NamedComponent.class);
                }
            });

            final Dependency<NamedComponent> dependency1 = container.resolve(NamedComponent.class, ContextProvider.class, method, 0);
            final Dependency<NamedComponent> dependency2 = container.resolve(NamedComponent.class, ContextProvider.class, method, 1);
            final Dependency<NamedGroup[]> dependency3 = container.resolve(NamedGroup[].class, ContextProvider.class, method, 2);

            final Field field1 = SuperClass.class.getDeclaredField("field3");
            final Field field2 = SuperClass.class.getDeclaredField("field2");

            final Dependency<NamedComponent> dependency4 = container.resolve(NamedComponent.class, ContextProvider.class, field1);
            final Dependency<NamedGroup[]> dependency5 = dependencies.resolve(NamedGroup[].class, ContextProvider.class, field2);

            return new Instance() {
                public void bind(final Registry registry) throws Exception {
                    final ContextProvider instance = ContextProviderFactory.factoryMethod(dependency1.instance(), dependency2.instance(), dependency3.instance());

                    final NamedComponent value1 = dependency4.instance();
                    assert value1 != null;

                    field1.setAccessible(true);
                    field1.set(instance, value1);

                    final NamedGroup[] value2 = dependency5.instance();
                    assert value2 != null;

                    field2.setAccessible(true);
                    field2.set(instance, value2);

                    registry.bindInstance(instance);
                }
            };

        }
    }

    @Component(api = DependentKey.class, automatic = false)
    private static class DependentFactory implements ComponentFactory {

        public static ComponentFactory delegate;

        public DependentFactory(final FactoryDependency dependency) {
            assert dependency != null;
        }

        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws Exception {
            assert delegate != null;
            final Instance instance = delegate.resolve(context, dependencies);

            return new Instance() {
                public void bind(final Registry registry) throws Exception {
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

        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws Exception {
            assert delegate != null;
            final Instance instance = delegate.resolve(context, dependencies);

            return new Instance() {
                public void bind(final Registry registry) throws Exception {
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

        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws Exception {
            assert delegate != null;
            final Instance instance = delegate.resolve(context, dependencies);

            return new Instance() {
                public void bind(final Registry registry) throws Exception {
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

            final ComponentFactory.Dependency<?> dependency = resolver.resolve(checkKey, (Type) null, null);
            assert dependency != null && dependency.instance() == checkValue : "Container does not check up";

            return instance;
        }
    }

    @Component(automatic = false)
    private static class DynamicComponent1 {

        private DynamicComponent1(final ComponentContainer container) {
            assert container != null : ComponentContainer.class;
            container.instantiate(KeyCheck.class);
        }
    }

    @Component(api = DynamicComponent1.class)
    private static class DynamicFactory1 implements ComponentFactory {

        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws Exception {
            final Dependency<?>[] args = dependencies.discover(DynamicComponent1.class);

            return new Instance() {
                public void bind(final Registry registry) throws Exception {
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
            return container.instantiate(KeyCheck.class).key;
        }
    }

    @Component(api = DynamicComponent2.class)
    private static class DynamicFactory2 implements ComponentFactory {

        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws Exception {
            final Dependency<?>[] args = dependencies.discover(DynamicComponent1.class);

            return new Instance() {
                public void bind(final Registry registry) throws Exception {
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

        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws Exception {
            assert context.defines(Component.Reference.class);
            assert context.annotations(Component.Reference.class) != null;

            final Component.Reference annotation = context.annotation(Component.Reference.class, getClass());
            final Type reference = annotation.type();
            final Class<?> parameter = annotation.parameter(0);

            assert parameter != null : reference;
            assert Generics.rawType(reference) == CustomDependency.class : Generics.rawType(reference);
            assert CustomParameter.class.isAssignableFrom(parameter) : Generics.typeParameter(reference, 0);

            return new Instance() {
                public void bind(final Registry registry) throws Exception {
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

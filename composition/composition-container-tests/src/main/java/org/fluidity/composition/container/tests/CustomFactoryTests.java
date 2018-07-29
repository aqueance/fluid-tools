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

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Objects;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.Inject;
import org.fluidity.composition.Optional;
import org.fluidity.composition.Qualifier;
import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.composition.spi.Dependency;
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

    CustomFactoryTests(final ArtifactFactory factory) {
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

        EasyMock.expect(factory.resolve(EasyMock.notNull(), EasyMock.notNull()))
                .andAnswer(new FactoryInvocation(Check.class, check, instance))
                .anyTimes();
        instance.bind(EasyMock.notNull());
        EasyMock.expectLastCall().anyTimes();

        verify(() -> verifyComponent(container));
    }

    @Test
    public void invokesStandaloneFactoryClassOnceInLinkedContainer() throws Exception {
        registry.bindComponent(Value.class);
        final ComponentContainer.Registry child = registry.isolateComponent(DependentFactory.class);

        final Check check = new Check();

        child.bindComponent(FactoryDependency.class);
        child.bindInstance(check);

        EasyMock.expect(factory.resolve(EasyMock.notNull(), EasyMock.notNull()))
                .andAnswer(new FactoryInvocation(Check.class, check, instance))
                .anyTimes();
        instance.bind(EasyMock.notNull());
        EasyMock.expectLastCall().anyTimes();

        verify(() -> verifyComponent(container));
    }

    @Test
    public void circularFactoryInvocation() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(DependentFactory.class);
        registry.bindComponent(FactoryDependency.class);

        final Check check = new Check();

        registry.bindInstance(check);

        EasyMock.expect(factory.resolve(EasyMock.notNull(), EasyMock.notNull())).andReturn(instance).anyTimes();
        instance.bind(EasyMock.notNull());
        EasyMock.expectLastCall().anyTimes();

        verify(() -> verifyComponent(container));
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
        EasyMock.expect(factory.resolve(EasyMock.notNull(), EasyMock.notNull())).andReturn(instance).anyTimes();
        instance.bind(EasyMock.notNull());
        EasyMock.expectLastCall().times(factories);

        final GroupApi[] group = verify(() -> container.getComponentGroup(GroupApi.class));

        assert group != null : GroupApi.class;
        assert group.length == 2 : group.length;
        assert group[0] != group[1];
    }

    @Test(expectedExceptions = ComponentContainer.ResolutionException.class, expectedExceptionsMessageRegExp = ".*[Dd]ynamic.*Factory.*")
    public void testRestrictedContainer1() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(DependentValue.class);
        registry.bindComponent(DynamicFactory1.class);

        verify((Task) () -> container.getComponent(DynamicComponent1.class));

        assert false : "Dynamic dependency should have been prevented";
    }

    @Test
    public void testRestrictedContainer2() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(DependentValue.class);
        registry.bindComponent(DynamicFactory2.class);

        final DynamicComponent2 component = verify(() -> container.getComponent(DynamicComponent2.class));

        assert component.key() != null;
    }

    @Test
    public void testTypeReference() throws Exception {
        registry.bindComponent(CustomDependent.class);
        registry.bindComponent(CustomDependencyFactory.class);

        final CustomDependent component = verify(() -> container.getComponent(CustomDependent.class));

        assert component != null;
    }

    @Test(expectedExceptions = ComponentContainer.BindingException.class, expectedExceptionsMessageRegExp = ".*BadFactory.*DependentKey.*")
    public void testBadFactory() throws Exception {
        registry.bindComponent(BadFactory.class);
        container.getComponent(DependentKey.class);
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

        final ContextProvider component = verify(() -> container.getComponent(ContextProvider.class));

        assert component != null;
        assert Objects.equals(component.name1(), "name-1") : component.name1();
        assert Objects.equals(component.name2(), "name-2") : component.name2();
        assert Objects.equals(component.name3(), "name-5") : component.name3();

        final NamedGroup[] group = component.group();
        assert group != null;
        assert group.length == 3 : group.length;

        for (final NamedGroup member : group) {
            assert Objects.equals(member.name(), "name-3") : String.format("%s: %s", member.getClass(), member.name());
        }

        final NamedGroup[] field1 = component.field1();
        assert field1 != null;
        assert field1.length == 2 : field1.length;

        for (final NamedGroup member : field1) {
            assert Objects.equals(member.name(), "name-4") : String.format("%s: %s", member.getClass(), member.name());
        }

        final NamedGroup[] field2 = component.field2();
        assert field2 != null;
        assert field2.length == 2 : field2.length;

        for (final NamedGroup member : field2) {
            assert Objects.equals(member.name(), "name-6") : String.format("%s: %s", member.getClass(), member.name());
        }
    }

    @Test
    public void testResolver() throws Exception {

        @Component(automatic = false)
        class Main { }

        @Component(api = Secondary.class, automatic = false)
        @Component.Qualifiers(Component.Reference.class)
        class Secondary implements Serializable {
            public Secondary(final ComponentContext context) {
                assert context.qualifier(Component.Reference.class, null).type() == Secondary.class : context;
            }
        }

        @Component(api = Main.class, automatic = false)
        class Factory implements ComponentFactory {
            public Instance resolve(final ComponentContext context, final Container dependencies) throws Exception {
                final Resolver container = dependencies.resolver(Main.class);

                final Dependency<?> dependency = container.lookup(Secondary.class);
                assert dependency != null : Secondary.class;

                return Instance.of(Main.class, registry -> {
                    registry.bindInstance(dependency.instance());
                    registry.bindComponent(Main.class);
                });
            }
        }

        registry.bindInstance(this, CustomFactoryTests.class);
        registry.bindInstance(1234);
        registry.bindComponent(Secondary.class);
        registry.bindComponent(Factory.class);

        assert verify(() -> container.getComponent(Main.class)) != null;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Qualifier(Qualifier.Composition.IMMEDIATE)
    public @interface Name {

        String value();
    }

    private static class SuperClass {

        @Inject
        @Name("name-4")
        @ComponentGroup NamedGroup[] field1;

        @Inject
        @Name("name-6")
        @ComponentGroup NamedGroup[] field2;

        @Inject
        @Name("name-5")
        NamedComponent field3;
    }

    @Component(automatic = false)
    @Name("name-1")
    private static class ContextProvider extends SuperClass {

        private final NamedComponent dependency1;
        private final NamedComponent dependency2;
        private final NamedGroup[] group;

        ContextProvider(final NamedComponent dependency1,
                        final @Name("name-2") NamedComponent dependency2,
                        final @Name("name-3") @ComponentGroup NamedGroup[] group) {
            this.dependency1 = dependency1;
            this.dependency2 = dependency2;
            this.group = group;
        }

        String name1() {
            return dependency1.name;
        }

        String name2() {
            return dependency2.name;
        }

        String name3() {
            return field3.name;
        }

        NamedGroup[] field1() {
            return field1;
        }

        NamedGroup[] field2() {
            return field2;
        }

        public NamedGroup[] group() {
            return group;
        }
    }

    @Component(automatic = false)
    @Component.Qualifiers(Name.class)
    private static class NamedComponent {

        final String name;

        @SuppressWarnings("UnusedDeclaration")
        private NamedComponent(final ComponentContext context) {
            this.name = context.qualifier(Name.class, NamedComponent.class).value();
        }
    }

    @ComponentGroup
    private interface NamedGroup {

        String name();
    }

    private static abstract class NamedGroupMember implements NamedGroup {

        private final String name;

        NamedGroupMember(final ComponentContext context) {
            this.name = context.qualifier(Name.class, getClass()).value();
        }

        public String name() {
            return name;
        }
    }

    @Component.Qualifiers(Name.class)
    private final static class NamedGroupMember1 extends NamedGroupMember {

        NamedGroupMember1(final ComponentContext context) {
            super(context);
        }
    }

    @Component.Qualifiers(Name.class)
    private final static class NamedGroupMember2 extends NamedGroupMember {

        NamedGroupMember2(final ComponentContext context) {
            super(context);
        }
    }

    @Component.Qualifiers(Name.class)
    private final static class NamedGroupMember3 extends NamedGroupMember {

        NamedGroupMember3(final ComponentContext context) {
            super(context);
        }
    }

    @Component(automatic = false)
    private static class ConstructorDelegatingFactory1 implements ComponentFactory {

        public Instance resolve(final ComponentContext context, final Container dependencies) throws Exception {
            final Resolver container = dependencies.resolver(ContextProvider.class, registry -> {
                registry.bindComponent(NamedGroupMember3.class);
                registry.bindComponent(NamedComponent.class);
            });

            final Constructor<?> constructor = container.constructor(ContextProvider.class);

            container.discover(constructor);

            final Dependency<?>[] resolved = container.resolve(constructor);

            final Field field1 = SuperClass.class.getDeclaredField("field3");
            final Field field2 = SuperClass.class.getDeclaredField("field2");

            final Dependency<NamedComponent> dependency4 = (Dependency<NamedComponent>) container.resolve(null, field1);

            final Resolver dummy = dependencies.resolver(ContextProvider.class);
            final Dependency<NamedGroup[]> dependency5 = (Dependency<NamedGroup[]>) dummy.resolve(null, field2);

            return Instance.of(ContextProvider.class, registry -> {
                final ContextProvider instance = (ContextProvider) dummy.invoke(constructor, resolved);

                final NamedComponent value1 = dependency4.instance();
                assert value1 != null;

                field1.setAccessible(true);
                field1.set(instance, value1);

                final NamedGroup[] value2 = dependency5.instance();
                assert value2 != null;

                field2.setAccessible(true);
                field2.set(instance, value2);

                registry.bindInstance(instance);
            });

        }
    }

    @Component(automatic = false)
    private static class ConstructorDelegatingFactory2 implements ComponentFactory {

        public Instance resolve(final ComponentContext context, final Container dependencies) throws Exception {
            final Resolver container = dependencies.resolver(ContextProvider.class, registry -> {
                registry.bindComponent(NamedGroupMember3.class);
                registry.bindComponent(NamedComponent.class);
            });

            final Constructor<?> constructor = container.constructor(ContextProvider.class);

            container.discover(constructor);

            final Dependency<NamedComponent> dependency1 = (Dependency<NamedComponent>) container.resolve(constructor, 0);
            final Dependency<NamedComponent> dependency2 = (Dependency<NamedComponent>) container.resolve(constructor, 1);
            final Dependency<NamedGroup[]> dependency3 = (Dependency<NamedGroup[]>) container.resolve(constructor, 2);

            final Field field1 = SuperClass.class.getDeclaredField("field3");
            final Field field2 = SuperClass.class.getDeclaredField("field2");

            final Dependency<NamedComponent> dependency4 = (Dependency<NamedComponent>) container.resolve(null, field1);

            final Resolver dummy = dependencies.resolver(ContextProvider.class);
            final Dependency<NamedGroup[]> dependency5 = (Dependency<NamedGroup[]>) dummy.resolve(null, field2);

            return Instance.of(ContextProvider.class, registry -> {
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
            });
        }
    }

    private static class ContextProviderFactory {

        @Name("name-1")
        @SuppressWarnings("UnusedDeclaration")
        static ContextProvider factoryMethod(final @Optional NamedComponent dependency1,
                                             final @Optional @Name("name-2") NamedComponent dependency2,
                                             final @Optional @Name("name-3") @ComponentGroup NamedGroup[] group) {
            return new ContextProvider(dependency1, dependency2, group);
        }
    }

    @Component(automatic = false)
    private static class FactoryDelegatingFactory1 implements ComponentFactory {

        @SuppressWarnings("JavaReflectionInvocation")
        public Instance resolve(final ComponentContext context, final Container dependencies) throws Exception {
            final Resolver container = dependencies.resolver(null, registry -> {
                registry.bindComponent(NamedGroupMember3.class);
                registry.bindComponent(NamedComponent.class);
            });

            final Method method = ContextProviderFactory.class.getDeclaredMethod("factoryMethod", NamedComponent.class, NamedComponent.class, NamedGroup[].class);

            container.discover(ContextProviderFactory.class, method);

            final Dependency<?>[] resolved = container.resolve(ContextProvider.class, method);

            final Field field1 = SuperClass.class.getDeclaredField("field3");
            final Field field2 = SuperClass.class.getDeclaredField("field2");

            final Dependency<NamedComponent> dependency4 = (Dependency<NamedComponent>) container.resolve(ContextProvider.class, field1);

            final Resolver dummy = dependencies.resolver(ContextProvider.class);
            final Dependency<NamedGroup[]> dependency5 = (Dependency<NamedGroup[]>) dummy.resolve(ContextProvider.class, field2);

            return Instance.of(ContextProvider.class, registry -> {
                final ContextProvider instance = (ContextProvider) method.invoke(null, dummy.instantiate(resolved));

                final NamedComponent value1 = dependency4.instance();
                assert value1 != null;

                field1.setAccessible(true);
                field1.set(instance, value1);

                final NamedGroup[] value2 = dependency5.instance();
                assert value2 != null;

                field2.setAccessible(true);
                field2.set(instance, value2);

                registry.bindInstance(instance);
            });
        }
    }

    @Component(automatic = false)
    private static class FactoryDelegatingFactory2 implements ComponentFactory {

        public Instance resolve(final ComponentContext context, final Container dependencies) throws Exception {
            final Resolver container = dependencies.resolver(null, registry -> {
                registry.bindComponent(NamedGroupMember3.class);
                registry.bindComponent(NamedComponent.class);
            });

            final Method method = ContextProviderFactory.class.getDeclaredMethod("factoryMethod", NamedComponent.class, NamedComponent.class, NamedGroup[].class);

            dependencies.resolver().discover(ContextProviderFactory.class, method);

            final Dependency<NamedComponent> dependency1 = (Dependency<NamedComponent>) container.resolve(null, method, 0);
            final Dependency<NamedComponent> dependency2 = (Dependency<NamedComponent>) container.resolve(null, method, 1);
            final Dependency<NamedGroup[]> dependency3 = (Dependency<NamedGroup[]>) container.resolve(null, method, 2);

            final Field field1 = SuperClass.class.getDeclaredField("field3");
            final Field field2 = SuperClass.class.getDeclaredField("field2");

            final Dependency<NamedComponent> dependency4 = (Dependency<NamedComponent>) container.resolve(ContextProvider.class, field1);
            final Dependency<NamedGroup[]> dependency5 = (Dependency<NamedGroup[]>) dependencies.resolver().resolve(ContextProvider.class, field2);

            return Instance.of(ContextProvider.class, registry -> {
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
            });
        }
    }

    @Component(api = DependentKey.class, automatic = false)
    private static class DependentFactory implements ComponentFactory {

        static ComponentFactory delegate;

        public DependentFactory(final FactoryDependency dependency) {
            assert dependency != null;
        }

        public Instance resolve(final ComponentContext context, final Container dependencies) throws Exception {
            assert delegate != null;
            final Instance instance = delegate.resolve(context, dependencies);

            return Instance.of(DependentValue.class, registry -> {
                registry.bindComponent(DependentValue.class);

                assert instance != null;
                instance.bind(registry);
            });
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

        static ComponentFactory delegate;

        public Instance resolve(final ComponentContext context, final Container dependencies) throws Exception {
            assert delegate != null;
            final Instance instance = delegate.resolve(context, dependencies);

            return Instance.of(GroupMember1.class, registry -> {
                registry.bindComponent(GroupMember1.class);

                assert instance != null;
                instance.bind(registry);
            });
        }
    }

    @Component(api = GroupMember2Api.class, automatic = false)
    private static class GroupMember2Factory implements ComponentFactory {

        static ComponentFactory delegate;

        public Instance resolve(final ComponentContext context, final Container dependencies) throws Exception {
            assert delegate != null;
            final Instance instance = delegate.resolve(context, dependencies);

            return Instance.of(GroupMember2.class, registry -> {
                registry.bindComponent(GroupMember2.class);

                assert instance != null;
                instance.bind(registry);
            });
        }
    }

    private static class FactoryInvocation implements IAnswer<ComponentFactory.Instance> {

        private final Class<?> checkKey;
        private final Object checkValue;
        private final ComponentFactory.Instance instance;

        FactoryInvocation(final Class<?> checkKey, final Object checkValue, final ComponentFactory.Instance instance) {
            this.checkKey = checkKey;
            this.checkValue = checkValue;
            this.instance = instance;
        }

        public ComponentFactory.Instance answer() throws Throwable {
            final ComponentFactory.Container container = (ComponentFactory.Container) EasyMock.getCurrentArguments()[1];
            assert container != null : "Received no container";

            final ComponentFactory.Resolver resolver = container.resolver();
            assert resolver != null : "Received no resolver";

            final Dependency<?> dependency = resolver.lookup(checkKey);
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

        public Instance resolve(final ComponentContext context, final Container dependencies) throws Exception {
            final Dependency<?>[] arguments = dependencies.resolver().discover(DynamicComponent1.class);
            return Instance.of(DynamicComponent1.class, registry -> registry.bindInstance(new DynamicComponent1((ComponentContainer) arguments[0].instance())));
        }
    }

    @Component(automatic = false)
    private static class DynamicComponent2 {

        private final ComponentContainer container;

        private DynamicComponent2(final ComponentContainer container) {
            assert container != null : ComponentContainer.class;
            this.container = container;
        }

        Key key() {
            return container.instantiate(KeyCheck.class).key;
        }
    }

    @Component(api = DynamicComponent2.class)
    private static class DynamicFactory2 implements ComponentFactory {

        public Instance resolve(final ComponentContext context, final Container dependencies) throws Exception {
            final Dependency<?>[] arguments = dependencies.resolver().discover(DynamicComponent1.class);
            return Instance.of(DynamicComponent2.class, registry -> registry.bindInstance(new DynamicComponent2((ComponentContainer) arguments[0].instance())));
        }
    }

    private interface CustomParameter { }

    private interface CustomParameter1 extends CustomParameter { }

    private interface CustomParameter2 extends CustomParameter { }

    @SuppressWarnings("UnusedDeclaration")
    private static class CustomDependency<T> { }

    @Component(api = CustomDependency.class)
    @Component.Qualifiers(Component.Reference.class)
    private static class CustomDependencyFactory implements ComponentFactory {

        public Instance resolve(final ComponentContext context, final Container dependencies) throws Exception {
            assert context.defines(Component.Reference.class);
            assert context.qualifiers(Component.Reference.class) != null;

            final Component.Reference annotation = context.qualifier(Component.Reference.class, getClass());
            final Type reference = annotation.type();
            final Class<?> parameter = annotation.parameter(0);

            assert parameter != null : reference;
            assert Generics.rawType(reference) == CustomDependency.class : Generics.rawType(reference);
            assert CustomParameter.class.isAssignableFrom(parameter) : Generics.typeParameter(reference, 0);

            return Instance.of(CustomDependency.class, registry -> registry.bindInstance(new CustomDependency(), CustomDependency.class));
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

    @Component(api = DependentKey.class, automatic = false)
    private static class BadFactory implements ComponentFactory {

        public Instance resolve(final ComponentContext context, final Container dependencies) throws Exception {
            return Instance.of(DependentKey.class, registry -> {
                // empty
            });
        }
    }
}

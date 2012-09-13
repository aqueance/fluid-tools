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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.Inject;
import org.fluidity.composition.spi.ComponentFactory;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
public final class ComponentContextTests extends AbstractContainerTests {

    public ComponentContextTests(final ArtifactFactory factory) {
        super(factory);
    }

    @BeforeMethod
    public void setup() throws Exception {
        ContextConsumer1.context = null;
        ContextConsumer2.context = null;
    }

    @DataProvider(name = "component-types")
    public Object[][] componentTypes() {
        return new Object[][] {
                new Object[] { ContextAware1Impl.class },
                new Object[] { ContextAware2Impl.class },
                new Object[] { ContextAwareVariants1.class },
                new Object[] { ContextAwareVariants2.class },
        };
    }

    private static class TestDependent {

        public ContextAware dependency;
    }

    @Setting2("value1")
    private static class Test1 extends TestDependent {

        @SuppressWarnings("UnusedDeclaration")
        private Test1(@Setting1("value1") final ContextAware dependency) {
            this.dependency = dependency;
        }
    }

    @Setting1("value2")
    private static class Test2 extends TestDependent {

        @SuppressWarnings("UnusedDeclaration")
        private Test2(@Setting2("value2") final ContextAware dependency) {
            this.dependency = dependency;
        }
    }

    private static class Test3 extends TestDependent {

        @SuppressWarnings("UnusedDeclaration")
        private Test3(@Setting3("value") final ContextAware dependency) {
            this.dependency = dependency;
        }
    }

    @Test(dataProvider = "component-types")
    public <T extends ContextAware, F extends ComponentFactory> void testExplicitContext(final Class<? extends T> type) throws Exception {
        if (type != null) {
            registry.bindComponent(type);
        }

        registry.bindComponent(Test1.class);
        registry.bindComponent(Test2.class);
        registry.bindComponent(Test3.class);

        final ContextAware variant0 = container.getComponent(ContextAware.class);
        final ContextAware variant1 = container.getComponent(Test1.class).dependency;
        final ContextAware variant2 = container.getComponent(Test2.class).dependency;
        final ContextAware variant3 = container.getComponent(Test3.class).dependency;

        assert variant0 != null : "Container failed to create default instance";
        assert variant1 != null : "Container failed to create instance for valid context";
        assert variant2 != null : "Container failed to create instance for valid context";
        assert variant3 != null : "Container failed to create instance for invalid context";

        assert variant0 != variant1 : "Default context and variant context are the same";
        assert variant0 != variant2 : "Default context and variant context are the same";
        assert variant1 != variant2 : "Instances for two valid contexts are the same";
        assert variant0 == variant3 : "Default instance and instance for invalid context are not the same";

        assert variant0.setting().equals("missing-value") : "Context not properly set";
        assert variant1.setting().equals("value1") : "Context not properly set";
        assert variant2.setting().equals("value2") : "Context not properly set";
    }

    @Test
    public void testImplicitContext() throws Exception {
        registry.bindComponent(BaseComponent.class);
        registry.bindComponent(OverridingComponent.class);
        registry.bindComponent(ContextAwareComponent1Impl.class);
        registry.bindComponent(OrdinaryComponentVariants2.class);

        final BaseComponent root = container.getComponent(BaseComponent.class);

        final ContextAwareComponent1 context1 = root.getContextAware1();
        final OrdinaryComponent2 context21 = root.getOrdinary1();
        final OrdinaryComponent2 context22 = root.getOrdinary2();

        assert context1.setting().equals("value1");
        assert context21.setting().equals("value2-default") : "Component annotation was not picked up";
        assert context22.setting().equals("value2-overridden") : "Explicit context did not override component annotation";
    }

    @Test
    public void testContextInheritance() throws Exception {
        registry.bindComponent(ContextAwareComponent1Impl.class);
        registry.bindComponent(ContextAwareComponent2Impl.class);
        registry.bindComponent(OrdinaryComponentVariants1.class);
        registry.bindComponent(OrdinaryComponentVariants2.class);
        registry.bindComponent(InnocentBystanderComponent.class);
        registry.bindComponent(ContextSetterComponent1.class);
        registry.bindComponent(ContextSetterComponent2.class);
        registry.bindComponent(ContextFreeComponent.class);

        final ContextSetterComponent1 context1 = container.getComponent(ContextSetterComponent1.class);
        final ContextSetterComponent2 context2 = context1.getNext();
        final ContextFreeComponent contextFree = container.getComponent(ContextFreeComponent.class);

        final ContextAwareComponent1 context1dependency1 = context1.getContextAware();
        final OrdinaryComponent1 context1dependency2 = context1.getOrdinary();

        final ContextAwareComponent2 context2dependency1 = context2.getContextAware();
        final OrdinaryComponent2 context2dependency2 = context2.getOrdinary();

        assert container.getComponent(InnocentBystanderComponent.class) != context2.getInnocent() : "Context unaware intermediate component still a singleton";
        assert contextFree.getInnocent() != context2.getInnocent() : "Context unaware intermediate component still a singleton";

        assert context1dependency1.setting().equals("value1") : context1dependency1.setting();
        assert context1dependency2.setting().equals("value1") : context1dependency2.setting();
        assert context2dependency1.setting().equals("value2-context") : context2dependency1.setting();
        assert context2dependency2.setting().equals("value2-ordinary") : context2dependency2.setting();

        assert context2.getInnocent().getContextAware().setting().equals("value1") : "Context not passed through context unaware component";
        assert context2.getInnocent().getOrdinary().setting().equals("missing-value") : "Invalid context passed through context unaware component";

        assert contextFree.getInnocent().getContextAware().setting().equals("missing-value") : "Invalid context passed through context unaware component";
        assert contextFree.getInnocent().getOrdinary().setting().equals("missing-value") : "Invalid context passed through context unaware component";
    }

    @Test
    public void testEmbeddedContext() throws Exception {
        registry.bindComponent(ContextProvider1.class);
        registry.bindComponent(ContextConsumer1.class);

        final ContextProvider1 provider = container.getComponent(ContextProvider1.class);
        assert provider != null : ContextProvider1.class;

        provider.init();    // container not accessible inside constructor

        final ComponentContext context = ContextConsumer1.context;
        assert context != null;

        final Setting1 setting1 = context.annotation(Setting1.class, null);
        final Setting2 setting2 = context.annotation(Setting2.class, null);

        assert setting1 != null;
        assert setting2 != null;

        // values come from ContextProvider1
        assert "setting-1".equals(setting1.value()) : setting1.value();
        assert "setting-2".equals(setting2.value()) : setting2.value();
    }

    @Test
    public void testFieldContext() throws Exception {
        registry.bindComponent(ContextProvider2.class);
        registry.bindComponent(ContextConsumer2.class);

        assert container.getComponent(ContextProvider2.class) != null : ContextProvider2.class;

        final ComponentContext context = ContextConsumer2.context;
        assert context != null;

        final Setting1 setting1 = context.annotation(Setting1.class, null);
        final Setting3 setting3 = context.annotation(Setting3.class, null);

        assert setting1 != null;
        assert setting3 != null;

        // values come from ContextProvider2
        assert "setting-1".equals(setting1.value()) : setting1.value();
        assert "setting-3".equals(setting3.value()) : setting3.value();
    }

    @Test
    public void testGroupMembers() throws Exception {
        registry.bindComponent(GroupMember1.class);
        registry.bindComponent(GroupMember2.class);
        registry.bindComponent(GroupMember3.class);
        registry.bindComponent(GroupDependent1.class);
        registry.bindComponent(GroupDependent2.class);

        GroupDependent1 dependent1 = container.getComponent(GroupDependent1.class);
        GroupDependent2 dependent2 = container.getComponent(GroupDependent2.class);

        assert dependent1 != null;
        assert dependent2 != null;

        assert dependent1.group.size() == 3;
        assert dependent2.group.size() == 3;

        final List<Class<? extends GroupApi>> expected = Arrays.asList(GroupMember1.class, GroupMember2.class, GroupMember3.class);
        final List<Class<? extends GroupApi>> group1 = new ArrayList<Class<? extends GroupApi>>();
        final List<Class<? extends GroupApi>> group2 = new ArrayList<Class<? extends GroupApi>>();

        for (final GroupApi member : dependent1.group) {
            group1.add(member.getClass());

            if (member instanceof GroupMember2) {
                assert GroupDependent1.class.getAnnotation(Setting1.class).value()
                        .equals(((GroupMember2) member).setting) : ((GroupMember2) member).setting;
            }
        }

        for (final GroupApi member : dependent2.group) {
            group2.add(member.getClass());

            if (member instanceof GroupMember2) {
                assert ((Setting1) GroupDependent2.class.getConstructor(GroupApi[].class).getParameterAnnotations()[0][0]).value()
                        .equals(((GroupMember2) member).setting) : ((GroupMember2) member).setting;
            }
        }

        assert expected.equals(group1) : group1;
        assert expected.equals(group2) : group2;
    }

    @Test
    public void testInstantiatingFactory() throws Exception {
        registry.bindComponent(FirstComponent.class);
        registry.bindComponent(ThirdFactory.class);
        registry.bindComponent(SecondFactory.class);

        final FirstComponent first = container.getComponent(FirstComponent.class);
        assert first != null;
        assertContext(first.settings1);
        assertContext(first.settings2);
        assertContext(first.settings3);

        final SecondComponent second = first.dependency;
        assert second != null;
        assertContext(second.settings1);
        assertContext(second.settings2, "second");
        assertContext(second.settings3);

        final ThirdComponent third = second.dependency;
        assert third != null;
        assertContext(third.settings1, "first");
        assertContext(third.settings2);
        assertContext(third.settings3, "third");
    }

    @Test
    public void testIgnoredContext() throws Exception {
        registry.bindComponent(ContextProvider3.class);
        registry.bindComponent(ContextProvider4.class);
        registry.bindComponent(ContextConsumer1.class);
        registry.bindComponent(ContextConsumer2.class);

        final ContextProvider3 root = container.getComponent(ContextProvider3.class);
        assert root != null : ContextProvider3.class;

        final ComponentContext context1 = root.provider.consumer1.local;
        final ComponentContext context2 = root.provider.consumer2.local;
        final ComponentContext context3 = root.provider.consumer3.local;
        final ComponentContext context4 = root.provider.consumer4.local;
        assert context1 != null;
        assert context2 != null;
        assert context3 != null;
        assert context4 != null;

        final Setting1 setting11 = context1.annotation(Setting1.class, null);
        final Setting1 setting21 = context2.annotation(Setting1.class, null);
        final Setting2 setting12 = context1.annotation(Setting2.class, null);
        final Setting2 setting22 = context3.annotation(Setting2.class, null);
        final Setting3 setting13 = context2.annotation(Setting3.class, null);
        final Setting3 setting23 = context4.annotation(Setting3.class, null);

        assert setting11 == null;       // @Setting1 is ignored and not redefined
        assert setting21 == null;       // @Setting1 is ignored and not redefined
        assert setting12 == null;       // @Setting2 is ignored and not redefined
        assert setting22 != null;       // @Setting2 is ignored and then redefined
        assert setting13 != null;       // @Setting3 is ignored and then redefined
        assert setting23 != null;       // @Setting3 is ignored and then redefined

        assert "setting-2-n".equals(setting22.value()) : setting22.value();
        assert "setting-3-n".equals(setting23.value()) : setting23.value();
    }

    @ComponentGroup
    public interface GroupApi { }

    private static class GroupMember1 implements GroupApi { }

    @SuppressWarnings("UnusedDeclaration")
    @Component.Context(Setting1.class)
    private static class GroupMember2 implements GroupApi {

        public final String setting;

        private GroupMember2(final ComponentContext context) {
            final Setting1 annotation = context.annotation(Setting1.class, null);
            setting = annotation == null ? null : annotation.value();
        }
    }

    private static class GroupMember3 implements GroupApi { }

    @Setting1("context-1")
    private static class GroupDependent1 {

        public final List<GroupApi> group;

        public GroupDependent1(final @ComponentGroup GroupApi[] group) {
            this.group = Arrays.asList(group);
        }
    }

    public static class GroupDependent2 {

        public final List<GroupApi> group;

        public GroupDependent2(final @Setting1("context-2") @ComponentGroup GroupApi[] group) {
            this.group = Arrays.asList(group);
        }
    }

    @Setting1("first")
    private static class FirstComponent {

        public final Setting1[] settings1;
        public final Setting2[] settings2;
        public final Setting3[] settings3;

        public final SecondComponent dependency;

        @SuppressWarnings("UnusedParameters")
        public FirstComponent(final ThirdComponent cached, final @Setting2("second") SecondComponent second, final ComponentContext context) {
            settings1 = context.annotations(Setting1.class);
            settings2 = context.annotations(Setting2.class);
            settings3 = context.annotations(Setting3.class);
            dependency = second;
        }
    }

    private static class SecondComponent {

        public final Setting1[] settings1;
        public final Setting2[] settings2;
        public final Setting3[] settings3;

        public final ThirdComponent dependency;

        private SecondComponent(final @Setting3("third") ThirdComponent dependency,
                                final Setting1[] settings1,
                                final Setting2[] settings2,
                                final Setting3[] settings3) {
            this.settings1 = settings1;
            this.settings2 = settings2;
            this.settings3 = settings3;
            this.dependency = dependency;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class ThirdComponent {

        public final Setting1[] settings1;
        public final Setting2[] settings2;
        public final Setting3[] settings3;

        private ThirdComponent(final ComponentContext context) {
            settings1 = context.annotations(Setting1.class);
            settings2 = context.annotations(Setting2.class);
            settings3 = context.annotations(Setting3.class);
        }
    }

    @Component(api = SecondComponent.class)
    @Component.Context(Setting2.class)
    private static final class SecondFactory implements ComponentFactory {
        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws Exception {
            final Dependency<?>[] args = dependencies.discover(SecondComponent.class);

            return new Instance() {
                public void bind(final Registry registry) throws Exception {

                    // direct instantiation to bypass the container when instantiating ThirdComponent
                    registry.bindInstance(new SecondComponent((ThirdComponent) args[0].instance(),
                                                              context.annotations(Setting1.class),
                                                              context.annotations(Setting2.class),
                                                              context.annotations(Setting3.class)));
                }
            };
        }
    }

    @Component(api = ThirdComponent.class)
    @Component.Context({ Setting1.class, Setting3.class })
    private static final class ThirdFactory implements ComponentFactory {
        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws Exception {
            dependencies.discover(ThirdComponent.class);

            return new Instance() {
                public void bind(final Registry registry) throws Exception {

                    // direct instantiation to bypass the container
                    registry.bindInstance(new ThirdComponent(context));
                }
            };
        }
    }

    private void assertContext(final Object[] settings, final String... expected) throws Exception {
        final List<String> actual = new ArrayList<String>();

        if (settings != null) {
            for (final Object setting : settings) {
                actual.add((String) setting.getClass().getDeclaredMethod("value").invoke(setting));
            }
        }

        final List<String> got = Arrays.asList(expected);
        assert actual.equals(got) : String.format("Expected %s, got %s", got, actual);
    }

    private interface ContextAware {

        String setting();

        public interface Settings {

            String setting();
        }
    }

    private interface ContextAwareComponent1 extends ContextAware {

    }

    private interface ContextAwareComponent2 extends ContextAware {

    }

    private interface OrdinaryComponent1 extends ContextAware {

    }

    private interface OrdinaryComponent2 extends ContextAware {

    }

    @Component.Context(Setting1.class)
    @Component(automatic = false)
    private static class ContextAware1Impl implements ContextAware {

        private final String setting;

        public ContextAware1Impl(final ComponentContext context) {
            final Setting1 setting = context.annotation(Setting1.class, null);
            final String value = setting == null ? null : setting.value();
            this.setting = value == null ? "missing-value" : value;
        }

        public String setting() {
            return setting;
        }
    }

    @Component.Context(Setting2.class)
    @Component(automatic = false)
    private static class ContextAware2Impl implements ContextAware {

        private final String setting;

        public ContextAware2Impl(final ComponentContext context) {
            final Setting2 setting = context.annotation(Setting2.class, null);
            final String value = setting == null ? null : setting.value();
            this.setting = value == null ? "missing-value" : value;
        }

        public String setting() {
            return setting;
        }
    }

    @Component.Context(Setting1.class)
    @Component(automatic = false)
    private static class ContextAwareComponent1Impl implements ContextAwareComponent1 {

        private final String setting;

        public ContextAwareComponent1Impl(final ComponentContext context) {
            final Setting1 setting = context.annotation(Setting1.class, null);
            final String value = setting == null ? null : setting.value();
            this.setting = value == null ? "missing-value" : value;
        }

        public String setting() {
            return setting;
        }
    }

    @Component.Context(Setting2.class)
    @Component(automatic = false)
    private static class ContextAwareComponent2Impl implements ContextAwareComponent2 {

        private final String setting;

        public ContextAwareComponent2Impl(final ComponentContext context) {
            final Setting2 setting = context.annotation(Setting2.class, null);
            final String value = setting == null ? null : setting.value();
            this.setting = value == null ? "missing-value" : value;
        }

        public String setting() {
            return setting;
        }
    }

    @Component(automatic = false)
    private static class OrdinaryComponent1Impl implements OrdinaryComponent1 {

        private final String setting;

        public OrdinaryComponent1Impl(final Settings settings) {
            this.setting = settings.setting();
        }

        public String setting() {
            return setting;
        }
    }

    @Component(automatic = false)
    private static class OrdinaryComponent2Impl implements OrdinaryComponent2 {

        private final String setting;

        public OrdinaryComponent2Impl(final Settings settings) {
            this.setting = settings.setting();
        }

        public String setting() {
            return setting;
        }
    }

    @Component(automatic = false)
    private static class NotContextAware1Impl implements ContextAware {

        private final String setting;

        public NotContextAware1Impl(final Settings settings) {
            this.setting = settings.setting();
        }

        public String setting() {
            return setting;
        }
    }

    @Component(automatic = false)
    private static class NotContextAware2Impl implements ContextAware {

        private final String setting;

        public NotContextAware2Impl(final Settings settings) {
            this.setting = settings.setting();
        }

        public String setting() {
            return setting;
        }
    }

    @Component(api = ContextAware.class, automatic = false)
    @Component.Context(Setting1.class)
    private static class ContextAwareVariants1 implements ComponentFactory {
        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws Exception {
            return new Instance() {
                public void bind(final Registry registry) throws Exception {
                    registry.bindComponent(NotContextAware1Impl.class);
                    registry.bindInstance(new ContextAware.Settings() {
                        public String setting() {
                            final Setting1 setting = context.annotation(Setting1.class, null);
                            final String value = setting == null ? null : setting.value();
                            return value == null ? "missing-value" : value;
                        }
                    });
                }
            };
        }
    }

    @Component(api = ContextAware.class, automatic = false)
    @Component.Context(Setting2.class)
    private static class ContextAwareVariants2 implements ComponentFactory {
        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws Exception {
            return new Instance() {
                public void bind(final Registry registry) throws Exception {
                    registry.bindComponent(NotContextAware2Impl.class);
                    registry.bindInstance(new ContextAware.Settings() {
                        public String setting() {
                            final Setting2 setting = context.annotation(Setting2.class, null);
                            final String value = setting == null ? null : setting.value();
                            return value == null ? "missing-value" : value;
                        }
                    });
                }
            };
        }
    }

    @Component(api = OrdinaryComponent1.class, automatic = false)
    @Component.Context(Setting1.class)
    private static class OrdinaryComponentVariants1 implements ComponentFactory {
        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws Exception {
            return new Instance() {
                public void bind(final Registry registry) throws Exception {
                    registry.bindComponent(OrdinaryComponent1Impl.class);
                    registry.bindInstance(new ContextAware.Settings() {
                        public String setting() {
                            final Setting1 setting = context.annotation(Setting1.class, null);
                            final String value = setting == null ? null : setting.value();
                            return value == null ? "missing-value" : value;
                        }
                    });
                }
            };
        }
    }

    @Component(api = OrdinaryComponent2.class, automatic = false)
    @Component.Context(Setting2.class)
    private static class OrdinaryComponentVariants2 implements ComponentFactory {
        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws Exception {
            return new Instance() {
                public void bind(final Registry registry) throws Exception {
                    registry.bindComponent(OrdinaryComponent2Impl.class);
                    registry.bindInstance(new ContextAware.Settings() {
                        public String setting() {
                            final Setting2 setting = context.annotation(Setting2.class, null);
                            final String value = setting == null ? null : setting.value();
                            return value == null ? "missing-value" : value;
                        }
                    });
                }
            };
        }
    }

    @Component(automatic = false)
    private static class InnocentBystanderComponent {

        final ContextAwareComponent1 contextAware;
        final OrdinaryComponent2 ordinary;

        public InnocentBystanderComponent(final ContextAwareComponent1 contextAware, final OrdinaryComponent2 ordinary) {
            this.contextAware = contextAware;
            this.ordinary = ordinary;
        }

        public ContextAwareComponent1 getContextAware() {
            return contextAware;
        }

        public OrdinaryComponent2 getOrdinary() {
            return ordinary;
        }
    }

    @Component(automatic = false)
    @Setting1("value1")
    private static class ContextSetterComponent1 {

        final ContextSetterComponent2 next;
        final ContextAwareComponent1 contextAware;
        final OrdinaryComponent1 ordinary;

        public ContextSetterComponent1(final ContextSetterComponent2 next, final ContextAwareComponent1 contextAware, final OrdinaryComponent1 ordinary) {
            this.next = next;
            this.contextAware = contextAware;
            this.ordinary = ordinary;
        }

        public ContextAwareComponent1 getContextAware() {
            return contextAware;
        }

        public OrdinaryComponent1 getOrdinary() {
            return ordinary;
        }

        public ContextSetterComponent2 getNext() {
            return next;
        }
    }

    @Component(automatic = false)
    private static class ContextSetterComponent2 {

        final InnocentBystanderComponent innocent;
        final ContextAwareComponent2 contextAware;
        final OrdinaryComponent2 ordinary;

        public ContextSetterComponent2(final InnocentBystanderComponent innocent,
                                       @Setting2("value2-context") final ContextAwareComponent2 contextAware,
                                       @Setting2("value2-ordinary") final OrdinaryComponent2 ordinary) {
            this.innocent = innocent;
            this.contextAware = contextAware;
            this.ordinary = ordinary;
        }

        public InnocentBystanderComponent getInnocent() {
            return innocent;
        }

        public ContextAwareComponent2 getContextAware() {
            return contextAware;
        }

        public OrdinaryComponent2 getOrdinary() {
            return ordinary;
        }
    }

    private static class ContextFreeComponent {

        private final InnocentBystanderComponent innocent;

        public ContextFreeComponent(final InnocentBystanderComponent innocent) {
            this.innocent = innocent;
        }

        public InnocentBystanderComponent getInnocent() {
            return innocent;
        }
    }

    @Setting1("value1")
    @Setting2("value2-default")
    private static class BaseComponent {

        private final ContextAwareComponent1 contextAware1;
        private final OrdinaryComponent2 ordinary1;
        private final OrdinaryComponent2 ordinary2;

        public BaseComponent(final ContextAwareComponent1 contextAware1, final OrdinaryComponent2 ordinary1, final OverridingComponent overriding) {
            this.contextAware1 = contextAware1;
            this.ordinary1 = ordinary1;
            this.ordinary2 = overriding.ordinary2;
        }

        public ContextAwareComponent1 getContextAware1() {
            return contextAware1;
        }

        public OrdinaryComponent2 getOrdinary1() {
            return ordinary1;
        }

        public OrdinaryComponent2 getOrdinary2() {
            return ordinary2;
        }
    }

    @Setting1("value1")
    @Setting2("value2-overridden")
    private static class OverridingComponent {

        public final OrdinaryComponent2 ordinary2;

        @SuppressWarnings("UnusedDeclaration")
        private OverridingComponent(final OrdinaryComponent2 ordinary2) {
            this.ordinary2 = ordinary2;
        }
    }

    @Setting1("setting-1")
    private static class ContextProvider1 {

        private final ComponentContainer container;

        public ContextProvider1(final @Setting2("setting-2") ComponentContainer container) {
            this.container = container;
        }

        public void init() {
            container.getComponent(ContextConsumer1.class);
        }
    }

    @Setting1("setting-1")
    private static class ContextProvider2 {

        @Inject
        @Setting3("setting-3")
        public ContextConsumer2 consumer;
    }

    @Setting1("setting-1")
    private static class ContextProvider3 {

        @Inject
        @Setting2("setting-2")
        public ContextProvider4 provider;
    }

    @Setting3("setting-3")
    @Component.Context(ignore = Setting1.class)
    private static class ContextProvider4 {

        @Inject
        @Component.Context(ignore = { Setting2.class, Setting3.class })
        public ContextConsumer1 consumer1;

        @Inject
        @Component.Context(ignore = { Setting2.class, Setting3.class })
        public ContextConsumer2 consumer2;

        @Inject
        @Setting2("setting-2-n")
        @Component.Context(ignore = { Setting2.class, Setting3.class })
        public ContextConsumer1 consumer3;

        @Inject
        @Component.Context(ignore = { Setting2.class, Setting3.class })
        @Setting3("setting-3-n")
        public ContextConsumer2 consumer4;
    }

    @SuppressWarnings("UnusedDeclaration")
    @Component.Context({ Setting1.class, Setting2.class })
    private static class ContextConsumer1 {

        public static ComponentContext context;
        public final ComponentContext local;

        private ContextConsumer1(final ComponentContext context) {
            ContextConsumer1.context = context;
            this.local = context;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @Component.Context({ Setting1.class, Setting3.class })
    private static class ContextConsumer2 {

        public static ComponentContext context;
        public final ComponentContext local;

        private ContextConsumer2(final ComponentContext context) {
            ContextConsumer2.context = context;
            this.local = context;
        }
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER })
    public static @interface Setting1 {

        String value();
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER })
    public static @interface Setting2 {

        String value();
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER })
    public static @interface Setting3 {

        @SuppressWarnings("UnusedDeclaration")
        String value();
    }

    // TODO: various context series
}

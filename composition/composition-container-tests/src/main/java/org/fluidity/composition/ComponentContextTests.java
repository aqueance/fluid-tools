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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.fluidity.composition.spi.ComponentVariantFactory;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
public final class ComponentContextTests extends AbstractContainerTests {

    public ComponentContextTests(final ContainerFactory factory) {
        super(factory);
    }

    @DataProvider(name = "component-types")
    public Object[][] componentTypes() {
        return new Object[][] {
                new Object[] { ContextAware1Impl.class, null },
                new Object[] { ContextAware2Impl.class, null },
                new Object[] { NotContextAware1Impl.class, ContextAwareVariants1.class },
                new Object[] { NotContextAware2Impl.class, ContextAwareVariants2.class },
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
    public <T extends ContextAware, F extends ComponentVariantFactory> void explicitContext(final Class<? extends T> type, final Class<? extends F> factory)
            throws Exception {
        registry.bindComponent(type);
        registry.bindComponent(Test1.class);
        registry.bindComponent(Test2.class);
        registry.bindComponent(Test3.class);

        if (factory != null) {
            registry.bindComponent(factory);
        }

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
    public void implicitContext() throws Exception {
        registry.bindComponent(BaseComponent.class);
        registry.bindComponent(OverridingComponent.class);
        registry.bindComponent(ContextAwareComponent1Impl.class);
        registry.bindComponent(OrdinaryComponent2Impl.class);
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
    public void contextInheritance() throws Exception {
        registry.bindComponent(ContextAwareComponent1Impl.class);
        registry.bindComponent(ContextAwareComponent2Impl.class);
        registry.bindComponent(OrdinaryComponent1Impl.class);
        registry.bindComponent(OrdinaryComponent2Impl.class);
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
    public void embeddedContext() throws Exception {
        registry.bindComponent(ContextProvider1.class);
        registry.bindComponent(ContextConsumer1.class);

        assert container.getComponent(ContextProvider1.class) != null : ContextProvider1.class;

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
    public void fieldContext() throws Exception {
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

    @ComponentGroup
    public static interface GroupApi {
    }

    private static class GroupMember1 implements GroupApi {}

    @SuppressWarnings("UnusedDeclaration")
    @Context(Setting1.class)
    private static class GroupMember2 implements GroupApi {
        public final String setting;

        private GroupMember2(final ComponentContext context) {
            final Setting1 annotation = context.annotation(Setting1.class, null);
            setting = annotation == null ? null : annotation.value();
        }
    }

    private static class GroupMember3 implements GroupApi {}

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
                assert GroupDependent1.class.getAnnotation(Setting1.class).value().equals(((GroupMember2) member).setting) : ((GroupMember2) member).setting;
            }
        }

        for (final GroupApi member : dependent2.group) {
            group2.add(member.getClass());

            if (member instanceof GroupMember2) {
                assert ((Setting1) GroupDependent2.class.getConstructor(GroupApi[].class).getParameterAnnotations()[0][0]).value().equals(((GroupMember2) member).setting) : ((GroupMember2) member).setting;
            }
        }

        assert expected.equals(group1) : group1;
        assert expected.equals(group2) : group2;
    }

    private static interface ContextAware {

        String setting();

        public static interface Settings {

            String setting();
        }
    }

    private static interface ContextAwareComponent1 extends ContextAware {

    }

    private static interface ContextAwareComponent2 extends ContextAware {

    }

    private static interface OrdinaryComponent1 extends ContextAware {

    }

    private static interface OrdinaryComponent2 extends ContextAware {

    }

    @Context(Setting1.class)
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

    @Context(Setting2.class)
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

    @Context(Setting1.class)
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

    @Context(Setting2.class)
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
    @Context(Setting1.class)
    private static class ContextAwareVariants1 implements ComponentVariantFactory {

        public void newComponent(final OpenComponentContainer container, final ComponentContext context) {
            container.getRegistry().bindInstance(new ContextAware.Settings() {
                public String setting() {
                    final Setting1 setting = context.annotation(Setting1.class, null);
                    final String value = setting == null ? null : setting.value();
                    return value == null ? "missing-value" : value;
                }
            });
        }
    }

    @Component(api = ContextAware.class, automatic = false)
    @Context(Setting2.class)
    private static class ContextAwareVariants2 implements ComponentVariantFactory {

        public void newComponent(final OpenComponentContainer container, final ComponentContext context) {
            container.getRegistry().bindInstance(new ContextAware.Settings() {
                public String setting() {
                    final Setting2 setting = context.annotation(Setting2.class, null);
                    final String value = setting == null ? null : setting.value();
                    return value == null ? "missing-value" : value;
                }
            });
        }
    }

    @Component(api = OrdinaryComponent1.class, automatic = false)
    @Context(Setting1.class)
    private static class OrdinaryComponentVariants1 implements ComponentVariantFactory {

        public void newComponent(final OpenComponentContainer container, final ComponentContext context) {
            container.getRegistry().bindInstance(new ContextAware.Settings() {
                public String setting() {
                    final Setting1 setting = context.annotation(Setting1.class, null);
                    final String value = setting == null ? null : setting.value();
                    return value == null ? "missing-value" : value;
                }
            });
        }
    }

    @Component(api = OrdinaryComponent2.class, automatic = false)
    @Context(Setting2.class)
    private static class OrdinaryComponentVariants2 implements ComponentVariantFactory {

        public void newComponent(final OpenComponentContainer container, final ComponentContext context) {
            container.getRegistry().bindInstance(new ContextAware.Settings() {
                public String setting() {
                    final Setting2 setting = context.annotation(Setting2.class, null);
                    final String value = setting == null ? null : setting.value();
                    return value == null ? "missing-value" : value;
                }
            });
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

        public ContextProvider1(final @Setting2("setting-2") ComponentContainer container) {
            container.getComponent(ContextConsumer1.class);
        }
    }

    @Setting1("setting-1")
    private static class ContextProvider2 {

        @Inject
        @Setting3("setting-3")
        public ContextConsumer2 consumer;
    }

    @SuppressWarnings("UnusedDeclaration")
    @Context({Setting1.class, Setting2.class})
    private static class ContextConsumer1 {

        public static ComponentContext context;

        private ContextConsumer1(final ComponentContext context) {
            assert ContextConsumer1.context == null;
            ContextConsumer1.context = context;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @Context({Setting1.class, Setting3.class})
    private static class ContextConsumer2 {

        public static ComponentContext context;

        private ContextConsumer2(final ComponentContext context) {
            assert ContextConsumer2.context == null;
            ContextConsumer2.context = context;
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

        @SuppressWarnings("UnusedDeclaration") String value();
    }
}

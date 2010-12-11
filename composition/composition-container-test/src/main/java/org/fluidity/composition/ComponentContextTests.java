/*
 * Copyright (c) 2006-2010 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public final class ComponentContextTests extends AbstractContainerTests {

    public ComponentContextTests(final ContainerFactory factory) {
        super(factory);
    }

    @DataProvider(name = "component-types")
    public Object[][] componentTypes() {
        return new Object[][] {
                new Object[] { ContextAwareComponent1Impl.class, null, "setting1" },
                new Object[] { ContextAwareComponent2Impl.class, null, "setting2" },
                new Object[] { OrdinaryComponent1Impl.class, OrdinaryVariants1.class, "setting1" },
                new Object[] { OrdinaryComponent2Impl.class, OrdinaryVariants2.class, "setting2" },
        };
    }

    private static class TestDependent {
        public ContextAware dependency;
    }

    @Setting2("value1")
    private static class Test1 extends TestDependent {

        private Test1(@Setting1("value1") final ContextAware dependency) {
            this.dependency = dependency;
        }
    }

    @Setting1("value2")
    private static class Test2 extends TestDependent {

        private Test2(@Setting2("value2") final ContextAware dependency) {
            this.dependency = dependency;
        }
    }

    private static class Test3 extends TestDependent {

        private Test3(@Setting3("value") final ContextAware dependency) {
            this.dependency = dependency;
        }
    }

    @Test(dataProvider = "component-types")
    public void explicitContext(final Class<? extends ContextAware> type, final Class<? extends ComponentVariantFactory> factory, final String key)
            throws Exception {
        registry.bindComponent(type);
        registry.bindComponent(Test1.class);
        registry.bindComponent(Test2.class);
        registry.bindComponent(Test3.class);

        if (factory != null) {
            registry.bindComponent(factory);
        }

        final ContextAware variant0 = container.getComponent(type);
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
        registry.bindComponent(OrdinaryVariants2.class);

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
        registry.bindComponent(OrdinaryVariants1.class);
        registry.bindComponent(OrdinaryVariants2.class);
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

        assert context1dependency1.setting().equals("value1");
        assert context1dependency2.setting().equals("value1");
        assert context2dependency1.setting().equals("value2-context");
        assert context2dependency2.setting().equals("value2-ordinary");

        assert context2.getInnocent().getContextAware().setting().equals("value1") : "Context not passed through context unaware component";
        assert context2.getInnocent().getOrdinary().setting().equals("missing-value") : "Invalid context passed through context unaware component";

        assert contextFree.getInnocent().getContextAware().setting().equals("missing-value") : "Invalid context passed through context unaware component";
        assert contextFree.getInnocent().getOrdinary().setting().equals("missing-value") : "Invalid context passed through context unaware component";
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
    private static class ContextAwareComponent1Impl implements ContextAwareComponent1 {

        private final String setting;

        public ContextAwareComponent1Impl(final ComponentContext context) {
            final Setting1 setting = context.annotation(Setting1.class);
            final String value = setting == null ? null : setting.value();
            this.setting = value == null ? "missing-value" : value;
        }

        public String setting() {
            return setting;
        }
    }

    @Context(Setting2.class)
    private static class ContextAwareComponent2Impl implements ContextAwareComponent2 {

        private final String setting;

        public ContextAwareComponent2Impl(ComponentContext context) {
            final Setting2 setting = context.annotation(Setting2.class);
            final String value = setting == null ? null : setting.value();
            this.setting = value == null ? "missing-value" : value;
        }

        public String setting() {
            return setting;
        }
    }

    @Component
    private static class OrdinaryComponent1Impl implements OrdinaryComponent1 {

        private final String setting;

        public OrdinaryComponent1Impl(final Settings settings) {
            this.setting = settings.setting();
        }

        public String setting() {
            return setting;
        }
    }

    @Component
    private static class OrdinaryComponent2Impl implements OrdinaryComponent2 {

        private final String setting;

        public OrdinaryComponent2Impl(final Settings settings) {
            this.setting = settings.setting();
        }

        public String setting() {
            return setting;
        }
    }

    @Component(api = OrdinaryComponent1.class)
    @Context(Setting1.class)
    private static class OrdinaryVariants1 implements ComponentVariantFactory {

        public OpenComponentContainer newComponent(final OpenComponentContainer container, final ComponentContext context) {
            container.getRegistry().bindInstance(ContextAware.Settings.class, new ContextAware.Settings() {
                public String setting() {
                    final Setting1 setting = context.annotation(Setting1.class);
                    final String value = setting == null ? null : setting.value();
                    return value == null ? "missing-value" : value;
                }
            });

            return container;
        }
    }

    @Component(api = OrdinaryComponent2.class)
    @Context(Setting2.class)
    private static class OrdinaryVariants2 implements ComponentVariantFactory {

        public OpenComponentContainer newComponent(final OpenComponentContainer container, final ComponentContext context) {
            container.getRegistry().bindInstance(ContextAware.Settings.class, new ContextAware.Settings() {
                public String setting() {
                    final Setting2 setting = context.annotation(Setting2.class);
                    final String value = setting == null ? null : setting.value();
                    return value == null ? "missing-value" : value;
                }
            });
            return container;
        }
    }

    @Component
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

    @Component
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

    @Component
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

        private OverridingComponent(final OrdinaryComponent2 ordinary2) {
            this.ordinary2 = ordinary2;
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

        String value();
    }
}

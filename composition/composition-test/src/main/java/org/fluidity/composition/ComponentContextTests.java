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

import java.util.Properties;

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

    @Test(dataProvider = "component-types")
    public void explicitContext(final Class<? extends ContextAware> type, final Class<? extends ComponentVariantFactory> factory, final String key)
            throws Exception {
        registry.bindComponent(type);

        if (factory != null) {
            registry.bindComponent(factory);
        }

        final Properties context1 = new Properties();
        context1.setProperty(key, "value1");

        final Properties context2 = new Properties();
        context2.setProperty(key, "value2");

        final Properties context3 = new Properties();
        context2.setProperty("invalid", "value");

        final ContextAware variant0 = container.getComponent(type);
        final ContextAware variant1 = container.getComponent(type, container.makeContext(context1));
        final ContextAware variant2 = container.getComponent(type, container.makeContext(context2));
        final ContextAware variant3 = container.getComponent(type, container.makeContext(context3));

        assert variant0 != null : "Container failed to create default instance";
        assert variant1 != null : "Container failed to create instance for valid context";
        assert variant2 != null : "Container failed to create instance for valid context";
        assert variant3 != null : "Container failed to create instance for invalid";

        assert variant0 != variant1 : "Default context and variant context are the same";
        assert variant0 != variant2 : "Default context and variant context are the same";
        assert variant1 != variant2 : "Instances for two valid contexts are the same";
        assert variant0 == variant3 : "Default instance and instance for invalid context are not the same";

        assert variant0.setting().equals("missing-value") : "Context not properly set";
        assert variant1.setting().equals(context1.getProperty(key)) : "Context not properly set";
        assert variant2.setting().equals(context2.getProperty(key)) : "Context not properly set";
    }

    @Test
    public void implicitContext() throws Exception {
        registry.bindComponent(ContainerDependent.class);
        registry.bindComponent(ContextAwareComponent1Impl.class);
        registry.bindComponent(OrdinaryComponent2Impl.class);
        registry.bindComponent(OrdinaryVariants2.class);

        final ContainerDependent root = container.getComponent(ContainerDependent.class);

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

    @Context(names = { "setting1" })
    private static class ContextAwareComponent1Impl implements ContextAwareComponent1 {

        private final String setting;

        public ContextAwareComponent1Impl(ComponentContext context) {
            this.setting = context.value("setting1", "missing-value");
        }

        public String setting() {
            return setting;
        }
    }

    @Context(names = { "setting2" })
    private static class ContextAwareComponent2Impl implements ContextAwareComponent2 {

        private final String setting;

        public ContextAwareComponent2Impl(ComponentContext context) {
            this.setting = context.value("setting2", "missing-value");
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
    @Context(names = { "setting1" })
    private static class OrdinaryVariants1 implements ComponentVariantFactory {

        public OpenComponentContainer newComponent(final OpenComponentContainer container, final ComponentContext context) {
            container.getRegistry().bindInstance(ContextAware.Settings.class, new ContextAware.Settings() {
                public String setting() {
                    return context.value("setting1", "missing-value");
                }
            });

            return container;
        }
    }

    @Component(api = OrdinaryComponent2.class)
    @Context(names = { "setting2" })
    private static class OrdinaryVariants2 implements ComponentVariantFactory {

        public OpenComponentContainer newComponent(final OpenComponentContainer container, final ComponentContext context) {
            container.getRegistry().bindInstance(ContextAware.Settings.class, new ContextAware.Settings() {
                public String setting() {
                    return context.value("setting2", "missing-value");
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
    @Context(@Context.Value(name = "setting1", value = "value1"))
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
                                       @Context(@Context.Value(name = "setting2", value = "value2-context")) final ContextAwareComponent2 contextAware,
                                       @Context(@Context.Value(name = "setting2", value = "value2-ordinary")) final OrdinaryComponent2 ordinary) {
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

    @Context({ @Context.Value(name = "setting1", value = "value1"), @Context.Value(name = "setting2", value = "value2-default") })
    private static class ContainerDependent {

        private final ContextAwareComponent1 contextAware1;
        private final OrdinaryComponent2 ordinary1;
        private final OrdinaryComponent2 ordinary2;

        public ContainerDependent(final ComponentContainer container) {
            this.contextAware1 = container.getComponent(ContextAwareComponent1.class);
            this.ordinary1 = container.getComponent(OrdinaryComponent2.class);

            final Properties context = new Properties();
            context.setProperty("setting2", "value2-overridden");
            this.ordinary2 = container.getComponent(OrdinaryComponent2.class, container.makeContext(context));
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
}

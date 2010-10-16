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

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public final class ComponentVariantTests extends AbstractContainerTests {

    private ComponentVariantFactory variants = addControl(ComponentVariantFactory.class);

    public ComponentVariantTests(final ContainerFactory factory) {
        super(factory);
    }

    @BeforeMethod
    public void setMockFactory() {
        Variants.delegate = this.variants;
    }

    @Test
    public void invokesVariantsFactoryClassOnce() throws Exception {
        registry.bindComponent(Key.class, Value.class);
        registry.bindDefault(Variants.class);
        registry.bindDefault(FactoryDependency.class);
        registry.bindComponent(DependentKey.class, ContextDependentValue.class);

        final String check = "check";

        registry.bindInstance(Serializable.class, check);

        EasyMock.expect(variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull()))
                .andAnswer(new VariantContainerCheck(container, Serializable.class, check));

        replay();
        verifyComponent(Value.instanceCount, 1, container);
        Value.dependent.context();
        verify();
    }

    @Test
    public void invokesNestedVariantsFactoryClassOnce() throws Exception {
        registry.bindComponent(Key.class, Value.class);
        registry.bindDefault(FactoryDependency.class);
        final OpenComponentContainer nested = registry.makeNestedContainer(DependentKey.class, ContextDependentValue.class);

        final String check = "check";
        final ComponentContainer.Registry nestedRegistry = nested.getRegistry();

        nestedRegistry.bindDefault(Variants.class);
        nestedRegistry.bindInstance(Serializable.class, check);

        EasyMock.expect(variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull()))
                .andAnswer(new VariantContainerCheck(nested, Serializable.class, check));

        replay();
        verifyComponent(Value.instanceCount, 1, container);
        verify();
    }

    private void checkContext(final ComponentContext check, final ComponentContext against) {
        if (against != null) {
            assert check != null;
            assert !check.defines("invalid");

            for (final String key : against.keySet()) {
                final Object value = check.value(key, null);
                assert value != null : String.format("Context %s not found", key);
                assert value.equals(against.value(key, null)) : String.format("Context %s expected %s, got %s", key, value, against.value(key, null));
            }
        }
    }

    private Properties context(final Class<?> componentClass, final Class<? extends ComponentVariantFactory> factoryClass) {
        final Context contentContext = componentClass.getAnnotation(Context.class);
        final Context factoryContext = factoryClass == null ? null : factoryClass.getAnnotation(Context.class);
        final Set<String> validNames = factoryContext == null ? null : new HashSet<String>(Arrays.asList(factoryContext.accept()));

        final Properties properties = new Properties();
        if (contentContext != null) {
            for (final Context.Value value : contentContext.value()) {
                final String name = value.name();

                if (validNames == null || validNames.contains(name)) {
                    properties.setProperty(name, value.value());
                }
            }
        }

        return properties;
    }

    private void verifyContext(final ComponentContainer container, final Class<? extends ContextProvider> contextProvider, Class<?> contextConsumer, final Properties context) {
        final ContextProvider component = container.getComponent(contextProvider);
        assert component != null : String.format("Component %s not instantiated", contextProvider);
        checkContext(component.dependency.context(), filterContext(context, contextConsumer));

        assert component.dependency.context().equals(container.getComponent(DependentKey.class, container.makeContext(context)).context());
        if (context != null && context.isEmpty()) {
            assert component.dependency.context().equals(container.getComponent(DependentKey.class).context());
        }
    }

    private ComponentContext filterContext(final Properties properties, final Class<?> consumer) {
        final Context context = consumer.getAnnotation(Context.class);
        properties.keySet().retainAll(Arrays.asList(context.accept()));
        return container.makeContext(properties);
    }

    @Test
    public void containerCreatesMultipleInstances() throws Exception {
        final Properties context0 = context(ContextProvider0.class, null);
        final Properties context1 = context(ContextProvider1.class, null);
        final Properties context2 = context(ContextProvider2.class, null);

        registry.bindComponent(Key.class, Value.class);
        registry.bindComponent(DependentKey.class, ContextDependentValue.class);

        registry.bindDefault(ContextProvider0.class);
        registry.bindDefault(ContextProvider1.class);
        registry.bindDefault(ContextProvider2.class);

        final String check = "check";

        registry.bindInstance(Serializable.class, check);

        replay();

        verifyComponent(Value.instanceCount, 1, container);

        // get objects that specify all contexts
        verifyContext(container, ContextProvider0.class, ContextDependentValue.class, context0);
        verifyContext(container, ContextProvider1.class, ContextDependentValue.class, context1);
        verifyContext(container, ContextProvider2.class, ContextDependentValue.class, context2);

        verify();
    }

    @Test
    public void variantsFactoryCreatesMultipleInstances() throws Exception {
        final Properties context0 = context(ContextProvider0.class, Variants.class);
        final Properties context1 = context(ContextProvider1.class, Variants.class);
        final Properties context2 = context(ContextProvider2.class, Variants.class);

        registry.bindComponent(Key.class, Value.class);
        registry.bindComponent(DependentKey.class, ContextDependentValue.class);

        registry.bindDefault(ContextProvider0.class);
        registry.bindDefault(ContextProvider1.class);
        registry.bindDefault(ContextProvider2.class);

        // add variants factory to hijack component instantiation
        registry.bindComponent(Variants.class, Variants.class);
        registry.bindComponent(FactoryDependency.class, FactoryDependency.class);

        final String check = "check";

        registry.bindInstance(Serializable.class, check);

        EasyMock.expect(variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.eq(container.makeContext(context0))))
                .andAnswer(new VariantContainerCheck(container, Serializable.class, check));

        EasyMock.expect(variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.eq(container.makeContext(context1))))
                .andAnswer(new VariantContainerCheck(container, Serializable.class, check));

        EasyMock.expect(variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.eq(container.makeContext(context2))))
                .andAnswer(new VariantContainerCheck(container, Serializable.class, check));

        replay();

        verifyComponent(Value.instanceCount, 1, container);

        // get objects that specify all contexts
        verifyContext(container, ContextProvider0.class, Variants.class, context0);
        verifyContext(container, ContextProvider1.class, Variants.class, context1);
        verifyContext(container, ContextProvider2.class, Variants.class, context2);

        verify();
    }

    @Test
    public void nestedVariantFactoryCreatesMultipleInstances() throws Exception {
        final Properties context0 = context(ContextProvider0.class, Variants.class);
        final Properties context1 = context(ContextProvider1.class, Variants.class);
        final Properties context2 = context(ContextProvider2.class, Variants.class);

        registry.bindComponent(Key.class, Value.class);
        registry.bindComponent(FactoryDependency.class, FactoryDependency.class);
        final OpenComponentContainer nested = registry.makeNestedContainer(DependentKey.class, ContextDependentValue.class);

        final String check = "check";
        final ComponentContainer.Registry nestedRegistry = nested.getRegistry();

        nestedRegistry.bindDefault(ContextProvider0.class);
        nestedRegistry.bindDefault(ContextProvider1.class);
        nestedRegistry.bindDefault(ContextProvider2.class);

        nestedRegistry.bindComponent(Variants.class, Variants.class);
        nestedRegistry.bindInstance(Serializable.class, check);

        EasyMock.expect(variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.eq(container.makeContext(context0))))
                .andAnswer(new VariantContainerCheck(nested, Serializable.class, check));

        EasyMock.expect(variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.eq(container.makeContext(context1))))
                .andAnswer(new VariantContainerCheck(nested, Serializable.class, check));

        EasyMock.expect(variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.eq(container.makeContext(context2))))
                .andAnswer(new VariantContainerCheck(nested, Serializable.class, check));

        replay();

        verifyComponent(Value.instanceCount, 1, container);

        // get objects that specify all contexts
        verifyContext(nested, ContextProvider0.class, Variants.class, context0);
        verifyContext(nested, ContextProvider1.class, Variants.class, context1);
        verifyContext(nested, ContextProvider2.class, Variants.class, context2);

        verify();
    }

    @Test
    public void variantsFactoryCreatesMultipleInstancesInNestedContainer() throws Exception {
        final Properties context0 = context(ContextProvider0.class, Variants.class);
        final Properties context1 = context(ContextProvider1.class, Variants.class);
        final Properties context2 = context(ContextProvider2.class, Variants.class);

        registry.bindComponent(Key.class, Value.class);
        registry.bindComponent(DependentKey.class, ContextDependentValue.class);
        registry.bindComponent(FactoryDependency.class, FactoryDependency.class);

        final OpenComponentContainer nested = registry.makeNestedContainer();
        final ComponentContainer.Registry nestedRegistry = nested.getRegistry();

        nestedRegistry.bindDefault(ContextProvider0.class);
        nestedRegistry.bindDefault(ContextProvider1.class);
        nestedRegistry.bindDefault(ContextProvider2.class);

        // corresponding binding component took place in the parent container but must still be found
        nestedRegistry.bindComponent(Variants.class, Variants.class);

        final String check = "check";

        nestedRegistry.bindInstance(Serializable.class, check);

        EasyMock.expect(variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.eq(container.makeContext(context0))))
                .andAnswer(new VariantContainerCheck(nested, Serializable.class, check));

        EasyMock.expect(variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.eq(container.makeContext(context1))))
                .andAnswer(new VariantContainerCheck(nested, Serializable.class, check));

        EasyMock.expect(variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.eq(container.makeContext(context2))))
                .andAnswer(new VariantContainerCheck(nested, Serializable.class, check));

        replay();

        verifyComponent(Value.instanceCount, 1, container);

        // get objects that specify all contexts
        verifyContext(nested, ContextProvider0.class, Variants.class, context0);
        verifyContext(nested, ContextProvider1.class, Variants.class, context1);
        verifyContext(nested, ContextProvider2.class, Variants.class, context2);

        verify();
    }

    /**
     * This is intentionally private - makes sure the container is able to instantiate non-public classes
     */
    @Context(accept = "name1")
    private static class ContextDependentValue extends DependentValue {

        public ContextDependentValue(final ComponentContext context) {
            super(context);
        }
    }

    @Component(api = DependentKey.class)
    @Context(accept = { "name1", "name2" })
    private static class Variants implements ComponentVariantFactory {

        public static ComponentVariantFactory delegate;

        public Variants(final FactoryDependency dependent) {
            assert dependent != null;
        }

        public OpenComponentContainer newComponent(final OpenComponentContainer container, final ComponentContext context) {
            assert delegate != null;
            return delegate.newComponent(container, context);
        }
    }

    private static class ContextProvider {

        public final DependentKey dependency;

        private ContextProvider(final DependentKey dependency) {
            this.dependency = dependency;
        }
    }

    private static class ContextProvider0 extends ContextProvider {

        public ContextProvider0(final DependentKey dependency) {
            super(dependency);
        }
    }

    @Context({ @Context.Value(name = "name1", value = "value11"), @Context.Value(name = "name2", value = "value12") })
    private static class ContextProvider1 extends ContextProvider {

        public ContextProvider1(final DependentKey dependency) {
            super(dependency);
        }
    }

    @Context({ @Context.Value(name = "name1", value = "value21"), @Context.Value(name = "name2", value = "value22") })
    private static class ContextProvider2 extends ContextProvider {

        public ContextProvider2(final DependentKey dependency) {
            super(dependency);
        }
    }

    private static class VariantContainerCheck implements IAnswer<OpenComponentContainer> {

        private final OpenComponentContainer container;
        private final Class<?> checkKey;
        private final Object checkValue;

        public VariantContainerCheck(final OpenComponentContainer container, final Class<?> checkKey, final Object checkValue) {
            this.container = container;
            this.checkKey = checkKey;
            this.checkValue = checkValue;
        }

        public OpenComponentContainer answer() throws Throwable {
            final OpenComponentContainer received = (OpenComponentContainer) EasyMock.getCurrentArguments()[0];

            assert received != null : "Received no container";
            assert received.getComponent(checkKey) == checkValue : "Expected " + container.toString() + ", got " + received.toString();
            return received;
        }
    }
}

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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.composition.spi.ComponentVariantFactory;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public final class ComponentVariantTests extends AbstractContainerTests {

    private ComponentVariantFactory variants = addControl(ComponentVariantFactory.class);

    @SuppressWarnings("unchecked")
    private ComponentFactory<DependentKey> factory = (ComponentFactory<DependentKey>) addControl(ComponentFactory.class);

    public ComponentVariantTests(final ContainerFactory factory) {
        super(factory);
    }

    @BeforeMethod
    public void setMockFactory() {
        Variants.delegate = this.variants;
        Factory.delegate = this.factory;
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
            assert !check.defines(Annotation.class);

            for (final Class<? extends Annotation> key : against.types()) {
                final Annotation[] value = check.annotations(key);
                assert value != null : String.format("Context %s not found", key);
                assert Arrays.equals(value, against.annotations(key)) : String.format("Context %s expected %s, got %s",
                                                                                      key,
                                                                                      Arrays.asList(value),
                                                                                      Arrays.asList(against.annotations(key)));
            }
        }
    }

    private ComponentContext context(final Class<?> componentClass, final Class<? extends ComponentVariantFactory> factoryClass) {
        final Annotation[] contentContext = componentClass.getAnnotations();
        final Context factoryContext = factoryClass == null ? null : factoryClass.getAnnotation(Context.class);
        final Set<Class<? extends Annotation>> validTypes = factoryContext == null
                                                            ? null
                                                            : new HashSet<Class<? extends Annotation>>(Arrays.asList(factoryContext.value()));

        final Map<Class<? extends Annotation>, Annotation[]> map = new HashMap<Class<? extends Annotation>, Annotation[]>();

        if (contentContext != null) {
            for (final Annotation value : contentContext) {
                final Class<? extends Annotation> type = value.getClass();

                if (validTypes == null || validTypes.contains(type)) {
                    map.put(type, new Annotation[] { value });
                }
            }
        }

        return new ComponentContextImpl(map);
    }

    private void verifyContext(final ComponentContainer container, final Class<?> contextConsumer) {
        final ComponentContext context0 = context(ContextProvider0.class, null);
        final ComponentContext context1 = context(ContextProvider1.class, null);
        final ComponentContext context2 = context(ContextProvider2.class, null);

        final DependentKey dependency0 = container.getComponent(ContextProvider0.class).dependency;
        final DependentKey dependency1 = container.getComponent(ContextProvider1.class).dependency;
        final DependentKey dependency2 = container.getComponent(ContextProvider2.class).dependency;

        checkContext(dependency0.context(), filterContext(context0, contextConsumer));
        checkContext(dependency1.context(), filterContext(context1, contextConsumer));
        checkContext(dependency2.context(), filterContext(context2, contextConsumer));

        assert dependency0 == container.getComponent(DependentKey.class);
        assert dependency0 != dependency1;
        assert dependency0 != dependency2;
        assert dependency1 != dependency2;
    }

    private ComponentContext filterContext(final ComponentContext context, final Class<?> consumer) {
        final Context accepted = consumer.getAnnotation(Context.class);

        final Set<Class<? extends Annotation>> set = new HashSet<Class<? extends Annotation>>(context.types());
        set.retainAll(Arrays.asList(accepted.value()));

        final HashMap<Class<? extends Annotation>, Annotation[]> map = new HashMap<Class<? extends Annotation>, Annotation[]>();
        for (final Class<? extends Annotation> type : set) {
            map.put(type, context.annotations(type));
        }

        return new ComponentContextImpl(map);
    }

    @Test
    public void containerCreatesMultipleInstances() throws Exception {
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
        verifyContext(container, ContextDependentValue.class);

        verify();
    }

    @Test
    public void factoryCreatesMultipleInstances() throws Exception {
        registry.bindComponent(Key.class, Value.class);
        registry.bindComponent(Factory.class);
        registry.bindComponent(FactoryDependency.class, FactoryDependency.class);

        registry.bindDefault(ContextProvider0.class);
        registry.bindDefault(ContextProvider1.class);
        registry.bindDefault(ContextProvider2.class);

        final String check = "check";

        registry.bindInstance(Serializable.class, check);

        EasyMock.expect(factory.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull()))
                .andAnswer(new FactoryContainerCheck(container, Serializable.class, check));

        EasyMock.expect(factory.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull()))
                .andAnswer(new FactoryContainerCheck(container, Serializable.class, check));

        EasyMock.expect(factory.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull()))
                .andAnswer(new FactoryContainerCheck(container, Serializable.class, check));

        replay();

        verifyComponent(Value.instanceCount, 1, container);

        // get objects that specify all contexts
        verifyContext(container, ContextDependentValue.class);

        verify();
    }

    @Test
    public void variantsFactoryCreatesMultipleInstances() throws Exception {
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

        EasyMock.expect(variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull()))
                .andAnswer(new VariantContainerCheck(container, Serializable.class, check));

        EasyMock.expect(variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull()))
                .andAnswer(new VariantContainerCheck(container, Serializable.class, check));

        EasyMock.expect(variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull()))
                .andAnswer(new VariantContainerCheck(container, Serializable.class, check));

        replay();

        verifyComponent(Value.instanceCount, 1, container);

        // get objects that specify all contexts
        verifyContext(container, Variants.class);

        verify();
    }

    @Test
    public void nestedVariantFactoryCreatesMultipleInstances() throws Exception {
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

        EasyMock.expect(variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull()))
                .andAnswer(new VariantContainerCheck(nested, Serializable.class, check));

        EasyMock.expect(variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull()))
                .andAnswer(new VariantContainerCheck(nested, Serializable.class, check));

        EasyMock.expect(variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull()))
                .andAnswer(new VariantContainerCheck(nested, Serializable.class, check));

        replay();

        verifyComponent(Value.instanceCount, 1, container);

        // get objects that specify all contexts
        verifyContext(nested, Variants.class);

        verify();
    }

    @Test
    public void variantsFactoryCreatesMultipleInstancesInNestedContainer() throws Exception {
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

        EasyMock.expect(variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull()))
                .andAnswer(new VariantContainerCheck(nested, Serializable.class, check));

        EasyMock.expect(variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull()))
                .andAnswer(new VariantContainerCheck(nested, Serializable.class, check));

        EasyMock.expect(variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull()))
                .andAnswer(new VariantContainerCheck(nested, Serializable.class, check));

        replay();

        verifyComponent(Value.instanceCount, 1, container);

        // get objects that specify all contexts
        verifyContext(nested, Variants.class);

        verify();
    }

    @Test
    public void ContextConsumerShieldsDependenciesFromConsumedContext() throws Exception {
        registry.bindComponent(ContextConsumer.class);
        registry.bindComponent(ContextOblivious.class);

        final ContextConsumer consumer = container.getComponent(ContextConsumer.class);
        final ContextOblivious oblivious = container.getComponent(ContextOblivious.class);

        assert consumer.dependency == oblivious;
    }

    /**
     * This is intentionally private - makes sure the container is able to instantiate non-public classes
     */
    @Context(Setting1.class)
    private static class ContextDependentValue extends DependentValue {

        public ContextDependentValue(final ComponentContext context) {
            super(context);
        }
    }

    @Component(api = DependentKey.class)
    @Context({ Setting1.class, Setting2.class })
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

    @Component(api = DependentKey.class, type = ContextDependentValue.class)
    @Context({ Setting1.class, Setting2.class })
    private static class Factory implements ComponentFactory<DependentKey> {

        public static ComponentFactory<DependentKey> delegate;

        public Factory(final FactoryDependency dependent) {
            assert dependent != null;
        }

        public DependentKey newComponent(final OpenComponentContainer container, final ComponentContext context) {
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

    @Setting1("value11")
    @Setting2("value12")
    private static class ContextProvider1 extends ContextProvider {

        public ContextProvider1(final DependentKey dependency) {
            super(dependency);
        }
    }

    @Setting1("value21")
    @Setting2("value22")
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

    private static class FactoryContainerCheck implements IAnswer<DependentKey> {

        private final OpenComponentContainer container;
        private final Class<?> checkKey;
        private final Object checkValue;

        public FactoryContainerCheck(final OpenComponentContainer container, final Class<?> checkKey, final Object checkValue) {
            this.container = container;
            this.checkKey = checkKey;
            this.checkValue = checkValue;
        }

        public DependentKey answer() throws Throwable {
            final Object[] arguments = EasyMock.getCurrentArguments();
            final OpenComponentContainer received = (OpenComponentContainer) arguments[0];

            assert received != null : "Received no container";
            assert received.getComponent(checkKey) == checkValue : "Expected " + container.toString() + ", got " + received.toString();
            return new ContextDependentValue((ComponentContext) arguments[1]);
        }
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER })
    @SuppressWarnings("UnusedDeclaration")
    public static @interface Setting1 {

        String value();
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER })
    @SuppressWarnings("UnusedDeclaration")
    public static @interface Setting2 {

        String value();
    }

    @Setting1("value1")
    @Setting2("value2")
    @Context(Setting1.class)
    public static class ContextConsumer {

        public ContextOblivious dependency;

        public ContextConsumer(final ContextOblivious dependency) {
            this.dependency = dependency;
        }
    }

    public static class ContextOblivious { }
}

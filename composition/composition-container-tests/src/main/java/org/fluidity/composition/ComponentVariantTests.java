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
@SuppressWarnings("unchecked")
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
        Factory.instances.clear();
    }

    @Test
    public void invokesVariantsFactoryClassOnce() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(Variants.class);
        registry.bindComponent(FactoryDependency.class);
        registry.bindComponent(ContextDependentValue.class);

        final String check = "check";

        registry.bindInstance(check, Serializable.class);

        variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull());
        EasyMock.expectLastCall().andAnswer(new VariantContainerCheck(container, Serializable.class, check));

        replay();
        verifyComponent(Value.instanceCount, 1, container);
        Value.dependent.context();
        verify();
    }

    @Test
    public void invokesVariantsFactoryClassOnceInChildContainer() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(FactoryDependency.class);
        final OpenComponentContainer child = registry.makeChildContainer(ContextDependentValue.class, DependentKey.class);

        final String check = "check";
        final ComponentContainer.Registry childRegistry = child.getRegistry();

        childRegistry.bindComponent(Variants.class);
        childRegistry.bindInstance(check, Serializable.class);

        variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull());
        EasyMock.expectLastCall().andAnswer(new VariantContainerCheck(child, Serializable.class, check));

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
        registry.bindComponent(Value.class);
        registry.bindComponent(ContextDependentValue.class);

        registry.bindComponent(ContextProvider0.class);
        registry.bindComponent(ContextProvider1.class);
        registry.bindComponent(ContextProvider2.class);

        final String check = "check";

        registry.bindInstance(check, Serializable.class);

        replay();

        verifyComponent(Value.instanceCount, 1, container);

        // get objects that specify all contexts
        verifyContext(container, ContextDependentValue.class);

        verify();
    }

    @Test
    public void factoryCreatesMultipleInstances() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(Factory.class);
        registry.bindComponent(FactoryDependency.class);

        registry.bindComponent(ContextProvider0.class);
        registry.bindComponent(ContextProvider1.class);
        registry.bindComponent(ContextProvider2.class);

        final String check = "check";

        registry.bindInstance(check, Serializable.class);

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

        // only one factory instance should be created as opposed to one for every context
        assert Factory.instances.size() == 1 : Factory.instances.size();
    }

    @Test
    public void variantsFactoryCreatesMultipleInstances() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(ContextDependentValue.class);

        registry.bindComponent(ContextProvider0.class);
        registry.bindComponent(ContextProvider1.class);
        registry.bindComponent(ContextProvider2.class);

        // add variants factory to hijack component instantiation
        registry.bindComponent(Variants.class);
        registry.bindComponent(FactoryDependency.class);

        final String check = "check";

        registry.bindInstance(check, Serializable.class);

        variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull());
        EasyMock.expectLastCall().andAnswer(new VariantContainerCheck(container, Serializable.class, check));

        variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull());
        EasyMock.expectLastCall().andAnswer(new VariantContainerCheck(container, Serializable.class, check));

        variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull());
        EasyMock.expectLastCall().andAnswer(new VariantContainerCheck(container, Serializable.class, check));

        replay();

        verifyComponent(Value.instanceCount, 1, container);

        // get objects that specify all contexts
        verifyContext(container, Variants.class);

        verify();
    }

    @Test
    public void variantFactoryCreatesMultipleInstancesInChildContainer() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(FactoryDependency.class);
        final OpenComponentContainer child = registry.makeChildContainer(ContextDependentValue.class, DependentKey.class);

        final String check = "check";
        final ComponentContainer.Registry childRegistry = child.getRegistry();

        childRegistry.bindComponent(ContextProvider0.class);
        childRegistry.bindComponent(ContextProvider1.class);
        childRegistry.bindComponent(ContextProvider2.class);

        childRegistry.bindComponent(Variants.class);
        childRegistry.bindInstance(check, Serializable.class);

        variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull());
        EasyMock.expectLastCall().andAnswer(new VariantContainerCheck(child, Serializable.class, check));

        variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull());
        EasyMock.expectLastCall().andAnswer(new VariantContainerCheck(child, Serializable.class, check));

        variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull());
        EasyMock.expectLastCall().andAnswer(new VariantContainerCheck(child, Serializable.class, check));

        replay();

        verifyComponent(Value.instanceCount, 1, container);

        // get objects that specify all contexts
        verifyContext(child, Variants.class);

        verify();
    }

    @Test
    public void variantsFactoryCreatesMultipleInstancesInChildContainer() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(ContextDependentValue.class);
        registry.bindComponent(FactoryDependency.class);

        final OpenComponentContainer child = registry.makeChildContainer();
        final ComponentContainer.Registry childRegistry = child.getRegistry();

        childRegistry.bindComponent(ContextProvider0.class);
        childRegistry.bindComponent(ContextProvider1.class);
        childRegistry.bindComponent(ContextProvider2.class);

        // corresponding binding component took place in the parent container but must still be found
        childRegistry.bindComponent(Variants.class);

        final String check = "check";

        childRegistry.bindInstance(check, Serializable.class);

        variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull());
        EasyMock.expectLastCall().andAnswer(new VariantContainerCheck(child, Serializable.class, check));

        variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull());
        EasyMock.expectLastCall().andAnswer(new VariantContainerCheck(child, Serializable.class, check));

        variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull());
        EasyMock.expectLastCall().andAnswer(new VariantContainerCheck(child, Serializable.class, check));

        replay();

        verifyComponent(Value.instanceCount, 1, container);

        // get objects that specify all contexts
        verifyContext(child, Variants.class);

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

        public void newComponent(final OpenComponentContainer container, final ComponentContext context) {
            assert delegate != null;
            delegate.newComponent(container, context);
        }
    }

    @Component(api = DependentKey.class)
    @Context({ Setting1.class, Setting2.class })
    private static class Factory implements ComponentFactory<DependentKey> {

        public static ComponentFactory<DependentKey> delegate;
        public static Set<Factory> instances = new HashSet<Factory>();

        public Factory(final FactoryDependency dependent) {
            assert dependent != null;
        }

        public DependentKey newComponent(final OpenComponentContainer container, final ComponentContext context) {
            instances.add(this);        // only instances in actual use count
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

    private static class VariantContainerCheck implements IAnswer<Void> {

        private final OpenComponentContainer container;
        private final Class<?> checkKey;
        private final Object checkValue;

        public VariantContainerCheck(final OpenComponentContainer container, final Class<?> checkKey, final Object checkValue) {
            this.container = container;
            this.checkKey = checkKey;
            this.checkValue = checkValue;
        }

        public Void answer() throws Throwable {
            final OpenComponentContainer received = (OpenComponentContainer) EasyMock.getCurrentArguments()[0];

            assert received != null : "Received no container";
            assert received.getComponent(checkKey) == checkValue : String.format("Expected %s, got %s", container.toString(), received.toString());
            return null;
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
            assert received.getComponent(checkKey) == checkValue : String.format("Expected %s, got %s", container.toString(), received.toString());
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

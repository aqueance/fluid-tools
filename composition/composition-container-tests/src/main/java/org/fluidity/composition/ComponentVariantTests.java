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

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
    private ComponentFactory factory = addControl(ComponentFactory.class);

    public ComponentVariantTests(final ContainerFactory factory) {
        super(factory);
    }

    @BeforeMethod
    public void setMockFactory() {
        Variants.delegate = this.variants;
        ExpandingVariants.delegate = this.variants;
        Factory.delegate = this.factory;
        Factory.instances.clear();
    }

    @Test
    public void invokesVariantsFactoryClassOnce() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(Variants.class);
        registry.bindComponent(FactoryDependency.class);
        registry.bindComponent(DependentValue.class);

        final Check check = new Check();

        registry.bindInstance(check);

        variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull());
        EasyMock.expectLastCall().andAnswer(new VariantContainerCheck(container, Check.class, check)).anyTimes();

        replay();
        verifyComponent(container);
        verify();

        assert Value.dependent.context() != null;
    }

    @Test
    public void invokesVariantsFactoryClassOnceInChildContainer() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(FactoryDependency.class);
        final OpenComponentContainer child = registry.makeChildContainer(DependentValue.class);

        final Check check = new Check();
        final ComponentContainer.Registry childRegistry = child.getRegistry();

        childRegistry.bindComponent(Variants.class);
        childRegistry.bindInstance(check);

        variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull());
        EasyMock.expectLastCall().andAnswer(new VariantContainerCheck(child, Check.class, check)).anyTimes();

        replay();
        verifyComponent(container);
        verify();
    }

    private void checkContext(final ComponentContext check, final ComponentContext against) {
        if (against != null) {
            assert check != null;
            assert !check.defines(Annotation.class);

            for (final Class<? extends Annotation> key : against.types()) {
                final Annotation[] value = check.annotations(key);
                assert value != null : String.format("Context %s not found", key);
                assert Arrays.equals(value, against.annotations(key)) : String.format("Context %s expected %s, got %s", key, Arrays.asList(value), Arrays.asList(against.annotations(key)));
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

        final Check check = new Check();

        registry.bindInstance(check);

        replay();

        verifyComponent(container);

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

        final Check check = new Check();

        registry.bindInstance(check);

        factory.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull());
        EasyMock.expectLastCall().andAnswer(new FactoryContainerCheck(container, Check.class, check)).anyTimes();

        replay();

        verifyComponent(container);

        // get objects that specify all contexts
        verifyContext(container, ContextDependentValue.class);

        verify();

        // only one factory instance should be created as opposed to one for every context
        assert Factory.instances.size() == 1 : Factory.instances.size();
    }

    @Test
    public void variantsFactoryCreatesMultipleInstances() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(DependentValue.class);

        registry.bindComponent(ContextProvider0.class);
        registry.bindComponent(ContextProvider1.class);
        registry.bindComponent(ContextProvider2.class);

        // add variants factory to hijack component instantiation
        registry.bindComponent(Variants.class);
        registry.bindComponent(FactoryDependency.class);

        final Check check = new Check();

        registry.bindInstance(check);

        variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull());
        EasyMock.expectLastCall().andAnswer(new VariantContainerCheck(container, Check.class, check)).anyTimes();

        replay();

        verifyComponent(container);

        // get objects that specify all contexts
        verifyContext(container, Variants.class);

        verify();
    }

    @Test
    public void variantFactoryCreatesMultipleInstancesInChildContainer() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(FactoryDependency.class);
        final OpenComponentContainer child = registry.makeChildContainer(DependentValue.class);

        final Check check = new Check();
        final ComponentContainer.Registry childRegistry = child.getRegistry();

        childRegistry.bindComponent(ContextProvider0.class);
        childRegistry.bindComponent(ContextProvider1.class);
        childRegistry.bindComponent(ContextProvider2.class);

        childRegistry.bindComponent(Variants.class);
        childRegistry.bindInstance(check);

        variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull());
        EasyMock.expectLastCall().andAnswer(new VariantContainerCheck(child, Check.class, check)).anyTimes();

        replay();

        verifyComponent(container);

        // get objects that specify all contexts
        verifyContext(child, Variants.class);

        verify();
    }

    @Test
    public void variantsFactoryCreatesMultipleInstancesInChildContainer() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(DependentValue.class);
        registry.bindComponent(FactoryDependency.class);

        final OpenComponentContainer child = registry.makeChildContainer();
        final ComponentContainer.Registry childRegistry = child.getRegistry();

        childRegistry.bindComponent(ContextProvider0.class);
        childRegistry.bindComponent(ContextProvider1.class);
        childRegistry.bindComponent(ContextProvider2.class);

        // corresponding binding component took place in the parent container but must still be found
        childRegistry.bindComponent(Variants.class);

        final Check check = new Check();

        childRegistry.bindInstance(check);

        variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull());
        EasyMock.expectLastCall().andAnswer(new VariantContainerCheck(child, Check.class, check)).anyTimes();

        replay();

        verifyComponent(container);

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

    @Test
    public void testVariantFactoryComponentFactoryChain() throws Exception {
        registry.bindComponent(ConfiguredComponentFactory.class);
        registry.bindComponent(ConfiguredComponentVariants.class);
        registry.bindComponent(ConfigurationProvider.class);

        final ConfigurationProvider component = container.getComponent(ConfigurationProvider.class);
        assert component != null;
    }

    @Test
    public void variantsFactoryExpandsContext() throws Exception {
        registry.bindComponent(ContextDependentValue.class);        // accepts Setting1
        registry.bindComponent(ExpandingVariants.class);            // accepts Setting2
        registry.bindComponent(ContextProvider1.class);

        variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {
            public Void answer() throws Throwable {
                final ComponentContext context = (ComponentContext) EasyMock.getCurrentArguments()[1];
                assert !context.defines(Setting1.class) : Setting1.class;
                assert context.defines(Setting2.class) : Setting2.class;
                return null;
            }
        });

        replay();
        final ContextProvider component = container.getComponent(ContextProvider1.class);
        assert component != null : ContextProvider1.class;
        verify();

        assert component.dependency.context().defines(Setting1.class) : Setting1.class;
        assert !component.dependency.context().defines(Setting2.class) : Setting2.class;
    }

    @Test
    public void variantsFactoryCanUseDelegateContext() throws Exception {
        registry.bindComponent(ContextDependentValue.class);        // accepts Setting1
        registry.bindComponent(Variants.class);                     // accepts setting1 and Setting2
        registry.bindComponent(ContextProvider1.class);
        registry.bindComponent(FactoryDependency.class);

        variants.newComponent(EasyMock.<OpenComponentContainer>notNull(), EasyMock.<ComponentContext>notNull());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {
            public Void answer() throws Throwable {
                final ComponentContext context = (ComponentContext) EasyMock.getCurrentArguments()[1];
                assert context.defines(Setting1.class) : Setting1.class;
                assert context.defines(Setting2.class) : Setting2.class;
                return null;
            }
        });

        replay();
        final ContextProvider component = container.getComponent(ContextProvider1.class);
        assert component != null : ContextProvider1.class;
        verify();

        assert component.dependency.context().defines(Setting1.class) : Setting1.class;
        assert !component.dependency.context().defines(Setting2.class) : Setting2.class;
    }

    @Test
    public void testGroupMembersOrder1() throws Exception {
        registry.bindComponent(GroupMember1.class);
        registry.bindComponent(GroupMember2.class);
        registry.bindComponent(GroupMember3.class);
        registry.bindComponent(GroupMember2Variants.class);
        groupMemberChecks(Arrays.asList(GroupMember1.class, GroupMember2.class, GroupMember3.class));
    }

    @Test
    public void testGroupMembersOrder2() throws Exception {
        registry.bindComponent(GroupMember2Variants.class);
        registry.bindComponent(GroupMember1.class);
        registry.bindComponent(GroupMember2.class);
        registry.bindComponent(GroupMember3.class);
        groupMemberChecks(Arrays.asList(GroupMember2.class, GroupMember1.class, GroupMember3.class));
    }

    private void groupMemberChecks(final List<Class<? extends GroupApi>> expected) throws NoSuchMethodException {
        registry.bindComponent(GroupDependent1.class);
        registry.bindComponent(GroupDependent2.class);

        final GroupDependent1 dependent1 = container.getComponent(GroupDependent1.class);
        final GroupDependent2 dependent2 = container.getComponent(GroupDependent2.class);

        assert dependent1 != null;
        assert dependent2 != null;

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

    @ComponentGroup
    public static interface GroupApi {
    }

    private static class GroupMember1 implements GroupApi {}

    @Context(Setting1.class)
    @Component(api = GroupMember2.class, automatic = false)
    private static class GroupMember2Variants implements ComponentVariantFactory {

        public void newComponent(final OpenComponentContainer container, final ComponentContext context) throws ComponentContainer.ResolutionException {
            final Setting1 annotation = context.annotation(Setting1.class, null);
            container.getRegistry().bindInstance(annotation == null ? null : annotation.value());
        }
    }

    private static class GroupMember2 implements GroupApi {
        public final String setting;

        public GroupMember2(final String setting) {
            this.setting = setting;
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

    /**
     * This is intentionally private - makes sure the container is able to instantiate non-public classes
     */
    @Component(automatic = false)
    @Context(Setting1.class)
    private static class ContextDependentValue extends DependentValue {

        public ContextDependentValue(final ComponentContext context) {
            super(context);
        }
    }

    @Component(api = DependentKey.class, automatic = false)
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

    @Component(api = DependentKey.class, automatic = false)
    @Context({ Setting2.class })
    private static class ExpandingVariants implements ComponentVariantFactory {

        public static ComponentVariantFactory delegate;

        public void newComponent(final OpenComponentContainer container, final ComponentContext context) {
            assert delegate != null;
            delegate.newComponent(container, context);
        }
    }

    @Component(api = DependentKey.class, automatic = false)
    @Context({ Setting1.class, Setting2.class })
    private static class Factory implements ComponentFactory {

        public static ComponentFactory delegate;
        public static Set<Factory> instances = new HashSet<Factory>();

        public Factory(final FactoryDependency dependent) {
            assert dependent != null;
        }

        public void newComponent(final OpenComponentContainer container, final ComponentContext context) {
            instances.add(this);        // only instances in actual use count
            assert delegate != null;
            delegate.newComponent(container, context);
        }
    }

    private static class ContextProvider {

        public final DependentKey dependency;

        private ContextProvider(final DependentKey dependency) {
            this.dependency = dependency;
        }
    }

    @Component(automatic = false)
    private static class ContextProvider0 extends ContextProvider {

        public ContextProvider0(final DependentKey dependency) {
            super(dependency);
        }
    }

    @Setting1("value11")
    @Setting2("value12")
    @Component(automatic = false)
    private static class ContextProvider1 extends ContextProvider {

        public ContextProvider1(final DependentKey dependency) {
            super(dependency);
        }
    }

    @Setting1("value21")
    @Setting2("value22")
    @Component(automatic = false)
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

    private static class FactoryContainerCheck implements IAnswer<Void> {

        private final OpenComponentContainer container;
        private final Class<?> checkKey;
        private final Object checkValue;

        public FactoryContainerCheck(final OpenComponentContainer container, final Class<?> checkKey, final Object checkValue) {
            this.container = container;
            this.checkKey = checkKey;
            this.checkValue = checkValue;
        }

        public Void answer() throws Throwable {
            final Object[] arguments = EasyMock.getCurrentArguments();
            final OpenComponentContainer received = (OpenComponentContainer) arguments[0];

            assert received != null : "Received no container";
            assert received.getComponent(checkKey) == checkValue : String.format("Expected %s, got %s", container.toString(), received.toString());

            final ComponentContainer.Registry registry = received.getRegistry();
            registry.bindInstance(arguments[1]);
            registry.bindComponent(ContextDependentValue.class);
            return null;
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
    @Component(automatic = false)
    public static class ContextConsumer {

        public ContextOblivious dependency;

        public ContextConsumer(final ContextOblivious dependency) {
            this.dependency = dependency;
        }
    }

    @Component(automatic = false)
    public static class ContextOblivious {

    }

    public static interface ConfiguredComponent {

        Configuration configuration();
    }

    @Component(automatic = false)
    public static class ConfiguredComponentImpl implements ConfiguredComponent {
        private final Configuration configuration;

        public ConfiguredComponentImpl(final Configuration configuration) {
            this.configuration = configuration;
        }

        public Configuration configuration() {
            return configuration;
        }
    }

    @Component(api = ConfiguredComponent.class, automatic = false)
    public static class ConfiguredComponentFactory implements ComponentFactory {

        public void newComponent(final OpenComponentContainer container, final ComponentContext context) {
            container.getRegistry().bindComponent(ConfiguredComponentImpl.class);
        }
    }

    @Context(Setting1.class)
    public static class Configuration {

        public final String value;

        public Configuration(final ComponentContext context) {
            this.value = context.annotation(Setting1.class, null).value();
        }
    }

    @Component(api = ConfiguredComponent.class, automatic = false)
    @Context(Setting1.class)
    public static class ConfiguredComponentVariants implements ComponentVariantFactory {

        public void newComponent(final OpenComponentContainer container, final ComponentContext context) throws ComponentContainer.ResolutionException {
            final ComponentContainer.Registry registry = container.getRegistry();
            registry.bindInstance(context);
            registry.bindComponent(Configuration.class);
        }
    }

    @Setting1(ConfigurationProvider.CONTEXT)
    public static class ConfigurationProvider {

        public static final String CONTEXT = "context-value";

        public ConfigurationProvider(final ConfiguredComponent component) {
            assert CONTEXT.equals(component.configuration().value);
        }
    }
}

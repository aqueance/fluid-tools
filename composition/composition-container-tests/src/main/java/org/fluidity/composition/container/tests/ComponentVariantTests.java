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
import java.util.Objects;
import java.util.Set;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.OpenContainer;
import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.composition.spi.Dependency;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings({ "unchecked", "WeakerAccess" })
public final class ComponentVariantTests extends AbstractContainerTests {

    private final ComponentFactory factory = dependencies().normal(ComponentFactory.class);

    ComponentVariantTests(final ArtifactFactory factory) {
        super(factory);
    }

    @BeforeMethod
    public void setMockFactory() {
        ContextExpansionFactory.delegate = this.factory;
        DependentFactory.delegate = this.factory;
        DependentFactory.used.clear();
    }

    @Test
    public void invokesVariantsFactoryClassOnce() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(DependentFactory.class);
        registry.bindComponent(FactoryDependency.class);

        final Check check = new Check();

        registry.bindInstance(check);

        EasyMock.expect(factory.resolve(EasyMock.notNull(), EasyMock.notNull()))
                .andAnswer(new FactoryAnswer(Check.class, check, DependentValue.class))
                .anyTimes();

        verify(() -> verifyComponent(container));

        assert Value.dependent.context() != null;
    }

    @Test
    public void invokesVariantsFactoryClassOnceInLinkedContainer() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(FactoryDependency.class);

        final ComponentContainer.Registry child = registry.isolateComponent(DependentFactory.class);

        final Check check = new Check();

        child.bindInstance(check);

        EasyMock.expect(factory.resolve(EasyMock.notNull(), EasyMock.notNull()))
                .andAnswer(new FactoryAnswer(Check.class, check, DependentValue.class))
                .anyTimes();

        verify(() -> verifyComponent(container));
    }

    private void checkContext(final ComponentContext check, final ComponentContext against) {
        if (against != null) {
            assert check != null;
            assert !check.defines(Annotation.class);

            for (final Class<? extends Annotation> key : against.types()) {
                final Annotation[] value = check.qualifiers(key);
                assert value != null : String.format("Context %s not found", key);
                assert Arrays.equals(value, against.qualifiers(key)) : String.format("Context %s expected %s, got %s", key, Arrays.asList(value), Arrays.asList(against.qualifiers(key)));
            }
        }
    }

    private ComponentContext context(final Class<?> componentClass, final Class<? extends ComponentFactory> factoryClass) {
        final Annotation[] contentContext = componentClass.getAnnotations();
        final Component.Qualifiers factoryContext = factoryClass == null ? null : factoryClass.getAnnotation(Component.Qualifiers.class);
        final Set<Class<? extends Annotation>> validTypes = factoryContext == null
            ? null
            : new HashSet<>(Arrays.asList(factoryContext.value()));

        final Map<Class<? extends Annotation>, Annotation[]> map = new HashMap<>();

        if (contentContext != null) {
            for (final Annotation value : contentContext) {
                final Class<? extends Annotation> type = value.getClass();

                if (validTypes == null || validTypes.contains(type)) {
                    map.put(type, new Annotation[] { value });
                }
            }
        }

        return artifacts.createContext(map);
    }

    private void verifyContext(final OpenContainer container, final Class<?> contextConsumer) {
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
        final Component.Qualifiers accepted = consumer.getAnnotation(Component.Qualifiers.class);

        final Set<Class<? extends Annotation>> set = new HashSet<>(context.types());
        set.retainAll(Arrays.asList(accepted.value()));

        final Map<Class<? extends Annotation>, Annotation[]> map = new HashMap<>();
        for (final Class<? extends Annotation> type : set) {
            map.put(type, context.qualifiers(type));
        }

        return artifacts.createContext(map);
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

        verify(() -> {
            verifyComponent(container);

            // get objects that specify all contexts
            verifyContext(container, ContextDependentValue.class);
        });
    }

    @Test
    public void factoryCreatesMultipleInstances() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(DependentFactory.class);
        registry.bindComponent(FactoryDependency.class);

        registry.bindComponent(ContextProvider0.class);
        registry.bindComponent(ContextProvider1.class);
        registry.bindComponent(ContextProvider2.class);

        final Check check = new Check();

        registry.bindInstance(check);

        EasyMock.expect(factory.resolve(EasyMock.notNull(), EasyMock.notNull()))
                .andAnswer(new FactoryAnswer(Check.class, check, ContextDependentValue.class))
                .anyTimes();

        verify(() -> {
            verifyComponent(container);

            // get objects that specify all contexts
            verifyContext(container, ContextDependentValue.class);
        });

        // only one factory instance should be created as opposed to one for every context
        assert DependentFactory.used.size() == 1 : DependentFactory.used.size();
    }

    @Test
    public void variantsFactoryCreatesMultipleInstances() throws Exception {
        registry.bindComponent(Value.class);

        registry.bindComponent(ContextProvider0.class);
        registry.bindComponent(ContextProvider1.class);
        registry.bindComponent(ContextProvider2.class);

        registry.bindComponent(DependentFactory.class);
        registry.bindComponent(FactoryDependency.class);

        final Check check = new Check();

        registry.bindInstance(check);

        EasyMock.expect(factory.resolve(EasyMock.notNull(), EasyMock.notNull()))
                .andAnswer(new FactoryAnswer(Check.class, check, DependentValue.class))
                .anyTimes();

        verify(() -> {
            verifyComponent(container);

            // get objects that specify all contexts
            verifyContext(container, DependentFactory.class);
        });
    }

    @Test
    public void variantsFactoryCreatesMultipleInstancesInLinkedContainer() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(FactoryDependency.class);

        registry.bindComponent(ContextProvider0.class);
        registry.bindComponent(ContextProvider1.class);
        registry.bindComponent(ContextProvider2.class);

        final Check check = new Check();

        registry.isolateComponent(DependentFactory.class).bindInstance(check);

        EasyMock.expect(factory.resolve(EasyMock.notNull(), EasyMock.notNull()))
                .andAnswer(new FactoryAnswer(Check.class, check, DependentValue.class))
                .anyTimes();

        verify(() -> {
            verifyComponent(container);

            // get objects that specify all contexts
            verifyContext(container, DependentFactory.class);
        });
    }

    @Test
    public void variantsFactoryCreatesMultipleInstancesInChildContainer() throws Exception {
        registry.bindComponent(Value.class);
        registry.bindComponent(FactoryDependency.class);
        registry.bindComponent(DependentValue.class);

        final Check check = new Check();

        final OpenContainer child = container.makeChildContainer(registry -> {
            registry.bindComponent(ContextProvider1.class);
            registry.bindComponent(ContextProvider0.class);
            registry.bindComponent(ContextProvider2.class);

            // dependency binding component was added to the parent container but must still be found from inside the child
            registry.bindComponent(DependentFactory.class);

            registry.bindInstance(check);
        });

        EasyMock.expect(factory.resolve(EasyMock.notNull(), EasyMock.notNull()))
                .andAnswer(new FactoryAnswer(Check.class, check, DependentValue.class))
                .anyTimes();

        verify(() -> {
            verifyComponent(container);

            // get objects that specify all contexts
            verifyContext(child, DependentFactory.class);
        });
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
    public void variantsFactoryExpandsContext() throws Exception {
        registry.bindComponent(ContextExpansionFactory.class);            // accepts Setting2 while the actual component accepts Setting1
        registry.bindComponent(ContextProvider1.class);

        EasyMock.expect(factory.resolve(EasyMock.notNull(), EasyMock.notNull())).andAnswer(() -> {
            final ComponentContext context = (ComponentContext) EasyMock.getCurrentArguments()[0];
            assert !context.defines(Setting1.class) : Setting1.class;
            assert context.defines(Setting2.class) : Setting2.class;
            return ComponentFactory.Instance.of(ContextDependentValue.class, registry -> registry.bindComponent(ContextDependentValue.class));
        });

        final ContextProvider component = verify(() -> container.getComponent(ContextProvider1.class));

        assert component != null : ContextProvider1.class;

        assert component.dependency.context().defines(Setting1.class) : Setting1.class;
        assert !component.dependency.context().defines(Setting2.class) : Setting2.class;
    }

    @Test
    public void variantsFactoryCanUseDelegateContext() throws Exception {
        registry.bindComponent(DependentFactory.class);            // accepts setting1 and Setting2
        registry.bindComponent(ContextProvider1.class);
        registry.bindComponent(FactoryDependency.class);

        EasyMock.expect(factory.resolve(EasyMock.notNull(), EasyMock.notNull())).andAnswer(() -> {
            final ComponentContext context = (ComponentContext) EasyMock.getCurrentArguments()[0];
            assert context.defines(Setting1.class) : Setting1.class;
            assert context.defines(Setting2.class) : Setting2.class;
            return ComponentFactory.Instance.of(ContextDependentValue.class, registry -> registry.bindComponent(ContextDependentValue.class));
        });

        final ContextProvider component = verify(() -> container.getComponent(ContextProvider1.class));

        assert component != null : ContextProvider1.class;

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
        registry.bindComponent(GroupMember2.class);
        registry.bindComponent(GroupMember1.class);
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

        final List<Class<? extends GroupApi>> group1 = new ArrayList<>();
        final List<Class<? extends GroupApi>> group2 = new ArrayList<>();

        for (final GroupApi member : dependent1.group) {
            group1.add(member.getClass());

            if (member instanceof GroupMember2) {
                assert Objects.equals(GroupDependent1.class.getAnnotation(Setting1.class).value(), ((GroupMember2) member).setting)
                        : ((GroupMember2) member).setting;
            }
        }

        for (final GroupApi member : dependent2.group) {
            group2.add(member.getClass());

            if (member instanceof GroupMember2) {
                assert Objects.equals(((Setting1) GroupDependent2.class.getConstructor(GroupApi[].class).getParameterAnnotations()[0][0]).value(), ((GroupMember2) member).setting)
                        : ((GroupMember2) member).setting;
            }
        }

        assert Objects.equals(expected, group1) : group1;
        assert Objects.equals(expected, group2) : group2;
    }

    @ComponentGroup
    public interface GroupApi { }

    private static class GroupMember1 implements GroupApi { }

    @Component(api = GroupMember2.class, automatic = false)
    @Component.Qualifiers(Setting1.class)
    private static class GroupMember2Variants implements ComponentFactory {

        public Instance resolve(final ComponentContext context, final Container dependencies) throws Exception {
            return Instance.of(GroupMember2.class, registry -> {
                final Setting1 annotation = context.qualifier(Setting1.class, null);
                registry.bindInstance(annotation == null ? null : annotation.value(), String.class);
                registry.bindComponent(GroupMember2.class);
            });
        }
    }

    private static class GroupMember2 implements GroupApi {
        public final String setting;

        public GroupMember2(final String setting) {
            this.setting = setting;
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

    /**
     * This is intentionally private - makes sure the container is able to instantiate non-public classes
     */
    @Component(automatic = false)
    @Component.Qualifiers(Setting1.class)
    private static class ContextDependentValue extends DependentValue {

        public ContextDependentValue(final ComponentContext context) {
            super(context);
        }
    }

    @Component(api = DependentKey.class, automatic = false)
    @Component.Qualifiers({ Setting1.class, Setting2.class })
    private static class DependentFactory implements ComponentFactory {

        public static ComponentFactory delegate;
        public static Set<DependentFactory> used = new HashSet<>();

        public DependentFactory(final FactoryDependency dependent) {
            assert dependent != null;
            used.add(DependentFactory.this);
        }

        @SuppressWarnings("TrivialMethodReference")
        public Instance resolve(final ComponentContext context, final Container dependencies) throws Exception {
            assert delegate != null;
            final Instance instance = delegate.resolve(context, dependencies);

            assert instance != null;
            return instance;
        }
    }

    @Component(api = DependentKey.class, automatic = false)
    @Component.Qualifiers(Setting2.class)
    private static class ContextExpansionFactory implements ComponentFactory {

        public static ComponentFactory delegate;

        @SuppressWarnings("TrivialMethodReference")
        public Instance resolve(final ComponentContext context, final Container dependencies) throws Exception {
            assert delegate != null;
            final Instance instance = delegate.resolve(context, dependencies);

            assert instance != null;
            return instance;
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

    private static class FactoryAnswer implements IAnswer<ComponentFactory.Instance> {

        private final Class<?> checkKey;
        private final Object checkValue;
        private final Class type;

        FactoryAnswer(final Class<?> checkKey, final Object checkValue, final Class type) {
            this.checkKey = checkKey;
            this.checkValue = checkValue;
            this.type = type;
        }

        public ComponentFactory.Instance answer() throws Throwable {
            final ComponentFactory.Container container = (ComponentFactory.Container) EasyMock.getCurrentArguments()[1];
            assert container != null : "Received no container";

            final ComponentFactory.Resolver resolver = container.resolver();
            assert resolver != null : "Received no resolver";

            final Dependency<?> dependency = resolver.lookup(checkKey);
            assert dependency != null && dependency.instance() == checkValue : "Container does not check up";

            final Object[] arguments = EasyMock.getCurrentArguments();

            return ComponentFactory.Instance.of(type, registry -> {
                registry.bindInstance(arguments[0]);
                registry.bindComponent(type);
            });
        }
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER })
    @SuppressWarnings("UnusedDeclaration")
    public @interface Setting1 {

        String value();
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER })
    @SuppressWarnings("UnusedDeclaration")
    public @interface Setting2 {

        String value();
    }

    @Setting1("value1")
    @Setting2("value2")
    @Component(automatic = false)
    @Component.Qualifiers(Setting1.class)
    public static class ContextConsumer {

        public ContextOblivious dependency;

        public ContextConsumer(final ContextOblivious dependency) {
            this.dependency = dependency;
        }
    }

    @Component(automatic = false)
    public static class ContextOblivious { }
}

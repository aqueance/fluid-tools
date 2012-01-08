/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.composition.tests;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.OpenComponentContainer;
import org.fluidity.composition.Optional;
import org.fluidity.composition.spi.CustomComponentFactory;
import org.fluidity.foundation.Generics;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
public final class ComponentGroupTests extends AbstractContainerTests {

    public ComponentGroupTests(final ArtifactFactory factory) {
        super(factory);
    }

    @Test
    public void componentResolved() throws Exception {
        registry.bindComponent(Filter1.class);
        registry.bindComponent(Filter2.class);

        checkComponentOrder(container.getComponentGroup(Filter.class), Filter1.class, Filter2.class);
    }

    @Test
    public void dependencyResolved() throws Exception {
        registry.bindComponent(Processor.class);
        registry.bindComponent(Filter1.class);
        registry.bindComponent(Filter2.class);
        assert container.getComponent(Processor.class) != null;
    }

    @Test
    public void testMemberOrder() throws Exception {
        registry.bindComponent(OrderedFilter8.class);
        registry.bindComponent(OrderedFilter7.class);
        registry.bindComponent(OrderedFilter6.class);
        registry.bindComponent(OrderedFilter5.class);
        registry.bindComponent(OrderedFilter4.class);
        registry.bindComponent(OrderedFilter3.class);
        registry.bindComponent(OrderedFilter2.class);
        registry.bindComponent(OrderedFilter1.class);
        registry.bindComponent(OrderedFilter9.class);

        container.getComponent(Serializable.class);     // instantiates OrderedFilter9 before any other group item; depends OrderedFilter1

        checkComponentOrder(container.getComponentGroup(Filter.class),
                            OrderedFilter1.class,
                            OrderedFilter9.class,
                            OrderedFilter2.class,
                            OrderedFilter3.class,
                            OrderedFilter4.class,
                            OrderedFilter5.class,
                            OrderedFilter6.class,
                            OrderedFilter7.class,
                            OrderedFilter8.class);
    }

    @Test
    public void testDynamicOrder() throws Exception {
        registry.bindComponent(DynamicFilter1Factory.class);   // dynamically depends on OrderedFilter4 and DynamicFilter3
        registry.bindComponent(DynamicFilter2Factory.class);   // dynamically depends on OrderedFilter8
        registry.bindComponent(DynamicFilter3Factory.class);   // dynamically depends on OrderedFilter2
        registry.bindComponent(OrderedFilter8.class);
        registry.bindComponent(OrderedFilter7.class);
        registry.bindComponent(OrderedFilter6.class);
        registry.bindComponent(OrderedFilter5.class);
        registry.bindComponent(OrderedFilter4.class);
        registry.bindComponent(OrderedFilter3.class);
        registry.bindComponent(OrderedFilter2.class);
        registry.bindComponent(OrderedFilter1.class);

        // first resolution
        final Filter[] group = container.getComponentGroup(Filter.class);

        checkComponentOrder(group,
                            OrderedFilter1.class,
                            OrderedFilter2.class,
                            DynamicFilter3.class,
                            OrderedFilter3.class,
                            OrderedFilter4.class,
                            DynamicFilter1.class,
                            OrderedFilter5.class,
                            OrderedFilter6.class,
                            OrderedFilter7.class,
                            OrderedFilter8.class,
                            DynamicFilter2.class);

        // subsequent resolution
        final Filter[] persistent = container.getComponentGroup(Filter.class);

        checkComponentOrder(persistent,
                            OrderedFilter1.class,
                            OrderedFilter2.class,
                            DynamicFilter3.class,
                            OrderedFilter3.class,
                            OrderedFilter4.class,
                            DynamicFilter1.class,
                            OrderedFilter5.class,
                            OrderedFilter6.class,
                            OrderedFilter7.class,
                            OrderedFilter8.class,
                            DynamicFilter2.class);
    }

    @Test
    public void testDependentGroupInHierarchy() throws Exception {
        final OpenComponentContainer child = registry.makeChildContainer();
        final ComponentContainer.Registry nested = child.getRegistry();

        registry.bindComponent(OrderedFilter4.class);
        registry.bindComponent(OrderedFilter3.class);
        registry.bindComponent(OrderedFilter2.class);
        registry.bindComponent(OrderedFilter1.class);

        nested.bindComponent(OrderedFilter8.class);
        nested.bindComponent(OrderedFilter7.class);
        nested.bindComponent(OrderedFilter6.class);
        nested.bindComponent(OrderedFilter5.class);

        checkComponentOrder(container.getComponentGroup(Filter.class), OrderedFilter1.class, OrderedFilter2.class, OrderedFilter3.class, OrderedFilter4.class);

        checkComponentOrder(child.getComponentGroup(Filter.class),
                            OrderedFilter1.class,
                            OrderedFilter2.class,
                            OrderedFilter3.class,
                            OrderedFilter4.class,
                            OrderedFilter5.class,
                            OrderedFilter6.class,
                            OrderedFilter7.class,
                            OrderedFilter8.class);
    }

    @Test
    public void testGroupInHierarchy() throws Exception {
        final OpenComponentContainer child = registry.makeChildContainer();
        final ComponentContainer.Registry nested = child.getRegistry();

        registry.bindComponent(Filter1.class);
        nested.bindComponent(Filter2.class);

        checkComponentOrder(container.getComponentGroup(Filter.class), Filter1.class);
        checkComponentOrder(child.getComponentGroup(Filter.class), Filter1.class, Filter2.class);
    }

    @Test(expectedExceptions = ComponentContainer.CircularReferencesException.class)
    public void testCircularDependency() throws Exception {
        registry.bindComponent(CircularFilter1.class);
        registry.bindComponent(CircularFilter2.class);
        registry.bindComponent(CircularFilter3.class);
        container.getComponentGroup(Filter.class);
    }

    @Test(expectedExceptions = ComponentContainer.CircularReferencesException.class)
    public void testGroupDependentMember() throws Exception {
        registry.bindComponent(GroupDependentMember.class);
        registry.bindComponent(Filter1.class);
        registry.bindComponent(Filter2.class);
        container.getComponentGroup(Filter.class);
    }

    @Test(expectedExceptions = ComponentContainer.ResolutionException.class, expectedExceptionsMessageRegExp = ".*Filter\\[\\].*")
    public void testMandatoryGroupDependency() throws Exception {
        registry.bindComponent(Processor.class);

        try {
            container.getComponent(Processor.class);
        } catch (final ComponentContainer.InstantiationException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test
    public void testOptionalGroupDependency() throws Exception {
        registry.bindComponent(OptionalProcessor.class);
        assert container.getComponent(OptionalProcessor.class) != null;
    }

    @Test
    public void testComponentMember() throws Exception {
        registry.bindComponent(ComponentImpl.class);

        final ComponentApi component = container.getComponent(ComponentApi.class);
        assert component != null : ComponentApi.class;
        assert container.getComponent(ComponentImpl.class) == component : ComponentImpl.class;

        final GroupApi[] group = container.getComponentGroup(GroupApi.class);
        assert group != null : GroupApi.class;
        assert group.length == 1;
        assert group[0] == component;
    }

    private <T> void checkComponentOrder(final T[] group, final Class<? extends T>... types) {
        final List<Class<? extends T>> expected = Arrays.asList(types);
        final List<Class<?>> actual = new ArrayList<Class<?>>();

        for (final T t : group) {
            actual.add(t.getClass());
        }

        assert actual.equals(expected) : String.format("Expected %s, got %s", expected, actual);
    }

    @ComponentGroup
    public interface Filter { }

    @Component(automatic = false)
    public static class Processor {

        public Processor(final @ComponentGroup Filter[] filters) {
            assert filters != null : "Component group dependency was not resolved";
            assert filters.length == 2 : String.format("Component group dependency did not find all implementations: %s", Arrays.toString(filters));

            final Set<Class<? extends Filter>> provided = new HashSet<Class<? extends Filter>>();
            for (final Filter filter : filters) {
                provided.add(filter.getClass());
            }

            assert provided.equals(new HashSet<Class<? extends Filter>>(Arrays.asList(Filter1.class, Filter2.class))) : provided;
        }
    }

    @Component(automatic = false)
    public static class OptionalProcessor {

        public OptionalProcessor(final @Optional @ComponentGroup Filter[] filters) {
            assert filters == null;
        }
    }

    public static class Filter1 implements Filter {

    }

    public static class Filter2 implements Filter {

    }

    private static class OrderedFilter1 implements Filter {

    }

    @SuppressWarnings("UnusedDeclaration")
    private static class OrderedFilter2 implements Filter {

        private OrderedFilter2(final OrderedFilter1 dependency) {
            assert dependency != null : OrderedFilter1.class;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class OrderedFilter3 implements Filter {

        private OrderedFilter3(final OrderedFilter2 dependency) {
            assert dependency != null : OrderedFilter2.class;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class OrderedFilter4 implements Filter {

        private OrderedFilter4(final OrderedFilter3 dependency) {
            assert dependency != null : OrderedFilter3.class;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class OrderedFilter5 implements Filter {

        private OrderedFilter5(final OrderedFilter4 dependency) {
            assert dependency != null : OrderedFilter4.class;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class OrderedFilter6 implements Filter {

        private OrderedFilter6(final OrderedFilter5 dependency) {
            assert dependency != null : OrderedFilter5.class;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class OrderedFilter7 implements Filter {

        private OrderedFilter7(final OrderedFilter6 dependency) {
            assert dependency != null : OrderedFilter6.class;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class OrderedFilter8 implements Filter {

        private OrderedFilter8(final OrderedFilter7 dependency) {
            assert dependency != null : OrderedFilter7.class;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @Component(api = Serializable.class, automatic = false)
    private static class OrderedFilter9 implements Filter, Serializable {

        private OrderedFilter9(final OrderedFilter1 dependency) {
            assert dependency != null : OrderedFilter1.class;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class CircularFilter1 implements Filter {

        private CircularFilter1(final CircularFilter2 dependency) {
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class CircularFilter2 implements Filter {

        private CircularFilter2(final CircularFilter3 dependency) {
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class CircularFilter3 implements Filter {

        private CircularFilter3(final CircularFilter1 dependency) {
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class GroupDependentMember implements Filter {

        public GroupDependentMember(final @ComponentGroup Filter[] filters) {
            assert false;
        }
    }

    private static abstract class DynamicFilterFactory implements CustomComponentFactory {

        private final Class<?> type;

        protected DynamicFilterFactory() {
            this.type = getClass().getAnnotation(Component.class).api()[0];
        }

        public final Instance resolve(final ComponentContext context, final Resolver dependencies) throws ComponentContainer.ResolutionException {
            final Type reference = context.annotation(Component.Reference.class, type).type();

            assert reference != null;
            assert !Generics.rawType(reference).isArray() : reference;

            dependencies.discover(type);

            return new Instance() {
                public void bind(final Registry registry) throws ComponentContainer.BindingException {
                    registry.bindComponent(type);
                }
            };
        }
    }

    @Component(api = DynamicFilter1.class)
    @Component.Context(typed = true)
    private static final class DynamicFilter1Factory extends DynamicFilterFactory { }

    @Component(api = DynamicFilter2.class)
    @Component.Context(typed = true)
    private static final class DynamicFilter2Factory extends DynamicFilterFactory { }

    @Component(api = DynamicFilter3.class)
    @Component.Context(typed = true)
    private static final class DynamicFilter3Factory extends DynamicFilterFactory { }

    @SuppressWarnings("UnusedDeclaration")
    @Component(automatic = false)
    private static class DynamicFilter1 implements Filter {

        private DynamicFilter1(final DynamicFilter3 dependency1, final OrderedFilter4 dependency2) { }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class DynamicFilter2 implements Filter {
        private final Filter filter = new Filter() { };     // should be ignored in the group calculations

        private DynamicFilter2(final OrderedFilter8 dependency) { }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class DynamicFilter3 implements Filter {

        private DynamicFilter3(final OrderedFilter2 dependency) { }
    }

    private interface GroupApi { }

    private interface ComponentApi { }

    @Component(automatic = false)
    @ComponentGroup(api = GroupApi.class)
    private static class ComponentImpl implements ComponentApi, GroupApi { }
}

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
public final class ComponentGroupTests extends AbstractContainerTests {

    public ComponentGroupTests(final ContainerFactory factory) {
        super(factory);
    }

    @Test
    public void componentResolved() throws Exception {
        registry.bindComponent(Filter1.class);
        registry.bindComponent(Filter2.class);

        final Filter[] group = container.getComponentGroup(Filter.class);

        assert group != null;
        assert group.length == 2;

        checkComponentOrder(group, Filter1.class, Filter2.class);
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
        final Filter[] group = container.getComponentGroup(Filter.class);

        assert group != null;
        assert group.length == 9;

        checkComponentOrder(group,
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
        registry.bindComponent(DynamicFilter1.class);   // dynamically depends on OrderedFilter4 and DynamicFilter3
        registry.bindComponent(DynamicFilter2.class);   // dynamically depends on OrderedFilter8
        registry.bindComponent(DynamicFilter3.class);   // dynamically depends on OrderedFilter2
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

        assert group != null;
        assert group.length == 11;

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

        final Filter[] parentGroup = container.getComponentGroup(Filter.class);

        assert parentGroup != null;
        assert parentGroup.length == 4;

        checkComponentOrder(parentGroup, OrderedFilter1.class, OrderedFilter2.class, OrderedFilter3.class, OrderedFilter4.class);

        final Filter[] childGroup = child.getComponentGroup(Filter.class);

        assert childGroup != null;
        assert childGroup.length == 8;

        checkComponentOrder(childGroup,
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

        final Filter[] group = child.getComponentGroup(Filter.class);

        assert group != null;
        assert group.length == 2;

        checkComponentOrder(group, Filter1.class, Filter2.class);
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
        container.getComponent(Processor.class);
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
    public static interface Filter {

    }

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

    @SuppressWarnings("UnusedDeclaration")
    private static class DynamicFilter1 implements Filter {

        private DynamicFilter1(final ComponentContainer container) {
            container.getComponent(DynamicFilter3.class);
            container.getComponent(OrderedFilter4.class);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class DynamicFilter2 implements Filter {
        private final Filter filter = new Filter() { };     // should be ignored in the group calculations

        private DynamicFilter2(final ComponentContainer container) {
            container.getComponent(OrderedFilter8.class);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class DynamicFilter3 implements Filter {

        private DynamicFilter3(final ComponentContainer container) {
            container.getComponent(OrderedFilter2.class);
        }
    }

    private static interface GroupApi { }

    private static interface ComponentApi { }

    @Component(automatic = false)
    @ComponentGroup(api = GroupApi.class)
    private static class ComponentImpl implements ComponentApi, GroupApi { }
}
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
        registry.bindGroup(Filter.class, Filter1.class, Filter2.class);

        final Filter[] group = container.getComponentGroup(Filter.class);

        assert group != null;
        assert group.length == 2;

        assertOrder(group, Filter1.class, Filter2.class);
    }

    @Test
    public void dependencyResolved() throws Exception {
        registry.bindComponent(Processor.class);
        registry.bindGroup(Filter.class, Filter1.class, Filter2.class);
        assert container.getComponent(Processor.class) != null;
    }

    @Test
    public void testMemberOrder() throws Exception {
        registry.bindGroup(Filter.class,
                           OrderedFilter8.class,
                           OrderedFilter7.class,
                           OrderedFilter6.class,
                           OrderedFilter5.class,
                           OrderedFilter4.class,
                           OrderedFilter3.class,
                           OrderedFilter2.class,
                           OrderedFilter1.class);

        final Filter[] group = container.getComponentGroup(Filter.class);

        assert group != null;
        assert group.length == 8;

        assertOrder(group,
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
    public void testDynamicOrder() throws Exception {
        registry.bindGroup(Filter.class,
                           DynamicFilter1.class,    // dynamically depends on OrderedFilter4 and DynamicFilter3
                           DynamicFilter2.class,    // dynamically depends on OrderedFilter8
                           DynamicFilter3.class,    // dynamically depends on OrderedFilter2
                           OrderedFilter8.class,
                           OrderedFilter7.class,
                           OrderedFilter6.class,
                           OrderedFilter5.class,
                           OrderedFilter4.class,
                           OrderedFilter3.class,
                           OrderedFilter2.class,
                           OrderedFilter1.class);

        // first resolution
        final Filter[] group = container.getComponentGroup(Filter.class);

        assert group != null;
        assert group.length == 11;

        assertOrder(group,
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

        assertOrder(persistent,
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

        registry.bindGroup(Filter.class, OrderedFilter4.class, OrderedFilter3.class, OrderedFilter2.class, OrderedFilter1.class);

        nested.bindGroup(Filter.class, OrderedFilter8.class, OrderedFilter7.class, OrderedFilter6.class, OrderedFilter5.class);

        final Filter[] parentGroup = container.getComponentGroup(Filter.class);

        assert parentGroup != null;
        assert parentGroup.length == 4;

        assertOrder(parentGroup, OrderedFilter1.class, OrderedFilter2.class, OrderedFilter3.class, OrderedFilter4.class);

        final Filter[] childGroup = child.getComponentGroup(Filter.class);

        assert childGroup != null;
        assert childGroup.length == 8;

        assertOrder(childGroup,
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

        registry.bindGroup(Filter.class, Filter1.class);
        nested.bindGroup(Filter.class, Filter2.class);

        final Filter[] group = child.getComponentGroup(Filter.class);

        assert group != null;
        assert group.length == 2;

        assertOrder(group, Filter1.class, Filter2.class);
    }

    @Test(expectedExceptions = ComponentContainer.CircularReferencesException.class)
    public void testCircularDependency() throws Exception {
        registry.bindGroup(Filter.class, CircularFilter1.class, CircularFilter2.class, CircularFilter3.class);
        container.getComponentGroup(Filter.class);
    }

    @Test(expectedExceptions = ComponentContainer.CircularReferencesException.class)
    public void testGroupDependentMember() throws Exception {
        registry.bindGroup(Filter.class, GroupDependentMember.class, Filter1.class, Filter2.class);
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

    private <T> void assertOrder(final T[] group, final Class<? extends T>... types) {
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

    @Component
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

    @Component
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
}

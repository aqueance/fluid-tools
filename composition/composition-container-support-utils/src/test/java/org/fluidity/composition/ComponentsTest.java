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
import java.util.Arrays;

import org.fluidity.composition.types.AbstractComponent;
import org.fluidity.composition.types.BareComponent;
import org.fluidity.composition.types.ComponentApi1;
import org.fluidity.composition.types.ComponentApi2;
import org.fluidity.composition.types.ComponentApi3;
import org.fluidity.composition.types.ComponentApi4;
import org.fluidity.composition.types.ComponentSubClass;
import org.fluidity.composition.types.FallbackComponent;
import org.fluidity.composition.types.GroupApi1;
import org.fluidity.composition.types.GroupApi2;
import org.fluidity.composition.types.GroupApi3;
import org.fluidity.composition.types.GroupMember1;
import org.fluidity.composition.types.GroupMember2;
import org.fluidity.composition.types.GroupMember3;
import org.fluidity.composition.types.GroupMember4;
import org.fluidity.composition.types.GroupMemberComponent1;
import org.fluidity.composition.types.GroupMemberComponent2;
import org.fluidity.composition.types.GroupMemberComponent3;
import org.fluidity.composition.types.GroupMemberComponent4;
import org.fluidity.composition.types.GroupMemberFactory1;
import org.fluidity.composition.types.GroupMemberFactory2;
import org.fluidity.composition.types.ManualBareComponent;
import org.fluidity.composition.types.ManualFallbackComponent;
import org.fluidity.composition.types.MultipleComponent;
import org.fluidity.composition.types.NoComponent;
import org.fluidity.composition.types.NoComponentFactory;
import org.fluidity.composition.types.NoGroupMember;
import org.fluidity.composition.types.NoVariantFactory;
import org.fluidity.composition.types.OrdinaryClass;
import org.fluidity.composition.types.OrdinarySubClass;
import org.fluidity.composition.types.OverridingComponent;
import org.fluidity.composition.types.OverridingSubComponent;
import org.fluidity.composition.types.ReferencingComponent;
import org.fluidity.composition.types.SelfReferringComponent;
import org.fluidity.composition.types.SingleComponent;
import org.fluidity.composition.types.SingleMultipleComponent;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class ComponentsTest {

    @Test(expectedExceptions = ComponentContainer.BindingException.class, expectedExceptionsMessageRegExp = ".*abstract.*")
    public void abstractClass() throws Exception {
        Components.inspect(AbstractComponent.class);
    }

    @Test(expectedExceptions = ComponentContainer.BindingException.class, expectedExceptionsMessageRegExp = ".*missing.*")
    public void bareComponentFactory() throws Exception {
        Components.inspect(NoComponentFactory.class);
    }

    @Test(expectedExceptions = ComponentContainer.BindingException.class, expectedExceptionsMessageRegExp = ".*missing.*")
    public void bareVariantFactory() throws Exception {
        Components.inspect(NoVariantFactory.class);
    }

    @Test(expectedExceptions = ComponentContainer.BindingException.class, expectedExceptionsMessageRegExp = ".*bound.*")
    public void overridingComponent() throws Exception {
        Components.inspect(OverridingComponent.class);
    }

    @Test(expectedExceptions = ComponentContainer.BindingException.class, expectedExceptionsMessageRegExp = ".*bound.*")
    public void overridingSubComponent() throws Exception {
        Components.inspect(OverridingSubComponent.class);
    }

    @Test(expectedExceptions = ComponentContainer.BindingException.class, expectedExceptionsMessageRegExp = ".*incompatible.*")
    public void noComponent() throws Exception {
        Components.inspect(NoComponent.class);
    }

    @Test(expectedExceptions = ComponentContainer.BindingException.class, expectedExceptionsMessageRegExp = ".*incompatible.*")
    public void noGroupMember() throws Exception {
        Components.inspect(NoGroupMember.class);
    }

    @Test
    public void noComponents() throws Exception {
        checkComponent(OrdinaryClass.class, false, true);
        checkComponent(OrdinarySubClass.class, false, true);
    }

    @Test
    public void bareComponents() throws Exception {
        checkComponent(BareComponent.class, false, false);
        checkComponent(ManualBareComponent.class, true, false);
        checkComponent(FallbackComponent.class, false, true);
        checkComponent(ManualFallbackComponent.class, true, true);
        checkComponent(ComponentSubClass.class, false, false);
    }

    @Test
    public void anonymousComponents() throws Exception {
        final Class<? extends Serializable> innerClass = new Serializable() { }.getClass();
        final Components.Interfaces interfaces = Components.inspect(innerClass);

        checkFlags(innerClass, interfaces, false, true);
        checkInterfaces(innerClass, interfaces.api, Serializable.class);
        checkGroups(interfaces.api[0]);
    }

    @Test
    public void selfReference() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(SelfReferringComponent.class);

        checkFlags(SelfReferringComponent.class, interfaces, false, false);
        checkInterfaces(SelfReferringComponent.class, interfaces.api, SelfReferringComponent.class);
        checkGroups(interfaces.api[0]);
    }

    @Test
    public void singleComponent() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(SingleComponent.class);

        checkFlags(SingleComponent.class, interfaces, false, false);
        checkInterfaces(SingleComponent.class, interfaces.api, ComponentApi1.class);
        checkGroups(interfaces.api[0]);
    }

    @Test
    public void multipleComponent() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(MultipleComponent.class);

        checkFlags(MultipleComponent.class, interfaces, false, false);
        checkInterfaces(MultipleComponent.class, interfaces.api, ComponentApi1.class, ComponentApi2.class);
        checkGroups(interfaces.api[0]);
        checkGroups(interfaces.api[1]);
    }

    @Test
    public void singleMultipleComponent() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(SingleMultipleComponent.class);

        checkFlags(SingleMultipleComponent.class, interfaces, false, false);
        checkInterfaces(SingleMultipleComponent.class, interfaces.api, ComponentApi2.class);
        checkGroups(interfaces.api[0]);
    }

    @Test
    public void referencingComponent() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(ReferencingComponent.class);

        checkFlags(ReferencingComponent.class, interfaces, false, false);
        checkInterfaces(ReferencingComponent.class, interfaces.api, ComponentApi1.class, ComponentApi2.class);
        checkGroups(interfaces.api[0]);
        checkGroups(interfaces.api[1]);
    }

    @Test
    public void groupApiDirectlyInherited() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(GroupMember1.class);

        checkFlags(GroupMember1.class, interfaces, false, false);
        checkInterfaces(GroupMember1.class, interfaces.api, GroupMember1.class);
        checkGroups(interfaces.api[0], GroupApi1.class, GroupApi2.class);
    }

    @Test
    public void groupApiIndirectlyInherited() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(GroupMember2.class);

        checkFlags(GroupMember2.class, interfaces, false, false);
        checkInterfaces(GroupMember2.class, interfaces.api, GroupMember2.class);
        checkGroups(interfaces.api[0], GroupApi1.class, GroupApi2.class);
    }

    @Test
    public void groupApiInferred() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(GroupMember3.class);

        checkFlags(GroupMember3.class, interfaces, false, false);
        checkInterfaces(GroupMember3.class, interfaces.api, GroupMember3.class);
        checkGroups(interfaces.api[0], GroupApi3.class);
    }

    @Test
    public void groupApiExcluded() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(GroupMember4.class);

        checkFlags(GroupMember4.class, interfaces, false, false);
        checkInterfaces(GroupMember4.class, interfaces.api, GroupMember4.class);
        checkGroups(interfaces.api[0], GroupApi3.class);
    }

    @Test
    public void componentGroupMember1() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(GroupMemberComponent1.class);

        checkFlags(GroupMemberComponent1.class, interfaces, false, false);
        checkInterfaces(GroupMemberComponent1.class, interfaces.api, GroupMemberComponent1.class, ComponentApi1.class);
        checkGroups(interfaces.api[0], GroupApi1.class);
        checkGroups(interfaces.api[1]);
    }

    @Test
    public void componentGroupMember2() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(GroupMemberComponent2.class);

        checkFlags(GroupMemberComponent2.class, interfaces, false, false);
        checkInterfaces(GroupMemberComponent2.class, interfaces.api, GroupMemberComponent2.class, ComponentApi2.class);
        checkGroups(interfaces.api[0], GroupApi3.class);
        checkGroups(interfaces.api[1]);
    }

    @Test
    public void componentGroupMember3() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(GroupMemberComponent3.class);

        checkFlags(GroupMemberComponent3.class, interfaces, true, false);
        checkInterfaces(GroupMemberComponent3.class, interfaces.api, GroupMemberComponent3.class, ComponentApi3.class);
        checkGroups(interfaces.api[0], GroupApi1.class, GroupApi2.class);
        checkGroups(interfaces.api[1]);
    }

    @Test
    public void componentGroupMember4() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(GroupMemberComponent4.class);

        checkFlags(GroupMemberComponent4.class, interfaces, true, false);
        checkInterfaces(GroupMemberComponent4.class, interfaces.api, GroupMemberComponent4.class, ComponentApi3.class, ComponentApi4.class);
        checkGroups(interfaces.api[0], GroupApi2.class);
        checkGroups(interfaces.api[1]);
        checkGroups(interfaces.api[2], GroupApi1.class);
    }

    @Test
    public void componentGroupMemberFactory1() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(GroupMemberFactory1.class);

        checkFlags(GroupMemberFactory1.class, interfaces, false, false);
        checkInterfaces(GroupMemberFactory1.class, interfaces.api, GroupMemberComponent3.class, ComponentApi3.class);
        checkGroups(interfaces.api[0], GroupApi1.class, GroupApi2.class);
        checkGroups(interfaces.api[1]);
    }

    @Test
    public void componentGroupMemberFactory2() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(GroupMemberFactory2.class);

        checkFlags(GroupMemberFactory2.class, interfaces, false, false);
        checkInterfaces(GroupMemberFactory2.class, interfaces.api, GroupMemberComponent4.class, ComponentApi3.class, ComponentApi4.class);
        checkGroups(interfaces.api[0], GroupApi2.class);
        checkGroups(interfaces.api[1]);
        checkGroups(interfaces.api[2], GroupApi1.class);
    }

    @Test
    public void primitiveTypes() throws Exception {
        checkPrimitive(Byte.class, Byte.TYPE);
        checkPrimitive(Short.class, Short.TYPE);
        checkPrimitive(Integer.class, Integer.TYPE);
        checkPrimitive(Long.class, Long.TYPE);
        checkPrimitive(Float.class, Float.TYPE);
        checkPrimitive(Double.class, Double.TYPE);
        checkPrimitive(Boolean.class, Boolean.TYPE);
    }

    private void checkPrimitive(final Class<?> type, final Class<?> primitive) {
        final Components.Interfaces interfaces = Components.inspect(type);

        checkFlags(type, interfaces, false, true);
        checkInterfaces(type, interfaces.api, type, primitive);
        checkGroups(interfaces.api[0]);
        checkGroups(interfaces.api[1]);
    }

    private void checkComponent(final Class<?> type, final boolean ignored, final boolean fallback) {
        final Components.Interfaces interfaces = Components.inspect(type);

        checkFlags(type, interfaces, ignored, fallback);
        checkInterfaces(type, interfaces.api, type);
        checkGroups(interfaces.api[0]);
    }

    private void checkFlags(final Class<?> type, final Components.Interfaces interfaces, final boolean ignored, final boolean fallback) {
        assert interfaces != null : type;
        assert interfaces.implementation == type : interfaces.implementation;
        assert interfaces.ignored == ignored : type;
        assert interfaces.fallback == fallback : type;
    }

    private void checkInterfaces(final Class<?> type, final Components.Specification[] specifications, final Class<?>... list) {
        assert specifications != null : type;
        assert specifications.length == list.length : String.format("%s - %s", Arrays.toString(list), Arrays.toString(specifications));

        for (int i = 0, limit = specifications.length; i < limit; i++) {
            final Components.Specification spec = specifications[i];
            assert spec != null : type;
            assert spec.api == list[i] : String.format("%s - %s", list[i], spec.api);
            assert spec.groups != null : type;
        }
    }

    private void checkGroups(final Components.Specification specification, final Class<?>... list) {
        assert specification != null;

        final Class<?> type = specification.api;
        final Class<?>[] groups = specification.groups;

        assert groups != null : type;
        assert groups.length == list.length : Arrays.toString(groups);

        for (int i = 0, limit = groups.length; i < limit; i++) {
            final Class<?> group = groups[i];
            assert group == list[i] : String.format("%s - %s", list[i], group);
        }
    }
}

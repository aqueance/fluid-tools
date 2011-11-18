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
@SuppressWarnings("unchecked")
public class ComponentsTest {

    @Test(expectedExceptions = OpenComponentContainer.BindingException.class, expectedExceptionsMessageRegExp = ".*abstract.*")
    public void abstractClass() throws Exception {
        Components.inspect(AbstractComponent.class);
    }

    @Test(expectedExceptions = OpenComponentContainer.BindingException.class, expectedExceptionsMessageRegExp = ".*missing.*")
    public void bareComponentFactory() throws Exception {
        Components.inspect(NoComponentFactory.class);
    }

    @Test(expectedExceptions = OpenComponentContainer.BindingException.class, expectedExceptionsMessageRegExp = ".*missing.*")
    public void bareVariantFactory() throws Exception {
        Components.inspect(NoVariantFactory.class);
    }

    @Test(expectedExceptions = OpenComponentContainer.BindingException.class, expectedExceptionsMessageRegExp = ".*bound.*")
    public void overridingComponent() throws Exception {
        Components.inspect(OverridingComponent.class);
    }

    @Test(expectedExceptions = OpenComponentContainer.BindingException.class, expectedExceptionsMessageRegExp = ".*bound.*")
    public void overridingSubComponent() throws Exception {
        Components.inspect(OverridingSubComponent.class);
    }

    @Test(expectedExceptions = OpenComponentContainer.BindingException.class, expectedExceptionsMessageRegExp = ".*incompatible.*")
    public void noComponent() throws Exception {
        Components.inspect(NoComponent.class);
    }

    @Test(expectedExceptions = OpenComponentContainer.BindingException.class, expectedExceptionsMessageRegExp = ".*incompatible.*")
    public void noGroupMember() throws Exception {
        Components.inspect(NoGroupMember.class);
    }

    @Test
    public void noComponents() throws Exception {
        checkComponent(OrdinaryClass.class);
        checkComponent(OrdinarySubClass.class);
    }

    @Test
    public void bareComponents() throws Exception {
        checkComponent(BareComponent.class);
        checkComponent(ManualBareComponent.class);
        checkComponent(FallbackComponent.class);
        checkComponent(ManualFallbackComponent.class);
        checkComponent(ComponentSubClass.class);
    }

    @Test
    public void anonymousComponents() throws Exception {
        final Class<? extends Serializable> innerClass = new Serializable() { }.getClass();
        final Components.Interfaces interfaces = Components.inspect(innerClass);

        checkImplementation(innerClass, interfaces);
        checkInterfaces(innerClass, interfaces.api, Serializable.class);
        checkGroups(interfaces.api[0]);
    }

    @Test
    public void selfReference() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(SelfReferringComponent.class);

        checkImplementation(SelfReferringComponent.class, interfaces);
        checkInterfaces(SelfReferringComponent.class, interfaces.api, SelfReferringComponent.class);
        checkGroups(interfaces.api[0]);
    }

    @Test
    public void singleComponent() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(SingleComponent.class);

        checkImplementation(SingleComponent.class, interfaces);
        checkInterfaces(SingleComponent.class, interfaces.api, ComponentApi1.class);
        checkGroups(interfaces.api[0]);
    }

    @Test
    public void multipleComponent() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(MultipleComponent.class);

        checkImplementation(MultipleComponent.class, interfaces);
        checkInterfaces(MultipleComponent.class, interfaces.api, ComponentApi1.class, ComponentApi2.class);
        checkGroups(interfaces.api[0]);
        checkGroups(interfaces.api[1]);
    }

    @Test
    public void singleMultipleComponent() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(SingleMultipleComponent.class);

        checkImplementation(SingleMultipleComponent.class, interfaces);
        checkInterfaces(SingleMultipleComponent.class, interfaces.api, ComponentApi2.class);
        checkGroups(interfaces.api[0]);
    }

    @Test
    public void referencingComponent() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(ReferencingComponent.class);

        checkImplementation(ReferencingComponent.class, interfaces);
        checkInterfaces(ReferencingComponent.class, interfaces.api, ComponentApi1.class, ComponentApi2.class);
        checkGroups(interfaces.api[0]);
        checkGroups(interfaces.api[1]);
    }

    @Test
    public void groupApiDirectlyInherited() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(GroupMember1.class);

        checkImplementation(GroupMember1.class, interfaces);
        checkInterfaces(GroupMember1.class, interfaces.api, GroupMember1.class);
        checkGroups(interfaces.api[0], GroupApi1.class, GroupApi2.class);
    }

    @Test
    public void groupApiIndirectlyInherited() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(GroupMember2.class);

        checkImplementation(GroupMember2.class, interfaces);
        checkInterfaces(GroupMember2.class, interfaces.api, GroupMember2.class);
        checkGroups(interfaces.api[0], GroupApi1.class, GroupApi2.class);
    }

    @Test
    public void groupApiInferred() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(GroupMember3.class);

        checkImplementation(GroupMember3.class, interfaces);
        checkInterfaces(GroupMember3.class, interfaces.api, GroupMember3.class);
        checkGroups(interfaces.api[0], GroupApi3.class);
    }

    @Test
    public void groupApiExcluded() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(GroupMember4.class);

        checkImplementation(GroupMember4.class, interfaces);
        checkInterfaces(GroupMember4.class, interfaces.api, GroupMember4.class);
        checkGroups(interfaces.api[0], GroupApi3.class);
    }

    @Test
    public void componentGroupMember1() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(GroupMemberComponent1.class);

        checkImplementation(GroupMemberComponent1.class, interfaces);
        checkInterfaces(GroupMemberComponent1.class, interfaces.api, GroupMemberComponent1.class, ComponentApi1.class);
        checkGroups(interfaces.api[0], GroupApi1.class);
        checkGroups(interfaces.api[1]);
    }

    @Test
    public void componentGroupMember2() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(GroupMemberComponent2.class);

        checkImplementation(GroupMemberComponent2.class, interfaces);
        checkInterfaces(GroupMemberComponent2.class, interfaces.api, GroupMemberComponent2.class, ComponentApi2.class);
        checkGroups(interfaces.api[0], GroupApi3.class);
        checkGroups(interfaces.api[1]);
    }

    @Test
    public void componentGroupMember3() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(GroupMemberComponent3.class);

        checkImplementation(GroupMemberComponent3.class, interfaces);
        checkInterfaces(GroupMemberComponent3.class, interfaces.api, GroupMemberComponent3.class, ComponentApi3.class);
        checkGroups(interfaces.api[0], GroupApi1.class, GroupApi2.class);
        checkGroups(interfaces.api[1]);
    }

    @Test
    public void componentGroupMember4() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(GroupMemberComponent4.class);

        checkImplementation(GroupMemberComponent4.class, interfaces);
        checkInterfaces(GroupMemberComponent4.class, interfaces.api, GroupMemberComponent4.class, ComponentApi3.class, ComponentApi4.class);
        checkGroups(interfaces.api[0], GroupApi2.class);
        checkGroups(interfaces.api[1]);
        checkGroups(interfaces.api[2], GroupApi1.class);
    }

    @Test
    public void componentGroupMemberFactory1() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(GroupMemberFactory1.class);

        checkImplementation(GroupMemberFactory1.class, interfaces);
        checkInterfaces(GroupMemberFactory1.class, interfaces.api, GroupMemberComponent3.class, ComponentApi3.class);
        checkGroups(interfaces.api[0], GroupApi1.class, GroupApi2.class);
        checkGroups(interfaces.api[1]);
    }

    @Test
    public void componentGroupMemberFactory2() throws Exception {
        final Components.Interfaces interfaces = Components.inspect(GroupMemberFactory2.class);

        checkImplementation(GroupMemberFactory2.class, interfaces);
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

        checkImplementation(type, interfaces);
        checkInterfaces(type, interfaces.api, type, primitive);
        checkGroups(interfaces.api[0]);
        checkGroups(interfaces.api[1]);
    }

    private void checkComponent(final Class<?> type) {
        final Components.Interfaces interfaces = Components.inspect(type);

        checkImplementation(type, interfaces);
        checkInterfaces(type, interfaces.api, type);
        checkGroups(interfaces.api[0]);
    }

    private void checkImplementation(final Class<?> type, final Components.Interfaces interfaces) {
        assert interfaces != null : type;
        assert interfaces.implementation == type : interfaces.implementation;
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

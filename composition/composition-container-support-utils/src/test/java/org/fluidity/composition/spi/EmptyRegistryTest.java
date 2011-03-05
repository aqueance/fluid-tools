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

package org.fluidity.composition.spi;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.OpenComponentContainer;
import org.fluidity.tests.MockGroupAbstractTest;

import org.easymock.EasyMock;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class EmptyRegistryTest extends MockGroupAbstractTest {

    private final ComponentRegistry mock = addControl(ComponentRegistry.class);
    private final OpenComponentContainer container = addControl(OpenComponentContainer.class);
    private final ComponentContainer.Registry registry = new EmptyRegistry(mock);

    @Test
    public void childContainer() throws Exception {
        EasyMock.expect(mock.makeChildContainer()).andReturn(container);

        replay();
        final OpenComponentContainer container = registry.makeChildContainer();
        verify();

        assert container == this.container;
    }

    @Test
    public void linkingContainer() throws Exception {
        EasyMock.expect(mock.makeChildContainer(EasyMock.same(MarkedGroupComponent.class),
                                                EasyMock.aryEq(new Class[] { MarkedGroupComponent.class, Interface1.class, Interface2.class }),
                                                EasyMock.aryEq(new Class[] { GroupInterface3.class, GroupInterface1.class, GroupInterface2.class })))
                .andReturn(container);

        replay();
        final OpenComponentContainer container = registry.makeChildContainer(MarkedGroupComponent.class);
        verify();

        assert container == this.container;
    }

    @Test
    public void specifiedLinkingContainer() throws Exception {
        EasyMock.expect(mock.makeChildContainer(EasyMock.same(MarkedGroupComponent.class),
                                                EasyMock.aryEq(new Class[] { MarkedGroupComponent.class, Interface1.class, Interface2.class }),
                                                EasyMock.aryEq(new Class[] { GroupInterface3.class, GroupInterface1.class, GroupInterface2.class })))
                .andReturn(container);

        replay();
        final OpenComponentContainer container = registry.makeChildContainer(MarkedGroupComponent.class);
        verify();

        assert container == this.container;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void overriddenLinkingContainer() throws Exception {
        EasyMock.expect(mock.makeChildContainer(EasyMock.same(MarkedGroupComponent.class),
                                                EasyMock.aryEq(new Class[] { MarkedGroupComponent.class, Interface2.class }),
                                                EasyMock.aryEq(new Class[] { GroupInterface3.class, GroupInterface1.class, GroupInterface2.class })))
                .andReturn(container);

        replay();
        final OpenComponentContainer container = registry.makeChildContainer(MarkedGroupComponent.class, Interface2.class);
        verify();

        assert container == this.container;
    }

    @Test(expectedExceptions = ComponentContainer.BindingException.class, expectedExceptionsMessageRegExp = ".*assign.*")
    public void invalidComponent() throws Exception {

        // group api refers to incompatible interface
        replay();
        registry.bindComponent(InvalidComponent.class);
        verify();

        final Object component = new InvalidComponent();

        replay();
        registry.bindInstance(component);
        verify();
    }

    @Test(expectedExceptions = ComponentContainer.BindingException.class, expectedExceptionsMessageRegExp = ".*assign.*")
    public void invalidGroupComponent() throws Exception {

        // group api refers to incompatible interface
        replay();
        registry.bindComponent(InvalidGroupComponent.class);
        verify();

        final Object component = new InvalidGroupComponent();

        replay();
        registry.bindInstance(component);
        verify();
    }

    @Test
    public void standaloneComponentWithNoAnnotation() throws Exception {

        // no annotation and no interfaces: class itself for component API and no group API
        mock.bindComponent(EasyMock.same(UnmarkedStandaloneComponent.class),
                           EasyMock.aryEq(new Class[] { UnmarkedStandaloneComponent.class }),
                           EasyMock.<Class[]>isNull());

        replay();
        registry.bindComponent(UnmarkedStandaloneComponent.class);
        verify();

        final Object component = new UnmarkedStandaloneComponent();

        mock.bindInstance(EasyMock.same(component), EasyMock.aryEq(new Class[] { UnmarkedStandaloneComponent.class }), EasyMock.<Class[]>isNull());

        replay();
        registry.bindInstance(component);
        verify();
    }

    @Test
    public void implementationWithNoAnnotation() throws Exception {

        // no annotation and has interfaces: class itself for component API and no group API
        mock.bindComponent(EasyMock.same(UnmarkedComponent.class), EasyMock.aryEq(new Class[] { UnmarkedComponent.class }), EasyMock.<Class[]>isNull());

        replay();
        registry.bindComponent(UnmarkedComponent.class);
        verify();

        final Object component = new UnmarkedComponent();

        mock.bindInstance(EasyMock.same(component), EasyMock.aryEq(new Class[] { UnmarkedComponent.class }), EasyMock.<Class[]>isNull());

        replay();
        registry.bindInstance(component);
        verify();
    }

    @Test
    public void subclassWithNoAnnotation() throws Exception {

        // no annotation with super class that has interfaces: class itself for component API and no group API
        mock.bindComponent(EasyMock.same(UnmarkedSubclass.class), EasyMock.aryEq(new Class[] { UnmarkedSubclass.class }), EasyMock.<Class[]>isNull());

        replay();
        registry.bindComponent(UnmarkedSubclass.class);
        verify();

        final Object component = new UnmarkedSubclass();

        mock.bindInstance(EasyMock.same(component), EasyMock.aryEq(new Class[] { UnmarkedSubclass.class }), EasyMock.<Class[]>isNull());

        replay();
        registry.bindInstance(component);
        verify();
    }

    @Test
    public void subclassWithComponentAnnotation() throws Exception {

        // no annotation with interface and super class that has interfaces: direct interface for component API and no group API
        mock.bindComponent(EasyMock.same(ComponentSubclass.class), EasyMock.aryEq(new Class[] { ComponentSubclass.class, Interface3.class }), EasyMock.<Class[]>isNull());

        replay();
        registry.bindComponent(ComponentSubclass.class);
        verify();

        final Object component = new ComponentSubclass();

        mock.bindInstance(EasyMock.same(component), EasyMock.aryEq(new Class[] { ComponentSubclass.class, Interface3.class }), EasyMock.<Class[]>isNull());

        replay();
        registry.bindInstance(component);
        verify();
    }

    @Test
    public void componentSubclassWithApi() throws Exception {

        // marker annotation with API and a super class that has interfaces: specified interfaces for component API and no group API
        mock.bindComponent(EasyMock.same(ComponentImplementation.class),
                           EasyMock.aryEq(new Class[] { ComponentImplementation.class, Interface1.class, Interface2.class }),
                           EasyMock.<Class[]>isNull());

        replay();
        registry.bindComponent(ComponentImplementation.class);
        verify();

        final Object component = new ComponentImplementation();

        mock.bindInstance(EasyMock.same(component),
                          EasyMock.aryEq(new Class[] { ComponentImplementation.class, Interface1.class, Interface2.class }),
                          EasyMock.<Class[]>isNull());

        replay();
        registry.bindInstance(component);
        verify();
    }

    @Test
    public void implementationWithGroupAnnotation() throws Exception {

        // group annotation and has interfaces: direct interfaces for group API and class itself component API
        mock.bindComponent(EasyMock.same(GroupComponent.class),
                           EasyMock.aryEq(new Class<?>[] { GroupComponent.class }),
                           EasyMock.aryEq(new Class[] { Interface1.class, Interface2.class }));

        replay();
        registry.bindComponent(GroupComponent.class);
        verify();

        final Object component = new GroupComponent();

        mock.bindInstance(EasyMock.same(component),
                          EasyMock.aryEq(new Class<?>[] { GroupComponent.class }),
                          EasyMock.aryEq(new Class[] { Interface1.class, Interface2.class }));

        replay();
        registry.bindInstance(component);
        verify();
    }

    @Test
    public void subclassWithGroupAnnotation() throws Exception {

        // group annotation with super class that has interfaces: all interfaces for group API and class itself component API
        mock.bindComponent(EasyMock.same(GroupSubclass.class),
                           EasyMock.aryEq(new Class<?>[] { GroupSubclass.class }),
                           EasyMock.aryEq(new Class[] { Interface1.class, Interface2.class }));

        replay();
        registry.bindComponent(GroupSubclass.class);
        verify();

        final Object component = new GroupSubclass();

        mock.bindInstance(EasyMock.same(component),
                          EasyMock.aryEq(new Class<?>[] { GroupSubclass.class }),
                          EasyMock.aryEq(new Class[] { Interface1.class, Interface2.class }));

        replay();
        registry.bindInstance(component);
        verify();
    }

    @Test
    public void componentWithGroupAnnotation() throws Exception {

        // group annotation with interface and super class that has interfaces: direct interfaces for group API and class itself component API
        mock.bindComponent(EasyMock.same(GroupSubclassComponent.class),
                           EasyMock.aryEq(new Class<?>[] { GroupSubclassComponent.class }),
                           EasyMock.aryEq(new Class[] { Interface3.class }));

        replay();
        registry.bindComponent(GroupSubclassComponent.class);
        verify();

        final Object component = new GroupSubclassComponent();

        mock.bindInstance(EasyMock.same(component),
                          EasyMock.aryEq(new Class<?>[] { GroupSubclassComponent.class }),
                          EasyMock.aryEq(new Class[] { Interface3.class }));

        replay();
        registry.bindInstance(component);
        verify();
    }

    @Test
    public void groupSubclassWithApi() throws Exception {

        // group annotation with API and super class that has interfaces: specified interfaces for group API and class itself component API
        mock.bindComponent(EasyMock.same(GroupImplementation.class),
                           EasyMock.aryEq(new Class<?>[] { GroupImplementation.class }),
                           EasyMock.aryEq(new Class[] { Interface1.class, Interface2.class }));

        replay();
        registry.bindComponent(GroupImplementation.class);
        verify();

        final Object component = new GroupImplementation();

        mock.bindInstance(EasyMock.same(component),
                          EasyMock.aryEq(new Class<?>[] { GroupImplementation.class }),
                          EasyMock.aryEq(new Class[] { Interface1.class, Interface2.class }));

        replay();
        registry.bindInstance(component);
        verify();
    }

    @Test
    public void inheritedGroup() throws Exception {

        // no annotation with super group interfaces: all specified interfaces for group API and clas itself component API
        mock.bindComponent(EasyMock.same(InheritedGroup.class),
                           EasyMock.aryEq(new Class<?>[] { InheritedGroup.class }),
                           EasyMock.aryEq(new Class[] { GroupInterface1.class, GroupInterface2.class }));

        replay();
        registry.bindComponent(InheritedGroup.class);
        verify();

        final Object component = new InheritedGroup();

        mock.bindInstance(EasyMock.same(component),
                          EasyMock.aryEq(new Class<?>[] { InheritedGroup.class }),
                          EasyMock.aryEq(new Class[] { GroupInterface1.class, GroupInterface2.class }));

        replay();
        registry.bindInstance(component);
        verify();
    }

    @Test
    public void inheritedGroupComponent() throws Exception {

        // component annotation with API and super group interfaces: all specified interfaces for group API and specified interfaces for component API
        mock.bindComponent(EasyMock.same(UnmarkedGroupComponent.class),
                           EasyMock.aryEq(new Class[] { UnmarkedGroupComponent.class, Interface1.class, Interface2.class }),
                           EasyMock.aryEq(new Class[] { GroupInterface1.class, GroupInterface2.class }));

        replay();
        registry.bindComponent(UnmarkedGroupComponent.class);
        verify();

        final Object component = new UnmarkedGroupComponent();

        mock.bindInstance(EasyMock.same(component),
                          EasyMock.aryEq(new Class[] { UnmarkedGroupComponent.class, Interface1.class, Interface2.class }),
                          EasyMock.aryEq(new Class[] { GroupInterface1.class, GroupInterface2.class }));

        replay();
        registry.bindInstance(component);
        verify();
    }

    @Test
    public void inheritedMarkedGroupComponent() throws Exception {

        // group and component annotations with API and super group interfaces: all specified interfaces for group API and all specified interfaces for component API
        mock.bindComponent(EasyMock.same(MarkedGroupComponent.class),
                           EasyMock.aryEq(new Class[] { MarkedGroupComponent.class, Interface1.class, Interface2.class }),
                           EasyMock.aryEq(new Class[] { GroupInterface3.class, GroupInterface1.class, GroupInterface2.class }));

        replay();
        registry.bindComponent(MarkedGroupComponent.class);
        verify();

        final Object component = new MarkedGroupComponent();

        mock.bindInstance(EasyMock.same(component),
                          EasyMock.aryEq(new Class[] { MarkedGroupComponent.class, Interface1.class, Interface2.class }),
                          EasyMock.aryEq(new Class[] { GroupInterface3.class, GroupInterface1.class, GroupInterface2.class }));

        replay();
        registry.bindInstance(component);
        verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void overridesComponentMark() throws Exception {

        // group and component annotations with API and super group interfaces: all specified interfaces for group API and all specified interfaces for component API
        mock.bindComponent(EasyMock.same(MarkedGroupComponent.class),
                           EasyMock.aryEq(new Class[] { MarkedGroupComponent.class, Interface2.class }),
                           EasyMock.aryEq(new Class[] { GroupInterface3.class, GroupInterface1.class, GroupInterface2.class }));

        replay();
        registry.bindComponent(MarkedGroupComponent.class, Interface2.class);
        verify();

        final MarkedGroupComponent component = new MarkedGroupComponent();

        mock.bindInstance(EasyMock.same(component),
                          EasyMock.aryEq(new Class[] { MarkedGroupComponent.class, Interface2.class }),
                          EasyMock.aryEq(new Class[] { GroupInterface3.class, GroupInterface1.class, GroupInterface2.class }));

        replay();
        registry.bindInstance(component, Interface2.class);
        verify();
    }

    @Test
    public void unmarkedFactoryRegistration() throws Exception {
        mock.bindComponent(EasyMock.same(UnmarkedFactory.class),
                           EasyMock.aryEq(new Class[] { Interface1.class, Interface2.class }),
                           EasyMock.<Class[]>isNull());

        replay();
        registry.bindFactory(UnmarkedFactory.class, Interface1.class, Interface2.class);
        verify();
    }

    @Test
    public void markedFactoryRegistration() throws Exception {

        // we can override the @Component API list
        mock.bindComponent(EasyMock.same(MarkedFactory.class),
                           EasyMock.aryEq(new Class[] { Interface1.class, Interface2.class }),
                           EasyMock.<Class[]>isNull());

        replay();
        registry.bindFactory(MarkedFactory.class, Interface1.class, Interface2.class);
        verify();
    }

    private static class UnmarkedStandaloneComponent {

    }

    private static interface Interface1 {

    }

    private static interface Interface2 {

    }

    private static interface Interface3 {

    }

    @ComponentGroup
    private static interface GroupInterface1 {

    }

    @ComponentGroup
    private static interface GroupInterface2 {

    }

    private static interface GroupInterface3 extends Interface2 {

    }

    private static interface InheritingInterface1 extends GroupInterface1 {

    }

    private static interface InheritingInterface2 extends GroupInterface2 {

    }

    @Component(api = Interface1.class)
    private static class InvalidComponent {

    }

    @ComponentGroup(api = Interface1.class)
    private static class InvalidGroupComponent {

    }

    private static class UnmarkedComponent implements Interface1, Interface2 {

    }

    private static class UnmarkedSubclass extends UnmarkedComponent {

    }

    @Component
    private static class ComponentSubclass extends UnmarkedComponent implements Interface3 {

    }

    @Component(api = { Interface1.class, Interface2.class })
    private static class ComponentImplementation extends UnmarkedComponent implements Interface3 {

    }

    @ComponentGroup
    private static class GroupComponent implements Interface1, Interface2 {

    }

    @ComponentGroup
    private static class GroupSubclass extends UnmarkedComponent {

    }

    @ComponentGroup
    private static class GroupSubclassComponent extends UnmarkedComponent implements Interface3 {

    }

    @ComponentGroup(api = { Interface1.class, Interface2.class })
    private static class GroupImplementation extends UnmarkedComponent implements Interface3 {

    }

    private static class InheritedGroup implements InheritingInterface1, InheritingInterface2, GroupInterface3 {

    }

    @Component(api = { Interface1.class, Interface2.class })
    private static class UnmarkedGroupComponent implements Interface1, Interface2, InheritingInterface1, InheritingInterface2, GroupInterface3 {

    }

    @Component(api = { Interface1.class, Interface2.class })
    @ComponentGroup(api = GroupInterface3.class)
    private static class MarkedGroupComponent implements Interface1, Interface2, InheritingInterface1, InheritingInterface2, GroupInterface3 {

    }

    private static class UnmarkedFactory implements ComponentFactory {

        public void newComponent(final OpenComponentContainer container, final ComponentContext context) {
            throw new UnsupportedOperationException();
        }
    }

    @Component(api = Interface1.class)
    private static class MarkedFactory implements ComponentFactory {

        public void newComponent(final OpenComponentContainer container, final ComponentContext context) {
            throw new UnsupportedOperationException();
        }
    }
}

/*
 * Copyright (c) 2006-2008 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Softweare"), to deal
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.fluidity.composition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IMocksControl;
import org.fluidity.tests.MockGroupAbstractTest;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class ComponentContainerAccessTest extends MockGroupAbstractTest {

    private final BootstrapServices services = addControl(BootstrapServices.class);
    private final ContainerBootstrap bootstrap = addControl(ContainerBootstrap.class);
    private final ClassDiscovery discovery = addControl(ClassDiscovery.class);
    private final OpenComponentContainer container = addControl(OpenComponentContainer.class);

    @Test
    public void populatesTopLevelContainer() throws Exception {

        final Map<String, String> properties = new HashMap<String, String>();
        properties.put("key1", "value1");
        properties.put("key2", "value2");

        // find the top level class loader
        ClassLoader classLoader;
        for (classLoader = getClass().getClassLoader();
             classLoader.getParent() != null;
             classLoader = classLoader.getParent()) {
            // empty
        }

        // set up the test class
        final ComponentContainerAccess access = new ComponentContainerAccess(classLoader);
        access.reset(services);

        for (final Map.Entry<String, String> entry: properties.entrySet()) {
            access.setBindingsProperty(entry.getKey(), entry.getValue());
        }

        // make testee receive its dependencies from the top-level class loader
        EasyMock.expect(services.findInstance(ContainerBootstrap.class, classLoader)).andReturn(bootstrap);
        EasyMock.expect(services.findInstance(ClassDiscovery.class, classLoader)).andReturn(discovery);

        // give testee a container for that class loader
        EasyMock.expect(bootstrap.populateContainer(EasyMock.same(discovery), (Properties) EasyMock.notNull(),
            (OpenComponentContainer) EasyMock.same(null),
            EasyMock.same(classLoader))).andAnswer(new IAnswer<OpenComponentContainer>() {
            public OpenComponentContainer answer() throws Throwable {

                // check that the properties received by bootstrap is contains exactly what we set up above
                assert properties.equals(EasyMock.getCurrentArguments()[1]);

                return container;
            }
        });

        replay();

        // first access goes through the above interaction
        assert access.getContainer() == container;

        verify();

        replay();

        // second access should simply return the cached container
        assert access.getContainer() == container;

        verify();
    }

    @Test
    public void populatesStandaloneContainer() throws Exception {
        final Map<String, String> properties = new HashMap<String, String>();
        properties.put("key1", "value1");
        properties.put("key2", "value2");

        final ComponentContainerAccess access = new ComponentContainerAccess();
        access.reset(services);

        for (final Map.Entry<String, String> entry: properties.entrySet()) {
            access.setBindingsProperty(entry.getKey(), entry.getValue());
        }

        // find all class loaders on the ancestry except ours
        List<ClassLoader> classLoaders = new ArrayList<ClassLoader>();
        for (ClassLoader cl = getClass().getClassLoader().getParent(); cl != null; cl = cl.getParent()) {
            classLoaders.add(cl);
        }

        // make testee receive no dependency for all class loaders except ours
        for (final ListIterator<ClassLoader> i = classLoaders.listIterator(classLoaders.size()); i.hasPrevious();) {
            ClassLoader cl = i.previous();
            EasyMock.expect(services.findInstance(ContainerBootstrap.class, cl)).andReturn(null);
            EasyMock.expect(services.findInstance(ClassDiscovery.class, cl)).andReturn(null);
        }

        // make testee receive its dependencies from our class loader
        EasyMock.expect(services.findInstance(ContainerBootstrap.class, getClass().getClassLoader()))
            .andReturn(bootstrap);
        EasyMock.expect(services.findInstance(ClassDiscovery.class, getClass().getClassLoader())).andReturn(discovery);

        // give testee a container for our class loader
        EasyMock.expect(bootstrap.populateContainer(EasyMock.same(discovery), (Properties) EasyMock.notNull(),
            (OpenComponentContainer) EasyMock.same(null),
            EasyMock.same(getClass().getClassLoader()))).andAnswer(new IAnswer<OpenComponentContainer>() {
            public OpenComponentContainer answer() throws Throwable {

                // check that the properties received by bootstrap is contains exactly what we set up above
                assert properties.equals(EasyMock.getCurrentArguments()[1]);

                return container;
            }
        });

        replay();

        // first access goes through the above interaction
        assert access.getContainer() == container;

        verify();

        replay();

        // second access should simply return the cached container
        assert access.getContainer() == container;

        verify();
    }

    @Test
    public void populatesConnectedContainer() throws Exception {
        final ComponentContainerAccess access = new ComponentContainerAccess();
        access.reset(services);

        final IMocksControl containersControl = EasyMock.createControl();
        final Map<ClassLoader, OpenComponentContainer> containers = new HashMap<ClassLoader, OpenComponentContainer>();

        // find all class loaders on the ancestry
        final List<ClassLoader> classLoaders = new ArrayList<ClassLoader>();
        final ClassLoader ourClassLoader = getClass().getClassLoader();
        assert ourClassLoader != null;

        for (ClassLoader cl = ourClassLoader; cl != null; cl = cl.getParent()) {
            classLoaders.add(cl);
            containers.put(cl, containersControl.createMock(OpenComponentContainer.class));
        }

        // find the top level class loader
        final ClassLoader classLoader = classLoaders.get(classLoaders.size() - 1);

        // make testee receive its dependencies from the top-level class loader
        EasyMock.expect(services.findInstance(ContainerBootstrap.class, classLoader)).andReturn(bootstrap);
        EasyMock.expect(services.findInstance(ClassDiscovery.class, classLoader)).andReturn(discovery);

        // go through the whole class loader ancestry
        for (final ListIterator<ClassLoader> i = classLoaders.listIterator(classLoaders.size()); i.hasPrevious();) {
            final ClassLoader cl = i.previous();

            // make testee receive a container (the same) at each level
            EasyMock.expect(bootstrap.populateContainer(EasyMock.same(discovery), (Properties) EasyMock.notNull(),
                EasyMock.same(containers.get(cl.getParent())),
                EasyMock.same(cl))).andReturn(containers.get(cl));
        }

        final OpenComponentContainer ourContainer = containers.get(ourClassLoader);
        assert ourContainer != null;

        replay();

        // first access goes through the above interaction
        assert access.getContainer() == ourContainer;

        verify();

        replay();

        // second access should simply return the cached container
        assert access.getContainer() == ourContainer;

        verify();

        if (ourClassLoader.getParent() != null) {
            replay();

            // access to higher level container should simply return the cached container
            assert new ComponentContainerAccess(ourClassLoader.getParent()).getContainer() == containers
                .get(ourClassLoader.getParent());

            verify();
        }
    }
}

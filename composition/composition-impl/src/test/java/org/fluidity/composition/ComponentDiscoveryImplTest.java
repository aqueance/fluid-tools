/*
 * Copyright (c) 2006-2008 Tibor Adam Varga (tibor.adam.varga@gmail.com)
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
 *
 */
package org.fluidity.composition;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.fluidity.tests.MockGroupAbstractTest;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class ComponentDiscoveryImplTest extends MockGroupAbstractTest {

    private final ComponentContainer container = addControl(ComponentContainer.class);

    private final ComponentContainer.Registry registry = addControl(ComponentContainer.Registry.class);

    private final ClassDiscovery discovery = addControl(ClassDiscovery.class);

    @SuppressWarnings({ "unchecked" })
    @Test
    public void findsBoundImplementations() throws Exception {

        // we need to create a new service provider (http://java.sun.com/j2se/1.4.2/docs/guide/jar/jar.html#Service%20Provider)
        final File classDir = File.createTempFile("classes", ".dir", new File(System.getProperty("java.io.tmpdir")));
        classDir.delete();
        classDir.mkdir();
        final File servicesFile = new File(classDir, "META-INF/services/" + Interface.class.getName());
        servicesFile.getParentFile().mkdirs();

        final PrintWriter pw = new PrintWriter(new FileWriter(servicesFile, false));
        pw.println(Impl1.class.getName());
        pw.println(Impl2.class.getName());
        pw.println(Impl3.class.getName());
        pw.close();

        assert servicesFile.exists();

        try {
            final URLClassLoader classLoader =
                new URLClassLoader(new URL[] { classDir.toURL() }, getClass().getClassLoader());
            Thread.currentThread().setContextClassLoader(classLoader);

            EasyMock.expect(discovery.findComponentClasses(Interface.class, null, false)).andReturn(
                new Class[] { Impl1.class, Impl2.class, Impl3.class });

            final Interface[] instances = new Interface[] { new Impl1(), new Impl2(), new Impl3() };

            for (final Interface instance : instances) {
                EasyMock.expect(container.getComponent(instance.getClass())).andReturn(instance);
            }

            replay();
            assert new ArrayList<Interface>(Arrays.asList(instances)).equals(new ArrayList<Interface>(
                Arrays.asList(new ComponentDiscoveryImpl(discovery).findComponentInstances(container, Interface.class))));
            verify();

        } finally {
            servicesFile.delete();
            servicesFile.getParentFile().delete();
            servicesFile.getParentFile().getParentFile().delete();
            classDir.delete();
            Thread.currentThread().setContextClassLoader(null);
        }
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void findsImplementations() throws Exception {

        // we need to create a new service provider (http://java.sun.com/j2se/1.4.2/docs/guide/jar/jar.html#Service%20Provider)
        final File classDir = File.createTempFile("classes", ".dir", new File(System.getProperty("java.io.tmpdir")));
        classDir.delete();
        classDir.mkdir();
        final File servicesFile = new File(classDir, "META-INF/services/" + Interface.class.getName());
        servicesFile.getParentFile().mkdirs();

        final PrintWriter pw = new PrintWriter(new FileWriter(servicesFile, false));
        pw.println(Impl1.class.getName());
        pw.println(Impl2.class.getName());
        pw.println(Impl3.class.getName());
        pw.close();

        assert servicesFile.exists();

        try {
            final URLClassLoader classLoader =
                new URLClassLoader(new URL[] { classDir.toURL() }, getClass().getClassLoader());
            Thread.currentThread().setContextClassLoader(classLoader);

            EasyMock.expect(discovery.findComponentClasses(Interface.class, null, false))
                .andReturn(new Class[] { Impl1.class, Impl2.class, Impl3.class });

            final Interface[] instances = new Interface[] { new Impl1(), new Impl2(), new Impl3() };

            for (final Interface instance : instances) {
                final Class<? extends Interface> instanceClass = instance.getClass();
                EasyMock.expect(container.getComponent(instanceClass)).andReturn(null);
                EasyMock.expect(container.getComponent(EasyMock.same(instanceClass),
                    (ComponentContainer.Bindings) EasyMock.notNull())).andAnswer(new IAnswer<Interface>() {
                    public Interface answer() throws Throwable {
                        ComponentContainer.Bindings bindings =
                            (ComponentContainer.Bindings) EasyMock.getCurrentArguments()[1];

                        // invoke the testee supplied parameter
                        bindings.registerComponents(registry);

                        // return to the testee
                        return instance;
                    }
                });

                // expect the testee to do this when invoked from the inner class above
                registry.bind(instanceClass);
            }

            replay();
            assert new ArrayList<Interface>(Arrays.asList(instances)).equals(new ArrayList<Interface>(
                Arrays.asList(new ComponentDiscoveryImpl(discovery).findComponentInstances(container, Interface.class))));
            verify();
        } finally {
            servicesFile.delete();
            servicesFile.getParentFile().delete();
            servicesFile.getParentFile().getParentFile().delete();
            classDir.delete();
            Thread.currentThread().setContextClassLoader(null);
        }
    }

    public static interface Interface {

        // empty
    }

    public static class AbstractInterfaceImpl implements Interface {

        // empty
    }

    public static class Impl1 extends AbstractInterfaceImpl {

        // private to enforce container access override
        private Impl1() {
            // empty
        }
    }

    public static class Impl2 extends AbstractInterfaceImpl {

    }

    public static class Impl3 extends AbstractInterfaceImpl {

    }
}

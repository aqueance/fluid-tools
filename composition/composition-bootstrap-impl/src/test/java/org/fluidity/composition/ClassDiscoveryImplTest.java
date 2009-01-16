/*
 * Copyright (c) 2006-2009 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.fluidity.foundation.ClassLoaderUtils;
import org.fluidity.foundation.SystemSettings;
import org.fluidity.foundation.logging.BootstrapLog;
import org.fluidity.tests.MockGroupAbstractTest;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings({ "ResultOfMethodCallIgnored" })
public class ClassDiscoveryImplTest extends MockGroupAbstractTest {

    static {
        SystemSettings.set(BootstrapLog.SUPPRESS_LOGS, BootstrapLog.ALL_LOGS);
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void findsClassesInAnyClassLoader() throws Exception {

        // we need to create a new service provider (http://java.sun.com/j2se/1.4.2/docs/guide/jar/jar.html#Service%20Provider)
        final File classDir = File.createTempFile("classes", ".dir", new File(System.getProperty("java.io.tmpdir")));
        classDir.delete();
        classDir.mkdir();

        final File servicesFile = new File(classDir, "META-INF/services/" + Interface.class.getName());
        servicesFile.getParentFile().mkdirs();

        final ArrayList<File> fileList = new ArrayList<File>();
        fileList.add(servicesFile);

        final PrintWriter pw = new PrintWriter(new FileWriter(servicesFile, false));
        pw.println(Impl1.class.getName());
        pw.println(Impl2.class.getName());
        pw.println(Impl3.class.getName());
        pw.close();

        assert servicesFile.exists();

        try {
            final URLClassLoader classLoader =
                    new URLClassLoader(new URL[] { classDir.toURL() }, getClass().getClassLoader());

            replay();
            final Class[] classes = new ClassDiscoveryImpl().findComponentClasses(Interface.class, classLoader, false);
            verify();

            assert new ArrayList<Class>(Arrays.asList(Impl1.class, Impl2.class, Impl3.class))
                    .equals(new ArrayList<Class>(Arrays.asList(classes)));
        } finally {
            deleteDirectory(classDir, fileList);
        }
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void findsClassesOnlyByGivenClassLoader() throws Exception {

        // we need to create a new service provider (http://java.sun.com/j2se/1.4.2/docs/guide/jar/jar.html#Service%20Provider)
        final File classDir1 = File.createTempFile("classes1", ".dir", new File(System.getProperty("java.io.tmpdir")));
        classDir1.delete();
        classDir1.mkdir();

        final File classDir2 = File.createTempFile("classes2", ".dir", new File(System.getProperty("java.io.tmpdir")));
        classDir2.delete();
        classDir2.mkdir();

        final File servicesFile = new File(classDir2, "META-INF/services/" + Interface.class.getName());
        servicesFile.getParentFile().mkdirs();

        final List<File> fileList = new ArrayList<File>();
        fileList.add(servicesFile);

        final PrintWriter pw = new PrintWriter(new FileWriter(servicesFile, false));
        pw.println(Impl1.class.getName());

        // superclass and interfaces in parent class loader
        copyClassFile(Interface.class, classDir1, fileList);
        copyClassFile(AbstractInterfaceImpl.class, classDir1, fileList);

        // actual class in child class loader
        copyClassFile(Impl1.class, classDir2, fileList);
        pw.close();

        assert servicesFile.exists();

        try {
            final URLClassLoader classLoader1 = new URLClassLoader(new URL[] { classDir1.toURL() }, null);
            final URLClassLoader classLoader2 = new URLClassLoader(new URL[] { classDir2.toURL() }, classLoader1);

            replay();
            final Class[] classes = new ClassDiscoveryImpl()
                    .findComponentClasses(classLoader1.loadClass(Interface.class.getName()), classLoader2, true);
            verify();

            assert new ArrayList<Class>(Arrays.asList(classLoader2.loadClass(Impl1.class.getName())))
                    .equals(new ArrayList<Class>(Arrays.asList(classes)));
        } finally {
            deleteDirectory(classDir1, fileList);
            deleteDirectory(classDir2, fileList);
        }
    }

    private void copyClassFile(Class impl, File classDir, List<File> fileList) throws IOException {
        final String fileName = impl.getName().replace('.', '/') + ".class";

        final File outputFile = new File(classDir, fileName);
        fileList.add(outputFile);

        outputFile.getParentFile().mkdirs();
        outputFile.delete();
        outputFile.createNewFile();

        final InputStream input =
                getClass().getClassLoader().getResourceAsStream(ClassLoaderUtils.absoluteResourceName(fileName));
        OutputStream output = new FileOutputStream(outputFile);

        assert input != null : fileName;

        try {
            final byte buffer[] = new byte[1024];
            int len;
            while ((len = input.read(buffer)) > 0) {
                output.write(buffer, 0, len);
            }
        } finally {
            try {
                input.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }

            try {
                output.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteDirectory(File rootDir, List<File> fileList) {
        for (final File file : fileList) {
            file.delete();

            for (File directory = file.getParentFile();
                 !directory.equals(rootDir);
                 directory = directory.getParentFile()) {
                if (!directory.delete()) {
                    break;
                }
            }
        }

        rootDir.delete();
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

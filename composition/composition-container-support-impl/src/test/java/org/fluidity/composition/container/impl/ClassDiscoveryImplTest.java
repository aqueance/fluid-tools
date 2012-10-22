/*
 * Copyright (c) 2006-2012 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.composition.container.impl;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.fluidity.foundation.ClassLoaders;
import org.fluidity.foundation.Log;
import org.fluidity.foundation.NoLogFactory;
import org.fluidity.foundation.Streams;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings({ "unchecked", "ResultOfMethodCallIgnored" })
public class ClassDiscoveryImplTest {

    private final Log<ClassDiscoveryImpl> log = NoLogFactory.consume(ClassDiscoveryImpl.class);

    @Test
    public void findsClassesInAnyClassLoader() throws Exception {

        // we need to create a new service provider (http://java.sun.com/j2se/1.4.2/docs/guide/jar/jar.html#Service%20Provider)
        final File classDir = File.createTempFile("classes", ".dir", new File(System.getProperty("java.io.tmpdir")));
        classDir.delete();
        classDir.mkdir();

        final File servicesFile = new File(classDir, "META-INF/services/" + Interface.class.getName());
        servicesFile.getParentFile().mkdirs();

        final List<File> fileList = new ArrayList<File>();
        fileList.add(servicesFile);

        final PrintWriter pw = new PrintWriter(new FileWriter(servicesFile, false));
        pw.println(Impl1.class.getName());
        pw.println(Impl2.class.getName());
        pw.println(Impl3.class.getName());
        pw.close();

        assert servicesFile.exists();

        ClassLoader loader = null;
        try {
            final ClassLoader classLoader = loader = ClassLoaders.create(getClass().getClassLoader(), classDir.toURI().toURL());

            final Class[] classes = new ClassDiscoveryImpl(log).findComponentClasses(Interface.class, classLoader, false);

            assert new ArrayList<Class>(Arrays.asList(Impl1.class, Impl2.class, Impl3.class)).equals(new ArrayList<Class>(Arrays.asList(classes)));
        } finally {
            if (loader != null) {
                ((Closeable) loader).close();
            }

            deleteDirectory(classDir, fileList);
        }
    }

    @Test
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

        ClassLoader loader1 = null;
        ClassLoader loader2 = null;
        try {
            final ClassLoader classLoader1 = loader1 = ClassLoaders.create(null, classDir1.toURI().toURL());
            final ClassLoader classLoader2 = loader2 = ClassLoaders.create(loader1, classDir2.toURI().toURL());

            final Class[] classes = new ClassDiscoveryImpl(log).findComponentClasses(classLoader1.loadClass(Interface.class.getName()), classLoader2, true);

            assert new ArrayList<Class>(Arrays.asList(loader2.loadClass(Impl1.class.getName()))).equals(new ArrayList<Class>(Arrays.asList(classes)));
        } finally {
            if (loader1 != null) {
                ((Closeable) loader1).close();
            }

            if (loader2 != null) {
                ((Closeable) loader2).close();
            }

            deleteDirectory(classDir1, fileList);
            deleteDirectory(classDir2, fileList);
        }
    }

    private void copyClassFile(final Class<?> impl, final File classDir, final List<File> fileList) throws IOException {
        final String fileName = ClassLoaders.classResourceName(impl);

        final File outputFile = new File(classDir, fileName);
        fileList.add(outputFile);

        outputFile.getParentFile().mkdirs();
        outputFile.delete();
        outputFile.createNewFile();

        final InputStream input = ClassLoaders.readClassResource(impl);
        assert input != null : fileName;

        Streams.copy(input, new FileOutputStream(outputFile), new byte[1024], true, true);
    }

    private void deleteDirectory(final File rootDir, final List<File> fileList) {
        for (final File file : fileList) {
            file.delete();

            for (File directory = file.getParentFile(); !directory.equals(rootDir); directory = directory.getParentFile()) {
                if (!directory.delete()) {
                    break;
                }
            }
        }

        rootDir.delete();
    }

    public interface Interface {

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

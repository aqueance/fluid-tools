/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.fluidity.foundation.Archives;
import org.fluidity.foundation.ClassLoaders;
import org.fluidity.foundation.IOStreams;
import org.fluidity.foundation.Log;
import org.fluidity.foundation.NoLogFactory;
import org.fluidity.foundation.ServiceProviders;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings({ "unchecked", "ResultOfMethodCallIgnored", "WeakerAccess" })
public class ComponentDiscoveryImplTest {

    private static final String SERVICES = String.format("%s/%s/", Archives.META_INF, ServiceProviders.TYPE);

    private final Log<ComponentDiscoveryImpl> log = NoLogFactory.consume(ComponentDiscoveryImpl.class);

    @Test
    public void findsClassesInAnyClassLoader() throws Exception {

        // we need to create a new service provider (https://docs.oracle.com/javase/8/docs/technotes/guides/jar/jar.html#Service_Provider)
        final File classDir = File.createTempFile("classes", ".dir", new File(System.getProperty("java.io.tmpdir")));
        classDir.delete();
        classDir.mkdir();

        final File servicesFile = new File(classDir, SERVICES.concat(Interface.class.getName()));
        servicesFile.getParentFile().mkdirs();

        final List<File> fileList = new ArrayList<>();
        fileList.add(servicesFile);

        try (final PrintWriter pw = new PrintWriter(new FileWriter(servicesFile, false))) {
            pw.println(Impl1.class.getName());
            pw.println(Impl2.class.getName());
            pw.println(Impl3.class.getName());
        }

        assert servicesFile.exists();

        try {
            final ClassLoader loader = ClassLoaders.create(Collections.singleton(classDir.toURI().toURL()), getClass().getClassLoader(), null);
            final Class[] classes = new ComponentDiscoveryImpl(log).findComponentClasses(Interface.class, loader, false);

            assert new ArrayList<Class>(Arrays.asList(Impl1.class, Impl2.class, Impl3.class)).equals(new ArrayList<>(Arrays.asList(classes)));
        } finally {
            deleteDirectory(classDir, fileList);
        }
    }

    @Test
    public void findsClassesOnlyByGivenClassLoader() throws Exception {

        // we need to create a new service provider (https://docs.oracle.com/javase/8/docs/technotes/guides/jar/jar.html#Service_Provider)
        final File classDir1 = File.createTempFile("classes1", ".dir", new File(System.getProperty("java.io.tmpdir")));
        classDir1.delete();
        classDir1.mkdir();

        final File classDir2 = File.createTempFile("classes2", ".dir", new File(System.getProperty("java.io.tmpdir")));
        classDir2.delete();
        classDir2.mkdir();

        final File servicesFile = new File(classDir2, SERVICES.concat(Interface.class.getName()));
        servicesFile.getParentFile().mkdirs();

        final List<File> fileList = new ArrayList<>();
        fileList.add(servicesFile);

        try (final PrintWriter pw = new PrintWriter(new FileWriter(servicesFile, false))) {
            pw.println(Impl1.class.getName());

            // superclass and interfaces in parent class loader
            copyClassFile(Interface.class, classDir1, fileList);
            copyClassFile(AbstractInterfaceImpl.class, classDir1, fileList);

            // actual class in child class loader
            copyClassFile(Impl1.class, classDir2, fileList);
        }

        assert servicesFile.exists();

        try {
            final ClassLoader loader1 = ClassLoaders.create(Collections.singleton(classDir1.toURI().toURL()), null, null);
            final ClassLoader loader2 = ClassLoaders.create(Collections.singleton(classDir2.toURI().toURL()), loader1, null);

            final Class[] classes = new ComponentDiscoveryImpl(log).findComponentClasses(loader1.loadClass(Interface.class.getName()), loader2, true);

            assert Collections.singletonList(loader2.loadClass(Impl1.class.getName())).equals(Arrays.asList(classes));
        } finally {
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

        try (final InputStream input = ClassLoaders.readClassResource(impl);
             final OutputStream output = new FileOutputStream(outputFile)) {
            assert input != null : fileName;
            IOStreams.pipe(input, output, new byte[1024]);
        }
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

    public interface Interface { }

    public static class AbstractInterfaceImpl implements Interface { }

    public static class Impl1 extends AbstractInterfaceImpl { }

    public static class Impl2 extends AbstractInterfaceImpl { }

    public static class Impl3 extends AbstractInterfaceImpl { }
}

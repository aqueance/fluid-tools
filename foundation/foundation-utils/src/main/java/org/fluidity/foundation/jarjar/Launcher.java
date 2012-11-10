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

package org.fluidity.foundation.jarjar;

import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;

import org.fluidity.foundation.Archives;
import org.fluidity.foundation.ClassLoaders;

import static org.fluidity.foundation.Command.Function;

/**
 * Launches a main class from a JAR file using a class loader that can load classes from JAR files nested inside the main JAR. Nested JAR files must be located
 * in the path denoted by the manifest attribute name returned by {@link org.fluidity.foundation.Archives.Nested#attribute(String)
 * Archives.Nested.attribute(null)}. The main class to be loaded is defined by the manifest attribute named in {@link #ORIGINAL_MAIN_CLASS}. The
 * <code>Main-Class</code> manifest attribute has to point to this class, obviously.
 * <p/>
 * The above manifest attributes are set by the appropriate {@link org.fluidity.deployment.plugin.spi.JarManifest} processor when used by the
 * <code>org.fluidity.maven:standalone-jar-maven-plugin</code> Maven plugin.
 *
 * @author Tibor Varga
 */
@SuppressWarnings("JavadocReference")
public final class Launcher {

    private Launcher() { }

    /**
     * The JAR manifest attribute that specifies the application's main class, one with a <code>public static void main(final String[] args) throws
     * Exception</code> method.
     */
    public static final String ORIGINAL_MAIN_CLASS = "Original-Main-Class";

    /**
     * The command line entry point.
     *
     * @param args the command line arguments.
     *
     * @throws Exception when anything goes wrong.
     */
    public static void main(final String[] args) throws Exception {
        final Class<?> main = Launcher.class;

        final URL root = Archives.containing(main);
        final String mainClass = Archives.attributes(true, root, ORIGINAL_MAIN_CLASS)[0];

        if (mainClass == null) {
            throw new IllegalStateException(String.format("%s is not a defined in the %s manifest", ORIGINAL_MAIN_CLASS, root));
        }

        final List<URL> urls = new ArrayList<URL>();

        urls.add(root);
        urls.addAll(Archives.Nested.dependencies(true, null));

        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                public Void run() throws Exception {
                    final ClassLoader parent = ClassLoaders.findClassLoader(main, true);
                    final ClassLoader loader = ClassLoaders.create(urls, parent, null);

                    ClassLoaders.context(loader, new Function<Object, ClassLoader, Exception>() {
                        public Object run(final ClassLoader loader) throws Exception {
                            return loader.loadClass(mainClass).getMethod("main", String[].class).invoke(null, new Object[] { args });
                        }
                    });

                    return null;
                }
            });
        } catch (final PrivilegedActionException e) {
            throw (Exception) e.getCause();
        }
    }
}

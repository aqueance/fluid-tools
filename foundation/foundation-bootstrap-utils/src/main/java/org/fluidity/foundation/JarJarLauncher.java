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

package org.fluidity.foundation;

import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;

import org.fluidity.foundation.jarjar.Handler;

/**
 * Launches a main class from a jar file using a class loader that can load classes from jar files nested inside the main jar. Nested jar files must be located
 * in the path denoted by the Dependencies-Path manifest attribute. The main class to be loaded is defined by the Original-Main-Class manifest attribute. The
 * Main-Class manifest attribute, obviously, points to this class.
 *
 * @author Tibor Varga
 */
public class JarJarLauncher {

    public static final String NESTED_DEPENDENCIES = "Nested-Dependencies";
    public static final String ORIGINAL_MAIN_CLASS = "Original-Main-Class";

    public static void main(final String[] args) throws Exception {
        final Class<?> main = JarJarLauncher.class;

        final URL url = ClassLoaders.findClassResource(main);
        final JarURLConnection jar = JarStreams.jarFile(url);

        if (jar != null) {
            final URL jarURL = jar.getJarFileURL();
            final Attributes attributes = jar.getMainAttributes();

            final String jarPath = jarURL.getFile();
            final String mainClass = getMandatoryAttribute(jarPath, attributes, ORIGINAL_MAIN_CLASS);
            final String dependencies = getMandatoryAttribute(jarPath, attributes, NESTED_DEPENDENCIES);

            final List<URL> urls = new ArrayList<URL>();
            urls.add(jarURL);

            for (final String path : dependencies.split(" ")) {

                // TODO: handle jars embedded in the embedded jar, recursively
                urls.add(Handler.formatURL(jarURL, path));
            }

            final URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[urls.size()]), ClassLoaders.findClassLoader(main));

            final Thread thread = Thread.currentThread();
            final ClassLoader saved = thread.getContextClassLoader();

            thread.setContextClassLoader(loader);
            try {
                loader.loadClass(mainClass).getMethod("main", String[].class).invoke(null, new Object[] { args });
            } finally {
                thread.setContextClassLoader(saved);
            }
        } else {
            throw new IllegalStateException(String.format("%s does not point to a jar file", url));
        }
    }

    private static String getMandatoryAttribute(final String file, final Attributes attributes, final String name) {
        final String mainClass = attributes.getValue(name);

        if (mainClass == null) {
            throw new IllegalStateException(String.format("%s is not a defined in the %s manifest", name, file));
        }

        return mainClass;
    }
}

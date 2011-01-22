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

package org.fluidity.foundation;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.Attributes;

/**
 * Launches a main class from a jar file using a class loader that can load classes from jar files nested inside the main jar. Nested jar files must be located
 * in the path denoted by the Dependencies-Path manifest attribute. The main class to be loaded is defined by the Original-Main-Class manifest attribute. The
 * Main-Class manifest attribute, obviously, points to this class.
 *
 * @author Tibor Varga
 */
public class JarJarLauncher {

    public static final String DEPENDENCIES_PATH = "Dependencies-Path";
    public static final String ORIGINAL_MAIN_CLASS = "Original-Main-Class";

    public static void main(final String[] args)
            throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final Class<?> main = JarJarLauncher.class;
        final ClassLoader parent = main.getClassLoader();
        final URL url = parent.getResource(ClassLoaders.classResourceName(main));

        final URLConnection connection = url.openConnection();

        if (connection instanceof JarURLConnection) {
            final JarURLConnection jar = (JarURLConnection) connection;

            final URL jarURL = jar.getJarFileURL();
            final Attributes attributes = jar.getMainAttributes();

            final String jarPath = jarURL.getFile();
            final String mainClass = getMandatoryAttribute(jarPath, attributes, ORIGINAL_MAIN_CLASS);
            final String dependenciesPath = getMandatoryAttribute(jarPath, attributes, DEPENDENCIES_PATH);

            final JarJarClassLoader loader = new JarJarClassLoader(jarURL, parent, dependenciesPath);
            Thread.currentThread().setContextClassLoader(loader);

            loader.loadClass(mainClass).getMethod("main", String[].class).invoke(null, new Object[] { args });
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

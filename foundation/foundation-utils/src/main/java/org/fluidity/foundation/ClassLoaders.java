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

import java.io.InputStream;
import java.net.URL;

/**
 * Finds a suitable class loader in various application containers (web, ejb, junit, testng, maven).
 * <p/>
 * In test cases this class can be controlled by setting the context class loader to a mock class loader that returns what the test case desires. This is done
 * by calling
 * <pre>
 * Thread.currentThread().setContextClassLoader(...);
 * </pre>
 *
 * @author Tibor Varga
 */
public final class ClassLoaders {

    public static final String CLASS_SUFFIX = ".class";

    private ClassLoaders() {
        throw new UnsupportedOperationException("No instance allowed");
    }

    public static ClassLoader findClassLoader(final Class sourceClass) {
        ClassLoader result = Thread.currentThread().getContextClassLoader();
        result = result == null ? sourceClass.getClassLoader() : result;
        result = result == null ? ClassLoaders.class.getClassLoader() : result;
        return result == null ? ClassLoader.getSystemClassLoader() : result;
    }

    public static String absoluteResourceName(final String resourceName) {
        return resourceName.startsWith("/") ? resourceName.substring(1) : resourceName;
    }

    public static URL findResource(final Class sourceClass, final String resourceName) {
        return findClassLoader(sourceClass).getResource(absoluteResourceName(resourceName));
    }

    public static String classResourceName(final Class sourceClass) {
        return classResourceName(sourceClass.getName());
    }

    public static String classResourceName(final String sourceClass) {
        return sourceClass.replace('.', '/').concat(CLASS_SUFFIX);
    }

    public static URL findClassResource(final Class sourceClass) {
        return findClassLoader(sourceClass).getResource(classResourceName(sourceClass));
    }

    public static InputStream readClassResource(final Class sourceClass) {
        return findClassLoader(sourceClass).getResourceAsStream(classResourceName(sourceClass));
    }
}

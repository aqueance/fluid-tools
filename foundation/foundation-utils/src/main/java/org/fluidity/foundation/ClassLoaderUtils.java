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
package org.fluidity.foundation;

import java.net.URL;

/**
 * Finds a suitable class loader in known application containers (web, ejb, junit, testng, maven).
 *
 * <p/>
 *
 * In test cases this class can be controlled by setting the context class loader to a mock class loader that returns
 * what the test case desires. This is done by calling
 *
 * <pre>
 * Thread.curentThread.setContextClassLoader(...);
 * </pre>
 *
 * @author Tibor Varga
 */
public final class ClassLoaderUtils {

    public static ClassLoader findClassLoader(final Class sourceClass) {
        ClassLoader result = Thread.currentThread().getContextClassLoader();
        result = result == null ? sourceClass.getClassLoader() : result;
        result = result == null ? ClassLoaderUtils.class.getClassLoader() : result;

        // any idea how to test defaulting to the system class loader?
        return result == null ? ClassLoader.getSystemClassLoader() : result;
    }

    public static String absoluteResourceName(final String resourceName) {
        return resourceName.startsWith("/") ? resourceName.substring(1) : resourceName;
    }

    public static String classResourceName(final Class sourceClass) {
        return sourceClass.getName().replace('.', '/') + ".class";
    }

    public static URL findClassResource(final Class sourceClass) {
        return findClassLoader(sourceClass).getResource(classResourceName(sourceClass));
    }
}

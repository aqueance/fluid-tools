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

import java.io.InputStream;
import java.net.URL;

/**
 * Loads resources and classes using the most appropriate class loader (context, etc.)
 *
 * @author Tibor Varga
 * @version $Revision$
 */
public interface Resources {

    /**
     * Validates the given resource name.
     *
     * @param name is a resource name.
     *
     * @return a valid resource name.
     */
    String resourceName(String name);

    /**
     * Finds the location of the given resource.
     *
     * @param name is the name of the resource.
     *
     * @return a resource locator if the resource was found, <code>null</code> otherwise.
     */
    URL locateResource(String name);

    /**
     * Finds the locations of all resources with the given name.
     *
     * @param name is the name of the resources.
     *
     * @return an array of resource locators, never <code>null</code>.
     */
    URL[] locateResources(String name);

    /**
     * Loads the resource with the given name.
     *
     * @param name is the name of the resource.
     *
     * @return a stream if found, <code>null</code> otherwise.
     */
    InputStream loadResource(String name);

    /**
     * Loads the given class as a resource.
     *
     * @param className is the name of the class.
     *
     * @return a stream if found, <code>null</code> otherwise.
     */
    InputStream loadClassResource(String className);

    /**
     * Loads a class with the given name.
     *
     * @param className is the name of the class.
     *
     * @return the class if found, <code>null</code> otherwise.
     */
    Class loadClass(String className);
}

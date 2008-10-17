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

/**
 * Partially implements the Service Provider discovery mechanism described in the Jar File Specification. This mechanism
 * is documented at http://java.sun.com/j2se/1.4.2/docs/guide/jar/jar.html#Service%20Provider <p/> The implementation is
 * partial because this component does not instantiate the discovered classes, it merely discovers them. <p/> This is
 * useful not so much for client components as for those providing core composition functionality such as component
 * container bootstrap. Client components normally need <code>ComponentDiscovery</code> instead.
 *
 * @author Tibor Varga
 */
public interface ClassDiscovery {

    /**
     * Finds all classes in the class path that have been registered according the standard service discovery
     * specification.
     *
     * @param componentInterface is the interface all discovered classes should implement.
     * @param classLoader        is the class loader to use to find components.
     * @param strict             specifies whether the component may be loaded by only the given class loader
     *                           (<code>true</code>) or any of its parent class loaders (<code>false</code>).
     *
     * @return a list of <code>Class</code> objects for the discovered classes.
     */
    <T> Class<? extends T>[] findComponentClasses(final Class<T> componentInterface,
                                                  final ClassLoader classLoader,
                                                  final boolean strict);
}
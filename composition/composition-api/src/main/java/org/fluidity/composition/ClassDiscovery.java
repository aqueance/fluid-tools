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

package org.fluidity.composition;

/**
 * Partially implements the Service Provider discovery mechanism described in the <a href="http://download.oracle.com/javase/1.5.0/docs/guide/jar/jar.html#Service
 * Provider">JAR File Specification</a>.
 * <p/>
 * The implementation is partial because this component does not instantiate the discovered classes, it merely discovers them.
 * <p/>
 * This is useful not so much for client components as for those providing core composition functionality such as component container bootstrap. Client
 * components normally need to use a {@link ComponentGroup} annotated array parameter instead.
 *
 * @author Tibor Varga
 */
public interface ClassDiscovery {

    /**
     * Finds all classes in the class path that have been registered according the standard service discovery specification. Calls {@link
     * #findComponentClasses(String, Class, ClassLoader, boolean)} with <code>services</code> for the <code>type</code> parameter.
     *
     * @param api         is the interface all discovered classes should implement.
     * @param classLoader is the class loader to use to find components.
     * @param strict      specifies whether the component may be loaded by only the given class loader (<code>true</code>) or any of its parent class loaders
     *                    (<code>false</code>).
     *
     * @return a list of <code>Class</code> objects for the discovered classes.
     */
    <T> Class<T>[] findComponentClasses(Class<T> api, ClassLoader classLoader, boolean strict);

    /**
     * Finds all classes in the class path that have been registered according the standard service discovery specification.
     *
     * @param type        the service provider type. The value is used to look up service provider files in <code>META-INF/&lt;type>/&lt;api></code>.
     * @param api         is the interface all discovered classes should implement.
     * @param classLoader is the class loader to use to find components.
     * @param strict      specifies whether the component may be loaded by only the given class loader (<code>true</code>) or any of its parent class loaders
     *                    (<code>false</code>).
     *
     * @return a list of <code>Class</code> objects for the discovered classes.
     */
    <T> Class<T>[] findComponentClasses(String type, Class<T> api, ClassLoader classLoader, boolean strict);
}

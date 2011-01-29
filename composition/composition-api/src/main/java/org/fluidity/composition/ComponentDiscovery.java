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
 * Implements the Service Provider discovery mechanism described in the <a href="http://download.oracle.com/javase/1.5.0/docs/guide/jar/jar.html#Service
 * Provider">JAR File Specification</a>.
 * <p/>
 * The difference between this implementation and the one provided with the JDK is that, in addition to this not being under the <code>com.sun</code> package as
 * with JDK up to and including version 5, rather than instantiating components by calling their default constructor, components are placed in an anonymous
 * child container of the host application's dependency injection container nearest to the component class to get their dependencies resolved.
 * <p/>
 * Components depending on this may also need to depend on a {@link ComponentContainer}.
 *
 * @author Tibor Varga
 */
public interface ComponentDiscovery {

    /**
     * Finds all classes in the class path that have been exposed through the standard service discovery mechanism and instantiates them, resolving their
     * dependencies using the given dependency injection container.
     *
     * @param container      is the container to use to resolve dependencies of the found components.
     * @param componentClass is the interface all discovered classes should implement.
     *
     * @return a list of instances of the discovered classes.
     */
    <T> T[] findComponentInstances(ComponentContainer container, Class<T> componentClass);
}

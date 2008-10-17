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

import java.util.List;
import java.util.Map;

/**
 * This is a dependency injection container that components can be added to.
 *
 * @author Tibor Varga
 */
public interface OpenComponentContainer extends ComponentContainer {

    /**
     * Returns the interface through which component bindings can be added to this container.
     *
     * @return a <code>ComponentContainer.Registry</code> instance.
     */
    ComponentContainer.Registry getRegistry();

    /**
     * Returns a list of unresolved dependencies. The keys are classes representing the interfaces of the missing
     * dependencies. The values are List objects containing the class objects representing the components that require
     * the missing dependency.
     *
     * @return a Map of Class objects with keys and List of Class objects as values.
     */
    Map<Class, List<Class>> getUnresolvedDependencies();
}

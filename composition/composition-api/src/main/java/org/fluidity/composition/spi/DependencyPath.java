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

package org.fluidity.composition.spi;

import java.util.List;

/**
 * A dependency path.
 *
 * @author Tibor Varga
 */
public interface DependencyPath {

    /**
     * The component interface at the head of the path.
     *
     * @param api specifies whether the interface (<code>true</code>) or the class (<code>false</code>), if available, is to be returned.
     *
     * @return the component interface at the head of the path.
     */
    Class<?> head(boolean api);

    /**
     * The list of component interfaces or classes that comprise the dependency path.
     *
     * @param api specifies whether the interface (<code>true</code>) or the class (<code>false</code>), if available, is to be returned for each path element.
     *
     * @return the list of component interfaces or classes that comprise the dependency path.
     */
    List<Class<?>> path(boolean api);

    /**
     * Returns a string representation of the path.
     *
     * @param api specifies whether the interface (<code>true</code>) or the class (<code>false</code>), if available, is to be returned for each path element.
     *
     * @return a string representation of the path.
     */
    String toString(boolean api);
}

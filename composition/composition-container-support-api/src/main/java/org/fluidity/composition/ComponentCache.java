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
 * Caches components by context. Implementations must be thread safe.
 *
 * @author Tibor Varga
 */
public interface ComponentCache {

    /**
     * Looks up and instantiates if necessary using the supplied command, the component whose class is also specified to find its annotations.
     *
     *
     * @param source  something to identify who is creating instances through this cache.
     * @param context the context for the component.
     * @param api     the interface the component implements.
     * @param create  the command that performs instantiation of the component; if <code>null</code>, only a lookup is made otherwise instantiation is attempted
     *                if the no cached instance is found.
     *
     * @return the component instance.
     */
    Object lookup(Object source, ComponentContext context, Class<?> api, Instantiation create);

    /**
     * A command to create a component instance in some context.
     */
    interface Instantiation {

        /**
         * Creates and returns a new instance of a component.
         *
         * @return a new instance of a component; never <code>null</code>.
         */
        Object perform();
    }
}

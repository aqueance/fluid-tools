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

import org.fluidity.composition.spi.ComponentMapping;

/**
 * Caches components by context. Implementations must be thread safe.
 *
 * @author Tibor Varga
 */
public interface ComponentCache {

    /**
     * Looks up and instantiates if necessary using the supplied command, the component whose class is also specified to find its annotations.
     *
     * @param source             something to identify who is creating instances through this cache.
     * @param componentInterface the interface the component implements.
     * @param componentClass     the class of the component to return.
     * @param create             the command that performs instantiation of the component.
     *
     * @return the component instance.
     */
    Object lookup(Object source, Class<?> componentInterface, ComponentMapping componentClass, Command create);

    /**
     * A command to create a component instance in some context.
     */
    interface Command {

        /**
         * Creates and returns a new instance of a component.
         *
         * @param context the context in which to perform the instantiation.
         *
         * @return a new instance of a component.
         */
        Object run(ComponentContext context);
    }

    /**
     * Listens to component instantiation. The {@link ComponentCache#lookup(Object, Class, ComponentMapping, ComponentCache.Command)} method must
     * call an object implementing this interface when it has created a new instance of a component. The listener object must be passed to the cache
     * in its constructor.
     *
     * Implementations must be thread safe.
     */
    interface Listener {

        /**
         * Called when a component is instantiated.
         *
         * @param componentInterface the interface reference that triggered the instantiation.
         * @param component          the component that has just been instantiated.
         */
        void created(Class<?> componentInterface, Object component);
    }
}

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
 * Provides means to keep track of a dependency reference path.
 *
 * @author Tibor Varga
 */
public interface DependencyPath {

    /**
     * Follows a reference path, picking up a prevalent one if exists, else creating one. Circular references are handled when possible using proxies to delay
     * instantiation until first method call.
     *
     * @param api     the component interface being referred to.
     * @param context the original context for the instantiation path.
     * @param mapping the mapping, e.g., the component interface and class, of the reference.
     * @param command the command that performs the resolution.
     *
     * @return whatever the command returns.
     */
    Object follow(final Class<?> api, ContextDefinition context, ComponentMapping mapping, Command command);

    /**
     * The command to invoke in the context of a new dependency reference established by calling {@link #follow(Class, ContextDefinition, ComponentMapping,
     * Command)}.
     *
     * @author Tibor Varga
     */
    interface Command {

        /**
         * @param context the current context in the instantiation path.
         *
         * @return whatever the caller of {@link #follow(Class, ContextDefinition, ComponentMapping, Command)} expects to be returned.
         */
        Object run(ContextDefinition context);
    }
}

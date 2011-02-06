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
 * Provides means to keep track of a dependency reference chain.
 *
 * @author Tibor Varga
 */
public interface ReferenceChain {

    /**
     * Adds a new reference to the chain and then removes it after the resolution completes.
     *
     * @param references the object to keep track of dependency reference chains.
     * @param context    the original context for the instantiation chain.
     * @param mapping    the mapping, e.g., the component interface and class, of the reference.
     * @param dependency the defined type of the reference.
     * @param command    the command that performs the resolution.
     *
     * @return whatever the command returns.
     */
    <T> T track(Reference references, ContextDefinition context, ComponentMapping mapping, Class<?> dependency, Command<T> command);

    /**
     * The command to invoke in the context of a new dependency reference established by calling {@link ReferenceChain#track(ReferenceChain.Reference,
     * ContextDefinition, ComponentMapping, Class, ReferenceChain.Command)}.
     *
     * @author Tibor Varga
     */
    interface Command<T> {

        /**
         *
         * @param references the object to keep track of dependency reference chains.
         * @param context    the current context in the instantiation chain.
         * @return whatever the caller of {@link ReferenceChain#track(ReferenceChain.Reference, ContextDefinition, ComponentMapping, Class,
         *         ReferenceChain.Command)} expects to get returned.
         */
        T run(Reference references, ContextDefinition context);
    }

    // TODO: document
    interface Reference {

        Class<?> type();

        <T> T next(Class<?> type, final ComponentMapping mapping, ContextDefinition context, Command<T> command);

        boolean isCircular();
    }
}

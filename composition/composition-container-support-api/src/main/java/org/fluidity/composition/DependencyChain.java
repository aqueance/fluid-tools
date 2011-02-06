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
public interface DependencyChain {

    /**
     * Follows a reference chain, picking up a prevalent one if exists, else creating one.
     *
     * @param context the original context for the instantiation chain.
     * @param mapping the mapping, e.g., the component interface and class, of the reference.
     * @param command the command that performs the resolution.
     *
     * @return whatever the command returns.
     */
    <T> T follow(ContextDefinition context, ComponentMapping mapping, Command<T> command);

    /**
     * The command to invoke in the context of a new dependency reference established by calling {@link DependencyChain#follow(ContextDefinition,
     * ComponentMapping, Command)}.
     *
     * @author Tibor Varga
     */
    interface Command<T> {

        /**
         * @param lineage the object to keep track of dependency reference chains.
         * @param context the current context in the instantiation chain.
         *
         * @return whatever the caller of {@link DependencyChain#follow(ContextDefinition, ComponentMapping, Command)} expects to be returned.
         */
        T run(Lineage lineage, ContextDefinition context);
    }

    /**
     * Represents the chain of references to the particular dependency where the object appears.
     */
    interface Lineage {

        /**
         * Tells if the chain of reference up to and including the dependency resolver receiving this object that it is being invoked the second time during one
         * dependency resolution cycle; i.e., there is circular reference in the dependency chain.
         *
         * @return <code>true</code> if the dependency resolver adds circular dependency to the dependency chain, <code>false</code> otherwise.
         */
        boolean isCircular();

        /**
         * Converts the dependency chain to a (more or less) human readable form. The chain includes the dependency resolver receiving this object.
         *
         * @return the {@link String} representation of the dependency chain.
         */
        String toString();
    }
}

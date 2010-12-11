/*
 * Copyright (c) 2006-2010 Tibor Adam Varga (tibor.adam.varga on gmail)
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

/**
 * Provides means to keep track of a dependency reference chain.
 *
 * @author Tibor Varga
 */
public interface ReferenceChain {

    /**
     * Returns the defined type of the last reference on the chain.
     *
     * @return the defined type of the last reference on the chain.
     */
    Class<?> lastReference();

    /**
     * Adds a new reference to the chain and then removes it after the resolution completes.
     *
     * @param mapping    the mapping, e.g., the component interface and class, of the reference.
     * @param dependency the defined type of the reference.
     * @param command    the command that performs the resolution.
     *
     * @return whatever the command returns.
     */
    <T> T nested(ComponentMapping mapping, Class<?> dependency, Command<T> command);

    /**
     * The command to invoke in the context of a new dependency reference established by calling {@link org.fluidity.composition.spi.ReferenceChain#nested(ComponentMapping,Class,org.fluidity.composition.spi.ReferenceChain.Command)}.
     *
     * @author Tibor Varga
     */
    interface Command<T> {

        /**
         * @param circular tells whether this particular producer has already been asked for a component in the current reference chain.
         *
         * @return whatever the caller of {@link org.fluidity.composition.spi.ReferenceChain#nested(ComponentMapping,Class,org.fluidity.composition.spi.ReferenceChain.Command)}
         *         expects to get returned.
         */
        T run(boolean circular);
    }

    /**
     * Visits each item on the reference chain.
     *
     * @param visitor the visitor to send each reference in turn.
     */
    void iterate(Visitor visitor);

    /**
     * Provides a log and user friendly string representation of the current chain.
     *
     * @return a log and user friendly String representation of the current chain.
     */
    String print();

    /**
     * The visitor to invoke while iterating through the reference chain using the {@link ReferenceChain#iterate(org.fluidity.composition.spi.ReferenceChain.Visitor)}
     * method.
     *
     * @author Tibor Varga
     */
    interface Visitor<T> {

        /**
         * Visits one reference in the reference chain.
         *
         * @param link the current link in the reference chain.
         *
         * @return <code>true</code> if more iterations are needed, <code>false</code> if iteration should terminate.
         */
        boolean visit(Link link);
    }

    /**
     * Represents a link in a reference chain.
     *
     * @author Tibor Varga
     */
    interface Link {

        /**
         * Returns the component mapping at this link.
         *
         * @return the component mapping at this link.
         */
        ComponentMapping mapping();

        /**
         * Returns the class the next component is referred to as at the point in the reference chain represented by this link.
         *
         * @return the class the next component is referred to as at the point in the reference chain represented by this link
         */
        Class<?> reference();
    }
}

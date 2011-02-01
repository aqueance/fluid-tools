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
 * Tracks context definitions and consumption along a reference chain.
 * <p/>
 * A context aware component or its factory declares, using the {@link org.fluidity.composition.Context} annotation, context annotations that they understand.
 * Upon instantiation, a context is passed to such a component or its factory in the form of a {@link ComponentContext} object, containing the annotation
 * instances used to define the context of the instantiation.
 * <p/>
 * The values in the context object are calculated from any context annotation encountered during the entire dependency chain starting at that base and ending
 * at the instantiation of a given component.
 * <p/>
 * The context consumed by a context aware component is inherited back through the dependency chain ending with the instantiation of that component up to the
 * point where the context is fully defined and may cause contextual instantiation of components in between that are not themselves context aware.
 *
 * @author Tibor Varga
 */
public interface ContextChain {

    /**
     * Returns the currently established context.
     *
     * @return the currently established context.
     */
    ComponentContext currentContext();

    /**
     * Adds the given context to the chain and then removes it when the given command completes.
     *
     * @param context the context to add to the stack.
     * @param command the command that will receive the new context, which includes the given properties and all others from the stack.
     *
     * @return whatever the given command returns.
     */
    <T> T track(ComponentContext context, Command<T> command);

    /**
     * Returns the actual context supported by the given component type using the possibly larger context established at this point.
     *
     *
     * @param componentType  the interface implemented by the component.
     * @param mapping the component type to check for supported context annotations.
     * @param context        the actual context established at this point.
     * @param resolutions    the dependency reference chain to search for the factory object that consumes the context on behalf of
     *                       <code>mapping</code>.
     *
     * @return the narrowed context that can be passed to an instance of the given component class.
     */
    ComponentContext consumedContext(Class<?> componentType, ComponentMapping mapping, ComponentContext context, ReferenceChain resolutions);

    /**
     * Adjusts the context consumed at this point and down the reference chain.
     *
     * @param context the context consumed at the point of calling this method.
     */
    void contextConsumed(ComponentContext context);

    /**
     * Returns the context consumed at this point and down the reference chain.
     *
     * @return the context consumed at this point and down the reference chain.
     */
    ComponentContext prevalentContext();

    /**
     * A command to run while tracking a new context. The established context is restored to its prior value after the command completes.
     *
     * @author Tibor Varga
     */
    interface Command<T> {

        /**
         * Runs the command in a new component context.
         *
         * @param context the context established at the point of invocation.
         *
         * @return whatever the caller of {@link ContextChain#track(ComponentContext, ContextChain.Command)} wishes to receive from the command.
         */
        T run(ComponentContext context);
    }
}

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

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.ContextChain;
import org.fluidity.composition.ContextFactory;
import org.fluidity.composition.ReferenceChain;

/**
 * Capable of resolving component references.
 */
public interface DependencyResolver {

    /**
     * The resolution stack associated with the resolver.
     *
     * @return the resolution stack, never <code>null</code>.
     */
    ReferenceChain referenceChain();

    /**
     * The context chain associated with the resolver.
     *
     * @return the context chain, never <code>null</code>.
     */
    ContextChain contextChain();

    /**
     * The context factory associated with the resolver.
     *
     * @return the context factory, never <code>null</code>.
     */
    ContextFactory contextFactory();

    /**
     * Returns the resolved component instance for the given component interface.
     *
     * @param type    the component interface sought.
     * @param context the reference context for the resolution.
     *
     * @return the resolved component instance or <code>null</code> is no unique resolution is possible.
     *
     * @throws org.fluidity.composition.ComponentContainer.ResolutionException
     *          when the type cannot be resolved.
     */
    <T> T resolve(Class<T> type, ComponentContext context) throws ComponentContainer.ResolutionException;

    /**
     * Returns a new component instance for the given component interface.
     *
     * @param type    the component interface sought.
     * @param context the reference context for the resolution.
     *
     * @return the resolved component instance or <code>null</code> is no unique resolution is possible.
     *
     * @throws org.fluidity.composition.ComponentContainer.ResolutionException
     *          when the type cannot be resolved.
     */
    <T> T create(Class<T> type, ComponentContext context);

    /**
     * Returns a new nested container with its base context set to the given properties.
     *
     * @param context the properties for the new container to use as base context.
     *
     * @return a new component container.
     */
    ComponentContainer container(ComponentContext context);
}
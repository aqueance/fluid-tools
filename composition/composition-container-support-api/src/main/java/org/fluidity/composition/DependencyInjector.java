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

import java.lang.reflect.Constructor;

import org.fluidity.composition.network.ContextDefinition;
import org.fluidity.composition.network.Graph;
import org.fluidity.composition.spi.ComponentMapping;
import org.fluidity.composition.spi.DependencyResolver;

/**
 * Performs dependency injection.
 *
 * @author Tibor Varga
 */
public interface DependencyInjector {

    /**
     * Resolves all parameters of the given constructor and invokes it to construct a new object with it.
     *
     * @param traversal
     * @param container
     * @param mapping     the mapping that triggered the dependency resolution.
     * @param context     the instantiation context of the object being constructed.
     * @param constructor the constructor to find the arguments for.
     *
     * @return the argument array for the given constructor.
     */
    Graph.Node constructor(Graph.Traversal traversal,
                           DependencyResolver container,
                           ComponentMapping mapping,
                           ContextDefinition context,
                           Constructor<?> constructor);

    /**
     * Sets all {@link Component} annotated fields of the receiver.
     *
     * @param traversal
     * @param container
     * @param mapping     the mapping that triggered the dependency resolution.
     * @param context  the instantiation context of the object being constructed.
     * @param instance the object to set the fields of.
     * @return the received instances.
     */
    <T> T fields(Graph.Traversal traversal, DependencyResolver container, ComponentMapping mapping, ContextDefinition context, T instance);
}

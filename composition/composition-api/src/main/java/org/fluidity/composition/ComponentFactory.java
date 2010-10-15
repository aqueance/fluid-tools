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

package org.fluidity.composition;

/**
 * Poses as a factory for a particular component. This factory produces a component when requested. The interface of the component is specified in the {@link
 * org.fluidity.composition.Component#api()} annotation of the the factory implementation class and the class of the component is specified in the {@link
 * org.fluidity.composition.Component#type()} annotation. Both these annotation parameters must be present.
 * <p/>
 * If the components are context dependent, the factory class must provide the valid context keys using the {@link org.fluidity.composition.Context#names()}
 * class annotation.
 *
 * @author Tibor Varga
 */
public interface ComponentFactory<T> {

    /**
     * Creates a new instance of the component that this is a factory for.
     *
     * @param container is the container to resolve dependencies of the component from.
     * @param context   is the context for the instance to create. When this is null or empty, the default instance must be created. The key set in the context
     *                  is taken from the list of names in the {@link Context#names()} annotation of the component class.
     *
     * @return the component created, never <code>null</code>.
     */
    T newComponent(OpenComponentContainer container, ComponentContext context);
}

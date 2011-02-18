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
import org.fluidity.composition.OpenComponentContainer;

/**
 * A variant factory offers context dependent instance variants of an otherwise singleton component that itself can in some way be configured to adapt to
 * various externally defined contexts. The variant factory lists the context annotations it understands in its {@link org.fluidity.composition.Context} class
 * annotation.
 * <p/>
 * A {@link ComponentVariantFactory} works in conjunction with an otherwise singleton component independently registered in a dependency injection container
 * accessible to the factory with a binding that allows new instances to be created, i.e., the component has not been bound by {@link
 * ComponentContainer.Registry#bindInstance(Object)} or {@link ComponentContainer.Registry#bindInstance(Object, Class[])}.
 *
 * @author Tibor Varga
 */
public interface ComponentVariantFactory {

    /**
     * Binds the component that this is a factory for in the provided container. The original bindings for the component will be applied to the container
     * returned by the factory.
     *
     *
     *
     *
     * @param container is the container to resolve dependencies of the component from.
     * @param context   is the context for the instance to create. When this is null or empty, the default instance must be returned. The key set in the context
     *                  is taken from the list of annotation classes in the {@link org.fluidity.composition.Context} annotation of this factory.
     *
     * @throws ComponentContainer.ResolutionException
     *          of a component cannot be created.
     */
    void newComponent(OpenComponentContainer container, ComponentContext context) throws ComponentContainer.ResolutionException;
}

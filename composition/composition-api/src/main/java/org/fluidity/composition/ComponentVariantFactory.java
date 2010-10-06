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
 * A variants factory offers context dependent instance variants of an otherwise singleton component that itself can in some way be configured to adapt to
 * various externally defined contexts. The variant factory lists the context keys it understands in its {@link org.fluidity.composition.Context#names()} class
 * annotation.
 * <p/>
 * A <code>ComponentVariantFactory</code> works in conjunction with an otherwise singleton component bound by either {@link
 * ComponentContainer.Registry#bindComponent(Class, Class)} or {@link ComponentContainer.Registry#bindDefault(Class)} such that contextual information collected
 * from the component instantiation stack and passed forward to the variant factory, it has the possibility to get various instances of the otherwise singleton
 * component.
 *
 * @author Tibor Varga
 */
public interface ComponentVariantFactory {

    /**
     * Binds the component that this is a factory for in the provided container. The component bindings, if any, will be applied to the container returned by
     * the factory.
     *
     * @param container is the container to resolve dependencies of the component from.
     * @param context   is the context for the instance to create. When this is null or empty, the default instance must be returned.
     *
     * @return a container to get the instance from; may be <code>null</code>, in which case no instance will be created, otherwise either the component bound
     *         by the factory is returned or the default one if the factory binds no component. The key set in the context is taken from the list of names in
     *         the {@link org.fluidity.composition.Context#names()} annotation.
     *
     * @throws org.fluidity.composition.ComponentContainer.ResolutionException
     *          of a component cannot be created.
     */
    OpenComponentContainer newComponent(OpenComponentContainer container, ComponentContext context) throws ComponentContainer.ResolutionException;
}

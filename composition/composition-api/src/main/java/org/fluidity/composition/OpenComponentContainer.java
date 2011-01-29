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

import java.util.List;

/**
 * This is a dependency injection container that components can be added to.
 *
 * @author Tibor Varga
 */
public interface OpenComponentContainer extends ComponentContainer {

    /**
     * Returns the parent container, if any.
     *
     * @return th parent container, if any.
     */
    OpenComponentContainer getParentContainer();

    /**
     * Returns the interface through which component bindings can be added to this container. The returned interface cannot be used to get components out of the
     * container. Thus, a container is write-only when it is being populated and read-only after it has been populated.
     *
     * @return a {@link ComponentContainer.Registry} instance.
     */
    ComponentContainer.Registry getRegistry();

    /**
     * Returns in instantiation order all registered components implementing the given interface.
     *
     * @param componentInterface filters the component instances returned.
     *
     * @return in instantiation order all registered components implementing the given interface.
     */
    <T> List<T> getAllComponents(Class<T> componentInterface);
}

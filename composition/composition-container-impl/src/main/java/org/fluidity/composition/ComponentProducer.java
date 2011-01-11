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
 * Extends the {@link ComponentMapping} interface with methods requied by the actual container implementation.
 *
 * @author Tibor Varga
 */
interface ComponentProducer extends ComponentMapping {

    /**
     * Returns the class of the component instance.
     *
     * @return the class of the component instance.
     */
    Class<?> componentClass();

    /**
     * Creates the actual component.
     *
     * @param container the container to use to resolve dependencies.
     * @param circular  a flag telling if this producer has been invoked the second time during one invocation chain, e.g., to signify circular reference in the
     *                  component dependency graph.
     *
     * @return the component instance, never <code>null</code>.
     */
    Object create(SimpleContainer container, boolean circular);

    /**
     * Tells whether this mapping has been created for an already instantiated component.
     *
     * @return <code>true</code> if this mapping represents an already instantiated component, <code>false</code> otherwise.
     */
    boolean isInstanceMapping();
}

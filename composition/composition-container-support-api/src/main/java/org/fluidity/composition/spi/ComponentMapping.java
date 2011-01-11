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

/**
 * Component mapping details used by container services.
 *
 * @author Tibor Varga
 */
public interface ComponentMapping {

    /**
     * Returns the (interface) class the component has been bound to.
     *
     * @return the (interface) class the component has been bound to.
     */
    Class<?> componentInterface();

    /**
     * Tells whther the mapping is that of a {@link org.fluidity.composition.spi.ComponentVariantFactory}.
     *
     * @return <code>true</code> if the mapping is that of a {@link org.fluidity.composition.spi.ComponentVariantFactory}, <code>false</code> otherwise.
     */
    boolean isVariantMapping();

    /**
     * Returns the component class in case of a {@link org.fluidity.composition.spi.ComponentVariantFactory} or a {@link sun.awt.ComponentFactory} mapping.
     *
     * @return the component class in case of a {@link org.fluidity.composition.spi.ComponentVariantFactory} or a {@link sun.awt.ComponentFactory} mapping.
     */
    Class<?> factoryClass();
}

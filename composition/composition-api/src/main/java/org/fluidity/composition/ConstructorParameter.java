/*
 * Copyright (c) 2006-2008 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Softweare"), to deal
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.fluidity.composition;

/**
 * An explicitly specified constructor parameter of a component, whose actual value is either another component from the
 * same container, a list of the same, or a constant value. The <code>ComponentContainer.Registry</code> object can
 * create instances of concrete classes that implement this interface. A forced component parameter is useful when you
 * want to explicitly specify what to pass to the constructor of a given component. <p/> This object is used in
 * <code>PackageBindings</code> implementations.
 *
 * @author Tibor Varga
 */
public interface ConstructorParameter {

    /**
     * Whatever the underlying dependency injection implementation recognises as a forced component parameter.
     *
     * @return an object representing the underlying parameter implementation.
     */
    Object representation();
}

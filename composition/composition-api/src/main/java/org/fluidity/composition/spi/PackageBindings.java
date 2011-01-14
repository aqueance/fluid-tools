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

/**
 * An implementation of this interface is placed, either automatically by the org.fluidity.composition:maven-composition-plugin or manually by the component
 * developer, in each package that contains component implementations. The goal of these classes is to add component bindings for component implementations in
 * the respective package to the supplied registry. The components thus bound may depend on other components in the container and vice versa.
 *
 * @author Tibor Varga
 */
public interface PackageBindings extends ComponentContainer.Bindings {

    /**
     * Perform component specific initialisation if necessary. This method is invoked once after the <code>registerComponents()</code> method of all
     * <code>PackageBinding</code> objects have been invoked.
     *
     * @param container is the container containing all global components that will be available during the life
     */
    void initializeComponents(ComponentContainer container);

    /**
     * Perform component specific shutdown if necessary. This method is invoked once when the application is being shut down.
     *
     * @param container is the container containing all global components that will be available during the life
     */
    void shutdownComponents(ComponentContainer container);
}
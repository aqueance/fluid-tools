/*
 * Copyright (c) 2006-2008 Tibor Adam Varga (tibor.adam.varga@gmail.com)
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
 *
 */
package org.fluidity.composition;

import java.util.Map;

/**
 * Bootstraps the component container. A concrete implementation normally wraps and populates a third-party dependency
 * injection container.
 *
 * @author Tibor Varga
 */
public interface ContainerBootstrap {

    /**
     * Finds all package component bindings and invokes them to add their bindings and initialises components after all
     * components have been registered. <p/> The idea is to find all <code>PackageBinding</code> objects using the
     * supplied discovery component, invoke them to add their bindings and then invoke their initialisation method.
     * Shutdown hooks are also expected to be used.
     *
     * @param discovery   is the component to use to find <code>PackageBindings</code> implementations.
     * @param parent      is the container to use as the parent of the one returned; can be <code>null</code>, in which
     *                    case a standalone container is returned.
     * @param classLoader is the class loader to use to discover package bindings. Package bindings found in the class
     *                    loader ancestry are ignored when the parent container is specified.
     * @param properties  is the properties to bind to the container as a means to configure binding instances at
     *                    run-time.
     *
     * @return the container containing the registered components.
     */
    OpenComponentContainer populateContainer(final ClassDiscovery discovery,
                                             final Map properties,
                                             final OpenComponentContainer parent,
                                             final ClassLoader classLoader);
}
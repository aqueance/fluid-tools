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

import org.fluidity.foundation.KeyedNamed;

/**
 * This is a component that is started/stopped as the application container starts/stops. There is no deterministic order in which deployed components are
 * started/stopped and so deployed components should be independent of one another.
 *
 * <p/>
 *
 * All subclasses of this interface will be marked as a service provider for this interface and will be automatically found and controlled by a suitable {@link
 * DeploymentBootstrap} implementation.
 *
 * TODO: move to a new module under deployments
 */
@ServiceProvider(api = DeployedComponent.class)
public interface DeployedComponent extends KeyedNamed {

    /**
     * The component has an ID that is returned by this method.
     *
     * @return a short String idenfifying this component among other {@link org.fluidity.composition.DeployedComponent}s.
     */
    String key();

    /**
     * The component has a name that is returned by this method.
     *
     * @return a String, never <code>null</code>.
     */
    String name();

    /**
     * Starts the component.
     *
     * @throws Exception when thrown is logged and will cause the application start-up to be aborted if possible.
     */
    void start() throws Exception;

    /**
     * Stops the component.
     *
     * @throws Exception when thrown is logged.
     */
    void stop() throws Exception;
}

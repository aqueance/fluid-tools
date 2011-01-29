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

package org.fluidity.deployment;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.fluidity.composition.ContainerBoundary;
import org.fluidity.composition.ServiceProvider;
import org.fluidity.foundation.Exceptions;

/**
 * A servlet that bootstraps and controls all {@link DeployedComponent} and {@link DeploymentObserver} objects in the application. The class requires
 * either to be explicitly registered as a <code>ServletContextListeners</code> or a mechanism that finds this class and forwards listener method invocations to
 * it. Fluid Tools has such mechanism in the form of a <code>ServletContextListeners</code>, <code>AggregatingServletContextListener</code>, which uses service
 * provider discovery to find any implementation of the <code>ServletContextListeners</code> interface, including this class, as long the implementation is
 * marked by the {@link ServiceProvider} annotation.
 *
 * @author Tibor Varga
 */
@ServiceProvider
public final class BootstrapListener implements ServletContextListener {

    private final DeploymentBootstrap bootstrap = new ContainerBoundary().getComponent(DeploymentBootstrap.class);

    public void contextInitialized(final ServletContextEvent event) {
        assert bootstrap != null : DeploymentBootstrap.class;

        Exceptions.wrap(new Exceptions.Command<Void>() {
            public Void run() throws Exception {
                bootstrap.load();
                return null;
            }
        });
    }

    public void contextDestroyed(final ServletContextEvent event) {
        bootstrap.unload();
    }
}

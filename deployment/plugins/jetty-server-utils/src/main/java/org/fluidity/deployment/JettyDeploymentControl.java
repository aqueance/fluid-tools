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

import java.util.Map;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ContainerBoundary;
import org.fluidity.composition.spi.EmptyPackageBindings;
import org.fluidity.foundation.Exceptions;

import org.eclipse.jetty.server.Server;

/**
 * @author Tibor Varga
 */
@Component(api = { DeploymentControl.class, RuntimeControl.class }, automatic = false)
final class JettyDeploymentControl implements DeploymentControl {

    private final Server server;
    private final boolean standalone;

    public JettyDeploymentControl(final Server server, final boolean standalone) {
        this.server = server;
        this.standalone = standalone;
    }

    public void completed() {
        // empty
    }

    public boolean isStandalone() {
        return standalone;
    }

    public void stop() {
        Exceptions.wrap("stopping Jetty server", new Exceptions.Command<Void>() {
            public Void run() throws Exception {
                server.stop();
                return null;
            }
        });
    }

    /**
     * @author Tibor Varga
     */
    static class Bindings extends EmptyPackageBindings {

        private static final Object BINDINGS_KEY = new Object();

        private final ComponentContainer.Bindings dependencies;

        @SuppressWarnings("unchecked")
        static void set(final ContainerBoundary container, final Server server, final boolean standalone) {
            container.setBindingProperty(BINDINGS_KEY, new ComponentContainer.Bindings() {
                public void bindComponents(final ComponentContainer.Registry registry) {
                    registry.bindInstance(server, Server.class);
                    registry.bindInstance(standalone, Boolean.TYPE);
                }
            });
        }

        public Bindings(final Map<Object, ComponentContainer.Bindings> properties) {
            dependencies = properties.get(BINDINGS_KEY);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void bindComponents(final ComponentContainer.Registry registry) {
            if (dependencies != null) {
                dependencies.bindComponents(registry.makeChildContainer(JettyDeploymentControl.class, DeploymentControl.class).getRegistry());
            }
        }
    }
}

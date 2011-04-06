/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
                    registry.bindInstance(server);
                    registry.bindInstance(standalone);
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

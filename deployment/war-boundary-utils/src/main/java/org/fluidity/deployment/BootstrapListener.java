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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.ContainerBoundary;
import org.fluidity.foundation.Exceptions;

/**
 * A servlet that bootstraps and controls all {@link DeployedComponent} and {@link DeploymentObserver} objects in the application. The class requires either to
 * be explicitly registered as a <code>ServletContextListeners</code> or a mechanism that finds this class and forwards listener method invocations to it.
 * Fluid Tools has such mechanism in the form of a <code>ServletContextListener</code>, <code>AggregatingServletContextListener</code>, which uses service
 * provider discovery to find any implementation of the <code>ServletContextListener</code> interface marked with the {@link ComponentGroup} annotation, such
 * as this class.
 *
 * @author Tibor Varga
 */
@ComponentGroup
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

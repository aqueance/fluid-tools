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

package org.fluidity.deployment.cli;

import org.fluidity.composition.Component;
import org.fluidity.composition.ContainerBoundary;
import org.fluidity.composition.Optional;
import org.fluidity.composition.spi.ShutdownTasks;
import org.fluidity.deployment.DeploymentBootstrap;
import org.fluidity.deployment.LaunchArguments;

/**
 * A command line main class that bootstraps the application's dependency injection container, invokes {@link DeploymentBootstrap} if implemented to load and
 * control deployment units and invokes the application supplied main loop, a class implementing the {@link MainLoop} interface. Any component can access the
 * command line parameters by having a constructor with, among other dependencies, a {@link LaunchArguments} parameter.
 * <p/>
 * This class is public for its main method to be found by the Java launcher.
 *
 * @author Tibor Varga
 */
@Component
public final class CommandLineBootstrap {

    private final MainLoop main;

    public CommandLineBootstrap(final @Optional DeploymentBootstrap bootstrap, final MainLoop main, final ShutdownTasks shutdown) throws Exception {
        this.main = main;

        if (bootstrap != null) {
            shutdown.add("bootstrap", new Runnable() {
                public void run() {
                    bootstrap.unload();
                }
            });

            bootstrap.load();
        }
    }

    private void run() {
        main.run();
    }

    public static void main(final String[] args) throws Exception {
        final ContainerBoundary container = new ContainerBoundary();
        container.setBindingProperty(LaunchArguments.ARGUMENTS_KEY, args);
        container.getComponent(CommandLineBootstrap.class).run();
    }
}
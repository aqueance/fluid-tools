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

/**
 * A command line main class that bootstraps the application's dependency injection container, invokes {@link Application#run()} to load and
 * run the application supplied main loop.
 * <p/>
 * This class is public for its main method to be found by the Java launcher.
 *
 * @author Tibor Varga
 */
@Component
public final class CommandLineBootstrap {

    private final Application application;

    public CommandLineBootstrap(final Application application) throws Exception {
        this.application = application;
    }

    private void run(final String[] args) {
        application.run(args);
    }

    public static void main(final String[] args) throws Exception {
        final ContainerBoundary container = new ContainerBoundary();
        container.getComponent(CommandLineBootstrap.class).run(args);
    }
}

/*
 * Copyright (c) 2006-2012 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.deployment.launcher;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ContainerBoundary;
import org.fluidity.deployment.cli.Application;

/**
 * A command line main class that populates the application's dependency injection container and invokes {@link Application#run(String[])} on the application
 * supplied implementation to load and run the application's main loop.
 * <p/>
 * <b>NOTE</b>: This class is public <em>only</em> so that its main method can be found by the Java launcher.
 * <h3>Usage</h3>
 * Use the <code>org.fluidity.maven:fluidity-archetype-standalone-jar</code> Maven archetype to generate the command line application wrapper project.
 *
 * @author Tibor Varga
 */
public final class ShellApplicationBootstrap {

    private final Application application;

    /**
     * Dependency injected constructor.
     *
     * @param application the main loop to invoke.
     *
     * @throws Exception in any exceptional situation.
     */
    public ShellApplicationBootstrap(final Application application) throws Exception {
        this.application = application;
    }

    private void run(final String[] args) throws Exception {
        application.run(args);
    }

    /**
     * Command line application entry point.
     *
     * @param args the command line arguments.
     *
     * @throws Exception whatever {@link Application#run(String[])} throws.
     */
    public static void main(final String[] args) throws Exception {
        new ContainerBoundary().getComponent(ShellApplicationBootstrap.class, new ComponentContainer.Bindings() {
            @SuppressWarnings("unchecked")
            public void bindComponents(final ComponentContainer.Registry registry) {
                registry.bindComponent(ShellApplicationBootstrap.class);
            }
        }).run(args);
    }
}

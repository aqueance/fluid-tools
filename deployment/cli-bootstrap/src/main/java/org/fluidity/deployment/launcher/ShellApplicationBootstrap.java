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

import org.fluidity.composition.Component;
import org.fluidity.composition.Containers;
import org.fluidity.composition.Optional;
import org.fluidity.deployment.cli.Application;

/**
 * A command line main class that populates the application's dependency injection container and invokes {@link Application#run(String[])} on the detected
 * implementation of the {@link Application} interface.
 * <p/>
 * <b>NOTE</b>: This class is public <em>only</em> so that its <code>main</code> method can be found by the Java launcher.
 * <h3>Usage</h3>
 * Use the <code>org.fluidity.maven:fluidity-archetype-standalone-jar</code> Maven archetype to generate the command line application wrapper project.
 *
 * @author Tibor Varga
 */
@Component(automatic = false)
public final class ShellApplicationBootstrap {

    /**
     * Command line application entry point.
     *
     * @param arguments the command line arguments.
     *
     * @throws IllegalStateException if no {@link Application} component is found.
     * @throws Exception whatever {@link Application#run(String[])} throws.
     */
    public static void main(final String[] arguments) throws Exception {
        Containers.global().instantiate(ShellApplicationBootstrap.class).run(arguments);
    }

    private final Application application;

    ShellApplicationBootstrap(final @Optional Application application) {
        if (application == null) {
            throw new IllegalStateException(String.format("Application not found; implement %s and annotate it with @%s",
                                                          Application.class.getName(),
                                                          Component.class.getSimpleName()));
        }

        this.application = application;
    }

    private void run(final String[] arguments) throws Exception {
        application.run(arguments);
    }
}

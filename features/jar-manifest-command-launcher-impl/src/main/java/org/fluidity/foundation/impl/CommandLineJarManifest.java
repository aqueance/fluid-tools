/*
 * Copyright (c) 2006-2018 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.foundation.impl;

import java.util.jar.Attributes;

import org.fluidity.deployment.launcher.ShellApplicationBootstrap;
import org.fluidity.deployment.maven.Logger;
import org.fluidity.deployment.plugin.spi.JarManifest;
import org.fluidity.deployment.plugin.spi.SecurityPolicy;
import org.fluidity.foundation.jarjar.Launcher;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * Launches a main class from a JAR file using a class loader that can load classes from JAR files nested inside the main JAR. Nested JAR files must be located
 * in the path denoted by the <code>Nested-Dependencies</code> manifest attribute. The main class to be loaded is defined by the
 * <code>Start-Class</code> manifest attribute. The <code>Main-Class</code> manifest attribute, obviously, points to this class.
 *
 * @author Tibor Varga
 */
@SuppressWarnings("UnusedDeclaration")
final class CommandLineJarManifest implements JarManifest {

    private static final String LAUNCHER = ShellApplicationBootstrap.class.getName();

    @SuppressWarnings("EqualsReplaceableByObjectsCall")
    public SecurityPolicy processManifest(final MavenProject project,
                                          final Attributes attributes,
                                          final SecurityPolicy policy,
                                          final Logger log,
                                          final Dependencies dependencies) throws MojoExecutionException {
        final boolean executable = dependencies.unpacked();

        if (executable) {
            final String original = attributes.getValue(Launcher.START_CLASS);

            if (original == null) {
                final String main = attributes.getValue(Attributes.Name.MAIN_CLASS);

                if (!Launcher.class.getName().equals(main)) {
                    attributes.putValue(Launcher.START_CLASS, main == null ? LAUNCHER : main);
                    attributes.put(Attributes.Name.MAIN_CLASS, Launcher.class.getName());
                }
            }
        } else if (attributes.getValue(Attributes.Name.MAIN_CLASS) == null) {
            attributes.put(Attributes.Name.MAIN_CLASS, LAUNCHER);
        }

        if (log.active()) {
            final String value = executable ? attributes.getValue(Launcher.START_CLASS) : attributes.getValue(Attributes.Name.MAIN_CLASS);
            log.detail("Main class: %s", value == null ? "none" : value.equals(LAUNCHER) ? "built in" : value);
        }

        return null;
    }
}

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

package org.fluidity.foundation.impl;

import java.util.Collection;
import java.util.List;
import java.util.jar.Attributes;

import org.fluidity.deployment.launcher.ShellApplicationBootstrap;
import org.fluidity.deployment.plugin.spi.JarManifest;
import org.fluidity.foundation.Archives;
import org.fluidity.foundation.Strings;
import org.fluidity.foundation.jarjar.Launcher;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

/**
 * Launches a main class from a JAR file using a class loader that can load classes from JAR files nested inside the main JAR. Nested JAR files must be located
 * in the path denoted by the <code>Nested-Dependencies</code> manifest attribute. The main class to be loaded is defined by the
 * <code>Original-Main-Class</code> manifest attribute. The <code>Main-Class</code> manifest attribute, obviously, points to this class.
 *
 * @author Tibor Varga
 */
@SuppressWarnings("UnusedDeclaration")
public final class CommandLineJarManifest implements JarManifest {

    public boolean needsCompileDependencies() {
        return false;
    }

    public Packaging packaging() {
        return Packaging.EXCLUDE;
    }

    public String dependencyPath() {
        return null;
    }

    public void processManifest(final MavenProject project, final Attributes attributes, final List<String> paths, final Collection<Artifact> dependencies) {
        final String originalMainClass = attributes.getValue(Launcher.ORIGINAL_MAIN_CLASS);

        if (originalMainClass == null) {
            final String mainClass = attributes.getValue(Attributes.Name.MAIN_CLASS);
            attributes.putValue(Launcher.ORIGINAL_MAIN_CLASS, mainClass == null ? ShellApplicationBootstrap.class.getName() : mainClass);
            attributes.put(Attributes.Name.MAIN_CLASS, Launcher.class.getName());
        }

        attributes.putValue(Archives.Nested.attribute(null), Strings.delimited(" ", paths));
    }

    @Override
    public String toString() {
        return "Fluid Tools executable JAR manifest handler";
    }
}

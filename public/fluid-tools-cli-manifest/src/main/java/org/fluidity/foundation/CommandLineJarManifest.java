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

package org.fluidity.foundation;

import java.util.Collection;
import java.util.List;
import java.util.jar.Attributes;

import org.fluidity.deployment.JarManifest;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

/**
 * Launches a main class from a jar file using a class loader that can load classes from jar files nested inside the main jar. Nested jar files must be located
 * in the path denoted by the Nested-Dependencies manifest attribute. The main class to be loaded is defined by the Original-Main-Class manifest attribute. The
 * Main-Class manifest attribute, obviously, points to this class.
 *
 * @author Tibor Varga
 */
public class CommandLineJarManifest implements JarManifest {

    public boolean needsCompileDependencies() {
        return false;
    }

    public boolean processManifest(final MavenProject project, final Attributes attributes, final List<String> paths, final Collection<Artifact> dependencies) {
        final String mainClass = attributes.getValue(Attributes.Name.MAIN_CLASS);

        if (mainClass == null) {
            throw new IllegalStateException(String.format("Manifest does not contain %s", Attributes.Name.MAIN_CLASS));
        }

        attributes.putValue(JarJarLauncher.ORIGINAL_MAIN_CLASS, mainClass);
        attributes.put(Attributes.Name.MAIN_CLASS, JarJarLauncher.class.getName());

        final StringBuilder dependencyList = new StringBuilder();

        for (final String dependency : paths) {
            if (dependencyList.length() > 0) {
                dependencyList.append(' ');
            }

            dependencyList.append(dependency);
        }

        attributes.putValue(JarJarLauncher.NESTED_DEPENDENCIES, dependencyList.toString());

        return true;
    }

}

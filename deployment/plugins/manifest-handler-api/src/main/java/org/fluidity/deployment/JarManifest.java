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

import java.util.Collection;
import java.util.List;
import java.util.jar.Attributes;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

/**
 * Interface that a manifest handler must implement. In practice this is a service provider but since no composition
 * functionality is available when implementations of this interface are resolved, we can't use the @ServiceProvider
 * annotation here and neither can we use it on the implementations.
 *
 * @author Tibor Varga
 */
public interface JarManifest {

    /**
     * Tells if the last parameter of the {@link #processManifest(MavenProject, Attributes, List, Collection)} method needs compile time dependencies or
     * run-time ones.
     *
     * @return <code>true</code> if compile time dependencies are required, <code>false</code> otherwise.
     */
    boolean needsCompileDependencies();

    /**
     * Set main manifest attributes to invoke this launcher.
     *
     * @param project      the Maven project to extract metadata from.
     * @param attributes   the main manifest attributes.
     * @param paths        the list of dependency paths pointing to the embedded JAR files relative to the JAR file root.
     * @param dependencies the actual project dependencies.
     *
     * @return <code>true</code> if the receiver and its dependencies should be copied to the root path in the JAR file, <code>false</code> if they should not.
     */
    boolean processManifest(MavenProject project, Attributes attributes, List<String> paths, Collection<Artifact> dependencies);

    /**
     * An interface to delegate execution to an object whose class is loaded possibly by a different class loader than the caller.
     */
    interface Command<R, P> {

        /**
         * Do some computation and return the result.
         *
         * @param object some parameter.
         *
         * @return the result.
         */
        R run(P object);
    }
}

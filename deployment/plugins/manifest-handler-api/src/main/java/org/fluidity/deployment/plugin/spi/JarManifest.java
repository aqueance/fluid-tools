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

package org.fluidity.deployment.plugin.spi;

import java.util.Collection;
import java.util.List;
import java.util.jar.Attributes;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

/**
 * JAR manifest transformation for application wrappers. The <code>org.fluidity.maven:standalone-jar-maven-plugin</code> Maven plugin can be configured with an
 * implementation of this interface to transform the manifest file of the host project.
 * <p/>
 * The implementation must be registered in a JAR service provider file for the above plugin to find the implementation.
 * <p/>
 * The execution environment of the implementation is the execution environment of a Maven plugin.
 *
 * @author Tibor Varga
 */
public interface JarManifest {

    /**
     * Maven profile names for the implementation project to declare dependencies <em>not to include</em>, <em>to include</em>, or <em>to unpack</em>,
     * respectively, in the project artifact being processed.
     */
    enum Packaging {

        /**
         * Dependencies not to include in the project artifact. The dependencies must be listed in a profile with activation (note the <b>exclamation mark</b>
         * before the property name):
         * <pre>
         * &lt;activation>&lt;property>&lt;name><b>!package-exclude</b>&lt;/name>&lt;/property>&lt;/activation>
         * </pre>
         */
        EXCLUDE("package-exclude"),

        /**
         * Dependencies to include as an embedded JAR in the project artifact. The dependencies must be listed in a profile with activation (note the
         * <b>exclamation mark</b> before the property name):
         * <pre>
         * &lt;activation>&lt;property>&lt;name><b>!package-include</b>&lt;/name>&lt;/property>&lt;/activation>
         * </pre>
         */
        INCLUDE("package-include"),

        /**
         * Dependencies to unpack into the root of the project artifact. The dependencies must be listed in a profile with activation (note the <b>exclamation
         * mark</b> before the property name):
         * <pre>
         * &lt;activation>&lt;property>&lt;name><b>!package-unpack</b>&lt;/name>&lt;/property>&lt;/activation>
         * </pre>
         */
        UNPACK("package-unpack");

        public final String profile;

        private Packaging(final String profile) {
            this.profile = profile;
        }
    }

    /**
     * Tells if the last parameter of the {@link #processManifest(MavenProject, Attributes, List, Collection) processManifest()} method needs compile-time
     * (<code>true</code>) or run-time (<code>false</code>) dependencies.
     *
     * @return <code>true</code> if compile-time dependencies are required, <code>false</code> if run-time dependencies are.
     */
    boolean needsCompileDependencies();

    /**
     * Tells how the artifact containing this manifest handler should be handled.
     *
     * @return {@link Packaging#UNPACK} if the receiver's enclosing JAR file should be copied to the root path in the project artifact, {@link
     *         Packaging#INCLUDE} if it should be added to the artifact's dependencies, {@link Packaging#EXCLUDE} if the enclosing JAR file should not be
     *         included or packaged in the project artifact.
     */
    Packaging packaging();

    /**
     * Names the folder under which to package the dependencies requested for inclusion by the manifest handler that are <em>not</em> dependencies of the
     * project artifact. The <code>paths</code> parameter of {@link #processManifest(MavenProject, Attributes, List, Collection) processManifest()} method will
     * have this name used for such dependencies.
     * <p/>
     * The returned name must not contain the '/' character.
     *
     * @return a directory name, or <code>null</code> if the manifest handler requests no extra dependencies for inclusion.
     */
    String dependencyPath();

    /**
     * Transforms the provided main manifest attributes of the given Maven project.
     * <p/>
     * The <code>org.fluidity.maven:standalone-jar-maven-plugin</code> Maven plugin packages the host project's run-time dependencies into the project
     * artifact, including the original project artifact itself, before invoking this method.
     * <p/>
     * The method receives in its <code>paths</code> parameter the paths, relative to the project artifact, of the packaged run-time dependencies, and in its
     * <code>dependencies</code> parameter either the run-time or compile-time dependencies of the host project, depending on what {@link
     * #needsCompileDependencies()} returns.
     *
     * @param project      the Maven project to extract metadata from.
     * @param attributes   the main manifest attributes.
     * @param paths        the list of dependency paths pointing to the embedded JAR files relative to the JAR file root.
     * @param dependencies the actual project dependencies.
     */
    void processManifest(MavenProject project, Attributes attributes, List<String> paths, Collection<Artifact> dependencies);
}

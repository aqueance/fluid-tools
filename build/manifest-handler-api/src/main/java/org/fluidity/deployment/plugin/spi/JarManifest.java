/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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
import java.util.jar.Attributes;

import org.fluidity.deployment.maven.Logger;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * JAR manifest transformation for application wrappers. The <code>org.fluidity.maven:standalone-jar-maven-plugin</code> Maven plugin can be configured with an
 * implementation of this interface to transform the manifest file of the host project.
 * <p>
 * The implementation must be registered in a JAR service provider file for the above plugin to find the implementation.
 * <p>
 * The execution environment of the implementation is the execution environment of a Maven plugin.
 *
 * @author Tibor Varga
 */
@SuppressWarnings("JavadocReference")
public interface JarManifest {

    /**
     * Identifies Fluid Tools as the creator of standalone archives; to be used with the {@link #CREATED_BY} manifest header.
     */
    String FRAMEWORK_ID = "Fluid Tools";

    /**
     * Manifest header that identifies the tool that created the archive.
     */
    String CREATED_BY = "Created-By";

    /**
     * Maven profile names for the implementation project to declare dependencies <em>not to include</em>, <em>to include</em>, or <em>to unpack</em>,
     * respectively, in the project artifact being processed.
     */
    enum Packaging {

        /**
         * Dependencies not to include in the project artifact. The dependencies must be listed in a profile with activation (note the <b>exclamation mark</b>
         * before the property name):
         * <pre>
         * &lt;activation&gt;&lt;property&gt;&lt;name&gt;<b>!package-exclude</b>&lt;/name&gt;&lt;/property&gt;&lt;/activation&gt;
         * </pre>
         */
        EXCLUDE("package-exclude"),

        /**
         * Dependencies to include as an embedded JAR in the project artifact. The dependencies must be listed in a profile with activation (note the
         * <b>exclamation mark</b> before the property name):
         * <pre>
         * &lt;activation&gt;&lt;property&gt;&lt;name&gt;<b>!package-include</b>&lt;/name&gt;&gt;lt;/property&gt;&lt;/activation&gt;
         * </pre>
         * <p>
         * The artifacts will be included in the artifact under the directory specified by a call to {@link JarManifest.Dependencies#include(String)}}.
         */
        INCLUDE("package-include"),

        /**
         * Dependencies to unpack into the root of the project artifact. The dependencies must be listed in a profile with activation (note the <b>exclamation
         * mark</b> before the property name):
         * <pre>
         * &lt;activation&gt;&lt;property&gt;&lt;name&gt;<b>!package-unpack</b>&lt;/name&gt;&lt;/property&gt;&lt;/activation&gt;
         * </pre>
         */
        UNPACK("package-unpack");

        public final String profile;

        Packaging(final String profile) {
            this.profile = profile;
        }
    }

    /**
     * Handles the dependencies of the host project.
     *
     * {@linkplain JarManifest JAR manifest handlers} receive an instance of this.
     *
     * @author Tibor Varga
     */
    interface Dependencies {

        /**
         * Tells if any dependencies are configured to be unpacked to the root of the archive. If the project uses some launcher, the classes that comprise it
         * must be unpacked at the root of the archive. This flag is normally <code>true</code> unless explicitly set to <code>false</code> by the host project.
         *
         * @return <code>true</code> if the archive is configured to have launcher related dependencies unpacked; <code>false</code> otherwise.
         */
        boolean unpacked();

        /**
         * Instructs the standalone JAR plugin to include the dependencies of the host project and set the dependency list as the {@linkplain
         * org.fluidity.foundation.Archives.Nested#dependencies(boolean, String) named custom dependency} attribute in the manifest.
         *
         * @param name      the name with which {@link org.fluidity.foundation.Archives.Nested#dependencies(boolean, String)} can find the dependencies at run
         *                  time.
         * @param delimiter the delimiter to use to separate the dependency entries.
         *
         * @throws MojoExecutionException when an error occurs.
         */
        void attribute(String name, String delimiter) throws MojoExecutionException;

        /**
         * Returns the run-time dependencies of the host project.
         *
         * @return the run-time dependencies of the host project.
         */
        Collection<Artifact> runtime();

        /**
         * Returns the compile time dependencies of the host project.
         *
         * @return the compile time dependencies of the host project.
         */
        Collection<Artifact> compiler();

        /**
         * If the handler contains dependencies to {@linkplain JarManifest.Packaging#INCLUDE included} dependencies, it <i>must</i> invoke this method to
         * specify the name under which the dependencies will be found at run time.
         *
         * @param name the name to pass to {@link org.fluidity.foundation.Archives.Nested#attribute(String)} at run time to find the included dependencies.
         *
         * @throws MojoExecutionException when an error occurs.
         */
        void include(String name) throws MojoExecutionException;
    }

    /**
     * Transforms the provided main manifest attributes of the given Maven project.
     * <p>
     * The <code>org.fluidity.maven:standalone-jar-maven-plugin</code> Maven plugin packages the host project's run-time dependencies into the project
     * artifact, including the original project artifact itself, before invoking this method. If the receiver is the first handler configured for the project,
     * it can call {@link JarManifest.Dependencies#attribute(String, String)} to specify the attribute name under which the packaged dependency list will be
     * available at run time. When not invoked, the default attribute name is {@linkplain org.fluidity.foundation.Archives.Nested#attribute(String)
     * Archives.Nested.attribute(null)}. Secondary handler may not call that method.
     *
     * @param project      the Maven project to extract metadata from.
     * @param attributes   the main manifest attributes.
     * @param policy       the security policy that that manages Java security permissions; never <code>null</code>.
     * @param log          the logger to send messages to.
     * @param dependencies allows the receiver to handle project dependencies.
     *
     * @return the policy processor for the caller to use in place of the one supplied to this method; may be <code>null</code>, in which case the supplied one
     *         will be used by the caller.
     *
     * @throws MojoExecutionException when processing the manifest fails.
     */
    SecurityPolicy processManifest(MavenProject project, Attributes attributes, SecurityPolicy policy, Logger log, Dependencies dependencies) throws MojoExecutionException;
}

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

package org.fluidity.deployment.maven;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Convenience methods to access the Maven dependency resolution mechanism.
 *
 * @author Tibor Vara
 */
public interface DependenciesSupport {

    String POM_TYPE = "pom";
    String JAR_TYPE = "jar";
    String WAR_TYPE = "war";

    String MANIFEST_MAVEN_GROUP_ID = "Maven-Group-Id";
    String MANIFEST_MAVEN_ARTIFACT_ID = "Maven-Artifact-Id";

    /**
     * Returns from the given list the dependency artifact that provides the given dependency class.
     *
     * @param type         a class from a dependency of the root artifact.
     * @param dependencies the dependencies of the root artifact.
     *
     * @return the Maven artifact containing the given class or <code>null</code> if the given list does not include the one looked for.
     *
     * @throws MojoExecutionException when something goes wrong.
     */
    Dependency dependency(Class<?> type, Collection<Dependency> dependencies) throws MojoExecutionException;

    /**
     * Returns from the given list the dependency artifact that provides the given dependency class.
     *
     * @param type         a class from a dependency of the root artifact.
     * @param dependencies the dependencies of the root artifact.
     *
     * @return the Maven artifact containing the given class or <code>null</code> if the given list does not include the one looked for.
     *
     * @throws MojoExecutionException when something goes wrong.
     */
    Artifact artifact(Class<?> type, Collection<Artifact> dependencies) throws MojoExecutionException;

    /**
     * Returns the transitive dependencies of the given artifact.
     *
     * @param session      a Maven component of the respective type.
     * @param repositories a Maven component of the respective type.
     * @param artifact     the artifact to find the transitive dependencies of.
     * @param compile      tells if compile-time dependencies (<code>true</code>) or run-time dependencies (<code>false</code>) should be found.
     * @param optionals    tells if optional dependencies and transitive dependencies thereof (<code>true</code>) should be included or not
     *                     (<code>false</code>).
     * @param exclusions   the list of "artifactId:groupId" specifications of the dependencies of the given artifact not to descend into.
     *
     * @return the list of transitive dependencies of the given dependency, including the dependency.
     *
     * @throws MojoExecutionException when something went wrong.
     */
    Collection<Artifact> dependencyClosure(RepositorySystemSession session,
                                           List<RemoteRepository> repositories,
                                           Artifact artifact,
                                           boolean compile,
                                           boolean optionals,
                                           Collection<Exclusion> exclusions) throws MojoExecutionException;

    /**
     * Converts a dependency to an artifact.
     *
     * @param dependency the dependency.
     *
     * @return the artifact.
     */
    Artifact dependencyArtifact(Dependency dependency);

    /**
     * Returns the transitive compile time dependencies of the given Maven project. Compile time dependencies include those with scope "compile", "provided",
     * and "system.
     *
     * @param session      dependency injected Maven @component
     * @param repositories dependency injected Maven @component
     * @param project      the Maven project to find transitive dependencies of.
     * @param optionals    whether include the optional direct dependencies of the project.
     *
     * @return a collection of Maven artifacts, each representing a dependency of the Maven project.
     *
     * @throws MojoExecutionException when anything goes wrong.
     */
    Collection<Artifact> runtimeDependencies(RepositorySystemSession session, List<RemoteRepository> repositories, MavenProject project, boolean optionals) throws MojoExecutionException;

    /**
     * Returns the transitive run time dependencies of the given Maven project. Run time dependencies include those with scope "compile", "runtime", and
     * "system.
     *
     * @param session      dependency injected Maven @component
     * @param repositories dependency injected Maven @component
     * @param project      the Maven project to find transitive dependencies of.
     * @param optionals    whether include the optional direct dependencies of the project.
     *
     * @return a collection of Maven artifacts, each representing a dependency of the Maven project.
     *
     * @throws MojoExecutionException when anything goes wrong.
     */
    Collection<Artifact> compileDependencies(RepositorySystemSession session, List<RemoteRepository> repositories, MavenProject project, boolean optionals) throws MojoExecutionException;

    /**
     * Save the given artifact. If the project already has a saved artifact and the given computed artifact name is the same as the name of the existing
     * artifact file, then the existing artifact file is deleted and the temporary artifact file is moved in its place. Otherwise, the temporary file is
     * renamed to the computed final name and then added to the given Maven project as an attached artifact.
     *
     * @param project    the Maven project to persist the artifact for.
     * @param file       the temporary file containing the artifact.
     * @param finalName  the Maven final name set for the project artifact.
     * @param classifier the Maven dependency classifier for the artifact.
     * @param packaging  the Maven packaging of the artifact.
     * @param log        the Maven log.
     *
     * @throws MojoExecutionException when anything goes wrong.
     */
    void saveArtifact(MavenProject project, File file, String finalName, String classifier, String packaging, Logger log) throws MojoExecutionException;

    /**
     * Attempts to find the given dependency in a Maven repository.
     *
     * @param session      required by Maven to traverse dependencies.
     * @param repositories the repositories used by the Maven build.
     * @param dependency   the dependency to resolve.
     *
     * @return a collection of artifacts that may or may not be resolved.
     *
     * @throws MojoExecutionException when anything goes wrong.
     */
    Artifact resolve(RepositorySystemSession session, List<RemoteRepository> repositories, Dependency dependency) throws MojoExecutionException;

    /**
     * Attempts to find each artifact in the given list of dependencies in a Maven repository.
     *
     * @param session      required by Maven to traverse dependencies.
     * @param repositories the repositories used by the Maven build.
     * @param dependencies the list of dependencies to resolve.
     *
     * @return a collection of artifacts that may or may not be resolved.
     *
     * @throws MojoExecutionException when anything goes wrong.
     */
    Collection<Artifact> resolve(RepositorySystemSession session, List<RemoteRepository> repositories, List<Dependency> dependencies) throws MojoExecutionException;

    /**
     * Lists the given artifact files in the Maven log.
     *
     * @param artifacts the artifacts to list.
     * @param prefix    the prefix to prepend to each artifact file name.
     * @param log       the logger to emit the list to.
     */
    void list(Collection<Artifact> artifacts, String prefix, Logger log);
}

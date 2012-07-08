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

package org.fluidity.deployment.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fluidity.foundation.Archives;
import org.fluidity.foundation.Utility;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.DependencyCollectionContext;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.collection.DependencySelector;
import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.util.FilterRepositorySystemSession;
import org.sonatype.aether.util.artifact.ArtifactProperties;
import org.sonatype.aether.util.artifact.DefaultArtifactType;
import org.sonatype.aether.util.artifact.JavaScopes;
import org.sonatype.aether.util.graph.PreorderNodeListGenerator;
import org.sonatype.aether.util.graph.selector.StaticDependencySelector;

/**
 * Convenience methods to access the Maven dependency resolution mechanism.
 *
 * @author Tibor Vara
 */
@SuppressWarnings("UnusedDeclaration")
public final class DependenciesSupport extends Utility {

    private DependenciesSupport() { }

    public static final String POM_TYPE = "pom";
    public static final String JAR_TYPE = "jar";
    public static final String WAR_TYPE = "war";

    private static final String MANIFEST_MAVEN_GROUP_ID = "Maven-Group-Id";
    private static final String MANIFEST_MAVEN_ARTIFACT_ID = "Maven-Artifact-Id";

    public static String artifactSpecification(final org.sonatype.aether.artifact.Artifact artifact) {
        return artifact.getGroupId() + ':' + artifact.getArtifactId();
    }

    public static String artifactSpecification(final org.sonatype.aether.graph.Exclusion artifact) {
        return artifact.getGroupId() + ':' + artifact.getArtifactId();
    }

    public static String artifactSpecification(final Artifact artifact) {
        return artifact.getGroupId() + ':' + artifact.getArtifactId();
    }

    public static String artifactSpecification(final Exclusion artifact) {
        return artifact.getGroupId() + ':' + artifact.getArtifactId();
    }

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
    public static Dependency dependency(final Class<?> type, final Collection<Dependency> dependencies) throws MojoExecutionException {
        final String[] spec = projectId(type);

        for (final Dependency artifact : dependencies) {
            if (spec[0].equals(artifact.getGroupId()) && spec[1].equals(artifact.getArtifactId())) {
                return artifact;
            }
        }

        return null;
    }

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
    @SuppressWarnings("UnusedDeclaration")
    public static Artifact artifact(final Class<?> type, final Collection<Artifact> dependencies) throws MojoExecutionException {
        final String[] spec = projectId(type);

        for (final Artifact artifact : dependencies) {
            if (spec[0].equals(artifact.getGroupId()) && spec[1].equals(artifact.getArtifactId())) {
                return artifact;
            }
        }

        return null;
    }

    /**
     * Returns the transitive dependencies of the given artifact.
     *
     * @param system       a Maven component of the respective type.
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
    public static Collection<Artifact> dependencyClosure(final RepositorySystem system,
                                                         final RepositorySystemSession session,
                                                         final List<RemoteRepository> repositories,
                                                         final Artifact artifact,
                                                         final boolean compile,
                                                         final boolean optionals,
                                                         final Collection<Exclusion> exclusions) throws MojoExecutionException {
        return closure(system, session, repositories, new TransitiveDependencySelector(compile, optionals), aetherDependency(artifact, exclusions));
    }

    private static Collection<Artifact> closure(final RepositorySystem system,
                                                final RepositorySystemSession session,
                                                final List<RemoteRepository> repositories,
                                                final DependencySelector selector,
                                                final org.sonatype.aether.graph.Dependency root) throws MojoExecutionException {

        /*
         * https://docs.sonatype.org/display/AETHER/Home
         * https://docs.sonatype.org/display/AETHER/Using+Aether+in+Maven+Plugins
         * https://docs.sonatype.org/display/AETHER/Introduction
         */

        final CollectRequest collectRequest = new CollectRequest(root, new ArrayList<RemoteRepository>(repositories));
        final DependencyFilterSession filter = new DependencyFilterSession(session, new DependencySelector() {
            public boolean selectDependency(final org.sonatype.aether.graph.Dependency dependency) {
                return true;  // accept the root artifact
            }

            public DependencySelector deriveChildSelector(final DependencyCollectionContext dependencyCollectionContext) {
                return selector;
            }
        });

        final DependencyNode node;
        try {
            node = system.collectDependencies(filter, collectRequest).getRoot();
            system.resolveDependencies(filter, new DependencyRequest(node, new DependencyFilter() {
                public boolean accept(final DependencyNode dependency, final List<DependencyNode> path) {
                    return true;
                }
            }));
        } catch (final DependencyCollectionException e) {
            throw new MojoExecutionException(String.format("Finding transitive dependencies of %s", root), e);
        } catch (final DependencyResolutionException e) {
            throw new MojoExecutionException(String.format("Finding transitive dependencies of %s", root), e);
        }

        final PreorderNodeListGenerator generator = new PreorderNodeListGenerator();
        node.accept(generator);

        final Collection<Artifact> dependencies = new HashSet<Artifact>();

        for (final org.sonatype.aether.artifact.Artifact artifact : generator.getArtifacts(true)) {
            final Artifact mavenArtifact = mavenArtifact(artifact);

            if (!mavenArtifact.isResolved()) {
                throw new MojoExecutionException(String.format("Could not resolve %s", mavenArtifact));
            }

            dependencies.add(mavenArtifact);
        }

        for (final Iterator<Artifact> results = dependencies.iterator(); results.hasNext(); ) {
            final Artifact artifact = results.next();

            if (POM_TYPE.equals(artifact.getType())) {
                results.remove();
            }
        }

        return dependencies;
    }

    private static String[] projectId(final Class<?> type) throws MojoExecutionException {
        final String[] spec = Archives.manifestAttributes(type, MANIFEST_MAVEN_GROUP_ID, MANIFEST_MAVEN_ARTIFACT_ID);

        if (spec == null || spec.length != 2 || spec[0] == null || spec[1] == null) {
            throw new MojoExecutionException(String.format("Could not find Maven project for %s", type));
        }

        return spec;
    }

    /**
     * Converts a dependency to an artifact.
     *
     * @param dependency the dependency.
     *
     * @return the artifact.
     */
    public static Artifact dependencyArtifact(final Dependency dependency) {
        return new DefaultArtifact(dependency.getGroupId(),
                                   dependency.getArtifactId(),
                                   dependency.getVersion(),
                                   dependency.getScope(),
                                   dependency.getType(),
                                   dependency.getClassifier(),
                                   new DefaultArtifactHandler(dependency.getType()));
    }

    /**
     * Converts a Maven artifact to an Aether artifact.
     *
     * @param original the Aether artifact.
     *
     * @return the Maven artifact.
     */
    public static Artifact mavenArtifact(final org.sonatype.aether.artifact.Artifact original) {
        final String setClassifier = original.getClassifier();
        final String classifier = (setClassifier == null || setClassifier.length() == 0) ? null : setClassifier;

        final String type = original.getProperty(ArtifactProperties.TYPE, original.getExtension());
        final DefaultArtifactHandler handler = new DefaultArtifactHandler(type);

        handler.setExtension(original.getExtension());
        handler.setLanguage(original.getProperty(ArtifactProperties.LANGUAGE, null));

        final Artifact artifact = new DefaultArtifact(original.getGroupId(), original.getArtifactId(), original.getVersion(), null, type, classifier, handler);

        final File file = original.getFile();
        artifact.setFile(file);
        artifact.setResolved(file != null);

        artifact.setDependencyTrail(Collections.singletonList(artifact.getId()));

        return artifact;
    }

    /**
     * Converts a Aether artifact to a Maven artifact.
     *
     * @param original the Maven artifact.
     *
     * @return the Aether artifact.
     */
    public static org.sonatype.aether.artifact.Artifact aetherArtifact(final Artifact original) {
        final String explicitVersion = original.getVersion();
        final VersionRange versionRange = original.getVersionRange();
        final String version = explicitVersion == null && versionRange != null ? versionRange.toString() : explicitVersion;

        final boolean isSystem = Artifact.SCOPE_SYSTEM.equals(original.getScope());
        final String path = (original.getFile() != null) ? original.getFile().getPath() : "";
        final Map<String, String> properties = isSystem ? Collections.singletonMap(ArtifactProperties.LOCAL_PATH, path) : null;

        final ArtifactHandler handler = original.getArtifactHandler();

        final String extension = handler.getExtension();
        final DefaultArtifactType type = new DefaultArtifactType(original.getType(),
                                                                 extension,
                                                                 handler.getClassifier(),
                                                                 handler.getLanguage(),
                                                                 handler.isAddedToClasspath(),
                                                                 handler.isIncludesDependencies());

        final org.sonatype.aether.artifact.Artifact artifact = new org.sonatype.aether.util.artifact.DefaultArtifact(original.getGroupId(),
                                                                                                                     original.getArtifactId(),
                                                                                                                     original.getClassifier(),
                                                                                                                     extension,
                                                                                                                     version,
                                                                                                                     properties,
                                                                                                                     type);
        return artifact.setFile(original.getFile());
    }

    /**
     * Converts a Maven dependency to an Aether dependency.
     *
     * @param original   the Aether dependency.
     * @param exclusions the list of dependency specifications to exclude.
     *
     * @return the Maven dependency.
     */
    public static org.sonatype.aether.graph.Dependency aetherDependency(final Artifact original, final Collection<Exclusion> exclusions) {
        final List<org.sonatype.aether.graph.Exclusion> exclusionList = new ArrayList<org.sonatype.aether.graph.Exclusion>();

        if (exclusions != null) {
            for (final Exclusion exclusion : exclusions) {
                exclusionList.add(new org.sonatype.aether.graph.Exclusion(exclusion.getGroupId(), exclusion.getArtifactId(), "*", "*"));
            }
        }

        final org.sonatype.aether.artifact.Artifact artifact = aetherArtifact(original);
        return new org.sonatype.aether.graph.Dependency(artifact, original.getScope(), original.isOptional(), exclusions == null ? null : exclusionList);
    }

    /**
     * Returns the transitive compile time dependencies of the given Maven project. Compile time dependencies include those with scope "compile", "provided",
     * and "system.
     *
     * @param system       dependency injected Maven @component
     * @param session      dependency injected Maven @component
     * @param repositories dependency injected Maven @component
     * @param project      the Maven project to find transitive dependencies of.
     * @param optionals    whether include the optional direct dependencies of the project.
     *
     * @return a collection of Maven artifacts, each representing a dependency of the Maven project.
     *
     * @throws MojoExecutionException when anything goes wrong.
     */
    public static Collection<Artifact> runtimeDependencies(final RepositorySystem system,
                                                           final RepositorySystemSession session,
                                                           final List<RemoteRepository> repositories,
                                                           final MavenProject project,
                                                           final boolean optionals) throws MojoExecutionException {
        return dependencyClosure(system, session, repositories, project.getArtifact(), false, optionals, null);
    }

    /**
     * Returns the transitive run time dependencies of the given Maven project. Run time dependencies include those with scope "compile", "runtime", and
     * "system.
     *
     * @param system       dependency injected Maven @component
     * @param session      dependency injected Maven @component
     * @param repositories dependency injected Maven @component
     * @param project      the Maven project to find transitive dependencies of.
     * @param optionals    whether include the optional direct dependencies of the project.
     *
     * @return a collection of Maven artifacts, each representing a dependency of the Maven project.
     *
     * @throws MojoExecutionException when anything goes wrong.
     */
    public static Collection<Artifact> compileDependencies(final RepositorySystem system,
                                                           final RepositorySystemSession session,
                                                           final List<RemoteRepository> repositories,
                                                           final MavenProject project,
                                                           final boolean optionals) throws MojoExecutionException {
        return dependencyClosure(system, session, repositories, project.getArtifact(), true, optionals, null);
    }

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
    public static void saveArtifact(final MavenProject project,
                                    final File file,
                                    final String finalName,
                                    final String classifier,
                                    final String packaging,
                                    final Log log) throws MojoExecutionException {
        final boolean classified = classifier == null || classifier.isEmpty();
        final String outputName = classified ? String.format("%s.%s", finalName, packaging) : String.format("%s-%s.%s", finalName, classifier, packaging);

        final Artifact artifact = project.getArtifact();
        assert artifact != null;

        final File artifactFile = artifact.getFile();

        if (artifactFile != null && artifactFile.getAbsolutePath().equals(outputName)) {
            log.info(String.format("Replacing %s: %s", packaging, artifactFile.getAbsolutePath()));

            if (!artifactFile.delete()) {
                throw new MojoExecutionException(String.format("Could not delete %s", artifactFile));
            }

            if (!file.renameTo(artifactFile)) {
                throw new MojoExecutionException(String.format("Could not create %s", artifactFile));
            }
        } else {
            final File attachmentFile = new File(outputName);

            if (attachmentFile.exists() && !attachmentFile.delete()) {
                throw new MojoExecutionException(String.format("Could not delete %s", attachmentFile));
            }

            if (!file.renameTo(attachmentFile)) {
                throw new MojoExecutionException(String.format("Could not create %s", attachmentFile));
            }

            log.info(String.format("Saving %s: %s", packaging, attachmentFile.getAbsolutePath()));

            final DefaultArtifact attachment = new DefaultArtifact(project.getGroupId(),
                                                                   project.getArtifactId(),
                                                                   project.getVersion(),
                                                                   artifact.getScope(),
                                                                   packaging,
                                                                   classifier,
                                                                   artifact.getArtifactHandler());
            attachment.setFile(attachmentFile);

            project.addAttachedArtifact(attachment);
        }
    }

    /**
     * @author Tibor Varga
     */
    private static class DependencyFilterSession extends FilterRepositorySystemSession {

        private final DependencySelector selector;

        public DependencyFilterSession(final RepositorySystemSession parent, final DependencySelector selector) {
            super(parent);
            this.selector = selector;
        }

        @Override
        public DependencySelector getDependencySelector() {
            return selector;
        }
    }

    /**
     * @author Tibor Varga
     */
    private static class TransitiveDependencySelector implements DependencySelector {

        private static final Set<String> RUNTIME_SCOPES = new HashSet<String>(Arrays.asList(JavaScopes.COMPILE, JavaScopes.RUNTIME, JavaScopes.SYSTEM));
        private static final Set<String> COMPILE_SCOPES = new HashSet<String>(Arrays.asList(JavaScopes.COMPILE, JavaScopes.PROVIDED, JavaScopes.SYSTEM));
        private static final Set<String> INTRANSITIVE_SCOPES = new HashSet<String>(Arrays.asList(JavaScopes.PROVIDED));

        private final boolean compile;
        private final boolean optionals;
        private final Set<String> excluded;

        public TransitiveDependencySelector(final boolean compile, boolean optionals, final String... exclusions) {
            this.compile = compile;
            this.optionals = optionals;
            this.excluded = new HashSet<String>(Arrays.asList(exclusions));
        }

        public boolean selectDependency(final org.sonatype.aether.graph.Dependency dependency) {
            return !excluded.contains(artifactSpecification(dependency.getArtifact()))
                   && (compile ? COMPILE_SCOPES : RUNTIME_SCOPES).contains(dependency.getScope())
                   && (optionals || !dependency.isOptional());
        }

        public DependencySelector deriveChildSelector(final DependencyCollectionContext context) {
            final org.sonatype.aether.graph.Dependency dependency = context.getDependency();

            if (dependency == null || INTRANSITIVE_SCOPES.contains(dependency.getScope()) || (!compile && dependency.isOptional())) {
                return new StaticDependencySelector(false);
            } else {
                final Collection<org.sonatype.aether.graph.Exclusion> exclusions = dependency.getExclusions();

                if (optionals || !exclusions.isEmpty()) {
                    final Set<String> excluded = new HashSet<String>(exclusions.size() + this.excluded.size());

                    excluded.addAll(this.excluded);

                    for (final org.sonatype.aether.graph.Exclusion exclusion : exclusions) {
                        excluded.add(artifactSpecification(exclusion));
                    }

                    // next level optionals and those excluded will not be selected
                    return new TransitiveDependencySelector(compile, false, excluded.toArray(new String[excluded.size()]));
                } else {
                    return this;
                }
            }
        }
    }
}

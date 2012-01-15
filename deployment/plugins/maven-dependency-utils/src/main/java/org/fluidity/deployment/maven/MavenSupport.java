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

/**
 * Convenience methods to access the Maven dependency resolution mechanism.
 *
 * @author Tibor Vara
 */
public final class MavenSupport extends Utility {

    private MavenSupport() { }

    public static final String JAR_TYPE = "jar";
    public static final String WAR_TYPE = "war";
    public static final String META_INF = "META-INF/";
    public static final String WEB_INF = "WEB-INF/";

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
     * Returns the transitive dependencies of the given dependency of the given root artifact.
     *
     * @param system             a Maven component of the respective type.
     * @param session            a Maven component of the respective type.
     * @param remoteRepositories a Maven component of the respective type.
     * @param dependency         a class from a dependency of the root artifact.
     * @param root               the root artifact.
     * @param dependencies       the dependencies of the root artifact.
     * @param types              the list of project types allowed in the result; may be <code>null</code>.
     *
     * @return the list of transitive dependencies of the given dependency, including the dependency itself.
     *
     * @throws MojoExecutionException when something went wrong.
     */
    public static Collection<Artifact> transitiveDependencies(final RepositorySystem system,
                                                              final RepositorySystemSession session,
                                                              final List<RemoteRepository> remoteRepositories,
                                                              final Class<?> dependency,
                                                              final Artifact root,
                                                              final List<Dependency> dependencies,
                                                              final Set<String> types) throws MojoExecutionException {
        final String[] spec = Archives.manifestAttributes(dependency, MANIFEST_MAVEN_GROUP_ID, MANIFEST_MAVEN_ARTIFACT_ID);

        if (spec == null || spec.length != 2 || spec[0] == null || spec[1] == null) {
            throw new MojoExecutionException(String.format("Could not find Maven project for %s", dependency));
        }

        // level 0
        final TransitiveDependencySelector dependencySelector = new TransitiveDependencySelector(false, false);
        final DependencySelector selector = new DependencySelector() {

            // level 0: root
            public boolean selectDependency(final org.sonatype.aether.graph.Dependency dependency) {
                throw new UnsupportedOperationException();
            }

            // level 1
            public DependencySelector deriveChildSelector(final DependencyCollectionContext dependencyCollectionContext) {
                return new DependencySelector() {

                    // level 1: the dependency we're after
                    public boolean selectDependency(final org.sonatype.aether.graph.Dependency dependency) {
                        final org.sonatype.aether.artifact.Artifact artifact = dependency.getArtifact();
                        return spec[0].equals(artifact.getGroupId()) && spec[1].equals(artifact.getArtifactId());
                    }

                    // level 2
                    public DependencySelector deriveChildSelector(final DependencyCollectionContext context) {
                        return dependencySelector;
                    }
                };
            }
        };

        final Collection<Artifact> artifacts = transitiveDependencies(system, remoteRepositories, root, session, selector, types);
        artifacts.remove(root);

        if (dependencies != null) {
            for (final Dependency artifact : dependencies) {
                artifacts.addAll(transitiveDependencies(system, remoteRepositories, dependencyArtifact(artifact), session, dependencySelector, types));
            }
        }

        if (types != null) {
            for (final Iterator<Artifact> results = artifacts.iterator(); results.hasNext();) {
                final Artifact artifact = results.next();

                if (!types.contains(artifact.getType())) {
                    results.remove();
                }
            }
        }

        return artifacts;
    }

    /**
     * Returns the transitive dependencies of the given root artifact.
     *
     * @param system             a Maven component of the respective type.
     * @param session            a Maven component of the respective type.
     * @param remoteRepositories a Maven component of the respective type.
     * @param root               the root artifact.
     * @param compile            should calculate compile time dependencies (<code>true</code>) or run-time dependencies (<code>false</code>).
     * @param optionals          should include optional dependencies and transitive dependencies thereof (value <code>true</code>) or not (value
     *                           <code>false</code>).
     * @param types              the optional list of accepted dependency types; may be <code>null</code>.
     * @param exclusions         the list of "artifactId:groupId" specifications not to include in the result.
     *
     * @return the list of transitive dependencies of the given dependency, including the dependency.
     *
     * @throws MojoExecutionException when something went wrong.
     */
    public static Collection<Artifact> transitiveDependencies(final RepositorySystem system,
                                                              final RepositorySystemSession session,
                                                              final List<RemoteRepository> remoteRepositories,
                                                              final Artifact root,
                                                              final boolean compile,
                                                              final boolean optionals,
                                                              final Set<String> types,
                                                              final String... exclusions) throws MojoExecutionException {
        return transitiveDependencies(system, remoteRepositories, root, session, new TransitiveDependencySelector(compile, optionals, exclusions), types);
    }

    private static Collection<Artifact> transitiveDependencies(final RepositorySystem system,
                                                               final List<RemoteRepository> remoteRepositories,
                                                               final Artifact root,
                                                               final RepositorySystemSession session,
                                                               final DependencySelector selector,
                                                               final Set<String> types) throws MojoExecutionException {

        /*
        * https://docs.sonatype.org/display/AETHER/Home
        * https://docs.sonatype.org/display/AETHER/Using+Aether+in+Maven+Plugins
        * https://docs.sonatype.org/display/AETHER/Introduction
        */

        final CollectRequest collectRequest = new CollectRequest();

        collectRequest.setRoot(aetherDependency(root, null));

        for (final RemoteRepository repository : remoteRepositories) {
            collectRequest.addRepository(repository);
        }

        final DependencyFilterSession filter = new DependencyFilterSession(session, selector);

        final DependencyNode node;
        try {
            node = system.collectDependencies(filter, collectRequest).getRoot();
            system.resolveDependencies(filter, new DependencyRequest(node, new DependencyFilter() {
                public boolean accept(final DependencyNode dependency, final List<DependencyNode> path) {
                    return true;
                }
            }));
        } catch (final DependencyCollectionException e) {
            throw new MojoExecutionException("Finding transitive dependencies of " + root, e);
        } catch (final DependencyResolutionException e) {
            throw new MojoExecutionException("Finding transitive dependencies of " + root, e);
        }

        final PreorderNodeListGenerator generator = new PreorderNodeListGenerator();
        node.accept(generator);

        final Collection<Artifact> dependencies = new HashSet<Artifact>();

        for (final org.sonatype.aether.artifact.Artifact artifact : generator.getArtifacts(true)) {
            final Artifact mavenArtifact = mavenArtifact(artifact);

            if (!mavenArtifact.isResolved()) {
                throw new MojoExecutionException(String.format("Could not resolve %s", mavenArtifact));
            }

            if (types == null || types.contains(mavenArtifact.getType())) {
                dependencies.add(mavenArtifact);
            }
        }

        return dependencies;
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

    public static Collection<Artifact> runtimeDependencies(final RepositorySystem system,
                                                           final RepositorySystemSession session,
                                                           final List<RemoteRepository> repositories,
                                                           final MavenProject project,
                                                           final Set<String> types) throws MojoExecutionException {
        return transitiveDependencies(system, session, repositories, project.getArtifact(), false, false, types);
    }

    public static Collection<Artifact> compileDependencies(final RepositorySystem system,
                                                           final RepositorySystemSession session,
                                                           final List<RemoteRepository> repositories,
                                                           final MavenProject project,
                                                           final Set<String> types) throws MojoExecutionException {
        return transitiveDependencies(system, session, repositories, project.getArtifact(), true, true, types);
    }

    public static void saveArtifact(final MavenProject project, final File file, final String finalName, final String classifier, final String packaging)
            throws MojoExecutionException {
        final Artifact artifact = project.getArtifact();
        final File artifactFile = artifact.getFile();

        if (classifier == null || classifier.length() == 0) {
            if (!artifactFile.delete()) {
                throw new MojoExecutionException(String.format("Could not delete %s", artifactFile));
            }

            if (!file.renameTo(artifactFile)) {
                throw new MojoExecutionException(String.format("Could not create %s", artifactFile));
            }
        } else {
            final File attachmentFile = new File(String.format("%s-%s.%s", finalName, classifier, packaging));

            if (attachmentFile.exists() && !attachmentFile.delete()) {
                throw new MojoExecutionException(String.format("Could not delete %s", attachmentFile));
            }

            if (!file.renameTo(attachmentFile)) {
                throw new MojoExecutionException(String.format("Could not create %s", attachmentFile));
            }

            final DefaultArtifact attachment = new DefaultArtifact(artifact.getGroupId(),
                                                                   artifact.getArtifactId(),
                                                                   artifact.getVersion(),
                                                                   artifact.getScope(),
                                                                   artifact.getType(),
                                                                   classifier,
                                                                   artifact.getArtifactHandler());
            attachment.setFile(attachmentFile);

            project.addAttachedArtifact(attachment);
        }
    }

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

    private static class TransitiveDependencySelector implements DependencySelector {

        private static final Set<String> RUNTIME_SCOPES = new HashSet<String>(Arrays.asList(JavaScopes.COMPILE, JavaScopes.RUNTIME, JavaScopes.SYSTEM));
        private static final Set<String> COMPILE_SCOPES = new HashSet<String>(Arrays.asList(JavaScopes.COMPILE, JavaScopes.PROVIDED, JavaScopes.SYSTEM));

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
            final Collection<org.sonatype.aether.graph.Exclusion> exclusions = context.getDependency().getExclusions();
            final Set<String> excluded = new HashSet<String>(exclusions.size() + this.excluded.size());

            excluded.addAll(this.excluded);

            for (final org.sonatype.aether.graph.Exclusion exclusion : exclusions) {
                excluded.add(artifactSpecification(exclusion));
            }

            // second level optionals and those excluded will not be selected
            return optionals || !exclusions.isEmpty() ? new TransitiveDependencySelector(compile, false, excluded.toArray(new String[excluded.size()])) : this;
        }
    }
}

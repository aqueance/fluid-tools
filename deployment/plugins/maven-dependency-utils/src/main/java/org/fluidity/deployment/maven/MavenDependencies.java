/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.fluidity.deployment.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fluidity.foundation.JarStreams;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.plugin.MojoExecutionException;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.DependencyCollectionContext;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.collection.DependencySelector;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactResolutionException;
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
public final class MavenDependencies {

    private static final String MANIFEST_MAVEN_GROUP_ID = "Maven-Group-Id";
    private static final String MANIFEST_MAVEN_ARTIFACT_ID = "Maven-Artifact-Id";

    private static final String[] PROJECT_ID = JarStreams.manifestAttributes(MavenDependencies.class, MANIFEST_MAVEN_GROUP_ID, MANIFEST_MAVEN_ARTIFACT_ID);

    public static final String GROUP_ID = PROJECT_ID[0];
    public static final String ARTIFACT_ID = PROJECT_ID[1];

    private MavenDependencies() {
        throw new UnsupportedOperationException("No instance allowed");
    }

    public static Collection<Artifact> transitiveDependencies(final RepositorySystem system,
                                                              final RepositorySystemSession session,
                                                              final List<RemoteRepository> remoteRepositories,
                                                              final Artifact root,
                                                              final boolean optionals,
                                                              final String... exclusions) throws MojoExecutionException {

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

        // we must override the getDependencySelector() method to add our own selector
        final FilterRepositorySystemSession filteringSession = new DependencyFilterSession(session, optionals, exclusions);

        final DependencyNode node;
        try {
            node = system.collectDependencies(filteringSession, collectRequest).getRoot();
            system.resolveDependencies(filteringSession, node, null);
        } catch (final DependencyCollectionException e) {
            throw new MojoExecutionException("Finding transitive dependencies of " + root, e);
        } catch (final ArtifactResolutionException e) {
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

            dependencies.add(mavenArtifact);
        }

        return dependencies;
    }

    public static Artifact dependencyArtifact(final Dependency dependency) {
        return new DefaultArtifact(dependency.getGroupId(),
                                   dependency.getArtifactId(),
                                   dependency.getVersion(),
                                   dependency.getScope(),
                                   dependency.getType(),
                                   dependency.getClassifier(),
                                   new DefaultArtifactHandler(dependency.getType()));
    }

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

    public static class DependencyFilterSession extends FilterRepositorySystemSession {

        private final DependencySelector selector;

        public DependencyFilterSession(final RepositorySystemSession parent, final boolean optionals, final String... exclusions) {
            super(parent);

            final Set<String> excluded = new HashSet<String>(Arrays.asList(exclusions));
            final Set<String> scopes = new HashSet<String>(Arrays.asList(JavaScopes.COMPILE, JavaScopes.RUNTIME));

            // filters out optional dependencies unless explicitly requested, in addition to dependencies not packaged in thw war
            selector = new DependencySelector() {
                public boolean selectDependency(final org.sonatype.aether.graph.Dependency dependency) {
                    final org.sonatype.aether.artifact.Artifact artifact = dependency.getArtifact();
                    final String spec = artifact.getGroupId() + ':' + artifact.getArtifactId();
                    return !excluded.contains(spec) && scopes.contains(dependency.getScope()) && (optionals || !dependency.isOptional());
                }

                public DependencySelector deriveChildSelector(final DependencyCollectionContext context) {
                    return this;
                }
            };
        }

        @Override
        public DependencySelector getDependencySelector() {
            return selector;
        }
    }
}

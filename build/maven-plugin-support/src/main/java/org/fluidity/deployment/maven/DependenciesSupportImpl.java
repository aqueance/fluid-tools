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

package org.fluidity.deployment.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.fluidity.foundation.Archives;
import org.fluidity.foundation.Deferred;
import org.fluidity.foundation.Exceptions;
import org.fluidity.foundation.Lists;
import org.fluidity.foundation.Proxies;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.aether.AbstractForwardingRepositorySystemSession;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.selector.StaticDependencySelector;

/**
 * @author Tibor Vara
 */
@Component(role = DependenciesSupport.class)
final class DependenciesSupportImpl implements DependenciesSupport {

    private static final StaticDependencySelector NO_SELECTOR = new StaticDependencySelector(false);

    private static String artifactSpecification(final org.eclipse.aether.artifact.Artifact artifact) {
        return artifact.getGroupId() + ':' + artifact.getArtifactId();
    }

    private static String artifactSpecification(final org.eclipse.aether.graph.Exclusion artifact) {
        return artifact.getGroupId() + ':' + artifact.getArtifactId();
    }

    /**
     * The entry point to Aether, i.e. the component doing all the work.
     */
    private final RepositorySystem system;

    @Inject
    public DependenciesSupportImpl(final RepositorySystem system) {
        this.system = system;
    }

    @Override
    public Dependency dependency(final Class<?> type, final Collection<Dependency> dependencies) throws MojoExecutionException {
        final String[] spec = projectId(type);

        for (final Dependency artifact : dependencies) {
            if (Objects.equals(spec[0], artifact.getGroupId()) && Objects.equals(spec[1], artifact.getArtifactId())) {
                return artifact;
            }
        }

        return null;
    }

    @Override
    public Artifact artifact(final Class<?> type, final Collection<Artifact> dependencies) throws MojoExecutionException {
        final String[] spec = projectId(type);

        for (final Artifact artifact : dependencies) {
            if (Objects.equals(spec[0], artifact.getGroupId()) && Objects.equals(spec[1], artifact.getArtifactId())) {
                return artifact;
            }
        }

        return null;
    }

    @Override
    public Collection<Artifact> dependencyClosure(final RepositorySystemSession session,
                                                  final List<RemoteRepository> repositories,
                                                  final Artifact artifact,
                                                  final boolean compile,
                                                  final boolean optionals,
                                                  final Collection<Exclusion> exclusions) throws MojoExecutionException {
        artifact.setScope(JavaScopes.COMPILE);
        return closure(session, repositories, new TransitiveDependencySelector(compile, optionals), !compile, artifact, exclusions);
    }

    private Collection<Artifact> closure(final RepositorySystemSession session,
                                         final List<RemoteRepository> repositories,
                                         final DependencySelector selector,
                                         final boolean runtime,
                                         final Artifact artifact,
                                         final Collection<Exclusion> exclusions) throws MojoExecutionException {
        final org.eclipse.aether.graph.Dependency root = aetherDependency(artifact, exclusions);

        // Guidelines for a possible upgrade path
        // http://labs.bsb.com/2012/10/using-aether-to-resolve-dependencies-in-a-maven-plugins/
        // http://git.eclipse.org/c/aether/aether-demo.git/tree/
        // Resolve one artifacto: http://git.eclipse.org/c/aether/aether-demo.git/tree/aether-demo-maven-plugin/src/main/java/org/eclipse/aether/examples/maven/ResolveArtifactMojo.java
        // Transitive dependencies: http://git.eclipse.org/c/aether/aether-demo.git/tree/aether-demo-snippets/src/main/java/org/eclipse/aether/examples/ResolveTransitiveDependencies.java

        /*
         * http://wiki.eclipse.org/Aether
         * http://wiki.eclipse.org/Aether/Using_Aether_in_Maven_Plugins
         * http://wiki.eclipse.org/Aether/Dependency_Graph
         * http://wiki.eclipse.org/Aether/Transitive_Dependency_Resolution
         * http://git.eclipse.org/c/aether/aether-demo.git/tree/aether-demo-snippets/src/main/java/org/eclipse/aether/examples
         */

        return Exceptions.wrap(Deferred.label("Finding transitive dependencies of %s", root), MojoExecutionException.class, () -> {
            final DependencyFilterSession filter = new DependencyFilterSession(patch(session), new DependencySelector() {
                public boolean selectDependency(final org.eclipse.aether.graph.Dependency dependency) {
                    return true;  // always accept the root artifact
                }

                public DependencySelector deriveChildSelector(final DependencyCollectionContext context) {
                    return root.isOptional() ? NO_SELECTOR : selector;
                }
            });

            final CollectRequest request = new CollectRequest(root, Collections.unmodifiableList(repositories));

            final CollectResult result = system.collectDependencies(filter, request);
            if (!result.getExceptions().isEmpty()) throw result.getExceptions().get(0);

            final DependencyResult resolved = system.resolveDependencies(filter, new DependencyRequest(result.getRoot(), null));
            if (!resolved.getCollectExceptions().isEmpty()) throw resolved.getCollectExceptions().get(0);

            final Collection<Artifact> dependencies = new HashSet<>();

            result.getRoot().accept(new UniqueArtifacts(dependency -> {
                if (!dependency.isResolved()) {
                    throw new IllegalArgumentException(String.format("Could not resolve %s", dependency));
                } else if (!(Objects.equals(dependency.getType(), POM_TYPE) || (runtime && Objects.equals(dependency.getScope(), JavaScopes.PROVIDED)))) {
                    dependencies.add(dependency);
                }
            }));

            return dependencies;
        });
    }

    private static String[] projectId(final Class<?> type) throws MojoExecutionException {
        try {
            final String[] spec = Archives.attributes(Archives.containing(type), true, MANIFEST_MAVEN_GROUP_ID, MANIFEST_MAVEN_ARTIFACT_ID);

            if (spec[0] == null || spec[1] == null) {
                throw new MojoExecutionException(String.format("Could not find Maven project for %s", type));
            }

            return spec;
        } catch (final IOException e) {
            throw new MojoExecutionException("Reading JAR manifest", e);
        }
    }

    @Override
    public Artifact dependencyArtifact(final Dependency dependency) {
        final DefaultArtifact artifact = new DefaultArtifact(dependency.getGroupId(),
                                                             dependency.getArtifactId(),
                                                             dependency.getVersion(),
                                                             dependency.getScope(),
                                                             dependency.getType(),
                                                             dependency.getClassifier(),
                                                             new DefaultArtifactHandler(dependency.getType()));
        artifact.setOptional(Boolean.valueOf(dependency.getOptional()));
        return artifact;
    }

    /**
     * Converts an Aether artifact to a Maven artifact.
     *
     * @param original the Aether artifact.
     *
     * @return the Maven artifact.
     */
    private static Artifact mavenArtifact(final org.eclipse.aether.artifact.Artifact original, final String scope) {
        final String setClassifier = original.getClassifier();
        final String classifier = (setClassifier == null || setClassifier.isEmpty()) ? null : setClassifier;

        final String type = original.getProperty(ArtifactProperties.TYPE, original.getExtension());
        final DefaultArtifactHandler handler = new DefaultArtifactHandler(type);

        handler.setExtension(original.getExtension());
        handler.setLanguage(original.getProperty(ArtifactProperties.LANGUAGE, null));

        final Artifact artifact = new DefaultArtifact(original.getGroupId(), original.getArtifactId(), original.getVersion(), null, type, classifier, handler);

        final File file = original.getFile();
        artifact.setFile(file);
        artifact.setResolved(file != null);

        artifact.setScope(scope);
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
    private static org.eclipse.aether.artifact.Artifact aetherArtifact(final Artifact original) {
        final String explicitVersion = original.getVersion();
        final VersionRange versionRange = original.getVersionRange();
        final String version = explicitVersion == null && versionRange != null ? versionRange.toString() : explicitVersion;

        final boolean isSystem = Objects.equals(original.getScope(), Artifact.SCOPE_SYSTEM);
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

        final org.eclipse.aether.artifact.Artifact artifact = new org.eclipse.aether.artifact.DefaultArtifact(original.getGroupId(),
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
    private static org.eclipse.aether.graph.Dependency aetherDependency(final Artifact original, final Collection<Exclusion> exclusions) {
        final List<org.eclipse.aether.graph.Exclusion> exclusionList = new ArrayList<>();

        if (exclusions != null) {
            for (final Exclusion exclusion : exclusions) {
                exclusionList.add(new org.eclipse.aether.graph.Exclusion(exclusion.getGroupId(), exclusion.getArtifactId(), "*", "*"));
            }
        }

        return new org.eclipse.aether.graph.Dependency(aetherArtifact(original), original.getScope(), original.isOptional(), exclusions == null ? null : exclusionList);
    }

    @Override
    public Collection<Artifact> runtimeDependencies(final RepositorySystemSession session,
                                                    final List<RemoteRepository> repositories,
                                                    final MavenProject project,
                                                    final boolean optionals) throws MojoExecutionException {
        return dependencyClosure(session, repositories, project.getArtifact(), false, optionals, null);
    }

    @Override
    public Collection<Artifact> compileDependencies(final RepositorySystemSession session,
                                                    final List<RemoteRepository> repositories,
                                                    final MavenProject project,
                                                    final boolean optionals) throws MojoExecutionException {
        return dependencyClosure(session, repositories, project.getArtifact(), true, optionals, null);
    }

    @Override
    public void saveArtifact(final MavenProject project,
                             final File file,
                             final String finalName,
                             final String classifier,
                             final String packaging,
                             final Logger log) throws MojoExecutionException {
        final boolean unclassified = classifier == null || classifier.isEmpty();
        final String outputName = unclassified ? String.format("%s.%s", finalName, packaging) : String.format("%s-%s.%s", finalName, classifier, packaging);

        final File outputFile = new File(outputName);
        final String outputPath = outputFile.getAbsolutePath();

        final Artifact artifact = project.getArtifact();
        assert artifact != null;

        final File artifactFile = artifact.getFile();

        if (artifactFile != null && Objects.equals(artifactFile.getAbsolutePath(), outputPath)) {
            log.info(String.format("Replacing %s: %s", packaging, artifactFile.getAbsolutePath()));

            if (!artifactFile.delete()) {
                throw new MojoExecutionException(String.format("Could not delete %s", artifactFile));
            }

            if (!file.renameTo(artifactFile)) {
                throw new MojoExecutionException(String.format("Could not create %s", artifactFile));
            }
        } else {
            boolean replacing = false;

            for (final Artifact attached : project.getAttachedArtifacts()) {
                if (Objects.equals(attached.getFile().getAbsolutePath(), outputPath)) {
                    replacing = true;
                    break;
                }
            }

            log.info(String.format("%s %s: %s", replacing ? "Replacing" : "Saving", packaging, outputPath));

            if (outputFile.exists() && !outputFile.delete()) {
                throw new MojoExecutionException(String.format("Could not delete %s", outputFile));
            }

            if (!file.renameTo(outputFile)) {
                throw new MojoExecutionException(String.format("Could not create %s", outputFile));
            }

            if (!replacing) {
                final DefaultArtifact attachment = new DefaultArtifact(project.getGroupId(),
                                                                       project.getArtifactId(),
                                                                       project.getVersion(),
                                                                       artifact.getScope(),
                                                                       packaging,
                                                                       classifier,
                                                                       artifact.getArtifactHandler());
                attachment.setFile(outputFile);

                project.addAttachedArtifact(attachment);
            }
        }
    }

    @Override
    public Artifact resolve(final RepositorySystemSession session, final List<RemoteRepository> repositories, final Dependency dependency) throws MojoExecutionException {
        final Collection<Artifact> artifacts = closure(session, repositories, NO_SELECTOR, false, dependencyArtifact(dependency), dependency.getExclusions());

        assert artifacts.size() == 1 : artifacts;
        return artifacts.iterator().next();
    }

    @Override
    public Collection<Artifact> resolve(final RepositorySystemSession session, final List<RemoteRepository> repositories, final List<Dependency> dependencies) throws MojoExecutionException {
        final DependencySelector selector = new TransitiveDependencySelector(false, false);
        final Collection<Artifact> artifacts = new LinkedHashSet<>();

        for (final Dependency dependency : dependencies) {
            artifacts.addAll(closure(session, repositories, selector, false, dependencyArtifact(dependency), dependency.getExclusions()));
        }

        return artifacts;
    }

    @Override
    public void list(final Collection<Artifact> artifacts, final String prefix, final Logger log) {
        assert log.active();

        artifacts.stream()
                .map(Artifact::getFile)
                .map(File::getName)
                .sorted()
                .map(prefix::concat)
                .forEach(log::detail);
    }

    /*
     * This class applies a hack to undo the hack introduced to Maven to speed up POM processing:
     * https://github.com/apache/maven/commit/be3fb200326208ca4b8c41ebf16d5ae6b8049792#diff-1b94ec0f0a29b1dc6aae1f6ad855484bR307
     */
    // TODO: see if that hack has been removed: https://issues.apache.org/jira/browse/MNG-5899
    private static RepositorySystemSession patch(final RepositorySystemSession original) {
        final DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(original);

        // We simply provide a general workspace reader that does not cache parsed POMs,
        // thereby allowing property changes to affect profile activations.

        final WorkspaceReader reader = session.getWorkspaceReader();
        session.setWorkspaceReader(Proxies.create(WorkspaceReader.class, (proxy, method, args) -> method.invoke(reader, args)));

        return session;
    }

    /**
     * @author Tibor Varga
     */
    private static class DependencyFilterSession extends AbstractForwardingRepositorySystemSession {

        private final DependencySelector selector;
        private final RepositorySystemSession parent;

        DependencyFilterSession(final RepositorySystemSession parent, final DependencySelector selector) {
            this.parent = parent;
            this.selector = selector;
        }

        @Override
        protected RepositorySystemSession getSession() {
            return parent;
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

        private static final Set<String> RUNTIME_SCOPES = new HashSet<>(Arrays.asList(JavaScopes.COMPILE, JavaScopes.PROVIDED, JavaScopes.RUNTIME, JavaScopes.SYSTEM));
        private static final Set<String> COMPILE_SCOPES = new HashSet<>(Arrays.asList(JavaScopes.COMPILE, JavaScopes.PROVIDED, JavaScopes.SYSTEM));

        private final boolean compile;
        private final boolean optionals;

        private final Set<String> excludedArtifacts;
        private final Set<String> acceptedScopes;

        TransitiveDependencySelector(final boolean compile, boolean optionals, final String... exclusions) {
            this.compile = compile;
            this.optionals = optionals;
            this.excludedArtifacts = new HashSet<>(Arrays.asList(exclusions));
            this.acceptedScopes = compile ? COMPILE_SCOPES : RUNTIME_SCOPES;
        }

        public boolean selectDependency(final org.eclipse.aether.graph.Dependency dependency) {
            return dependency != null
                   && !excludedArtifacts.contains(artifactSpecification(dependency.getArtifact()))
                   && acceptedScopes.contains(dependency.getScope())
                   && (optionals || !dependency.isOptional());
        }

        public DependencySelector deriveChildSelector(final DependencyCollectionContext context) {
            final org.eclipse.aether.graph.Dependency dependency = context.getDependency();

            if (dependency == null || Objects.equals(dependency.getScope(), JavaScopes.PROVIDED) || (!compile && dependency.isOptional())) {
                return NO_SELECTOR;
            } else {
                final Collection<org.eclipse.aether.graph.Exclusion> exclusions = dependency.getExclusions();

                if (optionals || !exclusions.isEmpty()) {
                    final Set<String> excluded = new HashSet<>(exclusions.size() + excludedArtifacts.size());

                    excluded.addAll(excludedArtifacts);

                    for (final org.eclipse.aether.graph.Exclusion exclusion : exclusions) {
                        excluded.add(artifactSpecification(exclusion));
                    }

                    // next level optionals and those excluded by the current dependency will not be selected during descent
                    return new TransitiveDependencySelector(compile, false, Lists.asArray(String.class, excluded));
                } else {
                    return this;
                }
            }
        }
    }

    /**
     * Streams a single instance of the artifacts in the dependency tree.
     *
     * @author Tibor Varga
     */
    private static class UniqueArtifacts implements DependencyVisitor {

        private final Consumer<Artifact> collect;
        private final Set<String> visited = new HashSet<>();

        UniqueArtifacts(final Consumer<Artifact> collect) {
            this.collect = collect;
        }

        public boolean visitEnter(final DependencyNode node) {
            final org.eclipse.aether.graph.Dependency dependency = node.getDependency();
            final org.eclipse.aether.artifact.Artifact artifact = dependency.getArtifact();

            final boolean accepted = visited.add(identity(artifact));

            if (accepted) {
                this.collect.accept(mavenArtifact(artifact, dependency.getScope()));
            }

            return accepted;
        }

        public boolean visitLeave(final DependencyNode node) {
            return true;
        }

        private static String identity(final org.eclipse.aether.artifact.Artifact artifact) {
            return String.format("%s:%s:%s:%s:%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getVersion(), artifact.getExtension());
        }
    }
}

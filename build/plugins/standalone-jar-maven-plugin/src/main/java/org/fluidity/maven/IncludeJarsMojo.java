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

package org.fluidity.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.fluidity.deployment.maven.ArchivesSupport;
import org.fluidity.deployment.maven.DependenciesSupport;
import org.fluidity.deployment.plugin.spi.JarManifest;
import org.fluidity.foundation.Archives;
import org.fluidity.foundation.Lists;
import org.fluidity.foundation.Streams;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.repository.RemoteRepository;

/**
 * Packages all intransitive dependencies of the plugin to its JAR artifact. This plugin assumes the artifact it is instructed to work on already exists and
 * will always overwrite that artifact.
 *
 * @author Tibor Varga
 * @goal include
 * @phase package
 * @threadSafe
 */
@SuppressWarnings("UnusedDeclaration")
public final class IncludeJarsMojo extends AbstractMojo {

    /**
     * The location of the compiled classes.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     * @readonly
     */
    @SuppressWarnings("UnusedDeclaration")
    private File outputDirectory;

    /**
     * The project artifact's final name.
     *
     * @parameter expression="${project.build.directory}/${project.build.finalName}"
     * @required
     * @readonly
     */
    @SuppressWarnings("UnusedDeclaration")
    private String finalName;

    /**
     * Instructs the plugin, when set, to use the given classifier.
     *
     * @parameter default-value=""
     */
    @SuppressWarnings("UnusedDeclaration")
    private String classifier;

    /**
     * List of profile IDs to package the dependencies of into the artifact.
     *
     * @parameter default-value=""
     */
    @SuppressWarnings({ "UnusedDeclaration", "MismatchedQueryAndUpdateOfCollection" })
    private List<String> profiles;

    /**
     * @parameter expression="${plugin.artifactId}"
     * @readonly
     * @required
     */
    @SuppressWarnings("UnusedDeclaration")
    private String pluginArtifactId;

    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    @SuppressWarnings("UnusedDeclaration")
    private MavenProject project;

    /**
     * The current repository/network configuration of Maven.
     *
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    @SuppressWarnings("UnusedDeclaration")
    private RepositorySystemSession repositorySession;

    /**
     * The entry point to Aether, i.e. the component doing all the work.
     *
     * @component
     */
    @SuppressWarnings("UnusedDeclaration")
    private RepositorySystem repositorySystem;

    /**
     * The project's remote repositories to use for the resolution of dependencies.
     *
     * @parameter default-value="${project.remoteProjectRepositories}"
     * @readonly
     */
    @SuppressWarnings({ "UnusedDeclaration", "MismatchedQueryAndUpdateOfCollection" })
    private List<RemoteRepository> repositories;

    public void execute() throws MojoExecutionException {
        if (profiles == null || profiles.isEmpty()) {
            return;
        }

        final File packageFile = new File(classifier == null || classifier.isEmpty()
                                          ? String.format("%s.%s", finalName, DependenciesSupport.JAR_TYPE)
                                          : String.format("%s-%s.%s", finalName, classifier, DependenciesSupport.JAR_TYPE));

        if (!packageFile.exists()) {
            throw new MojoExecutionException(String.format("%s does not exist", packageFile));
        }

        final Log log = getLog();

        try {
            final File file = createTempFile();
            final JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(file));

            try {
                final Map<String, Collection<Artifact>> dependencyMap = new LinkedHashMap<String, Collection<Artifact>>();

                final URL packageURL = packageFile.toURI().toURL();
                final Manifest manifest = Archives.manifest(false, packageURL);
                final Attributes mainAttributes = manifest.getMainAttributes();

                for (final Profile profile : project.getModel().getProfiles()) {
                    final String id = profile.getId();

                    if (profiles.remove(id)) {
                        final String attribute = Archives.Nested.attribute(id);

                        if (mainAttributes.containsKey(attribute)) {
                            throw new MojoExecutionException(String.format("Dependencies '%s' already included in the archive", id));
                        }

                        final Collection<Artifact> dependencies = DependenciesSupport.resolve(repositorySystem,
                                                                                              repositorySession,
                                                                                              repositories,
                                                                                              profile.getDependencies());

                        if (!dependencies.isEmpty()) {
                            for (final Artifact artifact : dependencies) {
                                final File dependency = artifact.getFile();

                                if (!dependency.exists()) {
                                    throw new MojoExecutionException(String.format("Dependency %s not found (tried: %s)", artifact, dependency));
                                }
                            }
                        } else {
                            throw new MojoExecutionException(String.format("Profile %s has no dependencies", id));
                        }

                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Packaged %s dependencies: %s", id, dependencies));
                        }

                        final String projectId = project.getArtifact().getId();
                        final String dependencyPath = String.format("%s/dependencies/%s/", Archives.META_INF, id);
                        final List<String> dependencyList = new ArrayList<String>();

                        for (final Iterator<Artifact> iterator = dependencies.iterator(); iterator.hasNext(); ) {
                            final Artifact artifact = iterator.next();
                            final File dependency = artifact.getFile();

                            if (!dependency.exists()) {
                                throw new MojoExecutionException(String.format("Dependency %s not found (tried: %s)", artifact, dependency));
                            }

                            if (!artifact.getType().equals(DependenciesSupport.JAR_TYPE)) {
                                log.warn(String.format("Ignoring non-JAR dependency %s", dependency));
                                iterator.remove();
                            } else if (dependency.isDirectory()) {
                                log.warn(String.format("Ignoring directory dependency %s", dependency));
                                iterator.remove();
                            } else if (artifact.getId().equals(projectId)) {
                                log.warn(String.format("Ignoring the project artifact %s", dependency));
                                iterator.remove();
                            } else {
                                dependencyList.add(dependencyPath.concat(dependency.getName()));
                            }
                        }

                        mainAttributes.putValue(attribute, Lists.delimited(" ", dependencyList));
                        dependencyMap.put(dependencyPath, dependencies);
                    }
                }

                if (!profiles.isEmpty()) {
                    log.warn(String.format("No profile(s) found matching the execution IDs %s", Lists.delimited(profiles)));
                }

                if (!dependencyMap.isEmpty()) {
                    final byte[] buffer = new byte[16384];

                    final SecurityPolicy policy = new SecurityPolicy(packageFile, 2, false, buffer);

                    for (final Map.Entry<String, Collection<Artifact>> entry : dependencyMap.entrySet()) {
                        final String dependencyPath = entry.getKey();

                        for (final Artifact artifact : entry.getValue()) {
                            policy.add(artifact.getFile(), 1, dependencyPath);
                        }
                    }

                    if (policy.found()) {
                        mainAttributes.putValue(Archives.SECURITY_POLICY, SecurityPolicy.SECURITY_POLICY_FILE);
                    }

                    // create the new manifest
                    outputStream.putNextEntry(new JarEntry(JarFile.MANIFEST_NAME));
                    manifest.write(outputStream);

                    if (policy.found()) {
                        outputStream.putNextEntry(new JarEntry(SecurityPolicy.SECURITY_POLICY_FILE));
                        Streams.store(outputStream, policy.generate(), "UTF-8", buffer, false);
                    }

                    // copy the original archive, excluding entries from our dependency paths
                    Archives.read(false, packageURL, new Archives.Entry() {
                        public boolean matches(final URL url, final JarEntry entry) throws IOException {
                            final String name = entry.getName();

                            if (name.equals(JarFile.MANIFEST_NAME) || name.equals(ArchivesSupport.META_INF)) {
                                return false;
                            }

                            for (final String path : dependencyMap.keySet()) {
                                if (name.startsWith(path)) {
                                    return false;
                                }
                            }

                            return !name.equals(SecurityPolicy.SECURITY_POLICY_FILE);
                        }

                        public boolean read(final URL url, final JarEntry entry, final InputStream stream) throws IOException {
                            outputStream.putNextEntry(new JarEntry(entry.getName()));
                            Streams.copy(stream, outputStream, buffer, false, false);
                            return true;
                        }
                    });

                    // copy the custom dependencies
                    for (final Map.Entry<String, Collection<Artifact>> entry : dependencyMap.entrySet()) {
                        final String dependencyPath = entry.getKey();

                        for (final Artifact artifact : entry.getValue()) {
                            final File dependency = artifact.getFile();

                            outputStream.putNextEntry(new JarEntry(dependencyPath.concat(dependency.getName())));
                            Streams.copy(new FileInputStream(dependency), outputStream, buffer, true, false);
                        }
                    }

                    DependenciesSupport.saveArtifact(project, file, finalName, classifier, DependenciesSupport.JAR_TYPE, log);
                }
            } finally {
                try {
                    outputStream.close();
                } catch (final IOException ignored) {
                    // ignored
                }
            }
        } catch (final IOException e) {
            throw new MojoExecutionException(String.format("Processing %s", packageFile), e);
        }
    }

    private void add(final Map<String, Collection<Artifact>> map, final String path, final Collection<Artifact> list) {
        if (map.containsKey(path)) {
            map.get(path).addAll(list);
        } else {
            map.put(path, list);
        }
    }

    private <V> Map<String, V> clean(final Map<String, V> properties) {
        final Map<String, V> copy = new HashMap<String, V>(properties);

        for (final JarManifest.Packaging packaging : JarManifest.Packaging.values()) {
            copy.remove(packaging.profile);
        }

        return copy;
    }

    private Map<String, String> include(final JarManifest.Packaging enabled, final Map<String, String> properties) {
        final Map<String, String> copy = new HashMap<String, String>(properties);

        for (final JarManifest.Packaging packaging : JarManifest.Packaging.values()) {
            if (packaging != enabled) {

                // profiles are turned *off* by having the corresponding property *defined*
                copy.put(packaging.profile, "true");
            }
        }

        return copy;
    }

    private File createTempFile() throws MojoExecutionException {
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new MojoExecutionException(String.format("Cannot create %s", outputDirectory));
        }

        File tempFile;

        do {
            tempFile = new File(outputDirectory, pluginArtifactId + '-' + System.nanoTime());
        } while (tempFile.exists());

        return tempFile;
    }
}

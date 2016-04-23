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

package org.fluidity.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
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
import org.fluidity.deployment.maven.Logger;
import org.fluidity.deployment.plugin.spi.SecurityPolicy;
import org.fluidity.foundation.Archives;
import org.fluidity.foundation.Lists;
import org.fluidity.foundation.Streams;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Packages all intransitive dependencies of the plugin to its JAR artifact. This plugin assumes the artifact it is instructed to work on already exists and
 * will always overwrite that artifact.
 *
 * @author Tibor Varga
 */
@Mojo(name = IncludeJarsMojo.name, defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public final class IncludeJarsMojo extends AbstractMojo {

    static final String name = "include";

    /**
     * The location of the compiled classes.
     */
    @SuppressWarnings("UnusedDeclaration")
    @Parameter(property = "project.build.directory", required = true, readonly = true)
    private File outputDirectory;

    /**
     * The project artifact's final name.
     */
    @SuppressWarnings("UnusedDeclaration")
    @Parameter(property = "project.build.finalName", required = true, readonly = true)
    private String finalName;

    /**
     * Instructs the plugin, when set, to use the given classifier.
     */
    @SuppressWarnings("UnusedDeclaration")
    @Parameter
    private String classifier;

    /**
     * Tells the plugin to emit details about its operation. The default value of this parameter is <code>false</code>.
     */
    @Parameter(property = "fluidity.maven.verbose")
    private boolean verbose;

    /**
     * List of profile IDs to package the dependencies of into the artifact.
     */
    @Parameter
    private List<String> profiles;

    /**
     * The plugin's artifact ID.
     */
    @Parameter(property = "plugin.artifactId", required = true, readonly = true)
    private String pluginArtifactId;

    /**
     * The Maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    /**
     * The current repository/network configuration of Maven.
     */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repositorySession;

    /**
     * The entry point to Aether, i.e. the component doing all the work.
     */
    @Component
    private RepositorySystem repositorySystem;

    /**
     * The project's remote repositories to use for the resolution of dependencies.
     */
    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
    private List<RemoteRepository> repositories;

    @Component
    private ArchivesSupport archives;

    @Component
    private DependenciesSupport dependencies;

    public void execute() throws MojoExecutionException {
        if (profiles == null || profiles.isEmpty()) {
            return;
        }

        final String baseName = String.format("%s/%s", outputDirectory, finalName);

        final File packageFile = new File(classifier == null || classifier.isEmpty()
                                          ? String.format("%s.%s", baseName, DependenciesSupport.JAR_TYPE)
                                          : String.format("%s-%s.%s", baseName, classifier, DependenciesSupport.JAR_TYPE));

        if (!packageFile.exists()) {
            throw new MojoExecutionException(String.format("%s does not exist", packageFile));
        }

        final Logger log = Logger.initialize(getLog(), verbose);

        try {
            final File file = createTempFile();

            try (final JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(file))) {
                final Map<String, Collection<Artifact>> dependencyMap = new LinkedHashMap<>();

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

                        final Collection<Artifact> dependencies = this.dependencies.resolve(repositorySystem,
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

                        if (log.active()) {
                            log.detail(String.format("Profile '%s' archives:", id));
                            this.dependencies.list(dependencies, "  ", log);
                        }

                        final String projectId = project.getArtifact().getId();
                        final String dependencyPath = String.format("%s/dependencies/%s/", Archives.META_INF, id);
                        final List<String> dependencyList = new ArrayList<>();

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

                    final SecurityPolicy policy = new JavaSecurityPolicy(2, false, buffer, packageFile, log);

                    policy.add(packageFile, 0, null);

                    for (final Map.Entry<String, Collection<Artifact>> entry : dependencyMap.entrySet()) {
                        final String dependencyPath = entry.getKey();

                        for (final Artifact artifact : entry.getValue()) {
                            policy.add(artifact.getFile(), 1, dependencyPath);
                        }
                    }

                    policy.update((name, content) -> {
                        if (content == null) {
                            mainAttributes.remove(new Attributes.Name(name));
                        } else {
                            mainAttributes.putValue(name, content);
                        }
                    });

                    // create the new manifest
                    outputStream.putNextEntry(new JarEntry(JarFile.MANIFEST_NAME));
                    manifest.write(outputStream);

                    policy.save((name, content) -> {
                        outputStream.putNextEntry(new JarEntry(name));
                        Streams.store(outputStream, content, "UTF-8", buffer, false);
                    });

                    final String policyName = policy.name(packageFile);

                    // copy the original archive, excluding entries from our dependency paths
                    Archives.read(false, packageURL, (url, entry) -> {
                        final String name = entry.getName();

                        if (name.equals(JarFile.MANIFEST_NAME) || name.equals(ArchivesSupport.META_INF)) {
                            return null;
                        }

                        for (final String path : dependencyMap.keySet()) {
                            if (name.startsWith(path)) {
                                return null;
                            }
                        }

                        return name.equals(policyName) ? null : (_url, _entry, stream) -> {
                            outputStream.putNextEntry(new JarEntry(_entry.getName()));
                            Streams.copy(stream, outputStream, buffer, false, false);
                            return true;
                        };
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

                    dependencies.saveArtifact(project, file, baseName, classifier, DependenciesSupport.JAR_TYPE, log);
                }
            }
        } catch (final IOException e) {
            throw new MojoExecutionException(String.format("Processing %s", packageFile), e);
        }
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

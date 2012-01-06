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

package org.fluidity.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.fluidity.deployment.maven.MavenSupport;
import org.fluidity.deployment.plugin.spi.JarManifest;
import org.fluidity.foundation.Archives;
import org.fluidity.foundation.ServiceProviders;
import org.fluidity.foundation.Streams;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.repository.RemoteRepository;

/**
 * Packages all transitive dependencies of the project to its JAR artifact. This plugin uses an implementation of the {@link JarManifest} interface, found as a
 * JAR service provider, to process the JAR's manifest attributes.
 *
 * @author Tibor Varga
 * @goal package
 * @phase package
 * @threadSafe
 */
public class StandaloneJarMojo extends AbstractMojo {

    private static final Set<String> DEPENDENCY_TYPES = Collections.singleton(MavenSupport.JAR_TYPE);

    /**
     * Instructs the plugin, when set, to create a new JAR with the given classifier and attach it to the project. When not set, the project's JAR artifact
     * is overwritten.
     *
     * @parameter default-value=""
     */
    @SuppressWarnings("UnusedDeclaration")
    private String classifier;

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
     * The project artifact file.
     *
     * @parameter expression="${project.build.directory}/${project.build.finalName}.${project.packaging}"
     * @required
     * @readonly
     */
    @SuppressWarnings("UnusedDeclaration")
    private File packageFile;

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
     * Packaging type of the artifact to be installed. Retrieved from POM file if specified
     *
     * @parameter expression="${project.packaging}"
     * @readonly
     */
    @SuppressWarnings("UnusedDeclaration")
    private String packaging;

    /**
     * @parameter expression="${plugin.groupId}"
     * @readonly
     * @required
     */
    @SuppressWarnings("UnusedDeclaration")
    private String pluginGroupId;

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
    private List<RemoteRepository> projectRepositories;

    public void execute() throws MojoExecutionException {
        if (!MavenSupport.JAR_TYPE.equals(packaging)) {
            throw new MojoExecutionException("This is not a .jar project");
        } else if (!packageFile.exists()) {
            throw new MojoExecutionException(String.format("%s does not exist", packageFile));
        }

        final JarManifest handler = ServiceProviders.findInstance(JarManifest.class, getClass().getClassLoader());
        if (handler == null) {
            throw new MojoExecutionException(String.format("No %s implementation found", JarManifest.class.getName()));
        }

        final Collection<Artifact> runtimeDependencies = MavenSupport.runtimeDependencies(repositorySystem, repositorySession, projectRepositories, project, DEPENDENCY_TYPES);

        // keep only JAR artifacts
        for (final Iterator<Artifact> i = runtimeDependencies.iterator(); i.hasNext();) {
            final Artifact artifact = i.next();

            if (!artifact.getType().equals(MavenSupport.JAR_TYPE)) {
                i.remove();
            }
        }

        if (runtimeDependencies.isEmpty()) {

            // no project dependencies: we're done
            return;
        }

        final Set<String> processedEntries = new HashSet<String>();

        try {
            final File file = createTempFile();
            final JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(file));

            try {
                final Manifest manifest;

                final JarFile jarInput = new JarFile(packageFile);
                try {
                    manifest = jarInput.getManifest();
                } finally {
                    try {
                        jarInput.close();
                    } catch (final IOException ignored) {
                        // ignored
                    }
                }
                final Attributes attributes = manifest.getMainAttributes();

                if (attributes.get(Attributes.Name.CLASS_PATH) != null) {
                    throw new MojoExecutionException(String.format("Manifest contains %s", Attributes.Name.CLASS_PATH));
                }

                final String dependencyPath = MavenSupport.META_INF.concat("dependencies/");
                final List<String> dependencyList = new ArrayList<String>();

                for (final Artifact artifact : runtimeDependencies) {
                    final File dependency = artifact.getFile();

                    if (!dependency.exists()) {
                        throw new MojoExecutionException(String.format("Dependency %s not found (tried: %s)", artifact, dependency));
                    }

                    dependencyList.add(dependencyPath.concat(dependency.getName()));
                }

                final Collection<Artifact> dependencies = handler.needsCompileDependencies() ? compileDependencies() : runtimeDependencies;
                final boolean copyHandler = handler.processManifest(project, attributes, dependencyList, dependencies);

                final String pluginKey = Plugin.constructKey(pluginGroupId, pluginArtifactId);
                final Plugin plugin = project.getPlugin(pluginKey);
                final Artifact pluginArtifact = project.getPluginArtifactMap().get(pluginKey);

                final Collection<Artifact> bootstrapDependencies = !copyHandler
                                                                   ? Collections.<Artifact>emptyList()
                                                                   : MavenSupport.transitiveDependencies(repositorySystem,
                                                                                                         repositorySession,
                                                                                                         projectRepositories,
                                                                                                         handler.getClass(),
                                                                                                         pluginArtifact,
                                                                                                         plugin.getDependencies(),
                                                                                                         DEPENDENCY_TYPES);

                final byte buffer[] = new byte[1024 * 16];

                // copy all entries except the manifest from all bootstrap artifacts to the new jar file
                for (final Artifact artifact : bootstrapDependencies) {
                    final JarFile input = new JarFile(artifact.getFile());

                    try {
                        for (final Enumeration entries = input.entries(); entries.hasMoreElements();) {
                            final JarEntry entry = (JarEntry) entries.nextElement();
                            final String entryName = entry.getName();

                            if (!processedEntries.contains(entryName)) {
                                if (!entryName.equals(JarFile.MANIFEST_NAME)) {
                                    outputStream.putNextEntry(entry);
                                    Streams.copy(input.getInputStream(entry), outputStream, buffer, false);
                                    processedEntries.add(entryName);
                                }
                            }
                        }
                    } finally {
                        try {
                            input.close();
                        } catch (final IOException ignored) {
                            // ignored
                        }
                    }
                }

                // create the new manifest
                outputStream.putNextEntry(new JarEntry(JarFile.MANIFEST_NAME));
                manifest.write(outputStream);

                outputStream.putNextEntry(new JarEntry(dependencyPath));

                final String projectId = project.getArtifact().getId();

                // copy the dependencies, including the original project artifact
                for (final Artifact artifact : runtimeDependencies) {
                    final File dependency = artifact.getFile();

                    final String entryName = dependencyPath.concat(dependency.getName());
                    outputStream.putNextEntry(new JarEntry(entryName));

                    if (artifact.getId().equals(projectId)) {

                        // got to check if our project artifact is something we have created in a previous run
                        // i.e., if it contains the project artifact we're about to copy
                        int read = Archives.readEntries(dependency.toURI().toURL(), new Archives.EntryReader() {
                            public boolean matches(final JarEntry entry) throws IOException {
                                return entryName.equals(entry.getName());
                            }

                            public boolean read(final JarEntry entry, final JarInputStream stream) throws IOException {
                                Streams.copy(stream, outputStream, buffer, false);
                                return false;
                            }
                        });

                        if (read > 0) {
                            continue;
                        }
                    }

                    Streams.copy(new FileInputStream(dependency), outputStream, buffer, false);
                }
            } finally {
                try {
                    outputStream.close();
                } catch (final IOException ignored) {
                    // ignored
                }
            }

            MavenSupport.saveArtifact(project, file, finalName, classifier, packaging);
        } catch (final IOException e) {
            throw new MojoExecutionException(String.format("Processing %s", packageFile), e);
        }
    }

    private Collection<Artifact> compileDependencies() throws MojoExecutionException {
        return MavenSupport.compileDependencies(repositorySystem, repositorySession, projectRepositories, project, DEPENDENCY_TYPES);
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

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.fluidity.deployment.maven.MavenDependencies;
import org.fluidity.foundation.JarManifest;
import org.fluidity.foundation.ServiceProviders;
import org.fluidity.foundation.Streams;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.repository.RemoteRepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.jar.*;

/**
 * Adds code to the project's standalone .jar artifact that allows it to embed its dependencies.
 *
 * @author Tibor Varga
 * @goal package
 * @phase package
 */
public class ExecutableJarMojo extends AbstractMojo {

    private static final String JAR_TYPE = "jar";
    private static final String META_INF = "META-INF/";

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
     * @parameter expression="${plugin.groupId}"
     * @required
     */
    @SuppressWarnings("UnusedDeclaration")
    private String pluginGroupId;

    /**
     * @parameter expression="${plugin.artifactId}"
     * @required
     */
    @SuppressWarnings("UnusedDeclaration")
    private String pluginArtifactId;

    /**
     * Packaging type of the artifact to be installed. Retrieved from POM file if specified
     *
     * @parameter expression="${project.packaging}"
     */
    @SuppressWarnings("UnusedDeclaration")
    private String packaging;

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
        if (!JAR_TYPE.equals(packaging)) {
            throw new MojoExecutionException("This is not a .jar project");
        } else if (!packageFile.exists()) {
            throw new MojoExecutionException(String.format("%s does not exist", packageFile));
        }

        final String pluginKey = Plugin.constructKey(pluginGroupId, pluginArtifactId);
        final Plugin plugin = project.getPlugin(pluginKey);
        final Artifact pluginArtifact = project.getPluginArtifactMap().get(pluginKey);

        final JarManifest handler = ServiceProviders.findInstance(JarManifest.class, getClass().getClassLoader());

        final Collection<Artifact> bootstrapDependencies = MavenDependencies.transitiveDependencies(repositorySystem,
                                                                                                    repositorySession,
                                                                                                    projectRepositories,
                                                                                                    handler.getClass(),
                                                                                                    pluginArtifact,
                                                                                                    plugin.getDependencies());

        final Collection<Artifact> projectDependencies = MavenDependencies.transitiveDependencies(repositorySystem,
                                                                                                  repositorySession,
                                                                                                  projectRepositories,
                                                                                                  project.getArtifact(),
                                                                                                  false);

        // keep only JAR artifacts
        for (final Iterator<Artifact> i = projectDependencies.iterator(); i.hasNext();) {
            final Artifact artifact = i.next();

            if (!artifact.getType().equals(JAR_TYPE)) {
                i.remove();
            }
        }

        if (projectDependencies.isEmpty()) {

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

                final String dependencyPath = META_INF.concat("dependencies/");
                final StringBuilder dependencyList = new StringBuilder();

                for (final Artifact artifact : projectDependencies) {
                    final File dependency = artifact.getFile();

                    if (!dependency.exists()) {
                        throw new MojoExecutionException(String.format("Dependency %s not found (tried: %s)", artifact, dependency));
                    }

                    if (dependencyList.length() > 0) {
                        dependencyList.append(' ');
                    }

                    dependencyList.append(dependencyPath).append(dependency.getName());
                }

                attributes.putValue(JarManifest.NESTED_DEPENDENCIES, dependencyList.toString());

                handler.processManifest(attributes);

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

                // copy the dependencies, including the original project artifact
                for (final Artifact artifact : projectDependencies) {
                    final File dependency = artifact.getFile();
                    outputStream.putNextEntry(new JarEntry(dependencyPath.concat(dependency.getName())));
                    Streams.copy(new FileInputStream(dependency), outputStream, buffer, false);
                }
            } finally {
                try {
                    outputStream.close();
                } catch (final IOException ignored) {
                    // ignored
                }
            }

            if (!packageFile.delete()) {
                throw new MojoExecutionException(String.format("Could not delete %s", packageFile));
            }

            if (!file.renameTo(packageFile)) {
                throw new MojoExecutionException(String.format("Could not create %s", packageFile));
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

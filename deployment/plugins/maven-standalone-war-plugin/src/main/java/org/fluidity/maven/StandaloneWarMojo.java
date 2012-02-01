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
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.fluidity.deployment.impl.WarBootstrapLoader;
import org.fluidity.deployment.maven.MavenSupport;
import org.fluidity.foundation.Streams;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.repository.RemoteRepository;

/**
 * Adds code to the project WAR file that allows it to be run as a .jar file, e.g. <code>$ java -jar &lt;file name>.war</code>. More .war files can be
 * specified in the command line and all will be deployed to the same application server.
 * <p/>
 * <b>Technical Details</b>
 * <p/>
 * Three different set of libraries are managed: bootstrap dependencies i.e., classes in the JAR/WAR root for <code>java -jar</code> to see, server dependencies
 * i.e., JARs needed to boot up the HTTP server, and application dependencies i.e., JARs in WEB-INF/lib.
 * <p/>
 * Bootstrap dependencies come from the transitive non-optional run-time dependencies of this plugin itself.
 * <p/>
 * Server dependencies come from the transitive non-optional run-time dependencies declared for the plugin in the host project's POM and are copied to
 * WEB-INF/boot.
 * <p/>
 * The list of JARs in WEB-INF/lib is already in the WAR file itself.
 * <p/>
 * There are various set operations performed in these dependencies to make sure nothing is included that shouldn't.
 *
 * @author Tibor Varga
 * @goal package
 * @phase package
 * @threadSafe
 */
@SuppressWarnings("UnusedDeclaration")
public class StandaloneWarMojo extends AbstractMojo {

    /**
     * Instructs the plugin, when set, to remove from the WEB-INF/lib directory all .jar files that the plugin puts in the WEB-INF/boot directory, effectively
     * making the resulting WAR smaller than otherwise but also making it executable via the command line only, i.e. the WAR file will not be deployable in an
     * ordinary web container.
     *
     * @parameter default-value="false"
     */
    @SuppressWarnings("UnusedDeclaration")
    private boolean commandLineOnly;

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
    private List<RemoteRepository> repositories;

    public void execute() throws MojoExecutionException {
        if (!MavenSupport.WAR_TYPE.equals(packaging)) {
            throw new MojoExecutionException("This is not a WAR project");
        } else if (!packageFile.exists()) {
            throw new MojoExecutionException(String.format("%s does not exist", packageFile));
        }

        final String pluginKey = Plugin.constructKey(pluginGroupId, pluginArtifactId);
        final Artifact pluginArtifact = project.getPluginArtifactMap().get(pluginKey);

        final Collection<Artifact> pluginDependencies = MavenSupport.dependencyClosure(repositorySystem, repositorySession, repositories, pluginArtifact, false, false, null);
        final Artifact handlerDependency = MavenSupport.artifact(WarBootstrapLoader.class, pluginDependencies);
        assert handlerDependency != null : WarBootstrapLoader.class;

        final Collection<Artifact> bootstrapDependencies = MavenSupport.dependencyClosure(repositorySystem, repositorySession, repositories, handlerDependency, false, false, null);

        final Set<Artifact> serverDependencies = new HashSet<Artifact>();

        for (final Dependency dependency : project.getPlugin(pluginKey).getDependencies()) {
            assert !dependency.isOptional() : dependency;
            serverDependencies.addAll(MavenSupport.dependencyClosure(repositorySystem, repositorySession, repositories, MavenSupport.dependencyArtifact(dependency), false, false, dependency.getExclusions()));
        }

        serverDependencies.removeAll(MavenSupport.dependencyClosure(repositorySystem, repositorySession, repositories, project.getArtifact(), false, false, null));
        serverDependencies.removeAll(pluginDependencies);
        serverDependencies.remove(pluginArtifact);

        final Set<String> processedEntries = new HashSet<String>();

        try {
            final File file = createTempFile();
            final JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(file));
            String mainClass = null;

            try {
                boolean manifestFound = false;
                final byte buffer[] = new byte[1024 * 16];

                for (final Artifact artifact : bootstrapDependencies) {
                    final JarFile jarInput = new JarFile(artifact.getFile());

                    if (mainClass == null) {
                        mainClass = (String) jarInput.getManifest().getMainAttributes().get(Attributes.Name.MAIN_CLASS);
                    }

                    try {
                        for (final Enumeration<JarEntry> entries = jarInput.entries(); entries.hasMoreElements();) {
                            final JarEntry entry = entries.nextElement();
                            final String entryName = entry.getName();

                            if (!processedEntries.contains(entryName)) {

                                // copy all entries except the META-INF directory
                                if (!entryName.startsWith(MavenSupport.META_INF)) {
                                    outputStream.putNextEntry(entry);
                                    Streams.copy(jarInput.getInputStream(entry), outputStream, buffer, false);
                                    processedEntries.add(entryName);
                                }
                            } else if (!entryName.endsWith("/")) {
                                throw new MojoExecutionException(String.format("Duplicate entry: %s", entryName));
                            }
                        }
                    } finally {
                        try {
                            jarInput.close();
                        } catch (final IOException ignored) {
                            // ignored
                        }
                    }
                }

                if (mainClass == null) {
                    throw new MojoExecutionException(String.format("None of the following dependencies specified a main class (manifest entry '%s'): %s",
                                                                   Attributes.Name.MAIN_CLASS,
                                                                   bootstrapDependencies));
                }

                final Set<String> bootLibraries = new HashSet<String>();

                final String libDirectory = MavenSupport.WEB_INF.concat("lib");
                for (final Artifact artifact : serverDependencies) {
                    bootLibraries.add(String.format("%s/%s", libDirectory, artifact.getFile().getName()));
                }

                final String bootDirectory = MavenSupport.WEB_INF.concat("boot/");
                final JarFile warInput = new JarFile(packageFile);

                try {
                    final Manifest manifest = warInput.getManifest();
                    final Attributes mainAttributes = manifest.getMainAttributes();

                    if (mainAttributes.getValue(Attributes.Name.MAIN_CLASS) != null) {
                        throw new MojoExecutionException(String.format("Manifest already contains %s: %s",
                                                                       Attributes.Name.MAIN_CLASS,
                                                                       mainAttributes.getValue(Attributes.Name.MAIN_CLASS)));
                    }

                    mainAttributes.putValue(Attributes.Name.MAIN_CLASS.toString(), mainClass);

                    for (final Enumeration entries = warInput.entries(); entries.hasMoreElements();) {
                        final JarEntry entry = (JarEntry) entries.nextElement();
                        final String entryName = entry.getName();

                        if (entryName.equalsIgnoreCase(JarFile.MANIFEST_NAME)) {
                            if (!manifestFound) {
                                outputStream.putNextEntry(new JarEntry(JarFile.MANIFEST_NAME));
                                manifest.write(outputStream);
                                manifestFound = true;
                            }
                        } else {
                            if (!processedEntries.contains(entryName) && !entryName.startsWith(bootDirectory)) {
                                if (!commandLineOnly || !bootLibraries.contains(entryName)) {
                                    outputStream.putNextEntry(entry);
                                    Streams.copy(warInput.getInputStream(entry), outputStream, buffer, false);
                                }
                            }
                        }
                    }

                    if (!manifestFound) {
                        throw new MojoExecutionException(String.format("No manifest found in %s", packageFile));
                    }

                    if (!serverDependencies.isEmpty()) {
                        outputStream.putNextEntry(new JarEntry(bootDirectory));

                        for (final Artifact artifact : serverDependencies) {
                            final File dependency = artifact.getFile();

                            if (!dependency.exists()) {
                                throw new MojoExecutionException(String.format("Dependency %s not found (tried: %s)", artifact, dependency));
                            }

                            outputStream.putNextEntry(new JarEntry(bootDirectory + dependency.getName()));
                            Streams.copy(new FileInputStream(dependency), outputStream, buffer, false);
                        }
                    }
                } finally {
                    try {
                        warInput.close();
                    } catch (final IOException ignored) {
                        // ignored
                    }
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

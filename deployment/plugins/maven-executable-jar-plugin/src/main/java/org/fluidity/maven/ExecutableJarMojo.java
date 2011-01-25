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

package org.fluidity.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

import org.fluidity.deployment.maven.MavenDependencies;
import org.fluidity.foundation.JarJarLauncher;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.repository.RemoteRepository;

/**
 * Adds code to the project's executable .jar artifact that allows it to embed its dependencies.
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
    @SuppressWarnings( { "UnusedDeclaration", "MismatchedQueryAndUpdateOfCollection" })
    private List<RemoteRepository> projectRepositories;

    public void execute() throws MojoExecutionException {
        if (!JAR_TYPE.equals(packaging)) {
            throw new MojoExecutionException("This is not a .jar project");
        } else if (!packageFile.exists()) {
            throw new MojoExecutionException(String.format("%s does not exist", packageFile));
        }

        final String pluginKey = Plugin.constructKey(pluginGroupId, pluginArtifactId);
        final Artifact pluginArtifact = project.getPluginArtifactMap().get(pluginKey);

        final Collection<Artifact> bootstrapDependencies = MavenDependencies.transitiveDependencies(repositorySystem,
                                                                                                    repositorySession,
                                                                                                    projectRepositories,
                                                                                                    pluginArtifact,
                                                                                                    JarJarLauncher.class);

        final Collection<Artifact> projectDependencies = MavenDependencies.transitiveDependencies(repositorySystem,
                                                                                                  repositorySession,
                                                                                                  projectRepositories,
                                                                                                  project.getArtifact(),
                                                                                                  false);

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

                final String dependencyPath = META_INF.concat("dependencies/");
                final Attributes mainAttributes = manifest.getMainAttributes();

                if (mainAttributes.get(Attributes.Name.MAIN_CLASS) == null) {
                    throw new MojoExecutionException(String.format("Manifest does not contain %s", Attributes.Name.MAIN_CLASS));
                } else {
                    mainAttributes.putValue(JarJarLauncher.ORIGINAL_MAIN_CLASS, mainAttributes.getValue(Attributes.Name.MAIN_CLASS));
                    mainAttributes.put(Attributes.Name.MAIN_CLASS, JarJarLauncher.class.getName());
                    mainAttributes.putValue(JarJarLauncher.DEPENDENCIES_PATH, dependencyPath);
                }

                // jar files needed by the project artifact, i.e., the Class-Path attribute in its manifest
                final Set<String> bootLibraries = new HashSet<String>();

                final String classPath = mainAttributes.getValue(Attributes.Name.CLASS_PATH);
                if (classPath != null) {
                    for (final String path : classPath.split("\\s+")) {
                        bootLibraries.add(path.trim());
                    }
                }

                if (bootLibraries.removeAll(absolutePaths(projectDependencies))) {
                    final StringBuilder strippedClassPath = new StringBuilder();
                    for (final String library : bootLibraries) {
                        if (strippedClassPath.length() > 0) {
                            strippedClassPath.append(' ');
                        }

                        strippedClassPath.append(library);
                    }

                    if (strippedClassPath.length() > 0) {
                        mainAttributes.put(Attributes.Name.CLASS_PATH, strippedClassPath.toString());
                    } else {
                        mainAttributes.remove(Attributes.Name.CLASS_PATH);
                    }
                }

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
                                    copyStream(outputStream, input.getInputStream(entry), buffer);
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

                    if (!dependency.exists()) {
                        throw new MojoExecutionException(String.format("Dependency %s not found (tried: %s)", artifact, dependency));
                    }

                    outputStream.putNextEntry(new JarEntry(dependencyPath.concat(dependency.getName())));
                    copyStream(outputStream, new FileInputStream(dependency), buffer);
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

    private Collection<String> absolutePaths(final Collection<Artifact> artifacts) {
        final HashSet<String> list = new HashSet<String>();

        for (final Artifact artifact : artifacts) {
            list.add(artifact.getFile().getAbsolutePath());
        }

        return list;
    }

    private void copyStream(final JarOutputStream output, final InputStream input, final byte[] buffer) throws IOException {
        int bytesRead;

        try {
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        } finally {
            try {
                input.close();
            } catch (final IOException e) {
                // ignore
            }
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

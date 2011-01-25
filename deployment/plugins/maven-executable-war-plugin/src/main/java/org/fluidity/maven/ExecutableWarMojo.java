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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.fluidity.deployment.WarBootstrapLoader;
import org.fluidity.deployment.maven.MavenDependencies;

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
 * Adds code to the project .war file that allows it to be run as a .jar file, e.g. <code>$ java -jar &lt;file name>.war</code>. More .war files can be
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
 */
public class ExecutableWarMojo extends AbstractMojo {

    private static final String WAR_TYPE = "war";
    private static final String JAR_TYPE = "jar";

    /**
     * Instructs the plugin, when set, to remove from the WEB-INF/lib directory all .jar files that the plugin puts in the WEB-INF/boot directory, effectively
     * making the resulting .war smaller than otherwise but also making it executable via the command line only, i.e. the .war file will not be deployable in an
     * ordinary web container.
     *
     * @parameter default-value="false"
     */
    @SuppressWarnings("UnusedDeclaration")
    private boolean commandLineOnly;

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
     * @parameter expression="${plugin.artifactMap}"
     * @required
     */
    @SuppressWarnings({ "UnusedDeclaration", "MismatchedQueryAndUpdateOfCollection" })
    private Map<String, Artifact> pluginArtifactMap;

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
        if (!WAR_TYPE.equals(packaging)) {
            throw new MojoExecutionException("This is not a .war project");
        } else if (!packageFile.exists()) {
            throw new MojoExecutionException(String.format("%s does not exist", packageFile));
        }

        final String pluginKey = Plugin.constructKey(pluginGroupId, pluginArtifactId);
        final Artifact pluginArtifact = project.getPluginArtifactMap().get(pluginKey);

        final Collection<Artifact> bootstrapDependencies = MavenDependencies.transitiveDependencies(repositorySystem,
                                                                                                    repositorySession,
                                                                                                    projectRepositories,
                                                                                                    pluginArtifact,
                                                                                                    WarBootstrapLoader.class);
        final Set<Artifact> serverDependencies = new HashSet<Artifact>();
        for (final Dependency dependency : project.getPlugin(pluginKey).getDependencies()) {
            if (!dependency.isOptional()) {
                serverDependencies.addAll(MavenDependencies.transitiveDependencies(repositorySystem,
                                                                                   repositorySession,
                                                                                   projectRepositories,
                                                                                   MavenDependencies.dependencyArtifact(dependency),
                                                                                   false));
            }
        }

        for (final Iterator<Artifact> list = serverDependencies.iterator(); list.hasNext();) {
            final Artifact dependency = list.next();
            if (!dependency.getType().equals(JAR_TYPE)) {
                list.remove();
            }
        }

        serverDependencies.removeAll(MavenDependencies.transitiveDependencies(repositorySystem, repositorySession, projectRepositories, project.getArtifact(), false));
        serverDependencies.removeAll(MavenDependencies.transitiveDependencies(repositorySystem, repositorySession, projectRepositories, pluginArtifact, false));
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
                                if (!entryName.startsWith("META-INF")) {
                                    outputStream.putNextEntry(entry);
                                    copyStream(outputStream, jarInput.getInputStream(entry), buffer);
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

                assert mainClass != null : bootstrapDependencies;

                final Set<String> bootLibraries = new HashSet<String>();

                for (final Artifact artifact : serverDependencies) {
                    bootLibraries.add("WEB-INF/lib/" + new File(artifact.getFile().getName()));
                }

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
                            if (!processedEntries.contains(entryName)) {
                                if (!commandLineOnly || !bootLibraries.contains(entryName)) {
                                    outputStream.putNextEntry(entry);
                                    copyStream(outputStream, warInput.getInputStream(entry), buffer);
                                }
                            } else if (!entryName.endsWith("/")) {
                                throw new MojoExecutionException(String.format("Duplicate entry: %s", entryName));
                            }
                        }
                    }

                    if (!manifestFound) {
                        throw new MojoExecutionException(String.format("No manifest found in %s", packageFile));
                    }

                    if (!serverDependencies.isEmpty()) {
                        final String bootDirectory = "WEB-INF/boot/";

                        outputStream.putNextEntry(new JarEntry(bootDirectory));

                        for (final Artifact artifact : serverDependencies) {
                            final File dependency = artifact.getFile();

                            if (!dependency.exists()) {
                                throw new MojoExecutionException(String.format("Dependency %s not found (tried: %s)", artifact, dependency));
                            }

                            outputStream.putNextEntry(new JarEntry(bootDirectory + dependency.getName()));
                            copyStream(outputStream, new FileInputStream(dependency), buffer);
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

    /*
     * The following conversion methods were created based on the logic found in org.apache.maven.RepositoryUtils
     */
}

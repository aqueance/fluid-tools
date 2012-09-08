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
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.fluidity.deployment.launcher.WebApplicationBootstrap;
import org.fluidity.deployment.maven.ArchivesSupport;
import org.fluidity.deployment.maven.DependenciesSupport;
import org.fluidity.foundation.Archives;
import org.fluidity.foundation.Streams;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.repository.RemoteRepository;

/**
 * Adds code to the project WAR file that allows it to be run as a JAR file; e.g. <code>$ java -jar &lt;file name>.war</code>. More WAR files can be
 * specified in the command line and all will be deployed to the same application server.
 * <p/>
 * <b>Technical Details</b>
 * <p/>
 * Three different set of libraries are managed: bootstrap dependencies i.e., classes in the JAR/WAR root for <code>java -jar</code> to see, server
 * dependencies i.e., JARs needed to boot up the HTTP server, and application dependencies i.e., JARs in WEB-INF/lib.
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
 * @goal standalone
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
     * @parameter expression="${project.build.directory}/${project.build.finalName}.war"
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
        if (!packageFile.exists()) {
            throw new MojoExecutionException(String.format("%s does not exist", packageFile));
        }

        final String pluginKey = Plugin.constructKey(pluginGroupId, pluginArtifactId);
        final Artifact pluginArtifact = project.getPluginArtifactMap().get(pluginKey);

        final Collection<Artifact> pluginDependencies = DependenciesSupport.dependencyClosure(repositorySystem, repositorySession, repositories, pluginArtifact, false, false, null);
        final Artifact handlerDependency = DependenciesSupport.artifact(WebApplicationBootstrap.class, pluginDependencies);
        assert handlerDependency != null : WebApplicationBootstrap.class;

        final Collection<Artifact> bootstrapDependencies = DependenciesSupport.dependencyClosure(repositorySystem, repositorySession, repositories, handlerDependency, false, false, null);

        final Set<Artifact> serverDependencies = new HashSet<Artifact>();

        for (final Dependency dependency : project.getPlugin(pluginKey).getDependencies()) {
            assert !dependency.isOptional() : dependency;
            serverDependencies.addAll(DependenciesSupport.dependencyClosure(repositorySystem, repositorySession, repositories, DependenciesSupport.dependencyArtifact(dependency), false, false, dependency.getExclusions()));
        }

        serverDependencies.removeAll(DependenciesSupport.dependencyClosure(repositorySystem, repositorySession, repositories, project.getArtifact(), false, false, null));
        serverDependencies.removeAll(pluginDependencies);
        serverDependencies.remove(pluginArtifact);

        final Log log = getLog();

        try {
            final byte buffer[] = new byte[1024 * 1024];

            final AtomicReference<String> mainClass = new AtomicReference<String>();
            final Map<String, Attributes> attributesMap = new HashMap<String, Attributes>();
            final Map<String, String[]> providerMap = new HashMap<String, String[]>();

            ArchivesSupport.load(attributesMap, providerMap, buffer, log, new ArchivesSupport.Feed() {
                private final Iterator<Artifact> iterator = bootstrapDependencies.iterator();

                public File next() throws IOException {
                    if (iterator.hasNext()) {
                        final File file = iterator.next().getFile();
                        final URL url = file.toURI().toURL();
                        mainClass.compareAndSet(null, Archives.manifestAttributes(url, Attributes.Name.MAIN_CLASS.toString())[0]);
                        return file;
                    } else {
                        return null;
                    }
                }

                public boolean include(final JarEntry entry) {
                    return true;
                }
            });

            final File file = createTempFile();
            final JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(file));

            try {
                if (mainClass.get() == null) {
                    throw new MojoExecutionException(String.format("None of the following dependencies specified a main class (manifest entry '%s'): %s",
                                                                   Attributes.Name.MAIN_CLASS,
                                                                   bootstrapDependencies));
                }

                final Set<String> bootLibraries = new HashSet<String>();

                final String libDirectory = Archives.WEB_INF.concat("/lib/");
                for (final Artifact artifact : serverDependencies) {
                    bootLibraries.add(libDirectory.concat(artifact.getFile().getName()));
                }

                final String bootDirectory = Archives.WEB_INF.concat("/boot/");

                final Manifest manifest = Archives.loadManifest(packageFile.toURI().toURL());
                final Attributes mainAttributes = manifest.getMainAttributes();

                if (mainAttributes.getValue(Attributes.Name.MAIN_CLASS) != null) {
                    throw new MojoExecutionException(String.format("Manifest already contains %s: %s",
                                                                   Attributes.Name.MAIN_CLASS,
                                                                   mainAttributes.getValue(Attributes.Name.MAIN_CLASS)));
                }

                mainAttributes.putValue(Attributes.Name.MAIN_CLASS.toString(), mainClass.get());

                ArchivesSupport.include(attributesMap, manifest);

                outputStream.putNextEntry(new JarEntry(ArchivesSupport.META_INF));
                outputStream.putNextEntry(new JarEntry(JarFile.MANIFEST_NAME));
                manifest.write(outputStream);

                ArchivesSupport.expand(outputStream, buffer, providerMap, new ArchivesSupport.Feed() {
                    private final Iterator<Artifact> iterator = bootstrapDependencies.iterator();
                    private final AtomicReference<Boolean> last = new AtomicReference<Boolean>(false);

                    public File next() throws IOException {
                        if (iterator.hasNext()) {
                            return iterator.next().getFile();
                        } else if (last.compareAndSet(false, true)) {
                            return packageFile;
                        } else {
                            return null;
                        }
                    }

                    public boolean include(final JarEntry entry) {
                        return !commandLineOnly || !bootLibraries.contains(entry.getName());
                    }
                });

                if (!serverDependencies.isEmpty()) {
                    outputStream.putNextEntry(new JarEntry(bootDirectory));

                    for (final Artifact artifact : serverDependencies) {
                        final File dependency = artifact.getFile();

                        if (!dependency.exists()) {
                            throw new MojoExecutionException(String.format("Dependency %s not found (tried: %s)", artifact, dependency));
                        }

                        outputStream.putNextEntry(new JarEntry(bootDirectory + dependency.getName()));
                        Streams.copy(new FileInputStream(dependency), outputStream, buffer, true, false);
                    }
                }
            } finally {
                try {
                    outputStream.close();
                } catch (final IOException ignored) {
                    // ignored
                }
            }

            DependenciesSupport.saveArtifact(project, file, finalName, classifier, DependenciesSupport.WAR_TYPE, log);
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

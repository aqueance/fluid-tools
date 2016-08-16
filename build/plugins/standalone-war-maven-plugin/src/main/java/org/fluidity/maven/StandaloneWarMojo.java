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
import org.fluidity.deployment.maven.Logger;
import org.fluidity.foundation.Archives;
import org.fluidity.foundation.Streams;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Adds code to the project WAR file that allows it to be run as a JAR file; e.g. <code>$ java -jar &lt;file name&gt;.war</code>. More WAR files can be
 * specified in the command line and all will be deployed to the same application server.
 * <p>
 * <b>Technical Details</b>
 * <p>
 * Three different set of libraries are managed: bootstrap dependencies i.e., classes in the JAR/WAR root for <code>java -jar</code> to see, server
 * dependencies i.e., JARs needed to boot up the HTTP server, and application dependencies i.e., JARs in WEB-INF/lib.
 * <p>
 * Bootstrap dependencies come from the transitive non-optional run-time dependencies of this plugin itself.
 * <p>
 * Server dependencies come from the transitive non-optional run-time dependencies declared for the plugin in the host project's POM and are copied to
 * WEB-INF/boot.
 * <p>
 * The list of JARs in WEB-INF/lib is already in the WAR file itself.
 * <p>
 * There are various set operations performed in these dependencies to make sure nothing is included that shouldn't.
 *
 * @author Tibor Varga
 */
@Mojo(name = "standalone", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public final class StandaloneWarMojo extends AbstractMojo {

    /**
     * Instructs the plugin, when set, to remove from the WEB-INF/lib directory all .jar files that the plugin puts in the WEB-INF/boot directory, effectively
     * making the resulting WAR smaller than otherwise but also making it executable via the command line only, i.e. the WAR file will not be deployable in an
     * ordinary web container.
     */
    @Parameter(defaultValue = "false")
    private boolean commandLineOnly;

    /**
     * Instructs the plugin, when set, to create a new JAR with the given classifier and attach it to the project. When not set, the project's JAR artifact
     * is overwritten.
     */
    @Parameter
    private String classifier;

    /**
     * Tells the plugin to emit details about its operation. The default value of this parameter is <code>false</code>.
     */
    @Parameter(property = "fluidity.maven.verbose")
    private boolean verbose;

    /**
     * The location of the compiled classes.
     */
    @Parameter(property = "project.build.directory", required = true, readonly = true)
    private File outputDirectory;

    /**
     * The project artifact file name.

     */
    @Parameter(property = "standalone.archive.name", defaultValue = "${project.build.finalName}.war", required = true, readonly = true)
    private String archiveName;

    /**
     * The project artifact's final name.
     */
    @Parameter(property = "project.build.finalName", required = true, readonly = true)
    private String finalName;

    /**
     * The plugin's group ID.
     */
    @Parameter(property = "plugin.groupId", required = true, readonly = true)
    private String pluginGroupId;

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
    private RepositorySystemSession session;

    @Component
    private ArchivesSupport archives;

    @Component
    private DependenciesSupport dependencies;

    public void execute() throws MojoExecutionException {
        final File packageFile = new File(outputDirectory, archiveName);

        if (!packageFile.exists()) {
            throw new MojoExecutionException(String.format("%s does not exist", packageFile));
        }

        final List<RemoteRepository> repositories = project.getRemoteProjectRepositories();

        final String pluginKey = Plugin.constructKey(pluginGroupId, pluginArtifactId);
        final Artifact pluginArtifact = project.getPluginArtifactMap().get(pluginKey);

        final Collection<Artifact> pluginDependencies = dependencies.dependencyClosure(session, repositories, pluginArtifact, false, false, null);
        final Artifact handlerDependency = dependencies.artifact(WebApplicationBootstrap.class, pluginDependencies);
        assert handlerDependency != null : WebApplicationBootstrap.class;

        final Collection<Artifact> bootstrapDependencies = dependencies.dependencyClosure(session, repositories, handlerDependency, false, false, null);

        final Set<Artifact> serverDependencies = new HashSet<>();

        for (final Dependency dependency : project.getPlugin(pluginKey).getDependencies()) {
            assert !dependency.isOptional() : dependency;
            serverDependencies.addAll(dependencies.dependencyClosure(session, repositories, dependencies.dependencyArtifact(dependency), false, false, dependency.getExclusions()));
        }

        serverDependencies.removeAll(dependencies.dependencyClosure(session, repositories, project.getArtifact(), false, false, null));
        serverDependencies.removeAll(pluginDependencies);
        serverDependencies.remove(pluginArtifact);

        final Logger log = Logger.initialize(getLog(), verbose);

        try {
            final byte buffer[] = new byte[16384];

            final AtomicReference<String> mainClass = new AtomicReference<>();
            final Map<String, Attributes> attributesMap = new HashMap<>();
            final Map<String, String[]> providerMap = new HashMap<>();

            archives.load(attributesMap, providerMap, buffer, log, new ArchivesSupport.Feed() {
                private final Iterator<Artifact> iterator = bootstrapDependencies.iterator();

                public File next() throws IOException {
                    if (iterator.hasNext()) {
                        final File file = iterator.next().getFile();
                        mainClass.compareAndSet(null, Archives.attributes(file.toURI().toURL(), false, Attributes.Name.MAIN_CLASS.toString())[0]);
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
            try (final JarOutputStream output = new JarOutputStream(new FileOutputStream(file))) {
                if (mainClass.get() == null) {
                    throw new MojoExecutionException(String.format("None of the following dependencies specified a main class (manifest entry '%s'): %s",
                                                                   Attributes.Name.MAIN_CLASS,
                                                                   bootstrapDependencies));
                }

                final Set<String> bootLibraries = new HashSet<>();

                final String libDirectory = Archives.WEB_INF.concat("/lib/");
                for (final Artifact artifact : serverDependencies) {
                    bootLibraries.add(libDirectory.concat(artifact.getFile().getName()));
                }

                final String bootDirectory = Archives.WEB_INF.concat("/boot/");

                final Manifest manifest = Archives.manifest(packageFile.toURI().toURL(), false);
                final Attributes mainAttributes = manifest.getMainAttributes();

                if (mainAttributes.getValue(Attributes.Name.MAIN_CLASS) != null) {
                    throw new MojoExecutionException(String.format("Manifest already contains %s: %s",
                                                                   Attributes.Name.MAIN_CLASS,
                                                                   mainAttributes.getValue(Attributes.Name.MAIN_CLASS)));
                }

                mainAttributes.putValue(Attributes.Name.MAIN_CLASS.toString(), mainClass.get());

                archives.include(attributesMap, manifest);

                output.putNextEntry(new JarEntry(JarFile.MANIFEST_NAME));
                manifest.write(output);

                archives.expand(output, buffer, providerMap, new ArchivesSupport.Feed() {
                    private final Iterator<Artifact> iterator = bootstrapDependencies.iterator();
                    private final AtomicReference<Boolean> last = new AtomicReference<>(false);

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
                        final String name = entry.getName();
                        return !name.startsWith(bootDirectory) && (!commandLineOnly || !bootLibraries.contains(name));
                    }
                });

                if (!serverDependencies.isEmpty()) {
                    for (final Artifact artifact : serverDependencies) {
                        final File dependency = artifact.getFile();

                        if (!dependency.exists()) {
                            throw new MojoExecutionException(String.format("Dependency %s not found (tried: %s)", artifact, dependency));
                        }

                        output.putNextEntry(new JarEntry(bootDirectory + dependency.getName()));

                        try (final InputStream input = new FileInputStream(dependency)) {
                            Streams.pipe(input, output, buffer);
                        }
                    }
                }
            }

            dependencies.saveArtifact(project, file, String.format("%s/%s", outputDirectory, finalName), classifier, DependenciesSupport.WAR_TYPE, log);
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

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.fluidity.foundation.ServiceProviders;
import org.fluidity.foundation.Streams;
import org.fluidity.foundation.Strings;

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
import org.sonatype.aether.util.DefaultRepositorySystemSession;

/**
 * Packages all transitive dependencies of the project to its JAR artifact. This plugin uses an implementation of the {@link JarManifest} interface, found as a
 * JAR service provider, to process the JAR's manifest attributes.
 *
 * @author Tibor Varga
 * @goal package
 * @phase package
 * @threadSafe
 */
@SuppressWarnings("UnusedDeclaration")
public class StandaloneJarMojo extends AbstractMojo {

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
     * @parameter expression="${project.build.directory}/${project.build.finalName}.jar"
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
    private List<RemoteRepository> repositories;

    public void execute() throws MojoExecutionException {
        if (!packageFile.exists()) {
            throw new MojoExecutionException(String.format("%s does not exist", packageFile));
        }

        final Log log = getLog();

        final List<JarManifest> handlers = ServiceProviders.findInstances(JarManifest.class, getClass().getClassLoader());

        if (handlers.isEmpty()) {
            throw new MojoExecutionException(String.format("No %s implementation found", JarManifest.class.getName()));
        }

        final Collection<Artifact> compileDependencies = DependenciesSupport.compileDependencies(repositorySystem, repositorySession, repositories, project, true);
        final Collection<Artifact> runtimeDependencies = DependenciesSupport.runtimeDependencies(repositorySystem, repositorySession, repositories, project, true);

        // keep only JAR artifacts
        for (final Iterator<Artifact> i = runtimeDependencies.iterator(); i.hasNext();) {
            final Artifact artifact = i.next();

            if (!artifact.getType().equals(DependenciesSupport.JAR_TYPE)) {
                i.remove();
            }
        }

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

                final Attributes mainAttributes = manifest.getMainAttributes();

                if (mainAttributes.get(Attributes.Name.CLASS_PATH) != null) {
                    throw new MojoExecutionException(String.format("Manifest contains %s", Attributes.Name.CLASS_PATH));
                }

                final String dependencyPath = Archives.META_INF.concat("/dependencies/");
                final List<String> dependencyList = new ArrayList<String>();

                for (final Artifact artifact : runtimeDependencies) {
                    final File dependency = artifact.getFile();

                    if (!dependency.exists()) {
                        throw new MojoExecutionException(String.format("Dependency %s not found (tried: %s)", artifact, dependency));
                    }

                    dependencyList.add(dependencyPath.concat(dependency.getName()));
                }

                final Collection<Artifact> unpackedDependencies = new HashSet<Artifact>();
                final Map<String, Collection<Artifact>> includedDependencies = new HashMap<String, Collection<Artifact>>();

                /*
                 * Manifest handlers use profiles to declare dependencies to include, exclude and unpack in our standalone artifact.
                 * These profiles are turned off by the presence of packaging properties.
                 * Thus we need to manipulate the properties when doing dependency traversal to turn these profiles on and off.
                 */

                final Map<String, String> systemProperties = clean(repositorySession.getSystemProperties());
                final Map<String, String> userProperties = clean(repositorySession.getUserProperties());
                final Map<String, Object> configProperties = clean(repositorySession.getConfigProperties());

                final DefaultRepositorySystemSession included = new DefaultRepositorySystemSession(repositorySession);
                included.setSystemProperties(include(JarManifest.Packaging.INCLUDE, systemProperties));
                included.setUserProperties(userProperties);
                included.setConfigProperties(configProperties);

                final DefaultRepositorySystemSession unpacked = new DefaultRepositorySystemSession(repositorySession);
                unpacked.setSystemProperties(include(JarManifest.Packaging.UNPACK, systemProperties));
                unpacked.setUserProperties(userProperties);
                unpacked.setConfigProperties(configProperties);

                final String pluginKey = Plugin.constructKey(pluginGroupId, pluginArtifactId);
                final Collection<Dependency> pluginDependencies = project.getPlugin(pluginKey).getDependencies();
                final Artifact pluginArtifact = project.getPluginArtifactMap().get(pluginKey);

                for (final JarManifest handler : handlers) {
                    final Class<? extends JarManifest> handlerClass = handler.getClass();

                    final boolean runtime = handler.needsCompileDependencies();
                    final JarManifest.Packaging packaging = handler.packaging();

                    final Collection<Artifact> dependencies = runtime ? compileDependencies : new HashSet<Artifact>(runtimeDependencies);

                    final Artifact handlerArtifact = DependenciesSupport.dependencyArtifact(DependenciesSupport.dependency(handlerClass, pluginDependencies));
                    final Collection<Artifact> unpackedClosure = DependenciesSupport.dependencyClosure(repositorySystem, unpacked, repositories, handlerArtifact, false, false, null);
                    final Collection<Artifact> includedClosure = DependenciesSupport.dependencyClosure(repositorySystem, included, repositories, handlerArtifact, false, false, null);

                    if (packaging != JarManifest.Packaging.UNPACK) {
                        unpackedClosure.remove(handlerArtifact);
                    }

                    if (packaging != JarManifest.Packaging.INCLUDE) {
                        includedClosure.remove(handlerArtifact);
                    }

                    unpackedDependencies.addAll(unpackedClosure);

                    if (!includedClosure.isEmpty()) {
                        final String directory = handler.dependencyPath();

                        if (directory != null && directory.contains("/")) {
                            throw new MojoExecutionException(String.format("Directory name '%s' returned by %s must not contain '/'", directory, handlerClass));
                        }

                        final String path = directory == null ? dependencyPath : String.format("%s%s/", dependencyPath, directory);

                        add(includedDependencies, path, includedClosure);

                        for (final Artifact artifact : includedClosure) {
                            final File dependency = artifact.getFile();

                            if (!dependency.exists()) {
                                throw new MojoExecutionException(String.format("Dependency %s not found (tried: %s)", artifact, dependency));
                            }

                            if (runtime && !dependencies.contains(artifact)) {
                                dependencyList.add(path.concat(dependency.getName()));
                            }
                        }

                        if (runtime) {
                            dependencies.addAll(includedClosure);
                        }
                    }

                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Invoking manifest handler: %s", Strings.printObject(false, handler)));
                        log.debug(String.format("Packaged dependencies: %s", dependencies));
                    }

                    handler.processManifest(project, mainAttributes, dependencyList, dependencies);
                }

                final byte[] buffer = new byte[1024 * 1024];

                final Map<String, Attributes> attributesMap = ArchivesSupport.expand(outputStream, buffer, log, new ArchivesSupport.Feed() {
                    private final Iterator<Artifact> iterator = unpackedDependencies.iterator();

                    public JarFile next() throws IOException {
                        if (iterator.hasNext()) {
                            return new JarFile(iterator.next().getFile());
                        } else {
                            return null;
                        }
                    }
                });

                ArchivesSupport.include(attributesMap, manifest);

                // create the new manifest
                outputStream.putNextEntry(new JarEntry(JarFile.MANIFEST_NAME));
                manifest.write(outputStream);

                add(includedDependencies, dependencyPath, runtimeDependencies);

                final String projectId = project.getArtifact().getId();

                // copy the dependencies, including the original project artifact and those requested by manifest handlers
                for (final Map.Entry<String, Collection<Artifact>> entry : includedDependencies.entrySet()) {
                    final String path = entry.getKey();
                    final Collection<Artifact> list = entry.getValue();

                    outputStream.putNextEntry(new JarEntry(path));

                    for (final Artifact artifact : list) {
                        final File dependency = artifact.getFile();

                        if (dependency.isDirectory()) {
                            log.warn(String.format("Ignoring non-JAR dependency %s", dependency));
                        } else {
                            final String entryName = path.concat(dependency.getName());
                            outputStream.putNextEntry(new JarEntry(entryName));

                            if (artifact.getId().equals(projectId)) {

                                // got to check if our project artifact is something we have created in a previous run
                                // i.e., if it contains the project artifact we're about to copy
                                int processed = Archives.readEntries(dependency.toURI().toURL(), new Archives.Entry() {
                                    public boolean matches(final JarEntry entry) throws IOException {
                                        return entryName.equals(entry.getName());
                                    }

                                    public boolean read(final JarEntry entry, final InputStream stream) throws IOException {
                                        Streams.copy(stream, outputStream, buffer, false);
                                        return false;
                                    }
                                });

                                if (processed > 0) {
                                    continue;
                                }
                            }

                            Streams.copy(new FileInputStream(dependency), outputStream, buffer, false);
                        }
                    }
                }
            } finally {
                try {
                    outputStream.close();
                } catch (final IOException ignored) {
                    // ignored
                }
            }

            DependenciesSupport.saveArtifact(project, file, finalName, classifier, DependenciesSupport.JAR_TYPE, log);
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

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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.fluidity.deployment.maven.ArchivesSupport;
import org.fluidity.deployment.maven.DependenciesSupport;
import org.fluidity.deployment.plugin.spi.JarManifest;
import org.fluidity.deployment.plugin.spi.SecurityPolicy;
import org.fluidity.foundation.Archives;
import org.fluidity.foundation.Lists;
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
 * @goal standalone
 * @phase package
 * @threadSafe
 */
@SuppressWarnings("UnusedDeclaration")
public final class StandaloneJarMojo extends AbstractMojo {

    /**
     * Instructs the plugin, when set, to create a new JAR with the given classifier and attach it to the project. When not set, the project's JAR artifact
     * is overwritten.
     *
     * @parameter default-value=""
     */
    @SuppressWarnings("UnusedDeclaration")
    private String classifier;

    /**
     * Tells the plugin to remove from the list of archived packaged in the main one those that it unpacks into the root of the main archive. In essence,
     * if the packaged archives are used with a class loader that delegates to the launch class loader, you should set this to <code>true</code>.
     *
     * @parameter default-value="false"
     */
    private boolean compact;

    /**
     * Tells the plugin to make an executable application. An executable application has some launcher unpacked in its root. The default value of this
     * parameter is <code>true</code>. Unless you are making an archive that requires a separate launcher, leave this parameter at its default.
     *
     * @parameter default-value="true"
     */
    private boolean executable;

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

        if (handlers.size() > 1) {
            throw new MojoExecutionException(String.format("Multiple %s implementations found", JarManifest.class.getName()));
        }

        try {
            final Manifest manifest = Archives.manifest(false, packageFile.toURI().toURL());

            if (Archives.Nested.list(manifest).length > 0) {
                throw new MojoExecutionException(String.format("Will not embed project artifact as it contains custom nested dependencies; configure the 'include' goal to follow this one"));
            }

            final Attributes mainAttributes = manifest.getMainAttributes();

            if (mainAttributes.get(Attributes.Name.CLASS_PATH) != null) {
                throw new MojoExecutionException(String.format("Manifest contains %s", Attributes.Name.CLASS_PATH));
            }

            final File file = createTempFile();
            final JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(file));

            final Collection<Artifact> compileDependencies = DependenciesSupport.compileDependencies(repositorySystem, repositorySession, repositories, project, true);
            final Collection<Artifact> runtimeDependencies = DependenciesSupport.runtimeDependencies(repositorySystem, repositorySession, repositories, project, false);

            final String dependencyPath = Archives.META_INF.concat("/dependencies/");

            try {

                /*
                 * Manifest handlers use profiles to declare dependencies to include, exclude, or unpack in our standalone artifact.
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

                final String original = mainAttributes.getValue(JarManifest.CREATED_BY);
                mainAttributes.putValue(JarManifest.CREATED_BY,
                                        original == null
                                        ? JarManifest.FRAMEWORK_ID
                                        : original.contains(JarManifest.FRAMEWORK_ID) ? original : String.format("%s, %s", JarManifest.FRAMEWORK_ID, original));

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Packaged dependencies: %s", runtimeDependencies));
                }

                final Map<String, Inclusion> includedDependencies = new HashMap<String, Inclusion>();
                final Collection<Artifact> unpackedDependencies = new HashSet<Artifact>();

                final byte[] buffer = new byte[16384];

                SecurityPolicy policy = new JavaSecurityPolicy(2, false, buffer, packageFile, log);

                final AtomicReference<String> dependenciesName = new AtomicReference<String>();

                if (!handlers.isEmpty()) {
                    final JarManifest handler = handlers.get(0);
                    final Class<?> handlerClass = handler.getClass();

                    final Artifact handlerArtifact = DependenciesSupport.dependencyArtifact(DependenciesSupport.dependency(handlerClass, pluginDependencies));
                    final Collection<Artifact> includedClosure = DependenciesSupport.dependencyClosure(repositorySystem, included, repositories, handlerArtifact, false, false, null);
                    includedClosure.remove(handlerArtifact);

                    if (executable) {
                        final Collection<Artifact> unpackedClosure = DependenciesSupport.dependencyClosure(repositorySystem, unpacked, repositories, handlerArtifact, false, false, null);
                        unpackedClosure.remove(handlerArtifact);
                        unpackedDependencies.addAll(unpackedClosure);
                    }

                    if (!includedClosure.isEmpty()) {
                        for (final Artifact artifact : includedClosure) {
                            final File dependency = artifact.getFile();

                            if (!dependency.exists()) {
                                throw new MojoExecutionException(String.format("Dependency %s not found (tried: %s)", artifact, dependency));
                            }
                        }
                    }

                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Invoking manifest handler %s", Strings.formatObject(false, true, handler)));
                    }

                    final AtomicBoolean inclusionNameSet = new AtomicBoolean();

                    final SecurityPolicy update = handler.processManifest(project, mainAttributes, policy, log, new JarManifest.Dependencies() {
                        public boolean unpacked() {
                            return executable;
                        }

                        public void attribute(final String name, final String delimiter) throws MojoExecutionException {
                            if (dependenciesName.compareAndSet(null, name)) {
                                includedDependencies.put(name, new Inclusion(dependencyPath, runtimeDependencies, delimiter));
                            } else {
                                throw new MojoExecutionException(String.format("Secondary manifest handler %s may not set the dependencies attribute",
                                                                               Strings.formatObject(false, true, handler)));
                            }
                        }

                        public Collection<Artifact> runtime() {
                            return Collections.unmodifiableCollection(runtimeDependencies);
                        }

                        public Collection<Artifact> compiler() {
                            return Collections.unmodifiableCollection(compileDependencies);
                        }

                        public void include(final String name) throws MojoExecutionException {
                            if (inclusionNameSet.compareAndSet(false, true)) {
                                assert name != null : Strings.formatObject(false, true, handler);

                                if (name.contains("/")) {
                                    throw new MojoExecutionException(String.format("Directory name '%s' returned by %s must not contain '/'",
                                                                                   name,
                                                                                   Strings.formatObject(false, true, handler)));
                                }

                                final String attribute = Archives.Nested.attribute(name);
                                if (includedDependencies.containsKey(attribute)) {
                                    throw new MojoExecutionException(String.format("Dependencies '%s' already included in the archive", name));
                                }

                                includedDependencies.put(attribute, new Inclusion(String.format("%s%s/", dependencyPath, name), includedClosure, " "));
                            } else {
                                throw new MojoExecutionException(String.format("Manifest handler %s tried to specify the inclusion name more than once",
                                                                               Strings.formatObject(false, true, handler)));
                            }
                        }
                    });

                    if (update != null) {
                        policy = update;
                    }

                    if (!includedClosure.isEmpty() && !inclusionNameSet.get()) {
                        throw new MojoExecutionException(String.format("Manifest handler %s failed to specify the inclusion name",
                                                                       Strings.formatObject(false, true, handler)));
                    }
                }

                final String dependencies = Archives.Nested.attribute(null);
                if (dependenciesName.compareAndSet(null, dependencies)) {
                    includedDependencies.put(dependencies, new Inclusion(dependencyPath, runtimeDependencies, " "));
                }

                final Map<String, Attributes> attributesMap = new HashMap<String, Attributes>();
                final Map<String, String[]> providerMap = new HashMap<String, String[]>();

                ArchivesSupport.load(attributesMap, providerMap, buffer, log, new DependencyFeed(policy, unpackedDependencies));
                ArchivesSupport.include(attributesMap, manifest);

                if (compact) {
                    includedDependencies.get(dependenciesName.get()).artifacts.removeAll(unpackedDependencies);
                }

                // list the various dependencies in manifest attributes
                for (final Map.Entry<String, Inclusion> entry : includedDependencies.entrySet()) {
                    final Inclusion inclusion = entry.getValue();
                    final List<String> dependencyList = new ArrayList<String>();

                    for (final Artifact artifact : inclusion.artifacts) {
                        final File dependency = artifact.getFile();

                        if (!dependency.exists()) {
                            throw new MojoExecutionException(String.format("Dependency %s not found (tried: %s)", artifact, dependency));
                        } else if (dependency.isDirectory()) {
                            log.warn(String.format("Ignoring non-JAR dependency %s", dependency));
                        } else {
                            final String entryName = inclusion.folder.concat(dependency.getName());

                            if (artifact.getType().equals(DependenciesSupport.JAR_TYPE)) {
                                dependencyList.add(entryName);
                                policy.add(artifact.getFile(), 1, inclusion.folder);
                            }
                        }
                    }

                    mainAttributes.putValue(entry.getKey(), Lists.delimited(inclusion.delimiter, dependencyList));
                }

                for (final Artifact artifact : unpackedDependencies) {
                    policy.add(artifact.getFile(), 0, null);
                }

                policy.update(new SecurityPolicy.Output() {
                    public void save(final String name, final String content) throws IOException {
                        if (content == null) {
                            mainAttributes.remove(new Attributes.Name(name));
                        } else {
                            mainAttributes.putValue(name, content);
                        }
                    }
                });

                // create the new manifest
                outputStream.putNextEntry(new JarEntry(JarFile.MANIFEST_NAME));
                manifest.write(outputStream);

                policy.save(new SecurityPolicy.Output() {
                    public void save(final String name, final String content) throws IOException {
                        outputStream.putNextEntry(new JarEntry(name));
                        Streams.store(outputStream, content, "UTF-8", buffer, false);
                    }
                });

                ArchivesSupport.expand(outputStream, buffer, providerMap, new DependencyFeed(policy, unpackedDependencies));

                final String projectId = project.getArtifact().getId();

                // copy the dependencies, including the original project artifact and those requested by manifest handlers
                for (final Map.Entry<String, Inclusion> entry : includedDependencies.entrySet()) {
                    final String name = entry.getKey();
                    final Inclusion inclusion = entry.getValue();

                    for (final Artifact artifact : inclusion.artifacts) {
                        final File dependency = artifact.getFile();

                        assert dependency.exists() : dependency;
                        assert !dependency.isDirectory() : dependency;

                        final String entryName = inclusion.folder.concat(dependency.getName());
                        outputStream.putNextEntry(new JarEntry(entryName));

                        if (artifact.getId().equals(projectId)) {

                            // got to check if our project artifact is something we have created in a previous run
                            // i.e., if it contains the project artifact we're about to copy
                            int processed = Archives.read(false, dependency.toURI().toURL(), new Archives.Entry() {
                                public boolean matches(final URL url, final JarEntry entry) throws IOException {
                                    return entryName.equals(entry.getName());
                                }

                                public boolean read(final URL url, final JarEntry entry, final InputStream stream) throws IOException {
                                    Streams.copy(stream, outputStream, buffer, true, false);
                                    return false;
                                }
                            });

                            if (processed > 0) {
                                continue;
                            }
                        }

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

    private static class Inclusion {

        public final String folder;
        public final Collection<Artifact> artifacts;
        public final String delimiter;

        private Inclusion(final String folder, final Collection<Artifact> artifacts, final String delimiter) {
            this.folder = folder;
            this.artifacts = artifacts;
            this.delimiter = delimiter;
        }
    }

    /**
     * @author Tibor Varga
     */
    private static class DependencyFeed implements ArchivesSupport.Feed {

        private final SecurityPolicy policy;
        private final Iterator<Artifact> iterator;
        private final Collection<String> excluded = new HashSet<String>();

        DependencyFeed(final SecurityPolicy policy, final Collection<Artifact> unpackedDependencies) throws IOException {
            this.policy = policy;
            this.iterator = unpackedDependencies.iterator();

            for (final Artifact artifact : unpackedDependencies) {
                final String name = policy.name(artifact.getFile());

                if (name != null) {
                    excluded.add(name);
                }
            }
        }

        public File next() throws IOException {
            if (iterator.hasNext()) {
                return iterator.next().getFile();
            } else {
                return null;
            }
        }

        public boolean include(final JarEntry entry) {
            return !excluded.contains(entry.getName());
        }
    }
}

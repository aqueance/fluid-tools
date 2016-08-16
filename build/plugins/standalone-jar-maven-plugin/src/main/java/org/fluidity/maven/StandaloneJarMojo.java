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
import org.fluidity.deployment.maven.Logger;
import org.fluidity.deployment.plugin.spi.JarManifest;
import org.fluidity.deployment.plugin.spi.SecurityPolicy;
import org.fluidity.foundation.Archives;
import org.fluidity.foundation.IOStreams;
import org.fluidity.foundation.Lists;
import org.fluidity.foundation.ServiceProviders;
import org.fluidity.foundation.Strings;

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
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Packages all transitive dependencies of the project to its JAR artifact. This plugin uses an implementation of the {@link JarManifest} interface, found as a
 * JAR service provider, to process the JAR's manifest attributes.
 *
 * @author Tibor Varga
 */
@Mojo(name = "standalone", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public final class StandaloneJarMojo extends AbstractMojo {

    /**
     * Instructs the plugin, when set, to create a new JAR with the given classifier and attach it to the project. When not set, the project's JAR artifact
     * is overwritten.
     */
    @Parameter
    private String classifier;

    /**
     * Tells the plugin to remove from the list of archived packaged in the main one those that it unpacks into the root of the main archive. In essence,
     * if the packaged archives are used with a class loader that delegates to the launch class loader, you should set this to <code>true</code>.
     */
    @Parameter(defaultValue = "false")
    private boolean compact;

    /**
     * Tells the plugin to make an executable application. An executable application has some launcher unpacked in its root. The default value of this
     * parameter is <code>true</code>. Unless you are making an archive that requires a separate launcher, leave this parameter at its default.
     */
    @Parameter(defaultValue = "true")
    private boolean executable;

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
    @Parameter(property = "standalone.archive.name", defaultValue = "${project.build.finalName}.jar", required = true, readonly = true)
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

        final Logger log = Logger.initialize(getLog(), verbose);

        final List<JarManifest> handlers = ServiceProviders.findInstances(JarManifest.class, getClass().getClassLoader());

        if (handlers.size() > 1) {
            throw new MojoExecutionException(String.format("Multiple %s implementations found", JarManifest.class.getName()));
        }

        final List<RemoteRepository> repositories = project.getRemoteProjectRepositories();

        try {
            final Manifest manifest = Archives.manifest(packageFile.toURI().toURL(), false);

            if (Archives.Nested.list(manifest).length > 0) {
                throw new MojoExecutionException(String.format("Will not embed project artifact as it contains custom nested dependencies; configure the '%s' goal to follow this one", IncludeJarsMojo.name));
            }

            final Attributes mainAttributes = manifest.getMainAttributes();

            if (mainAttributes.get(Attributes.Name.CLASS_PATH) != null) {
                throw new MojoExecutionException(String.format("Manifest contains %s", Attributes.Name.CLASS_PATH));
            }

            final File file = createTempFile();
            try (final JarOutputStream output = new JarOutputStream(new FileOutputStream(file))) {

                final String dependencyPath = Archives.META_INF.concat("/dependencies/");
                final Collection<Artifact> compileDependencies = dependencies.compileDependencies(session, repositories, project, true);
                final Collection<Artifact> runtimeDependencies = dependencies.runtimeDependencies(session, repositories, project, false);

                /*
                 * Manifest handlers use profiles to declare dependencies to include, exclude, or unpack in our standalone artifact.
                 * These profiles are turned off by the presence of packaging properties.
                 * Thus we need to manipulate the properties when doing dependency traversal to turn these profiles on and off.
                 */

                final Map<String, String> systemProperties = clean(session.getSystemProperties());
                final Map<String, String> userProperties = clean(session.getUserProperties());
                final Map<String, Object> configProperties = clean(session.getConfigProperties());

                final DefaultRepositorySystemSession included = new DefaultRepositorySystemSession(session);
                included.setUserProperties(include(JarManifest.Packaging.INCLUDE, userProperties));
                included.setSystemProperties(systemProperties);
                included.setConfigProperties(configProperties);

                final DefaultRepositorySystemSession unpacked = new DefaultRepositorySystemSession(session);
                unpacked.setUserProperties(include(JarManifest.Packaging.UNPACK, userProperties));
                unpacked.setSystemProperties(systemProperties);
                unpacked.setConfigProperties(configProperties);

                final String pluginKey = Plugin.constructKey(pluginGroupId, pluginArtifactId);
                final Collection<Dependency> pluginDependencies = project.getPlugin(pluginKey).getDependencies();

                final String original = mainAttributes.getValue(JarManifest.CREATED_BY);
                mainAttributes.putValue(JarManifest.CREATED_BY,
                                        original == null
                                        ? JarManifest.FRAMEWORK_ID
                                        : original.contains(JarManifest.FRAMEWORK_ID) ? original : String.format("%s, %s", JarManifest.FRAMEWORK_ID, original));

                final Map<String, Inclusion> includedDependencies = new HashMap<>();
                final Collection<Artifact> unpackedDependencies = new HashSet<>();

                final byte[] buffer = new byte[16384];

                SecurityPolicy policy = new JavaSecurityPolicy(2, false, buffer, packageFile, log);

                final AtomicReference<String> dependenciesName = new AtomicReference<>();

                if (!handlers.isEmpty()) {
                    final JarManifest handler = handlers.get(0);
                    final Class<?> handlerClass = handler.getClass();

                    final Artifact handlerArtifact = dependencies.resolve(session, repositories, dependencies.dependency(handlerClass, pluginDependencies));

                    final Collection<Artifact> includedClosure = dependencies.dependencyClosure(included, repositories, handlerArtifact, false, false, null);
                    includedClosure.remove(handlerArtifact);

                    if (executable) {
                        final Collection<Artifact> unpackedClosure = dependencies.dependencyClosure(unpacked, repositories, handlerArtifact, false, false, null);
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

                    if (log.active()) {
                        log.detail("Manifest handler %s", Archives.containing(handlerClass));
                    }

                    final AtomicBoolean inclusionNameSet = new AtomicBoolean();

                    final SecurityPolicy update = handler.processManifest(project, mainAttributes, policy, log, new JarManifest.Dependencies() {
                        public boolean unpacked() {
                            return executable;
                        }

                        public void attribute(final String name, final String delimiter) throws MojoExecutionException {
                            if (name == null) {
                                throw new MojoExecutionException(String.format("Dependencies attribute must have a name when explicitly set (%s)",
                                                                               Strings.formatObject(false, true, handler)));
                            }

                            if (dependenciesName.compareAndSet(null, name)) {
                                includedDependencies.put(name, new Inclusion(dependencyPath, runtimeDependencies, delimiter));
                            } else {
                                throw new MojoExecutionException(String.format("Dependencies attribute can only be set once (%s)",
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

                                if (log.active()) {
                                    log.detail("Included archives '%s':", name);
                                    dependencies.list(includedClosure, "  ", log);
                                }
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
                        throw new MojoExecutionException(String.format("Manifest handler %s failed to specify the name to include dependencies under",
                                                                       Strings.formatObject(false, true, handler)));
                    }
                }

                final String dependencies = Archives.Nested.attribute(null);
                if (dependenciesName.compareAndSet(null, dependencies)) {
                    includedDependencies.put(dependencies, new Inclusion(dependencyPath, runtimeDependencies, " "));
                }

                final Map<String, Attributes> attributesMap = new HashMap<>();
                final Map<String, String[]> providerMap = new HashMap<>();

                archives.load(attributesMap, providerMap, buffer, log, new DependencyFeed(policy, unpackedDependencies));
                archives.include(attributesMap, manifest);

                if (compact) {
                    includedDependencies.get(dependenciesName.get()).artifacts.removeAll(unpackedDependencies);
                }

                if (log.active()) {
                    log.detail("Dependency archives:");
                    this.dependencies.list(includedDependencies.get(dependenciesName.get()).artifacts, "  ", log);

                    log.detail("Unpacked archives:");
                    this.dependencies.list(unpackedDependencies, "  ", log);
                }

                // list the various dependencies in manifest attributes
                for (final Map.Entry<String, Inclusion> entry : includedDependencies.entrySet()) {
                    final Inclusion inclusion = entry.getValue();
                    final List<String> dependencyList = new ArrayList<>();

                    for (final Artifact artifact : inclusion.artifacts) {
                        final File dependency = artifact.getFile();

                        if (!dependency.exists()) {
                            throw new MojoExecutionException(String.format("Dependency %s not found (tried: %s)", artifact, dependency));
                        } else if (dependency.isDirectory()) {
                            log.warn("Ignoring non-JAR dependency %s", dependency);
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

                policy.update((name, content) -> {
                    if (content == null) {
                        mainAttributes.remove(new Attributes.Name(name));
                    } else {
                        mainAttributes.putValue(name, content);
                    }
                });

                // create the new manifest
                output.putNextEntry(new JarEntry(JarFile.MANIFEST_NAME));
                manifest.write(output);

                policy.save((name, content) -> {
                    output.putNextEntry(new JarEntry(name));
                    IOStreams.store(output, content, Strings.UTF_8, buffer);
                });

                archives.expand(output, buffer, providerMap, new DependencyFeed(policy, unpackedDependencies));

                final String projectId = project.getArtifact().getId();

                // copy the dependencies, including the original project artifact and those requested by manifest handlers
                for (final Inclusion inclusion : includedDependencies.values()) {
                    for (final Artifact artifact : inclusion.artifacts) {
                        final File dependency = artifact.getFile();

                        assert dependency.exists() : dependency;
                        assert !dependency.isDirectory() : dependency;

                        final String entryName = inclusion.folder.concat(dependency.getName());
                        output.putNextEntry(new JarEntry(entryName));

                        if (artifact.getId().equals(projectId)) {
                            final URL url = dependency.toURI().toURL();

                            // got to check if our project artifact is something we have created in a previous run
                            // i.e., if it contains the project artifact we're about to copy
                            final int processed = Archives.read(url, false, (_url, _entry) -> !entryName.equals(_entry.getName()) ? null : (__url, __entry, input) -> {
                                IOStreams.pipe(input, output, buffer);
                                return false;
                            });

                            if (processed > 0) {
                                continue;
                            }
                        }

                        try (final InputStream input = new FileInputStream(dependency)) {
                            IOStreams.pipe(input, output, buffer);
                        }
                    }
                }
            }

            dependencies.saveArtifact(project, file, String.format("%s/%s", outputDirectory, finalName), classifier, DependenciesSupport.JAR_TYPE, log);
        } catch (final IOException e) {
            throw new MojoExecutionException(String.format("Processing %s", packageFile), e);
        }
    }

    private <V> Map<String, V> clean(final Map<String, V> properties) {
        final Map<String, V> copy = new HashMap<>(properties);

        for (final JarManifest.Packaging packaging : JarManifest.Packaging.values()) {
            copy.remove(packaging.profile);
        }

        return copy;
    }

    private Map<String, String> include(final JarManifest.Packaging enabled, final Map<String, String> properties) {
        final Map<String, String> copy = new HashMap<>(properties);

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

        final String folder;
        final Collection<Artifact> artifacts;
        final String delimiter;

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

        private final Iterator<Artifact> iterator;
        private final Collection<String> excluded = new HashSet<>();

        DependencyFeed(final SecurityPolicy policy, final Collection<Artifact> unpackedDependencies) throws IOException {
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

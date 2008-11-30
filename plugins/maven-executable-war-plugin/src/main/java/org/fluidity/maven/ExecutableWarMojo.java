/*
 * Copyright (c) 2006-2008 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Softweare"), to deal
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.fluidity.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeResolutionListener;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;
import org.codehaus.plexus.logging.AbstractLogger;
import org.codehaus.plexus.logging.Logger;


/**
 * Adds code to the project .war file that allows it to be run as a .jar file, e.g. <code>$ java -jar &lt;file name>.war</code>.
 *
 * @goal package
 * @phase package
 */
public class ExecutableWarMojo extends AbstractMojo {

    private static final String BOOTSTRAP_MODULE_KEY = "org.fluidity.maven:war-bootstrap";
    private static final String DEFAULT_SERVER_KEY = "org.fluidity.maven:jetty-bootstrap";
    private static final String WAR_TYPE = "war";

    /**
     * The location of the compiled classes.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     * @readonly
     */
    @SuppressWarnings({"UnusedDeclaration"})
    private File outputDirectory;

    /**
     * The location of the compiled classes.
     *
     * @parameter expression="${project.build.directory}/${project.build.finalName}.${project.packaging}"
     * @required
     * @readonly
     */
    @SuppressWarnings({"UnusedDeclaration"})
    private File packageFile;

    /**
     * Identifies in the format groupId:artifactId the dependency that contains the server bootstrap classes. Dependencies of the identified dependency will be
     * copied under the WEB-INF/boot directory of the .war file.
     *
     * @parameter
     * @readonly
     */
    @SuppressWarnings({"UnusedDeclaration"})
    private String server;

    /**
     * Specifies the HTTP port to listen on.
     *
     * @parameter
     * @readonly
     */
    @SuppressWarnings({"UnusedDeclaration"})
    private int httpPort;

    /**
     * Name of the maven project
     *
     * @parameter expression="${plugin.artifactId}"
     * @required
     */
    @SuppressWarnings({"UnusedDeclaration"})
    private String pluginName;

    /**
     * @parameter expression="${plugin.artifacts}"
     * @required
     */
    @SuppressWarnings({"UnusedDeclaration", "MismatchedQueryAndUpdateOfCollection"})
    private List<Artifact> pluginArtifacts;

    /**
     * @parameter expression="${plugin.artifactMap}"
     * @required
     */
    @SuppressWarnings({"UnusedDeclaration", "MismatchedQueryAndUpdateOfCollection"})
    private Map<String, Artifact> pluginArtifactMap;

    /**
     * @parameter expression="${plugin.groupId}"
     * @required
     */
    @SuppressWarnings({"UnusedDeclaration"})
    private String pluginGroupId;

    /**
     * @parameter expression="${plugin.artifactId}"
     * @required
     */
    @SuppressWarnings({"UnusedDeclaration"})
    private String pluginArtifactId;

    /**
     * @parameter expression="${plugin.version}"
     * @required
     */
    @SuppressWarnings({"UnusedDeclaration"})
    private String pluginVersion;

    /**
     * Packaging type of the artifact to be installed. Retrieved from POM file if specified
     *
     * @parameter expression="${project.packaging}"
     */
    @SuppressWarnings({"UnusedDeclaration"})
    private String packaging;

    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    @SuppressWarnings({"UnusedDeclaration"})
    private MavenProject project;

    /**
     * The artifact collector to use.
     *
     * @component
     * @required
     * @readonly
     */
    @SuppressWarnings({"UnusedDeclaration"})
    private ArtifactCollector artifactCollector;

    /**
     * The artifact repository to use.
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    @SuppressWarnings({"UnusedDeclaration"})
    private ArtifactRepository localRepository;

    /**
     * The artifact factory to use.
     *
     * @component
     * @required
     * @readonly
     */
    @SuppressWarnings({"UnusedDeclaration"})
    private ArtifactFactory artifactFactory;

    /**
     * The artifact metadata source to use.
     *
     * @component
     * @required
     * @readonly
     */
    @SuppressWarnings({"UnusedDeclaration"})
    private ArtifactMetadataSource artifactMetadataSource;

//    private final Log log = getLog();

    @SuppressWarnings({"unchecked"})
    public void execute() throws MojoExecutionException {
        if (!WAR_TYPE.equals(packaging)) {
            throw new MojoExecutionException("This is not a .war project");
        } else if (!packageFile.exists()) {
            throw new MojoExecutionException(packageFile + " does not exist");
        }

        if (server == null) {
            server = DEFAULT_SERVER_KEY;
        }

        final DependencyNode dependencyRootNode;
        try {
            dependencyRootNode = calculateDependencyGraph(findPluginArtifact(), project.getRemoteArtifactRepositories());
        } catch (final ArtifactResolutionException e) {
            throw new MojoExecutionException("Cannot calculate dependency graph", e);
        }

        final Artifact bootstrap = findBootstrapDependency();

        final Collection<Artifact> bootstrapDependencies = addTransitiveDependencies(Collections.singleton(bootstrap), dependencyRootNode);
        final Collection<Artifact> serverDependencies = findDependencies(findPluginArtifact(), dependencyRootNode);

        serverDependencies.removeAll(bootstrapDependencies);

        final Set<String> processedEntries = new HashSet<String>();

        try {
            final File file = createTempFile();
            final JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(file));
            String mainClass = null;

            try {
                boolean manifestFound = false;
                final byte buffer[] = new byte[1024 * 16];

                for (final Artifact artifact : bootstrapDependencies) {
                    final JarFile jarInput = new JarFile(new File(localRepository.getBasedir(), localRepository.pathOf(artifact)));

                    if (mainClass == null) {
                        mainClass = (String) jarInput.getManifest().getMainAttributes().get(Attributes.Name.MAIN_CLASS);
                    }

                    try {
                        for (final Enumeration entries = jarInput.entries(); entries.hasMoreElements();) {
                            final JarEntry entry = (JarEntry) entries.nextElement();
                            final String entryName = entry.getName();

                            if (!processedEntries.contains(entryName)) {
                                if (!entryName.equals(JarFile.MANIFEST_NAME)) {
                                    outputStream.putNextEntry(entry);
                                    copyStream(outputStream, jarInput.getInputStream(entry), buffer);
                                    processedEntries.add(entryName);
                                }
                            } else if (!entryName.endsWith("/")) {
                                throw new MojoExecutionException("Duplicate entry: " + entryName);
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
                    throw new MojoExecutionException("No main class found in " + bootstrap);
                }

                final JarFile warInput = new JarFile(packageFile);

                try {
                    for (final Enumeration entries = warInput.entries(); entries.hasMoreElements();) {
                        final JarEntry entry = (JarEntry) entries.nextElement();
                        final String entryName = entry.getName();

                        if (entryName.equals(JarFile.MANIFEST_NAME)) {
                            if (!manifestFound) {
                                outputStream.putNextEntry(new JarEntry(JarFile.MANIFEST_NAME));
                                outputStream.write((Attributes.Name.MAIN_CLASS + ": " + mainClass + '\n').getBytes());

                                final BufferedReader bis = new BufferedReader(new InputStreamReader(warInput.getInputStream(entry)));
                                String line;
                                while ((line = bis.readLine()) != null) {
                                    if (line.startsWith(Attributes.Name.MAIN_CLASS.toString())) {
                                        throw new MojoExecutionException("Manifest already contains " + line);
                                    }

                                    outputStream.write((line + '\n').getBytes());
                                }

                                manifestFound = true;
                            }
                        } else {
                            if (!processedEntries.contains(entryName)) {
                                outputStream.putNextEntry(entry);
                                copyStream(outputStream, warInput.getInputStream(entry), buffer);
                            } else if (!entryName.endsWith("/")) {
                                throw new MojoExecutionException("Duplicate entry: " + entryName);
                            }
                        }
                    }

                    if (!manifestFound) {
                        throw new MojoExecutionException("No manifest found in " + packageFile);
                    }

                    if (!serverDependencies.isEmpty()) {
                        final String bootDirectory = "WEB-INF/boot/";

                        outputStream.putNextEntry(new JarEntry(bootDirectory));

                        for (final Artifact artifact : serverDependencies) {
                            final File dependency = new File(localRepository.getBasedir(), localRepository.pathOf(artifact));

                            if (!dependency.exists()) {
                                throw new MojoExecutionException("Dependency " + artifact + " not found (checked: " + dependency + ")");
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
                throw new MojoExecutionException("Could not delete " + packageFile);
            }

            if (!file.renameTo(packageFile)) {
                throw new MojoExecutionException("Could not create " + packageFile);
            }
        } catch (final IOException e) {
            throw new MojoExecutionException("Processing " + packageFile, e);
        }
    }

    private Artifact findPluginArtifact() throws MojoExecutionException {
        final Artifact root;
        try {
            root = artifactFactory.createPluginArtifact(pluginGroupId, pluginArtifactId, VersionRange.createFromVersionSpec(pluginVersion));
        } catch (final InvalidVersionSpecificationException e) {
            throw new MojoExecutionException("Resolving plugin artifact", e);
        }
        return root;
    }

    private Artifact findBootstrapDependency() throws MojoExecutionException {
        Artifact bootstrapModule = null;

        for (final Map.Entry<String, Artifact> entry : pluginArtifactMap.entrySet()) {
            if (entry.getKey().equals(BOOTSTRAP_MODULE_KEY)) {
                bootstrapModule = entry.getValue();
                break;
            }
        }

        if (bootstrapModule == null) {
            throw new MojoExecutionException("Bootstrap module not found " + BOOTSTRAP_MODULE_KEY);
        }

        return bootstrapModule;
    }

    /*
     * Finds all dependencies of the host project to include in the .war file. We can calculate the dependency list for this plugin, we can generate a complete
     * dependency tree and we have a root artifact whose transitive dependencies should be returned, but nothing more. The host project may add dependencies to
     * this plugin to be returned here in addition to those directly known by the host and that poses a problem: we need to throw away all dependencies of this
     * plugin that are not needed, transitively, by the root artifact and any other dependency added for inclusion in the .war file.
     */
    @SuppressWarnings({"unchecked"})
    private Collection<Artifact> findDependencies(final Artifact root, final DependencyNode rootNode) throws MojoExecutionException {

        // The dependency list of this plugin
        final Set<Artifact> pluginDependencies = findPluginDependencies(root, project.getRemoteArtifactRepositories());
        final Set<String> pluginDependencyIds = dependencyIds(addTransitiveDependencies(pluginDependencies, rootNode));

        pluginDependencyIds.add(root.getDependencyConflictId() + ':' + root.getVersion());

        // The root of the complete dependency tree for the host project including this plugin
        final Map<String, Artifact> dependencies = new HashMap<String, Artifact>();

        // What we already checked
        final Set<String> checked = new HashSet<String>();

        final DependencyNodeVisitor nodeVisitor = new DependencyNodeVisitor() {
            @SuppressWarnings({"unchecked"})
            public boolean visit(final DependencyNode node) {
                final Artifact artifact = node.getArtifact();
                final String artifactId = artifact.getDependencyConflictId();

                final boolean unseen = !checked.contains(artifactId);

                if (unseen) {
                    checked.add(artifactId);
                    final List<String> dependencyTrail = (List<String>) artifact.getDependencyTrail();

                    if (dependencyTrail != null) {
                        dependencyTrail.removeAll(pluginDependencyIds);

                        if (!dependencyTrail.isEmpty()) {
                            dependencies.put(artifactId, artifact);
                        }
                    }
                }

                return unseen;
            }

            public boolean endVisit(final DependencyNode node) {
                return true;
            }
        };

        int mapSize;
        do {
            mapSize = dependencies.size();
            rootNode.accept(nodeVisitor);
        } while (mapSize < dependencies.size());

        return dependencies.values();
    }

    private DependencyNode calculateDependencyGraph(final Artifact root, final List remoteRepositories) throws ArtifactResolutionException {
        final DependencyTreeResolutionListener listener = new DependencyTreeResolutionListener(new NullLogger());

        artifactCollector.collect(new HashSet<Artifact>(pluginArtifacts),
                                  root,
                                  project.getManagedVersionMap(),
                                  localRepository,
                                  remoteRepositories,
                                  artifactMetadataSource,
                                  null,
                                  Collections.singletonList(listener));


        return listener.getRootNode();
    }

    private Collection<Artifact> addTransitiveDependencies(final Set<Artifact> artifacts, final DependencyNode rootNode) {
        final Map<String, Artifact> dependencies = new HashMap<String, Artifact>();

        for (final Artifact artifact : artifacts) {
            dependencies.put(artifact.getDependencyConflictId(), artifact);
        }

        final DependencyNodeVisitor nodeVisitor = new DependencyNodeVisitor() {
            private String collecting = null;

            public boolean visit(final DependencyNode dependencyNode) {
                final Artifact artifact = dependencyNode.getArtifact();
                final String artifactId = artifact.getDependencyConflictId();

                if (collecting == null && dependencies.containsKey(artifactId)) {
                    collecting = artifactId;
                } else if (collecting != null) {
                    dependencies.put(artifactId, artifact);
                }

                return true;
            }

            public boolean endVisit(final DependencyNode dependencyNode) {
                final Artifact artifact = dependencyNode.getArtifact();

                if (artifact.getDependencyConflictId().equals(collecting)) {
                    collecting = null;
                }

                return true;
            }
        };

        int mapSize;
        do {
            mapSize = dependencies.size();
            rootNode.accept(nodeVisitor);
        } while (mapSize < dependencies.size());

        return dependencies.values();
    }

    @SuppressWarnings({"unchecked"})
    private Set<Artifact> findPluginDependencies(final Artifact root, final List remoteRepositories) throws MojoExecutionException {
        final Set<Artifact> pluginDependencies;
        try {
            pluginDependencies = (Set<Artifact>) artifactMetadataSource.retrieve(root, localRepository, remoteRepositories).getArtifacts();
        } catch (final ArtifactMetadataRetrievalException e) {
            throw new MojoExecutionException("Retrieving plugin dependencies", e);
        }

        for (final Iterator<Artifact> i = pluginDependencies.iterator(); i.hasNext();) {
            final Artifact artifact = i.next();
            final String artifactKey = artifact.getGroupId() + ':' + artifact.getArtifactId();

            if (server.equals(artifactKey) || DEFAULT_SERVER_KEY.equals(artifactKey) || BOOTSTRAP_MODULE_KEY.equals(artifactKey)) {
                i.remove();
            }
        }

        return pluginDependencies;
    }

    private Set<String> dependencyIds(final Collection<Artifact> artifacts) {
        final Set<String> dependencies = new HashSet<String>();

        for (final Artifact artifact : artifacts) {
            dependencies.add(artifact.getDependencyConflictId() + ':' + artifact.getVersion());
        }

        return dependencies;
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
            throw new MojoExecutionException("Cannot create " + outputDirectory);
        }

        File tempFile;

        do {
            tempFile = new File(outputDirectory, pluginName + '-' + System.nanoTime());
        } while (tempFile.exists());

        return tempFile;
    }

    private static class NullLogger extends AbstractLogger {

        public NullLogger() {
            super(0, null);
        }

        public void debug(final String s, final Throwable throwable) {
            // ignore
        }

        public void info(final String s, final Throwable throwable) {
            // ignore
        }

        public void warn(final String s, final Throwable throwable) {
            // ignore
        }

        public void error(final String s, final Throwable throwable) {
            // ignore
        }

        public void fatalError(final String s, final Throwable throwable) {
            // ignore
        }

        public Logger getChildLogger(final String s) {
            return this;
        }
    }
}

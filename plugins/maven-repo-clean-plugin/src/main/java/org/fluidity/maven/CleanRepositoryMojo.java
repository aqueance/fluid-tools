/*
 * Copyright (c) 2006-2010 Tibor Adam Varga (tibor.adam.varga on gmail)
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;

/**
 * Deletes the current version or all versions of this project from the local repository.
 *
 * @goal clean
 * @phase clean
 */
public final class CleanRepositoryMojo extends AbstractMojo {

    /**
     * Reference of the maven project
     *
     * @parameter expression="${project}"
     * @required
     */
    @SuppressWarnings({ "UnusedDeclaration" })
    private MavenProject project;

    /**
     * The local repository.
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    @SuppressWarnings({ "UnusedDeclaration" })
    private ArtifactRepository localRepository;

    /**
     * The artifact factory to use.
     *
     * @component
     * @required
     * @readonly
     */
    @SuppressWarnings({ "UnusedDeclaration" })
    private ArtifactFactory artifactFactory;

    /**
     * If true, all versions of this project will be deleted.
     *
     * @parameter
     */
    @SuppressWarnings({ "FieldCanBeLocal" })
    private boolean allVersions = false;

    private final Log log = getLog();

    @SuppressWarnings({ "unchecked" })
    public void execute() throws MojoExecutionException {
        MavenProject rootProject;
        for (rootProject = project; !rootProject.isExecutionRoot(); rootProject = rootProject.getParent()) {
            // empty
        }

        final Properties properties = rootProject.getProperties();

        final String dependencyMapKey = getClass().getName() + "#dependencyMap";
        final String artifactListKey = getClass().getName() + "#artifactList";

        Map<Artifact, Set<String>> dependencyMap = (Map<Artifact, Set<String>>) properties.get(dependencyMapKey);
        if (dependencyMap == null) {
            properties.put(dependencyMapKey, dependencyMap = new HashMap<Artifact, Set<String>>());

            final Map<String, Set<String>> trails = new HashMap<String, Set<String>>();

            final Set<Artifact> artifacts = new HashSet<Artifact>();
            final List<MavenProject> list = new ArrayList((List<MavenProject>) rootProject.getCollectedProjects());
            list.add(rootProject);
            for (final MavenProject childProject : list) {
                final Artifact artifact = childProject.getArtifact();
                artifacts.add(artifact);
            }

            properties.put(artifactListKey, artifacts);

            for (final MavenProject mavenProject : list) {
                final Set<Artifact> dependencies;

                try {
                    dependencies = mavenProject.createArtifacts(artifactFactory, "runtime", null);
                } catch (InvalidDependencyVersionException e) {
                    throw new MojoExecutionException("Resolving dependencies of " + mavenProject, e);
                }

                dependencies.retainAll(artifacts);

                final Artifact artifact = mavenProject.getArtifact();
                if (!dependencyMap.containsKey(artifact)) {
                    dependencyMap.put(artifact, new HashSet<String>());
                }

                for (final Artifact dependency : dependencies) {
                    Set<String> ids = dependencyMap.get(dependency);
                    if (ids == null) {
                        dependencyMap.put(dependency, ids = new HashSet<String>());
                    }

                    trails.put(dependencyId(dependency), ids);

                    ids.add(dependencyId(artifact));
                }
            }

            boolean changed;
            do {
                changed = false;

                for (final Set<String> trail : trails.values()) {
                    for (final String id : new HashSet<String>(trail)) {
                        final Set<String> deps = trails.get(id);

                        changed |= deps != null && trail.addAll(deps);
                    }
                }
            } while (changed);


/*
            for (final Map.Entry<String, Set<String>> entry : trails.entrySet()) {
                System.out.println(entry.getKey());
                System.out.println("  " + entry.getValue());
            }
*/
        }

        try {
            final String projectId = dependencyId(project.getArtifact());
            final Set<Artifact> artifacts = (Set<Artifact>) properties.get(artifactListKey);
            final String rootDirectory = new File(localRepository.getBasedir()).getCanonicalPath();

            for (final Map.Entry<Artifact, Set<String>> entry : dependencyMap.entrySet()) {
                final Artifact artifact = entry.getKey();

                if (artifacts.contains(artifact)) {
                    final Set<String> list = entry.getValue();
                    if (list != null) {
                        list.remove(projectId);
                    }

                    if (list == null || list.isEmpty()) {
                        final File file = new File(rootDirectory, localRepository.pathOf(artifact));
                        final File directory = allVersions ? file.getParentFile().getParentFile() : file.getParentFile();

                        if (directory.exists()) {
                            log.info("Deleting " + directory);
                            final boolean success = delete(directory, rootDirectory);
                            if (!success) {
                                log.error("Failed to delete " + directory);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Deleting directories", e);
        }
    }

    private String dependencyId(final Artifact artifact) {
        return artifact.getDependencyConflictId() + ':' + artifact.getVersion();
    }

    private boolean delete(final File directory, final String root) throws IOException {
        final File[] files = directory.listFiles();

        if (files == null) {
            return true;
        }

        for (final File file : files) {
            if (file.isDirectory()) {
                if (!delete(file, root)) {
                    return false;
                }
            } else {
                if (!file.delete()) {
                    return false;
                }
            }
        }

        boolean deleted = directory.delete();

        for (File parent = directory.getParentFile(); deleted && !parent.getCanonicalPath().equals(root); parent = parent.getParentFile()) {
            final String[] list = parent.list();
            if (list == null || list.length == 0) {
                log.info("Deleting " + parent);
                deleted = parent.delete();

                if (!deleted) {
                    log.error("Failed to delete " + parent);
                }
            }
        }

        return deleted;
    }
}

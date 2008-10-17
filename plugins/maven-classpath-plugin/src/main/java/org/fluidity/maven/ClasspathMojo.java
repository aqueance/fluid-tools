/*
 * Copyright (c) 2006-2008 Tibor Adam Varga (tibor.adam.varga@gmail.com)
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
 *
 */
package org.fluidity.maven;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

/**
 * Goal that composes a classpath from some dependencies and creates a project property with the value of the classpath.
 *
 * @goal classpaths
 * @phase process-sources
 * @requiresDependencyResolution compile
 */
@SuppressWarnings({ "MismatchedReadAndWriteOfArray" })
public class ClasspathMojo extends AbstractMojo {

    /**
     * Reference of the maven project
     *
     * @parameter expression="${project}"
     * @required
     */
    @SuppressWarnings({ "UnusedDeclaration" })
    private MavenProject project;

    /**
     * Dependency descriptors of the form
     * <xmp>
     * <classpaths>
     *   <classpath>
     *     <property>name of a project property to set this classpath as the value of</property>
     *     <dependencies>
     *       <dependency>
     *         <groupId>the artifact's groupId</groupId>
     *         <artifactId>the artifact's artifactId</artifactId>
     *       </dependency>
     *       ...
     *     </dependencies>
     *   </classpath>
     *   ...
     * </classpaths>
     * </xmp>
     *
     * @parameter
     * @required
     */
    @SuppressWarnings({ "UnusedDeclaration" })
    private Classpath[] classpaths;

    /**
     * Tells whether to print the constructed classpaths.
     *
     * @parameter
     */
    @SuppressWarnings({ "UnusedDeclaration" })
    private boolean verbose;

    private final Log log = getLog();

    @SuppressWarnings({ "unchecked" })
    public void execute() throws MojoExecutionException {
        final Set<Artifact> artifacts = (Set<Artifact>) project.getArtifacts();

        for (final Classpath definition : classpaths) {
            final StringBuilder classpath = new StringBuilder();

            final Set<String> inclusions = new HashSet<String>();
            final Set<String> exclusions = new HashSet<String>();

            if (definition.dependencies != null) {
                for (final Dependency include : definition.dependencies) {
                    inclusions.add(include.artifactId);

                    if (include.groupId != null) {
                        inclusions.add(include.groupId + ":" + include.artifactId);
                    }
                }
            }

            if (definition.exclusions != null) {
                for (final Dependency exclude : definition.exclusions) {
                    exclusions.add(exclude.artifactId);

                    if (exclude.groupId != null) {
                        exclusions.add(exclude.groupId + ":" + exclude.artifactId);
                    }
                }
            }

            for (final Artifact artifact : artifacts) {
                final String artifactId = artifact.getArtifactId();
                final String artifactKey = artifact.getGroupId() + ":" + artifactId;

                if (!exclusions.contains(artifactKey)
                    && (inclusions.isEmpty()
                    || inclusions.contains(artifactKey)
                    || ((inclusions.contains(artifactId) && !exclusions.contains(artifactId))))) {
                    if (classpath.length() > 0) {
                        classpath.append(File.pathSeparator);
                    }

                    try {
                        classpath.append(artifact.getFile().getCanonicalPath());
                    } catch (final IOException e) {
                        throw new MojoExecutionException("Resolving " + artifact.toString(), e);
                    }
                }
            }

            if (verbose) {
                log.info(definition.property + ": " + classpath);
            }

            if (definition.property != null) {
                project.getProperties().setProperty(definition.property, classpath.toString());
            } else {
                log.info(classpath);
            }
        }
    }
}

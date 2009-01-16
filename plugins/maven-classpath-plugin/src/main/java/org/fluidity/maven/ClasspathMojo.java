/*
 * Copyright (c) 2006-2009 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     *     <patterns>
     *       <pattern>a Java regex pattern to match "groupId:artifactId" of all dependencies of the host plugin against</pattern>
     *        ...
     *     </patterns>
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
            if (verbose) {
                log.info("Processing property " + definition.property);
            }

            final Set<Pattern> patterns = new HashSet<Pattern>();

            if (definition.patterns != null) {
                for (final String pattern : definition.patterns) {
                    if (verbose) {
                        log.info(" adding pattern " + pattern);
                    }

                    patterns.add(Pattern.compile(pattern));
                }
            }

            final Set<String> list = new HashSet<String>();

            for (final Artifact artifact : artifacts) {
                final String artifactId = artifact.getArtifactId();
                final String artifactKey = artifact.getGroupId() + ":" + artifactId;

                for (final Pattern pattern : patterns) {
                    final Matcher matcher = pattern.matcher(artifactKey);
                    final boolean matches = matcher.matches();

                    if (matches) {
                        try {
                            list.add(artifact.getFile().getCanonicalPath());
                        } catch (final IOException e) {
                            throw new MojoExecutionException("Resolving " + artifact.toString(), e);
                        }
                    }
                }
            }

            final StringBuilder classpath = new StringBuilder();
            for (final String path : list) {
                if (classpath.length() > 0) {
                    classpath.append(File.pathSeparator);
                }

                classpath.append(path);
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

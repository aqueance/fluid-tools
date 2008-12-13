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

import java.io.File;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

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
    @SuppressWarnings({"UnusedDeclaration"})
    private MavenProject project;

    /**
     * The local repository.
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    @SuppressWarnings({"UnusedDeclaration"})
    private ArtifactRepository localRepository;

    /**
     * If true, all versions of this project will be deleted.
     *
     * @parameter
     */
    private boolean allVersions = false;

    private final Log log = getLog();

    @SuppressWarnings({"unchecked"})
    public void execute() throws MojoExecutionException {
        final String basedir = localRepository.getBasedir();
        final String artifactPath = localRepository.pathOf(project.getArtifact());

        final File artifact = new File(basedir, artifactPath);

        final File directory = allVersions ? artifact.getParentFile().getParentFile() : artifact.getParentFile();
        if (directory.exists()) {
            log.info("Deleting " + directory);
            final boolean success = delete(directory);
            if (!success) {
                log.error("Failed to delete " + directory);
            }
        }
    }

    private boolean delete(final File directory) {
        final File[] files = directory.listFiles();

        if (files == null) {
            return true;
        }

        for (final File file : files) {
            if (file.isDirectory()) {
                if (!delete(file)) {
                    return false;
                }
            } else {
                if (!file.delete()) {
                    return false;
                }
            }
        }

        return directory.delete();
    }
}

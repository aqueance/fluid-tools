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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.fluidity.foundation.ApplicationInfo;


/**
 * Creates a JAR manifest containing the values of evaluating the "${project.artifactId}" and "${project.name} (${project.version})" Maven expressions as the
 * application's ID and name, respectively. This Mojo is used in conjuction with the <code>org.fluidity.foundation.ManifestApplicationInfoImpl</code> that
 * exposes these values to the application.
 *
 * @goal application-info
 * @phase process-classes
 */
public class ManifestAppInfoMojo extends AbstractMojo {

    /**
     * The location of the compiled classes.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    @SuppressWarnings({ "UnusedDeclaration" })
    private File outputDirectory;

    /**
     * @parameter expression="${project.artifactId}"
     * @required
     */
    @SuppressWarnings({ "UnusedDeclaration" })
    private String projectArtifactId;

    /**
     * @parameter expression="${project.name}"
     * @required
     */
    @SuppressWarnings({ "UnusedDeclaration" })
    private String projectName;

    /**
     * @parameter expression="${project.version}"
     * @required
     */
    @SuppressWarnings({ "UnusedDeclaration" })
    private String projectVersion;

    @SuppressWarnings({ "unchecked", "ResultOfMethodCallIgnored" })
    public void execute() throws MojoExecutionException {
        final File manifestFile = new File(outputDirectory, JarFile.MANIFEST_NAME);
        manifestFile.getParentFile().mkdirs();

        final Manifest manifest = new Manifest();
        final Attributes attributes = manifest.getMainAttributes();

        attributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
        attributes.putValue("Created-By", "Fluid Tools");
        attributes.putValue(ApplicationInfo.KEY_ATTRIBUTE, projectArtifactId);
        attributes.putValue(ApplicationInfo.NAME_ATTRIBUTE, projectName + " (" + projectVersion + ")");

        try {
            final FileOutputStream out = new FileOutputStream(manifestFile);
            manifest.write(out);
            out.close();
        } catch (final IOException e) {
            throw new MojoExecutionException("Saving manifest", e);
        }
    }
}
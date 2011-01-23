/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.fluidity.deployment.JettyServer;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * Takes all WAR files that it can find in the host project, deploys them in a Jetty server and then starts the server.
 *
 * @author Tibor Varga
 * @goal start
 * @phase package
 * @requiresDependencyResolution compile
 */
public final class WebRunMojo extends AbstractMojo {

    private static final String WAR_TYPE = "war";

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
     * HTTP port to listen on. No value means no HTTP listener.
     *
     * @parameter
     */
    @SuppressWarnings("UnusedDeclaration")
    private String listenPort;

    /**
     * Reference of the maven project
     *
     * @parameter expression="${project}"
     * @required
     */
    @SuppressWarnings("UnusedDeclaration")
    private MavenProject project;

    /**
     * Name of the maven project
     *
     * @parameter expression="${plugin.artifactId}"
     * @required
     */
    @SuppressWarnings("UnusedDeclaration")
    private String pluginName;

    private final Log log = getLog();

    public void execute() throws MojoExecutionException {
        final File jettyDirectory = createTempDirectory();

        final Set<String> deployed = new HashSet<String>();

        final WebAppContext defaultContext = addWebArtifact(jettyDirectory, project.getArtifact(), deployed, true);
        final List<WebAppContext> contextList = new ArrayList<WebAppContext>();

        for (final Artifact dependency : project.getDependencyArtifacts()) {
            final WebAppContext context = addWebArtifact(jettyDirectory, dependency, deployed, false);
            if (context != null) {
                contextList.add(context);
            }
        }

        if (!deployed.isEmpty()) {
            final int httpPort;

            if (listenPort != null) {
                try {
                    httpPort = Integer.parseInt(listenPort);
                } catch (final NumberFormatException e) {
                    throw new MojoExecutionException("Listen port not a number: " + listenPort);
                }
            } else {
                httpPort = 0;
            }

            JettyServer.start(httpPort, null, defaultContext, contextList);
        } else {
            log.warn("No web applications to deploy");
        }
    }

    private File createTempDirectory() throws MojoExecutionException {
        File tempDirectory;

        do {
            tempDirectory = new File(outputDirectory, pluginName + '-' + System.nanoTime());
        } while (tempDirectory.exists());

        if (!tempDirectory.mkdirs()) {
            throw new MojoExecutionException("Cannot create " + tempDirectory);
        }

        return tempDirectory;
    }

    private WebAppContext addWebArtifact(final File jettyDirectory, final Artifact artifact, final Set<String> deployed, final boolean defaultApp) {
        final File file = artifact.getFile();

        if (file != null) {
            if (WAR_TYPE.equals(artifact.getType()) && (defaultApp || Artifact.SCOPE_PROVIDED.equals(artifact.getScope()))) {
                final WebAppContext context = new WebAppContext();
                final String archiveName = artifact.getArtifactId();
                final String contextPath = String.format("/%s", defaultApp ? "" : archiveName);

                context.setTempDirectory(new File(jettyDirectory, archiveName));
                context.setContextPath(contextPath);
                context.setParentLoaderPriority(true);

                final String filePath = file.getPath();

                if (!deployed.contains(filePath)) {
                    context.setWar(filePath);
                    deployed.add(filePath);

                    log.info(String.format("Context %s: %s", context.getContextPath(), context.getWar()));

                    return context;
                }
            }
        }

        return null;
    }
}

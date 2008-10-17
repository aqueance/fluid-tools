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
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.fluidity.composition.ComponentContainerAccess;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.handler.RequestLogHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.resource.Resource;
import org.mortbay.thread.QueuedThreadPool;

/**
 * Takes all WAR files that it can find in the host project, deploys them in a Jetty server and then starts the server.
 *
 * @goal start
 * @phase package
 * @requiresDependencyResolution compile
 */
public class WebRunMojo extends AbstractMojo {

    private static final File TMP_DIR =
        new File(System.getProperty("java.io.tmpdir"), "jetty-" + System.identityHashCode(WebRunMojo.class));

    private static final String WAR_TYPE = "war";

    private static final String PROVIDED_SCOPE = "provided";

    /**
     * HTTP port to listen on. No value means no HTTP listener.
     *
     * @parameter
     */
    @SuppressWarnings({ "UnusedDeclaration" })
    private String listenPort;

    /**
     * Reference of the maven project
     *
     * @parameter expression="${project}"
     * @required
     */
    @SuppressWarnings({ "UnusedDeclaration" })
    private MavenProject project;

    private final Log log = getLog();

    @SuppressWarnings({ "unchecked" })
    public void execute() throws MojoExecutionException {
        TMP_DIR.mkdirs();

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                Thread.currentThread().setContextClassLoader(null);
                deleteDirectory(TMP_DIR);
            }

            private void deleteDirectory(File directory) {
                if (directory == null) return;

                final File[] files = directory.listFiles();
                if (files == null) return;

                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();

                        if (file.exists()) {
                            System.out.println(file + " could not be deleted");
                        }
                    }
                }

                directory.delete();

                if (directory.exists()) {
                    System.out.println(directory + " could not be deleted");
                }
            }
        });

        final ContextHandlerCollection contexts = new ContextHandlerCollection();

        final HandlerCollection handlers = new HandlerCollection();
        handlers.addHandler(contexts);
        handlers.addHandler(new DefaultHandler());
        handlers.addHandler(new RequestLogHandler());

        final Set<String> deployed = new HashSet<String>();

        addWebArtifact(project.getArtifact(), contexts, deployed, true);

        for (Artifact dependency : (Set<Artifact>) project.getDependencyArtifacts()) {
            addWebArtifact(dependency, contexts, deployed, false);
        }

        if (!deployed.isEmpty()) {
            final Server server = new Server();

            new ComponentContainerAccess().setBindingsProperty(WebRunControl.class, new WebRunControl() {
                public void stopServer() throws Exception {
                    server.stop();
                }
            });

            server.setThreadPool(new QueuedThreadPool());
            server.setHandler(handlers);

            if (listenPort != null) {
                try {
                    final SelectChannelConnector connector = new SelectChannelConnector();
                    connector.setPort(Integer.parseInt(listenPort));
                    server.addConnector(connector);
                } catch (NumberFormatException e) {
                    throw new MojoExecutionException("Listen port not a number: " + listenPort);
                }
            }

            Resource.setDefaultUseCaches(false);

            try {
                log.info("Starting server - press Ctrl-C to kill.");
                server.start();
                server.setStopAtShutdown(true);

                server.join();
            } catch (Exception e) {
                throw new MojoExecutionException("Starting server", e);
            }
        }
    }

    private void addWebArtifact(final Artifact artifact,
                                final ContextHandlerCollection webContexts,
                                final Set<String> deployed,
                                final boolean acceptAnyScope) {
        final File file = artifact.getFile();

        if (file != null) {
            if (WAR_TYPE.equals(artifact.getType()) && (acceptAnyScope || PROVIDED_SCOPE.equals(artifact.getScope()))) {
                final WebAppContext context = new WebAppContext();
                final String archiveName = artifact.getArtifactId();
                final String contextPath = "/" + archiveName;

                context.setTempDirectory(new File(TMP_DIR, archiveName));
                context.setContextPath(contextPath);
                context.setParentLoaderPriority(true);

                final String filePath = file.getPath();

                if (!deployed.contains(filePath)) {
                    context.setWar(filePath);
                    webContexts.addHandler(context);
                    deployed.add(filePath);

                    log.info("Context " + contextPath + " is " + filePath);
                }
            }
        }
    }
}

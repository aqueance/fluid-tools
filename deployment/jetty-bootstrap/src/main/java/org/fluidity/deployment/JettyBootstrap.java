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

package org.fluidity.deployment;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fluidity.composition.ComponentContainerAccess;
import org.fluidity.foundation.Exceptions;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.handler.RequestLogHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.thread.QueuedThreadPool;

/**
 * Bootstraps a Jetty web container and deploys the .war file that contains this class and then all other .war files supplied.
 *
 * @author Tibor Varga
 */
public final class JettyBootstrap implements ServerBootstrap {

    public void bootstrap(final int httpPort, final File bootApp, final List<File> managedApps, final File workDirectory, final String args[]) {
        final ContextHandlerCollection contexts = new ContextHandlerCollection();

        final HandlerCollection handlers = new HandlerCollection();
        handlers.addHandler(contexts);
        handlers.addHandler(new DefaultHandler());
        handlers.addHandler(new RequestLogHandler());

        contexts.addHandler(deployWar(bootApp, workDirectory, true));

        for (final File app : managedApps) {
            contexts.addHandler(deployWar(app, workDirectory, false));
        }

        final Server server = new Server();

        new ComponentContainerAccess().bindBootComponent(DeploymentControl.class, new DeploymentControl() {
            public void completed() {
                // empty
            }

            public boolean isStandalone() {
                return false;
            }

            public void stop() {
                Exceptions.wrap("stopping Jetty server", new Exceptions.Command<Void>() {
                    public Void run() throws Exception {
                        server.stop();
                        return null;
                    }
                });
            }
        });

        server.setHandler(handlers);

        if (httpPort > 0) {
            server.setThreadPool(new QueuedThreadPool());
            final SelectChannelConnector connector = new SelectChannelConnector();
            connector.setPort(httpPort);
            server.addConnector(connector);
        }

        try {
            System.out.println("Starting server - press Ctrl-C to kill.");
            server.setStopAtShutdown(true);
            server.start();
            server.join();
        } catch (final Exception e) {
            throw new RuntimeException("Starting server", e);
        }

    }

    private WebAppContext deployWar(final File warFile, final File workDirectory, final boolean root) {
        final WebAppContext context = new WebAppContext();
        final String archiveName = warFile.getName();
        final String contextPath = "/" + (root ? "" : artifactId(archiveName));

        context.setTempDirectory(new File(workDirectory, archiveName));
        context.setContextPath(contextPath);
        context.setParentLoaderPriority(true);

        context.setWar(warFile.getPath());

        System.out.println("Context " + context.getContextPath() + ": " + context.getWar());

        return context;
    }

    private final Pattern archiveNamePattern = Pattern.compile("(.+?)-\\d.*\\.war");

    private String artifactId(final String archiveName) {
        final Matcher matcher = archiveNamePattern.matcher(archiveName);

        if (!matcher.matches()) {
            throw new RuntimeException("Could not parse archive name " + archiveName + " using pattern " + archiveNamePattern);
        }

        return matcher.group(1);
    }
}

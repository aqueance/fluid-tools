package org.fluidity.web;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fluidity.composition.ComponentContainerAccess;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.handler.RequestLogHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.thread.QueuedThreadPool;

/**
 * Bootstraps a Jetty web container and deploys the .war file that contains this class.
 */
public final class JettyBootstrap implements ServerBootstrap {

    public void bootstrap(final File bootWar, final List<File> otherWars, final File workDirectory) {
        final ContextHandlerCollection contexts = new ContextHandlerCollection();

        final HandlerCollection handlers = new HandlerCollection();
        handlers.addHandler(contexts);
        handlers.addHandler(new DefaultHandler());
        handlers.addHandler(new RequestLogHandler());

        contexts.addHandler(deployWar(bootWar, workDirectory, true));

        for (final File war : otherWars) {
            contexts.addHandler(deployWar(war, workDirectory, false));
        }

        final Server server = new Server();

        final ComponentContainerAccess access = new ComponentContainerAccess();
        access.bindBootComponent(ServerControl.class, new ServerControl() {
            public void stopServer() throws Exception {
                server.stop();
            }
        });

        server.setThreadPool(new QueuedThreadPool());
        server.setHandler(handlers);

        final Settings settings = access.getComponent(Settings.class);

        if (settings != null) {
            final int httpPort = settings.httpPort();

            if (httpPort > 0) {
                final SelectChannelConnector connector = new SelectChannelConnector();
                connector.setPort(httpPort);
                server.addConnector(connector);
            }
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

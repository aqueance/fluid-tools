package org.fluidity.maven;

import java.io.File;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.handler.RequestLogHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.thread.QueuedThreadPool;
import org.fluidity.composition.ServiceProvider;
import org.fluidity.composition.ComponentContainerAccess;

/**
 * Bootstraps a Jetty web container and deploys the .war file that contains this class.
 */
@ServiceProvider
public final class JettyBootstrap implements ServerBootstrap {

    public void bootstrap(final File warFile, final File workDirectory) {
        final ContextHandlerCollection contexts = new ContextHandlerCollection();

        final HandlerCollection handlers = new HandlerCollection();
        handlers.addHandler(contexts);
        handlers.addHandler(new DefaultHandler());
        handlers.addHandler(new RequestLogHandler());

        final WebAppContext context = new WebAppContext();
        final String contextPath = "/";

        context.setTempDirectory(new File(workDirectory, warFile.getName()));
        context.setContextPath(contextPath);
        context.setParentLoaderPriority(true);

        context.setWar(warFile.getPath());
        contexts.addHandler(context);

        final Server server = new Server();

        final ComponentContainerAccess access = new ComponentContainerAccess();
        access.setBindingsProperty(WebServerControl.class, new WebServerControl() {
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
            server.start();
            server.setStopAtShutdown(true);

            server.join();
        } catch (final Exception e) {
            throw new RuntimeException("Starting server", e);
        }
    }
}

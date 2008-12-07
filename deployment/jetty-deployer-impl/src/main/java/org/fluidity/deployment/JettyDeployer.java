package org.fluidity.deployment;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fluidity.composition.ComponentContainerAccess;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.thread.QueuedThreadPool;

/**
 * Bootstraps a Jetty web container and deploys exposes a management interface to deploy/undeploy .war files specified in the command line, to stop the server
 * and to monitor and manage the Java virtual machine.
 */
public final class JettyDeployer implements ServerBootstrap {

    private final Server server = new Server();

    private final ContextHandlerCollection applications = new ContextHandlerCollection();

    private final Map<String, WebAppContext> contextMap = new LinkedHashMap<String, WebAppContext>();

    public void bootstrap(final WebApplicationInfo bootApp, final List<WebApplicationInfo> managedApps, final File workDirectory, final String args[]) {
        final HandlerCollection handlers = new HandlerCollection();
        handlers.addHandler(applications);

        final ContextHandlerCollection manager = new ContextHandlerCollection();
        handlers.addHandler(manager);
        manager.addHandler(createWarContext(bootApp.archive(), workDirectory, true));

//        handlers.addHandler(new DefaultHandler());      // TODO: this does too much
//        handlers.addHandler(new RequestLogHandler());   // TODO: is this needed?

        for (final WebApplicationInfo app : managedApps) {
            contextMap.put(app.key(), createWarContext(app.archive(), workDirectory, false));
        }

        final JettyDeploymentServer deployer = new JettyDeploymentServer();

        final ComponentContainerAccess access = new ComponentContainerAccess();
        access.bindBootComponent(DeploymentServer.class, deployer);

        server.setThreadPool(new QueuedThreadPool(/* TODO: configure pool size and other stuff */));
        server.setHandler(handlers);

        if (args.length > 0 && "-port".equals(args[0])) {
            try {
                final int httpPort = args.length > 2 ? Integer.parseInt(args[1]) : 80;
                final SelectChannelConnector connector = new SelectChannelConnector();
                connector.setPort(httpPort);
                server.addConnector(connector);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Parameter " + args[1] + " is not a port number");
            }
        }

        try {
            /*
             * Use <code>-Dcom.sun.management.jmxremote</code> to enable remote access to the platform MBean server
            server.getContainer().addEventListener(new MBeanContainer(ManagementFactory.getPlatformMBeanServer()));
             */
            server.setStopAtShutdown(true);
            server.start();
        } catch (final Exception e) {
            throw new RuntimeException("Running Jetty", e);
        }
    }

    private WebAppContext createWarContext(final File warFile, final File workDirectory, final boolean root) {
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

    private final class JettyDeploymentServer implements DeploymentServer {

        private final Set<String> deployments = new HashSet<String>();

        public String[] applicationKeys() {
            final Set<String> names = contextMap.keySet();
            return names.toArray(new String[names.size()]);
        }

        public boolean isApplicationDeployed(final String key) {
            return deployments.contains(key);
        }

        public void deployApplication(final String key) throws Exception {
            final WebAppContext context = contextMap.get(key);

            if (context != null && !deployments.contains(key)) {
                applications.addHandler(context);
                context.start();

                synchronized (deployments) {
                    deployments.add(key);
                }
            }
        }

        public void undeployApplication(final String key) throws Exception {
            final WebAppContext context = contextMap.get(key);

            if (context != null && deployments.contains(key)) {
                context.stop();

                applications.removeHandler(context);

                synchronized (deployments) {
                    deployments.remove(key);
                }
            }
        }

        public void stopServer() throws Exception {
            server.stop();
        }
    }
}
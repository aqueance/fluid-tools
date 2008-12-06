package org.fluidity.deployment;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import sun.misc.Service;

/**
 * Bootstraps a Jetty web container and deploys exposes a management interface to deploy/undeploy .war files specified in the command line, to stop the server
 * and to monitor and manage the Java virtual machine.
 */
public final class JettyDeployer implements ServerBootstrap {
    private final Server server = new Server();

    private final ContextHandlerCollection applications = new ContextHandlerCollection();

    private final Map<String, WebAppContext> contextMap = new LinkedHashMap<String, WebAppContext>();
    private final Map<String, Settings> settingsMap = new HashMap<String, Settings>();
    private final Map<Integer, Set<String>> portMap = new HashMap<Integer, Set<String>>();
    private final Map<Integer, SelectChannelConnector> connectorMap = new HashMap<Integer, SelectChannelConnector>();
    private final Set<String> deployedContexts = new HashSet<String>();

    public void bootstrap(final File bootWar, final List<File> otherWars, final File workDirectory) {
        final HandlerCollection handlers = new HandlerCollection();
        handlers.addHandler(applications);

        if (bootWar != null) {
            final ContextHandlerCollection deployer = new ContextHandlerCollection();
            handlers.addHandler(deployer);
            deployer.addHandler(createWarContext(bootWar, workDirectory, true));
        }

        handlers.addHandler(new DefaultHandler());      // TODO: this does too much
        handlers.addHandler(new RequestLogHandler());   // TODO: is this needed?

        for (final File war : otherWars) {
            try {
                final DeploymentDescriptor descriptor = findDescriptor(war);

                contextMap.put(descriptor.name(), createWarContext(war, workDirectory, false));
                settingsMap.put(descriptor.name(), descriptor.settings());
            } catch (final MalformedURLException e) {
                assert false : e;
            }
        }

        final JettyDeploymentServer deployer = new JettyDeploymentServer();

        final ComponentContainerAccess access = new ComponentContainerAccess();
        access.bindBootComponent(DeploymentServer.class, deployer);

        if (bootWar == null) {
            deployer.setupDeployment(null, access.getComponent(Settings.class));
        }

        server.setThreadPool(new QueuedThreadPool(/* TODO: configure pool size and other stuff */));
        server.setHandler(handlers);

        try {
            /*
            * Use <code>-Dcom.sun.management.jmxremote</code> to enable remote access to the platform MBean server
            server.getContainer().addEventListener(new MBeanContainer(ManagementFactory.getPlatformMBeanServer()));
            */
            server.setStopAtShutdown(true);
            server.start();
            server.join();
        } catch (final Exception e) {
            throw new RuntimeException("Running Jetty", e);
        }
    }

    private DeploymentDescriptor findDescriptor(final File war) throws MalformedURLException {
        final DeploymentDescriptor descriptor;
        final URLClassLoader warClassLoader = new URLClassLoader(new URL[] {war.toURL()}, getClass().getClassLoader());

        final Iterator apps = Service.providers(DeploymentDescriptor.class, warClassLoader);
        if (apps.hasNext()) {
            descriptor = (DeploymentDescriptor) apps.next();
        } else {
            descriptor = new DeploymentDescriptor() {
                public Settings settings() {
                    return null;
                }

                public String name() {
                    return artifactId(war.getName());
                }
            };

            final Settings settings = descriptor.settings();
            System.out.println(
                    "File " + war + " contains no " + DeploymentDescriptor.class.getName() + " implementation, assuming defaults (name: " + descriptor.name()
                            + (", " + (settings == null ? "no " : "") + "HTTP port"
                            + (settings != null && settings.httpPort() > 0 ? ": " + settings.httpPort() : "") + ")"));
        }

        return descriptor;
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

        public String[] applicationNames() {
            final Set<String> names = contextMap.keySet();
            return names.toArray(new String[names.size()]);
        }

        public boolean isApplicationDeployed(final String name) {
            return deployedContexts.contains(name);
        }

        public void deployApplication(final String name) throws Exception {
            final WebAppContext context = contextMap.get(name);

            if (context != null) {
                applications.addHandler(context);
                context.start();
                setupDeployment(name, settingsMap.get(name));
                deployedContexts.add(name);
            }
        }

        public void undeployApplication(final String name) throws Exception {
            final WebAppContext context = contextMap.get(name);

            if (context != null) {
                context.stop();

                applications.removeHandler(context);

                final Settings settings = settingsMap.get(name);
                setupUndeployment(name, settings);
                deployedContexts.remove(name);
            }
        }

        private void setupDeployment(final String name, final Settings settings) {
            if (settings != null) {
                final int httpPort = settings.httpPort();

                if (httpPort > 0) {
                    Set<String> applications;
                    boolean startListener = false;

                    synchronized (portMap) {
                        applications = portMap.get(httpPort);

                        if (applications == null) {
                            applications = new HashSet<String>();
                            applications.add(name);

                            portMap.put(httpPort, applications);
                            startListener = true;
                        }
                    }

                    if (startListener) {
                        synchronized (connectorMap) {
                            final SelectChannelConnector connector = new SelectChannelConnector();
                            connector.setPort(httpPort);
                            server.addConnector(connector);
                            connectorMap.put(httpPort, connector);
                        }
                    }
                }
            }
        }

        private void setupUndeployment(final String name, final Settings settings) {
            if (settings != null) {
                final int httpPort = settings.httpPort();

                if (httpPort > 0) {
                    Set<String> listeners;
                    boolean stopListener = false;

                    synchronized (portMap) {
                        listeners = portMap.get(httpPort);

                        if (listeners != null) {
                            listeners.remove(name);

                            if (listeners.isEmpty()) {
                                stopListener = true;
                                portMap.remove(httpPort);
                            }
                        }
                    }

                    if (stopListener) {
                        synchronized (connectorMap) {
                            final SelectChannelConnector connector = connectorMap.remove(httpPort);

                            assert connector != null : httpPort;
                            server.removeConnector(connector);
                        }
                    }
                }
            }
        }

        public void stopServer() throws Exception {
            server.stop();
        }
    }
}
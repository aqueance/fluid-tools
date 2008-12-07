package org.fluidity.deployment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import org.fluidity.composition.ComponentContainerAccess;
import org.fluidity.foundation.ApplicationInfo;
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

    // TODO: do we need a server at all? Only when HTTP is actually used...
    private final Server server = new Server();

    private final ContextHandlerCollection applications = new ContextHandlerCollection();

    private final Map<String, WebAppContext> contextMap = new LinkedHashMap<String, WebAppContext>();
    private final Set<ApplicationInfo> descriptors = new HashSet<ApplicationInfo>();

    public void bootstrap(final int httpPort, final File bootApp, final List<File> managedApps, final File workDirectory, final String args[]) {
        final HandlerCollection handlers = new HandlerCollection();
        handlers.addHandler(applications);

        final ContextHandlerCollection manager = new ContextHandlerCollection();
        handlers.addHandler(manager);
        manager.addHandler(createWarContext(bootApp, workDirectory, true));

        boolean needsHttp = describe(bootApp).needsHttp();

        if (needsHttp && httpPort <= 0) {
            throw new IllegalStateException("No HTTP port specified and " + bootApp + " needs HTTP");
        }

//        handlers.addHandler(new DefaultHandler());      // TODO: this does too much
//        handlers.addHandler(new RequestLogHandler());   // TODO: is this needed?

        for (final File app : managedApps) {
            final WebApplicationInfo descriptor = describe(app);
            final String key = descriptor.key();
            final WebAppContext context = createWarContext(app, workDirectory, false);

            if (key == null) {
                applications.addHandler(context);
            } else {
                contextMap.put(key, context);
                descriptors.add(descriptor);
            }

            needsHttp |= descriptor.needsHttp();

            if (needsHttp && httpPort <= 0) {
                throw new IllegalStateException("No HTTP port specified and " + app + " needs HTTP");
            }
        }

        final JettyDeploymentServer deployer = new JettyDeploymentServer();

        final ComponentContainerAccess access = new ComponentContainerAccess();
        access.bindBootComponent(DeploymentServer.class, deployer);

        server.setThreadPool(new QueuedThreadPool(/* TODO: configure pool size and other stuff */));
        server.setHandler(handlers);
        server.setSendServerVersion(false);

        if (needsHttp) {
            final SelectChannelConnector connector = new SelectChannelConnector();
            connector.setPort(httpPort);
            server.addConnector(connector);
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

    private WebApplicationInfo describe(final File archive) {
        String key = null;
        String name = null;
        boolean http = true;

        try {
            final JarFile jar = new JarFile(archive);
            try {
                final ZipEntry nestedManifestEntry = jar.getEntry("WEB-INF/classes/" + JarFile.MANIFEST_NAME);

                if (nestedManifestEntry != null) {
                    final InputStream stream = jar.getInputStream(nestedManifestEntry);
                    final Manifest manifest = new Manifest(stream);

                    stream.close();

                    final Attributes attributes = manifest.getMainAttributes();

                    key = attributes.getValue(ApplicationInfo.KEY_ATTRIBUTE);
                    name = attributes.getValue(ApplicationInfo.NAME_ATTRIBUTE);
                }

                final ZipEntry rootManifestEntry = jar.getEntry(JarFile.MANIFEST_NAME);

                if (rootManifestEntry != null) {
                    final InputStream stream = jar.getInputStream(rootManifestEntry);
                    final Manifest manifest = new Manifest(stream);

                    stream.close();

                    final String flag = manifest.getMainAttributes().getValue(WebApplicationInfo.HTTP_ATTRIBUTE);
                    http = flag == null || Boolean.parseBoolean(flag);
                }
            } finally {
                jar.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Reading " + archive, e);
        }

        assert key == null || name != null : key + " has no name";
        return new ArchiveInfo(archive, key, name, http);
    }

    private static class ArchiveInfo implements WebApplicationInfo {

        private final File archive;
        private final String key;
        private final String name;
        private final boolean http;

        public ArchiveInfo(final File archive, final String key, final String name, final boolean http) {
            this.archive = archive;
            this.key = key;
            this.name = name;
            this.http = http;
        }

        public File archive() {
            return archive;
        }

        public boolean needsHttp() {
            return http;
        }

        public String key() {
            return key;
        }

        public String name() {
            return name;
        }
    }

    private final class JettyDeploymentServer implements DeploymentServer {

        private final Set<String> deployments = new HashSet<String>();

        public ApplicationInfo[] applications() {
            return descriptors.toArray(new ApplicationInfo[descriptors.size()]);
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
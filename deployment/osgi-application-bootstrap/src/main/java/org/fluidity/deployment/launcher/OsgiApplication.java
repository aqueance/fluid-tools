/*
 * Copyright (c) 2006-2012 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fluidity.deployment.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.security.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.fluidity.composition.Component;
import org.fluidity.composition.Optional;
import org.fluidity.composition.spi.ContainerTermination;
import org.fluidity.deployment.cli.Application;
import org.fluidity.deployment.osgi.StartLevels;
import org.fluidity.foundation.Archives;
import org.fluidity.foundation.ClassLoaders;
import org.fluidity.foundation.Command;
import org.fluidity.foundation.Log;
import org.fluidity.foundation.Security;
import org.fluidity.foundation.ServiceProviders;

import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.RecursionInterceptor;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.interpolation.SimpleRecursionInterceptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;

/**
 * A command line main class that populates the application's dependency injection container, bootstraps an OSGi framework, installs all OSGi bundle JAR files
 * visible to its class loader, starts all non-fragment bundles, and then waits for the OSGi framework to stop.
 * <p/>
 * <b>NOTE</b>: This class is public <em>only</em> so that its <code>main</code> method can be found by the Java launcher.
 * <p/>
 * With OSGi version 4.3 or later, bundle and framework start levels default to 2 to enable starting and stopping all bundles simply by setting the framework
 * start level. Fine tuning of start levels is possible through an optional implementation of the {@link StartLevels} interface.
 * <p/>
 * With OSGi version earlier than 4.2, bundle and framework start levels are set to 1 and no fine tuning is possible.
 * <h3>Usage</h3>
 * <h4>POM</h4>
 * Use the <code>org.fluidity.maven:fluidity-archetype-standalone-osgi</code> Maven archetype to create the standalone OSGi application wrapper project.
 * <h4>Start Levels</h4>
 * See {@link StartLevels} for details.
 *
 * @author Tibor Varga
 */
@Component
final class OsgiApplication implements Application {

    /**
     * The name of the system property that specifies the application properties resource URL. The application properties are deployment independent. If not
     * given, the resource named "application.properties" will be loaded.
     */
    public static final String APPLICATION_PROPERTIES = "application.properties";

    /**
     * The name of the system property that specifies the deployment properties resource URL. The deployment properties are specific to a particular
     * deployment. Ignored if not given.
     */
    public static final String DEPLOYMENT_PROPERTIES = "deployment.properties";

    /**
     * The name of the system property that resolves to the JAR file containing this OSGi application. Relative paths can be appended to the value of this
     * property.
     */
    public static final String OSGI_APPLICATION_ROOT = "osgi.application.root";

    private final Log log;
    private final ContainerTermination termination;
    private final StartLevels levels;

    /**
     * Dependency injected constructor.
     *
     * @param log         the log sink to use.
     * @param termination the container termination component.
     * @param levels      the optional start level fine tuning component.
     */
    OsgiApplication(final Log<OsgiApplication> log, final ContainerTermination termination, final @Optional StartLevels levels) {
        this.log = log;
        this.termination = termination;
        this.levels = levels;
    }

    public void run(final String[] arguments) throws Exception {
        if (Security.CONTROLLED) {
            try {
                AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                    public Void run() throws Exception {
                        start();
                        return null;
                    }
                });
            } catch (final PrivilegedActionException e) {
                throw (Exception) e.getCause();
            }
        } else {
            start();
        }
    }

    private void start() throws Exception {
        final boolean cached = true;

        /*
         * Embedding OSGi: http://njbartlett.name/2011/03/07/embedding-osgi.html
         */

        final ClassLoader loader = ClassLoaders.findClassLoader(getClass(), true);

        final FrameworkFactory factory = ServiceProviders.findInstance(FrameworkFactory.class, loader);
        if (factory == null) {
            throw new IllegalStateException("No OSGi framework found");
        }

        final Properties global = System.getProperties();

        final RegexBasedInterpolator placeholders = new RegexBasedInterpolator();
        final RecursionInterceptor recursion = new SimpleRecursionInterceptor();

        placeholders.addValueSource(new PropertiesBasedValueSource(global));
        placeholders.setCacheAnswers(true);
        placeholders.setReusePatterns(true);

        final Class<? extends FrameworkFactory> frameworkType = factory.getClass();
        final URL frameworkURL = Archives.containing(frameworkType);

        Policy.setPolicy(new FrameworkPolicy(frameworkType));

        final Properties defaults = loadProperties(resource("osgi.properties"), null);

        final String application = global.getProperty(APPLICATION_PROPERTIES);
        final Properties distribution = loadProperties(application == null
                                                       ? resource(APPLICATION_PROPERTIES)
                                                       : Archives.open(cached, new URL(placeholders.interpolate(application, recursion))), defaults);

        final String deployment = global.getProperty(DEPLOYMENT_PROPERTIES);
        final Properties properties = deployment == null
                                      ? distribution
                                      : loadProperties(Archives.open(cached, new URL(placeholders.interpolate(deployment, recursion))), distribution);

        final Map<String, String> config = new HashMap<String, String>();

        @SuppressWarnings("unchecked")
        final List<String> keys = Collections.list((Enumeration<String>) properties.propertyNames());

        for (final String property : keys) {
            config.put(property, placeholders.interpolate((global.containsKey(property) ? global : properties).getProperty(property), recursion));
        }

        config.put(Constants.FRAMEWORK_BUNDLE_PARENT, Constants.FRAMEWORK_BUNDLE_PARENT_FRAMEWORK);
        config.put(OSGI_APPLICATION_ROOT, String.format("%s:%s%s", Archives.Nested.PROTOCOL, Archives.Nested.rootURL(frameworkURL), Archives.Nested.DELIMITER));

        for (final Map.Entry<String, String> entry : config.entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue());
        }

        log.debug("OSGi system properties: %s", config);

        final Framework framework = factory.newFramework(config);

        final List<Bundle> bundles = new ArrayList<Bundle>();
        final Set<URL> jars = new TreeSet<URL>(new Comparator<URL>() {
            public int compare(final URL url1, final URL url2) {
                return url1.getPath().compareTo(url2.getPath());
            }
        });

        final URL archive = Archives.containing(Archives.containing(getClass()));

        for (final String list : Archives.Nested.list(cached, archive)) {
            log.debug("Checking packaged archives '%s' in %s", list, archive);

            for (final URL url : Archives.Nested.dependencies(cached, archive, list)) {

                // make sure to select only those archives that have an OSGi bundle symbolic name (it is a mandatory OSGi header)
                if (Archives.attributes(cached, url, Constants.BUNDLE_SYMBOLICNAME)[0] != null) {
                    jars.add(url);
                }
            }
        }

        if (jars.isEmpty()) {
            throw new IllegalStateException(String.format("No OSGi bundles found in %s", archive));
        }

        framework.start();

        termination.add(new Command.Job<Exception>() {
            public void run() {
                try {
                    switch (framework.getState()) {
                    case Framework.ACTIVE:
                    case Framework.STARTING:
                        final PrivilegedAction<Void> stop = new PrivilegedAction<Void>() {
                            public Void run() {
                                try {
                                    framework.stop(0);
                                } catch (final BundleException e) {
                                    log.error(e, "Could not stop the OSGi framework");
                                }

                                return null;
                            }
                        };

                        if (Security.CONTROLLED) {
                            AccessController.doPrivileged(stop);
                        } else {
                            stop.run();
                        }

                        // fall through
                    case Framework.STOPPING:
                        try {
                            framework.waitForStop(0);
                        } catch (final InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }

                        log.debug("OSGi framework (%s) stopped", framework.getSymbolicName());

                    default:
                        break;
                    }
                } catch (final Exception e) {
                    log.error(e, "Error stopping the OSGi framework");
                }

                Archives.Nested.unload(frameworkURL);
            }
        });

        final BundleContext system = framework.getBundleContext();

        for (final URL url : jars) {
            log.debug("Installing bundle %s", url);

            final Bundle bundle = system.installBundle(url.toExternalForm());

            if (bundle.getHeaders().get(Constants.FRAGMENT_HOST) == null) {
                bundles.add(bundle);
            }
        }

        final FrameworkStartLevel startLevel = levels == null ? null : framework.adapt(FrameworkStartLevel.class);

        final List<List<Bundle>> order = new ArrayList<List<Bundle>>();

        if (startLevel != null) {

            // start level 0: framework stopped
            // start level 1: framework started
            // start level 2+ : bundle start levels

            final List<Bundle> remaining = new ArrayList<Bundle>(bundles);

            int level = 1;
            for (final List<Bundle> list : list(bundles)) {

                // make sure no bundle is included that has already been assigned a start level
                list.retainAll(remaining);

                // no bundle to start at this level: discard the rest
                if (list.isEmpty()) {
                    break;
                }

                remaining.removeAll(list);
                order.add(list);

                ++level;
                for (final Bundle bundle : list) {
                    bundle.adapt(BundleStartLevel.class).setStartLevel(level);
                }
            }

            if (!remaining.isEmpty()) {
                order.add(remaining);

                ++level;
                for (final Bundle bundle : remaining) {
                    bundle.adapt(BundleStartLevel.class).setStartLevel(level);
                }
            }
        }

        for (final Bundle bundle : bundles) {
            bundle.start(Bundle.START_ACTIVATION_POLICY);
        }

        if (startLevel != null) {
            final int limit = order.size() + 1;
            final int level = Math.max(1, Math.min(limit, levels.initial(limit)));
            startLevel.setStartLevel(level);

            log.debug("OSGi framework (%s) started at level %d of %d", framework.getSymbolicName(), level, limit);
        } else {
            log.debug("OSGi framework (%s) started", framework.getSymbolicName());
        }

        framework.waitForStop(0);
    }

    private InputStream resource(final String name) {
        return AccessController.doPrivileged(new PrivilegedAction<InputStream>() {
            public InputStream run() {
                return ClassLoaders.readResource(getClass(), name);
            }
        });
    }

    private List<List<Bundle>> list(final List<Bundle> bundles) {
        final List<List<Bundle>> lists = levels.bundles(new ArrayList<Bundle>(bundles));
        return lists == null ? Collections.<List<Bundle>>emptyList() : lists;
    }

    private Properties loadProperties(final InputStream stream, final Properties defaults) throws IOException {
        if (stream == null) {
            return defaults == null ? new Properties() : defaults;
        } else {
            final Properties properties = defaults == null ? new Properties() : new Properties(defaults);

            try {
                properties.load(stream);
            } finally {
                stream.close();
            }

            return properties;
        }
    }

    /**
     * Propagates the permissions of this domain (which must thus be {@link java.security.AllPermission}) to the OSGi framework.
     *
     * @author Tibor Varga
     */
    private static class FrameworkPolicy extends Policy {

        private final Policy delegate = Policy.getPolicy();

        private final ProtectionDomain launcherDomain = OsgiApplication.class.getProtectionDomain();
        private final ProtectionDomain frameworkDomain;

        public FrameworkPolicy(final Class<? extends FrameworkFactory> frameworkType) {
            this.frameworkDomain = frameworkType.getProtectionDomain();
        }

        @Override
        public Provider getProvider() {
            return delegate.getProvider();
        }

        @Override
        public String getType() {
            return delegate.getType();
        }

        @Override
        public Parameters getParameters() {
            return delegate.getParameters();
        }

        @Override
        public PermissionCollection getPermissions(final CodeSource source) {
            return delegate.getPermissions(source == frameworkDomain.getCodeSource() ? launcherDomain.getCodeSource() : source);
        }

        @Override
        public PermissionCollection getPermissions(final ProtectionDomain domain) {
            return delegate.getPermissions(domain == frameworkDomain ? launcherDomain : domain);
        }

        @Override
        public boolean implies(final ProtectionDomain domain, final Permission permission) {
            return delegate.implies(domain == frameworkDomain ? launcherDomain : domain, permission);
        }

        @Override
        public void refresh() {
            delegate.refresh();
        }
    }
}

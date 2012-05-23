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

package org.fluidity.deployment.osgi.launcher;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
import java.util.jar.JarFile;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ContainerBoundary;
import org.fluidity.composition.spi.ContainerTermination;
import org.fluidity.foundation.Archives;
import org.fluidity.foundation.ClassLoaders;
import org.fluidity.foundation.Log;
import org.fluidity.foundation.ServiceProviders;

import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.RecursionInterceptor;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.interpolation.SimpleRecursionInterceptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * A command line main class that bootstraps the application's dependency injection container, bootstraps an OSGi container, installs all JAR files as bundles
 * from <code>META-INF/bundles/</code>, starts all non-fragment bundles, and then waits for the OSGi framework to stop.
 * <p/>
 * This class is public for its main method to be found by the Java launcher.
 *
 * @author Tibor Varga
 */
public final class OsgiApplicationBootstrap {

    public static final String APPLICATION_PROPERTIES = "application.properties";
    public static final String DEPLOYMENT_PROPERTIES = "deployment.properties";

    private final Log<OsgiApplicationBootstrap> log;
    private final ContainerTermination termination;

    public OsgiApplicationBootstrap(final Log<OsgiApplicationBootstrap> log, final ContainerTermination termination) {
        this.log = log;
        this.termination = termination;
    }

    private void run() throws Exception {

        /*
         * Embedding OSGi: http://njbartlett.name/2011/03/07/embedding-osgi.html
         */

        final FrameworkFactory factory = ServiceProviders.findInstance(FrameworkFactory.class, ClassLoaders.findClassLoader(getClass()));
        if (factory == null) {
            throw new IllegalStateException("No OSGi framework found");
        }

        final URL frameworkURL = Archives.jarFile(ClassLoaders.findClassResource(factory.getClass())).getJarFileURL();
        final Properties defaults = loadProperties(ClassLoaders.readResource(getClass(), "osgi.properties"), null);
        final Properties distribution = loadProperties(ClassLoaders.readResource(getClass(), APPLICATION_PROPERTIES), defaults);

        final String deployment = System.getProperty(DEPLOYMENT_PROPERTIES);
        final Properties properties = deployment == null ? distribution : loadProperties(new FileInputStream(deployment), distribution);

        final Map<String, String> config = new HashMap<String, String>();

        final RegexBasedInterpolator interpolator = new RegexBasedInterpolator();

        interpolator.addValueSource(new PropertiesBasedValueSource(System.getProperties()));
        interpolator.addValueSource(new PropertiesBasedValueSource(properties));

        final RecursionInterceptor interceptor = new SimpleRecursionInterceptor();

        interpolator.setCacheAnswers(true);
        interpolator.setReusePatterns(true);

        @SuppressWarnings("unchecked")
        final List<String> keys = Collections.list((Enumeration<String>) properties.propertyNames());

        for (final String property : keys) {
            config.put(property, interpolator.interpolate(properties.getProperty(property), interceptor));
        }

        config.put(Constants.FRAMEWORK_BUNDLE_PARENT, Constants.FRAMEWORK_BUNDLE_PARENT_FRAMEWORK);

        log.info("OSGi system properties: %s", config);

        final Framework framework = factory.newFramework(config);

        final List<Bundle> bundles = new ArrayList<Bundle>();
        final Set<URL> jars = new TreeSet<URL>(new Comparator<URL>() {
            public int compare(final URL url1, final URL url2) {
                return url1.getPath().compareTo(url2.getPath());
            }
        });

        // find all JAR manifests visible to our class loader
        final List<URL> manifests = ClassLoaders.findResources(getClass(), JarFile.MANIFEST_NAME);
        for (final URL manifest : manifests) {
            final URL url = Archives.jarFile(manifest).getJarFileURL();

            if (!frameworkURL.equals(url)) {

                // find and install those JAR files that have an OSGi bundle symbolic name (it is a mandatory OSGi header)
                final String[] markers = Archives.manifestAttributes(manifest, Constants.BUNDLE_SYMBOLICNAME);

                if (markers[0] != null) {
                    jars.add(url);
                }
            }
        }

        framework.start();

        termination.run(new Runnable() {
            public void run() {
                if (framework.getState() == Framework.ACTIVE) {
                    try {
                        log.info("Stopping the OSGi container");
                        framework.stop(0);
                        framework.waitForStop(0);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (final Exception e) {
                        log.error(e, "Could not stop the OSGi container");
                    }
                }
            }
        });

        final BundleContext system = framework.getBundleContext();

        for (final URL url : jars) {
            log.info("Installing bundle %s", url);

            final Bundle bundle = system.installBundle(url.toExternalForm());

            if (bundle.getHeaders().get(Constants.FRAGMENT_HOST) == null) {
                bundles.add(bundle);
            }
        }

        for (final Bundle bundle : bundles) {
            log.info("Starting bundle %s", bundle.getSymbolicName());

            bundle.start();
        }

        framework.waitForStop(0);
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

    public static void main(final String[] args) throws Exception {
        new ContainerBoundary().getComponent(OsgiApplicationBootstrap.class, new ComponentContainer.Bindings() {
            @SuppressWarnings("unchecked")
            public void bindComponents(final ComponentContainer.Registry registry) {
                registry.bindComponent(OsgiApplicationBootstrap.class);
            }
        }).run();
    }
}

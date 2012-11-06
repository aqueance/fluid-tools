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

package org.fluidity.tests.osgi.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarFile;

import org.fluidity.deployment.osgi.BundleComponentContainer;
import org.fluidity.deployment.osgi.Service;
import org.fluidity.foundation.Archives;
import org.fluidity.foundation.ClassLoaders;
import org.fluidity.foundation.Lists;
import org.fluidity.foundation.ServiceProviders;
import org.fluidity.tests.osgi.BundleTest;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

/**
 * Launches an OSGi framework, and loads and starts integration test bundles.
 *
 * @author Tibor Varga
 */
public final class IntegrationTest {

    public static final String INTEGRATION_TEST_MARKER = "Integration-Test";
    private Framework framework;
    private final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

    @Factory
    @SuppressWarnings("unchecked")
    public Object[] startContainer() throws Exception {

        /*
         * Embedding OSGi: http://njbartlett.name/2011/03/07/embedding-osgi.html
         */

        try {
            final ClassLoader loader = ClassLoaders.findClassLoader(IntegrationTest.class, true);
            final Properties properties = loadProperties("system.properties");

            final Map<String, String> config = new HashMap<String, String>();

            for (final Map.Entry entry : properties.entrySet()) {
                config.put((String) entry.getKey(), (String) entry.getValue());
            }

            config.put(Constants.FRAMEWORK_BUNDLE_PARENT, Constants.FRAMEWORK_BUNDLE_PARENT_APP);

            final FrameworkFactory factory = ServiceProviders.findInstance(FrameworkFactory.class, loader);

            framework = factory.newFramework(config);
            framework.start();

            final BundleContext system = framework.getBundleContext();
            final List<Bundle> bundles = new ArrayList<Bundle>();

            // find all JAR manifests visible to our class loader
            for (final URL manifest : ClassLoaders.findResources(ClassLoaders.findClassLoader(IntegrationTest.class, true), JarFile.MANIFEST_NAME)) {

                // find and install those JAR files that have both an OSGi bundle symbolic name and our integration test marker
                final String[] markers = Archives.attributes(true, manifest, INTEGRATION_TEST_MARKER, Constants.BUNDLE_SYMBOLICNAME);

                if (markers[0] != null && markers[1] != null) {
                    final Bundle bundle = system.installBundle(Archives.containing(manifest).toExternalForm());

                    if (bundle.getHeaders().get(Constants.FRAGMENT_HOST) == null) {
                        bundles.add(bundle);
                    }
                }
            }

            final CountDownLatch starting = new CountDownLatch(bundles.size());

            final BundleListener bundleListener = new BundleListener() {
                public void bundleChanged(final BundleEvent event) {
                    if (event.getType() == BundleEvent.STARTED) {
                        starting.countDown();
                    }
                }
            };

            system.addBundleListener(bundleListener);

            for (final Bundle bundle : bundles) {
                bundle.start();
            }

            final boolean started = starting.await(1, TimeUnit.MINUTES);

            system.removeBundleListener(bundleListener);

            assert started : "Not all bundles started";

            // run this before any other test is discovered
            testRestartingBundles();

            final ServiceReference[] statuses = system.getServiceReferences(BundleComponentContainer.Status.class.getName(), null);

            assert statuses != null : String.format("No component status services registered (%s)", BundleComponentContainer.Status.class.getName());
            assert statuses.length == bundles.size() : String.format("Component status service count (%d) != bundle count (%d)", statuses.length, bundles.size());

            for (final ServiceReference reference : statuses) {
                final BundleComponentContainer.Status status = (BundleComponentContainer.Status) system.getService(reference);

                final String bundleName = reference.getBundle().getSymbolicName();
                final Collection<Class<?>> failed = status.failed();
                assert failed.isEmpty() : String.format("Failed components in bundle %s: %s", bundleName, failed);

                final Map<Class<?>, Collection<Service>> inactive = status.inactive();
                assert inactive.isEmpty() : String.format("Inactive components in bundle %s: %s", bundleName, inactive);
            }

            final List<BundleTest> tests = new ArrayList<BundleTest>();

            final ServiceReference[] references = system.getServiceReferences(BundleTest.class.getName(), null);
            assert references != null && references.length > 0: String.format("No integration tests found (%s)", BundleTest.class.getName());

            for (final ServiceReference reference : references) {
                tests.add((BundleTest) system.getService(reference));
            }

            assert !tests.isEmpty() : "No tests found to execute";

            return Lists.asArray(BundleTest.class, tests);
        } catch (final Throwable problem) {
            error.set(problem);
            return new BundleTest[0];
        }
    }

    private Properties loadProperties(final String name) throws IOException {
        final InputStream stream = ClassLoaders.readResource(IntegrationTest.class, name);
        final Properties properties = new Properties();

        try {
            properties.load(stream);
        } finally {
            stream.close();
        }

        return properties;
    }

    @Test(enabled = false)
    public void testRestartingBundles() throws Exception {

        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        final Throwable problem = error.get();
        assert problem == null : problem;

        for (final Bundle bundle : framework.getBundleContext().getBundles()) {
            if (bundle.getBundleId() > 0) {
                bundle.stop();
            }
        }

        for (final Bundle bundle : framework.getBundleContext().getBundles()) {
            if (bundle.getBundleId() > 0) {
                bundle.start();
            }
        }
    }

    @AfterSuite(alwaysRun = true)
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void stopContainer() throws Exception {
        framework.stop();
        framework.waitForStop(0);
        assert error.get() == null : error.get();
    }
}

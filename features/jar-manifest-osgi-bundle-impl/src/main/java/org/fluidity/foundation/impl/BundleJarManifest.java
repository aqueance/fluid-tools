/*
 * Copyright (c) 2006-2018 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.foundation.impl;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.jar.Attributes;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ContainerBoundary;
import org.fluidity.deployment.maven.Logger;
import org.fluidity.deployment.osgi.impl.BundleBootstrap;
import org.fluidity.deployment.plugin.spi.JarManifest;
import org.fluidity.deployment.plugin.spi.SecurityPolicy;
import org.fluidity.foundation.Archives;
import org.fluidity.foundation.ClassLoaders;
import org.fluidity.foundation.Exceptions;
import org.fluidity.foundation.Methods;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.PrefixAwareRecursionInterceptor;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.PrefixedPropertiesValueSource;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.RecursionInterceptor;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
import org.osgi.framework.Version;

import static org.osgi.framework.Constants.BUNDLE_ACTIVATOR;
import static org.osgi.framework.Constants.BUNDLE_CLASSPATH;
import static org.osgi.framework.Constants.BUNDLE_COPYRIGHT;
import static org.osgi.framework.Constants.BUNDLE_DESCRIPTION;
import static org.osgi.framework.Constants.BUNDLE_DOCURL;
import static org.osgi.framework.Constants.BUNDLE_MANIFESTVERSION;
import static org.osgi.framework.Constants.BUNDLE_NAME;
import static org.osgi.framework.Constants.BUNDLE_NATIVECODE;
import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME;
import static org.osgi.framework.Constants.BUNDLE_VENDOR;
import static org.osgi.framework.Constants.BUNDLE_VERSION;
import static org.osgi.framework.Constants.DYNAMICIMPORT_PACKAGE;
import static org.osgi.framework.Constants.EXPORT_PACKAGE;
import static org.osgi.framework.Constants.FRAGMENT_HOST;
import static org.osgi.framework.Constants.IMPORT_PACKAGE;
import static org.osgi.framework.Constants.REQUIRE_BUNDLE;
import static org.osgi.framework.Constants.REQUIRE_CAPABILITY;

/**
 * Modifies the JAR manifest of the host project's artifact to make the artifact a self-containing OSGi bundle. That is, the project's dependencies will be
 * embedded in the produced artifact and the proper OSGi manifest headers will be generated for the OSGi framework to treat those dependencies as part of the
 * bundle.
 *
 * @author Tibor Varga
 */
@SuppressWarnings("WeakerAccess")
final class BundleJarManifest implements JarManifest {

    public static final String DEFAULT_BUNDLE_VERSION = Version.emptyVersion.toString();
    public static final String VERSION_PREFIX = "version:";

    public SecurityPolicy processManifest(final MavenProject project,
                                          final Attributes attributes,
                                          final SecurityPolicy policy,
                                          final Logger log,
                                          final Dependencies dependencies) throws MojoExecutionException {
        dependencies.attribute(BUNDLE_CLASSPATH, ",");

        addEntry(attributes, BUNDLE_MANIFESTVERSION, "2");
        addEntry(attributes, BUNDLE_NAME, project::getName);
        addEntry(attributes, BUNDLE_SYMBOLICNAME, project::getArtifactId);
        addEntry(attributes, BUNDLE_VERSION, project::getVersion);
        addEntry(attributes, BUNDLE_DESCRIPTION, project::getDescription);
        addEntry(attributes, BUNDLE_DOCURL, project::getUrl);
        addEntry(attributes, BUNDLE_VENDOR, () -> project.getOrganization().getName());
        addEntry(attributes, BUNDLE_COPYRIGHT, () -> {
            final String year = project.getInceptionYear();
            return year == null ? null : String.format("Copyright %s (c) %s. All rights reserved.", project.getOrganization().getName(), year);
        });
        addEntry(attributes, REQUIRE_CAPABILITY, () -> {

            // https://www.oracle.com/technetwork/java/javase/versioning-naming-139433.html
            final String[] version = System.getProperty("java.version").split("\\.");
            return String.format("osgi.ee;filter:=(&(osgi.ee=JavaSE)(version>=%s.%s))", version[0], version[1]);
        });

        final String version = attributes.getValue(BUNDLE_VERSION);

        if (version != null) {
            attributes.putValue(BUNDLE_VERSION, verify(version));
        } else {
            addEntry(attributes, BUNDLE_VERSION, DEFAULT_BUNDLE_VERSION);
        }

        final RegexBasedInterpolator interpolator = new RegexBasedInterpolator();
        final List<String> prefixes = Collections.singletonList("project.");

        interpolator.addValueSource(new PropertiesBasedValueSource(System.getProperties()));
        interpolator.addValueSource(new PrefixedPropertiesValueSource(prefixes, project.getProperties(), true));
        interpolator.addValueSource(new PrefixedObjectValueSource(prefixes, project.getModel(), true));

        final RecursionInterceptor interceptor = new PrefixAwareRecursionInterceptor(prefixes, true);

        interpolator.setCacheAnswers(true);
        interpolator.setReusePatterns(true);

        interpolator.addPostProcessor((ignored, value) -> verify((String) value));

        try {
            interpolateHeaders(attributes, interpolator, interceptor,
                               EXPORT_PACKAGE, IMPORT_PACKAGE, DYNAMICIMPORT_PACKAGE, FRAGMENT_HOST, REQUIRE_BUNDLE, BUNDLE_NATIVECODE);
        } catch (final InterpolationException e) {
            throw new IllegalStateException(e);
        }

        final Collection<Artifact> artifacts = dependencies.compiler();

        // if embedding project dependencies, see if Fluid Tools composition is included and if so, add a bundle activator to bootstrap the system.
        if (!artifacts.isEmpty()) {
            try {

                // create a class loader that sees the project's compile-time dependencies
                final Set<URL> urls = new HashSet<>();

                final String skippedId = project.getArtifact().getId();
                for (final Artifact dependency : artifacts) {

                    // we don't need the project artifact and don't want to prevent the caller from overwriting it by locking the file...
                    if (!Objects.equals(skippedId, dependency.getId())) {
                        urls.add(dependency.getFile().toURI().toURL());
                    }
                }

                final Method method = Methods.get(BootstrapDiscovery.class, BootstrapDiscovery::activator)[0];

                urls.add((Archives.containing(BootstrapDiscovery.class)));
                urls.add((Archives.containing(BootstrapDiscoveryImpl.class)));
                urls.add((Archives.containing(ComponentContainer.class)));

                final String activator = isolate(null, urls, BootstrapDiscoveryImpl.class, method);

                if (activator != null) {
                    addEntry(attributes, BUNDLE_ACTIVATOR, activator);
                }

                if (log.active()) {
                    final String value = attributes.getValue(BUNDLE_ACTIVATOR);
                    log.detail("Bundle activator: %s", value == null ? "none" : Objects.equals(value, BundleBootstrap.class.getName()) ? "built in" : value);
                }
            } catch (final ClassNotFoundException e) {
                // that's OK
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        }

        return new OsgiLocalPermissions(policy, artifacts, attributes.getValue(DYNAMICIMPORT_PACKAGE), attributes.getValue(IMPORT_PACKAGE), attributes.getValue(EXPORT_PACKAGE));
    }

    private String verify(final String version) {
        try {
            Version.parseVersion(version);
            return version;
        } catch (final IllegalArgumentException ignored) {
            return correct(version);
        }
    }

    private void interpolateHeaders(final Attributes attributes,
                                    final Interpolator interpolator,
                                    final RecursionInterceptor interceptor,
                                    final String... headers) throws InterpolationException {
        assert interpolator != null;

        for (final String header : headers) {
            final String value = attributes.getValue(header);

            if (value != null) {
                attributes.putValue(header, interpolator.interpolate(value, VERSION_PREFIX, interceptor));
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private String correct(final String version) {
        final StringBuilder bundleVersion = new StringBuilder();

        int partCount = 0;
        for (final String part : version.split("[.-]")) {
            if (partCount < 3) {
                try {
                    Integer.parseInt(part);   // just checking if part is numeric

                    // part is indeed numeric
                    bundleVersion.append('.').append(part);
                } catch (final NumberFormatException e) {

                    // part is not numeric
                    partCount = numericVersion(bundleVersion, partCount);
                    bundleVersion.append('.').append(part.replaceAll("\\W", "_"));
                }
            } else {
                bundleVersion.append(partCount == 3 ? '.' : '-').append(part.replaceAll("\\W", "_"));
            }

            ++partCount;
        }

        numericVersion(bundleVersion, partCount);

        return bundleVersion.toString().substring(1);
    }

    /**
     * Creates an isolated URL class loader for the given list of URLs, loads using that class loader and instantiates the given <code>type</code>, and calls
     * the given <code>method</code> on it with the given <code>arguments</code> to return its return value.
     *
     * @param parent    the class loader to load the method parameters and the return values; this class loader must not see the given <code>type</code>.
     * @param urls      the list of URLs to use for the isolated class loader; make sure the list contains the JARs containing the type to load.
     * @param type      the command class to load and invoke the given method on; must have a public zero-argument constructor.
     * @param run       the public method to call; the parameter types and the return type must be loaded by the <code>parent</code> class loader and the declaring
     *                  class must be either visible to the <code>parent</code> class loader or listed in the given list of URLs.
     * @param arguments the arguments to pass to the command.
     * @param <T>       the return type of the given <code>method</code>.
     *
     * @return whatever the command returns.
     *
     * @throws ClassNotFoundException when the given class could not be found in the given URLs.
     * @throws InstantiationException when the given class could not be instantiated.
     * @throws org.fluidity.foundation.Exceptions.Wrapper when anything else goes wrong.
     */
    @SuppressWarnings("unchecked")
    private static <T> T isolate(final ClassLoader parent, final Collection<URL> urls, final Class<?> type, final Method run, final Object... arguments)
            throws Exceptions.Wrapper, ClassNotFoundException, InstantiationException {
        try {
            return Archives.Cache.access(() -> Exceptions.wrap(() -> {
                final ClassLoader isolated = ClassLoaders.create(urls, parent, null);

                try {

                    // find the command
                    final Object command = isolated.loadClass(type.getName()).newInstance();

                    // find the method to call in the other class loader
                    final Method method = isolated
                            .loadClass(run.getDeclaringClass().getName())
                            .getDeclaredMethod(run.getName(), (Class[]) run.getParameterTypes());

                    method.setAccessible(true);
                    return (T) method.invoke(command, arguments);
                } catch (final NoSuchMethodException | IllegalAccessException e) {
                    throw new AssertionError(e);
                }
            }));
        } catch (final Exceptions.Wrapper wrapper) {
            throw wrapper
                    .rethrow(ClassNotFoundException.class)
                    .rethrow(InstantiationException.class);
        }
    }

    /**
     * Returns the name of the bundle activator class if Fluid Tools is used in the host Maven project.
     */
    interface BootstrapDiscovery {

        /**
         * Returns the name of the bundle activator class, if any.
         *
         * @return the name of the bundle activator, or <code>null</code> if no Fluid Tools is used in the project.
         */
        String activator();
    }

    /**
     * This class is loaded from a bespoke class loader defined on the compile-time dependencies of the host Maven project. It checks if the {@link
     * ContainerBoundary} and {@link BundleBootstrap} classes can be found in the project's compile-time class path.
     */
    @SuppressWarnings("WeakerAccess")   // can't be loaded by BundleJarManifest unless made public
    public static final class BootstrapDiscoveryImpl implements BootstrapDiscovery {

        @SuppressWarnings("ConstantConditions")     // the condition in question is used to attempt to load a class
        public String activator() {
            try {

                // try to load the classes from the artifact's compile-time dependencies
                return ContainerBoundary.class != null ? BundleBootstrap.class.getName() : null;
            } catch (final NoClassDefFoundError e) {

                // class could not be found
                return null;
            }
        }
    }

    private void addEntry(final Attributes attributes, final String entry, final Supplier<String> metadata) {
        if (attributes.getValue(entry) == null) {
            String value;

            try {
                value = metadata.get();
            } catch (final NullPointerException | IndexOutOfBoundsException e) {
                value = null;
            }

            if (value != null) {
                attributes.putValue(entry, value);
            }
        }
    }

    private boolean addEntry(final Attributes attributes, final String entry, final String value) {
        if (attributes.getValue(entry) == null && value != null) {
            attributes.putValue(entry, value);
            return true;
        } else {
            return false;
        }
    }

    private int numericVersion(final StringBuilder bundleVersion, int partCount) {
        for (; partCount < 3; ++partCount) {
            bundleVersion.append(".0");
        }

        return partCount;
    }
}

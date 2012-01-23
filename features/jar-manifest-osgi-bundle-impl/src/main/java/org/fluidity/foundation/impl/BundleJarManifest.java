/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes;

import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.ContainerBoundary;
import org.fluidity.deployment.osgi.BundleBootstrap;
import org.fluidity.deployment.plugin.spi.JarManifest;
import org.fluidity.foundation.Archives;
import org.fluidity.foundation.ClassLoaders;
import org.fluidity.foundation.Methods;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.osgi.framework.Version;

import static org.osgi.framework.Constants.BUNDLE_ACTIVATOR;
import static org.osgi.framework.Constants.BUNDLE_CLASSPATH;
import static org.osgi.framework.Constants.BUNDLE_COPYRIGHT;
import static org.osgi.framework.Constants.BUNDLE_DESCRIPTION;
import static org.osgi.framework.Constants.BUNDLE_DOCURL;
import static org.osgi.framework.Constants.BUNDLE_MANIFESTVERSION;
import static org.osgi.framework.Constants.BUNDLE_NAME;
import static org.osgi.framework.Constants.BUNDLE_NATIVECODE;
import static org.osgi.framework.Constants.BUNDLE_NATIVECODE_OSVERSION;
import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME;
import static org.osgi.framework.Constants.BUNDLE_VENDOR;
import static org.osgi.framework.Constants.BUNDLE_VERSION;
import static org.osgi.framework.Constants.BUNDLE_VERSION_ATTRIBUTE;
import static org.osgi.framework.Constants.DYNAMICIMPORT_PACKAGE;
import static org.osgi.framework.Constants.EXPORT_PACKAGE;
import static org.osgi.framework.Constants.FRAGMENT_HOST;
import static org.osgi.framework.Constants.IMPORT_PACKAGE;
import static org.osgi.framework.Constants.PACKAGE_SPECIFICATION_VERSION;
import static org.osgi.framework.Constants.REQUIRE_BUNDLE;
import static org.osgi.framework.Constants.VERSION_ATTRIBUTE;

/**
 * Modifies the JAR manifest of the host project's artifact so that an OSGi container finds its embedded JAR files and adds them to the bundle's class path.
 *
 * @author Tibor Varga
 */
public class BundleJarManifest implements JarManifest {

    public static final String DEFAULT_BUNDLE_VERSION = Version.emptyVersion.toString();

    public boolean needsCompileDependencies() {
        return true;
    }

    public boolean processManifest(final MavenProject project,
                                   final Attributes attributes,
                                   final List<String> paths,
                                   final Collection<Artifact> dependencies) {
        final StringBuilder classpath = new StringBuilder();

        for (final String dependency : paths) {
            if (classpath.length() > 0) {
                classpath.append(',');
            }

            classpath.append(dependency);
        }

        if (classpath.length() > 0) {
            attributes.putValue(BUNDLE_CLASSPATH, classpath.toString());
        }

        addEntry(attributes, BUNDLE_MANIFESTVERSION, "2");
        addEntry(attributes, BUNDLE_NAME, new Metadata() { public String get() { return project.getName(); } });
        addEntry(attributes, BUNDLE_SYMBOLICNAME, new Metadata() { public String get() { return project.getArtifactId(); } });
        addEntry(attributes, BUNDLE_VERSION, new Metadata() { public String get() { return project.getVersion(); } });
        addEntry(attributes, BUNDLE_DESCRIPTION, new Metadata() { public String get() { return project.getDescription(); } });
        addEntry(attributes, BUNDLE_DOCURL, new Metadata() { public String get() { return project.getUrl(); } });
        addEntry(attributes, BUNDLE_VENDOR, new Metadata() { public String get() { return project.getOrganization().getName(); } });
        addEntry(attributes, BUNDLE_COPYRIGHT, new Metadata() {
            public String get() {
                final String year = project.getInceptionYear();
                return year == null ? null : String.format("Copyright %s (c) %s. All rights reserved.", project.getOrganization().getName(), year);
            }
        });

        final String version = attributes.getValue(BUNDLE_VERSION);

        if (version != null) {
            attributes.putValue(BUNDLE_VERSION, verify(version));
        } else {
            addEntry(attributes, BUNDLE_VERSION, DEFAULT_BUNDLE_VERSION);
        }

        final Properties properties = project.getProperties();

        if (properties != null && !properties.isEmpty()) {
            parseVersions(attributes, properties, VERSION_ATTRIBUTE, EXPORT_PACKAGE, IMPORT_PACKAGE, DYNAMICIMPORT_PACKAGE);
            parseVersions(attributes, properties, BUNDLE_VERSION_ATTRIBUTE, FRAGMENT_HOST, IMPORT_PACKAGE, DYNAMICIMPORT_PACKAGE, REQUIRE_BUNDLE);
            parseVersions(attributes, properties, BUNDLE_NATIVECODE_OSVERSION, BUNDLE_NATIVECODE);
            parseVersions(attributes, properties, PACKAGE_SPECIFICATION_VERSION, EXPORT_PACKAGE, IMPORT_PACKAGE);
        }

        if (!dependencies.isEmpty()) {
            ClassLoader classLoader = null;
            try {

                // create a class loader that sees the project's compile time dependencies
                final List<URL> urls = new ArrayList<URL>();

                final String skippedId = project.getArtifact().getId();
                for (final Artifact dependency : dependencies) {

                    // we don't need the project artifact and opening it may cause Windows to lock the file and prevent the caller to overwrite it
                    if (!skippedId.equals(dependency.getId())) {
                        urls.add(dependency.getFile().toURI().toURL());
                    }
                }

                final ClassLoader parent = getClass().getClassLoader();

                addJarFile(urls, parent, BundleActivatorProcessor.class);       // add the jar where the command we're about to invoke is found
                addJarFile(urls, parent, Command.class);                        // add the jar where the command interface is found

                // must not use our class loader as parent
                classLoader = ClassLoaders.create(null, urls.toArray(new URL[urls.size()]));

                // find the command
                final Object command = classLoader.loadClass(BundleActivatorProcessor.class.getName()).newInstance();

                // find the method to call in our class loader
                final Method run = Methods.get(Command.class, new Methods.Invoker<Command>() {
                    @SuppressWarnings("unchecked")
                    public void invoke(final Command capture) {
                        capture.run(null);
                    }
                });

                // find the method to call in the other class loader
                final Method method = classLoader.loadClass(run.getDeclaringClass().getName()).getDeclaredMethod(run.getName(), run.getParameterTypes());

                // see if the class loader can see the ContainerBoundary and BundleBootstrap classes and if so, find the bootstrap activator
                final String activator = (String) method.invoke(command, (Object) null);

                if (activator != null) {
                    if (attributes.getValue(BUNDLE_ACTIVATOR) == null) {
                        addEntry(attributes, BUNDLE_ACTIVATOR, activator);
                    } else {
                        throw new IllegalStateException(String.format(
                                "Bundle activator is already set: add @%s to %s and make sure the composition plugin is active in this project",
                                ComponentGroup.class.getName(),
                                activator.getClass()));
                    }
                }
            } catch (final ClassNotFoundException e) {
                // that's OK
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            } finally {
                if (classLoader != null) {
                    try {
                        ((Closeable) classLoader).close();
                    } catch (final IOException e) {
                        // ignore
                    }
                }
            }
        }

        return false;
    }

    private String verify(final String version) {
        try {
            Version.parseVersion(version);
            return version;
        } catch (final IllegalArgumentException ignored) {
            return correct(version);
        }
    }

    private String substituteVersion(final Properties map, final String specification) {
        final String version = map.getProperty(specification);
        return version == null ? specification : verify(version);
    }

    private void parseVersions(final Attributes attributes, Properties mapping, final String name, final String... headers) {
        assert mapping != null;
        final char[] parameter = name.concat("=").toCharArray();

        for (final String header : headers) {
            final String value = attributes.getValue(header);

            if (value != null) {
                final StringBuilder buffer = new StringBuilder();
                final StringBuilder version = new StringBuilder();
                final char[] chars = value.toCharArray();

                boolean quoting = false;
                boolean escaping = false;
                boolean collecting = false;

                for (int i = 0, limit = chars.length; i < limit; i++) {
                    final char c = chars[i];
                    switch (c) {
                    case '\\':
                        escaping = !escaping;
                        buffer.append(c);
                        break;

                    case '"':
                        if (!escaping) {
                            quoting = !quoting;
                        }

                        buffer.append(c);
                        escaping = false;

                        break;

                    case ']':
                    case ')':
                    case ',':
                        if (collecting) {
                            buffer.append(substituteVersion(mapping, version.toString()));
                            collecting = quoting;
                            version.setLength(0);
                        }

                        buffer.append(c);
                        escaping = false;

                        break;

                    case '[':
                    case '(':
                    case ' ':
                        buffer.append(c);
                        escaping = false;

                        break;

                    case ';':
                        if (!quoting && !escaping) {
                            if (collecting) {
                                buffer.append(substituteVersion(mapping, version.toString()));
                                collecting = false;

                                buffer.append(c);
                            } else {
                                buffer.append(c);

                                int matched;
                                for (matched = 0; matched < parameter.length && chars[++i] == parameter[matched]; ++matched) {
                                    buffer.append(chars[i]);
                                }

                                if (matched == parameter.length) {
                                    version.setLength(0);
                                    collecting = true;
                                } else {
                                    buffer.append(chars[i]);
                                }
                            }
                        } else {
                            buffer.append(c);
                        }

                        escaping = false;

                        break;

                    default:
                        (collecting ? version : buffer).append(c);

                        escaping = false;

                        break;
                    }
                }

                if (collecting) {
                    buffer.append(substituteVersion(mapping, version.toString()));
                }

                attributes.putValue(header, buffer.toString());
            }
        }
    }

    private String correct(final String version) {
        final StringBuilder bundleVersion = new StringBuilder();

        int partCount = 0;
        for (final String part : version.split("[\\.-]")) {
            if (partCount < 3) {
                try {
                    Integer.parseInt(part);   // just checking if part is numeric

                    // part is indeed numeric
                    bundleVersion.append('.').append(part);
                } catch (final NumberFormatException e) {

                    // part is not numeric
                    partCount = numericVersion(bundleVersion, partCount);
                    bundleVersion.append('.').append(part);
                }
            } else {
                bundleVersion.append(partCount == 3 ? '.' : '-').append(part.replaceAll("\\W", "_"));
            }

            ++partCount;
        }

        numericVersion(bundleVersion, partCount);

        return bundleVersion.toString().substring(1);
    }

    private void addJarFile(final List<URL> urls, final ClassLoader parent, final Class<?> type) {
        final URL source = parent.getResource(ClassLoaders.classResourceName(type));
        final URL jar = Archives.jarFile(source).getJarFileURL();

        if (jar != null) {
            urls.add(jar);
        } else {
            throw new IllegalArgumentException(String.format("Class %s was not loaded from a jar file: %s", type.getName(), source));
        }
    }

    /**
     * This class is loaded from a bespoke class loader defined on the compile time dependencies of the host project. It checks if the ContainerBoundary
     * and BundleBootstrap classes can be found in the project's compile time class path.
     */
    public static final class BundleActivatorProcessor implements Command<String, Void> {

        public String run(final Void ignored) {
            try {
                return ContainerBoundary.class != null ? BundleBootstrap.class.getName() : null;
            } catch (final NoClassDefFoundError e) {
                return null;
            }
        }
    }

    private void addEntry(final Attributes attributes, final String entry, final Metadata metadata) {
        if (attributes.getValue(entry) == null) {
            String value;

            try {
                value = metadata.get();
            } catch (final NullPointerException e) {
                value = null;
            } catch (final IndexOutOfBoundsException e) {
                value = null;
            }

            if (value != null) {
                attributes.putValue(entry, value);
            }
        }
    }

    private void addEntry(final Attributes attributes, final String entry, final String value) {
        if (attributes.getValue(entry) == null && value != null) {
            attributes.putValue(entry, value);
        }
    }

    private int numericVersion(final StringBuilder bundleVersion, int partCount) {
        for (; partCount < 3; ++partCount) {
            bundleVersion.append(".0");
        }

        return partCount;
    }

    /**
     * Encapsulates the action of getting some metadata. The idea is to be able to chain getters without considering <code>null</code> values along the way.
     */
    private interface Metadata {

        /**
         * Return some metadata without caring about calling methods on a <code>null</code> object.
         *
         * @return some metadata.
         */
        String get();
    }
}

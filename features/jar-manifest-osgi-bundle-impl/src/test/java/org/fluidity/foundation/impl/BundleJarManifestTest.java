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

import java.util.Collections;
import java.util.Objects;
import java.util.Properties;
import java.util.jar.Attributes;

import org.fluidity.deployment.maven.Logger;
import org.fluidity.deployment.plugin.spi.JarManifest;
import org.fluidity.deployment.plugin.spi.SecurityPolicy;
import org.fluidity.testing.Simulator;

import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.easymock.EasyMock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.osgi.framework.Constants.BUNDLE_CLASSPATH;
import static org.osgi.framework.Constants.BUNDLE_COPYRIGHT;
import static org.osgi.framework.Constants.BUNDLE_DESCRIPTION;
import static org.osgi.framework.Constants.BUNDLE_DOCURL;
import static org.osgi.framework.Constants.BUNDLE_NAME;
import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME;
import static org.osgi.framework.Constants.BUNDLE_VENDOR;
import static org.osgi.framework.Constants.BUNDLE_VERSION;
import static org.osgi.framework.Constants.BUNDLE_VERSION_ATTRIBUTE;
import static org.osgi.framework.Constants.DYNAMICIMPORT_PACKAGE;
import static org.osgi.framework.Constants.EXPORT_PACKAGE;
import static org.osgi.framework.Constants.FRAGMENT_HOST;
import static org.osgi.framework.Constants.IMPORT_PACKAGE;
import static org.osgi.framework.Constants.VERSION_ATTRIBUTE;

/**
 * @author Tibor Varga
 */
public class BundleJarManifestTest extends Simulator {

    private final MockObjects components = dependencies();
    private final JarManifest.Dependencies dependencies = components.normal(JarManifest.Dependencies.class);
    private final SecurityPolicy policy = components.normal(SecurityPolicy.class);
    private final JarManifest manifest = new BundleJarManifest();
    private final Log log = components.lenient(Log.class);

    private MavenProject project;

    @BeforeMethod
    protected void setup() throws Exception {
        project = new MavenProject(new Model());
    }

    private void expectations() throws MojoExecutionException {
        dependencies.attribute(BUNDLE_CLASSPATH, ",");

        EasyMock.expect(dependencies.compiler()).andReturn(Collections.emptyList());
    }

    @Test
    public void testEmptyClasspath() throws Exception {
        final Attributes attributes = new Attributes();

        expectations();

        verify((Task) () -> manifest.processManifest(project, attributes, policy, logger(), dependencies));

        assert attributes.getValue(BUNDLE_CLASSPATH) == null;

        final String version = attributes.getValue(BUNDLE_VERSION);
        assert Objects.equals(BundleJarManifest.DEFAULT_BUNDLE_VERSION, version) : version;
    }

    private Logger logger() {
        return Logger.initialize(log, false);
    }

    @DataProvider(name = "bundle-versions")
    public Object[][] versionNumbers() throws Exception {
        return new Object[][] {
                new Object[] { "1.0", "1.0" },
                new Object[] { "1.0.0", "1.0.0" },
                new Object[] { "1.2", "1.2" },
                new Object[] { "1.2.3", "1.2.3" },
                new Object[] { "1-beta-1", "1.0.0.beta-1" },
                new Object[] { "1.2-SNAPSHOT", "1.2.0.SNAPSHOT" },
                new Object[] { "1.2.3-SNAPSHOT", "1.2.3.SNAPSHOT" },
                new Object[] { "1.2.3.4-SNAPSHOT", "1.2.3.4-SNAPSHOT" },
                new Object[] { "1.2.3.4.whatever", "1.2.3.4-whatever" },
                new Object[] { "1-beta-1:2012", "1.0.0.beta-1_2012" },
        };
    }

    @Test(dataProvider = "bundle-versions")
    public void testVersion(final String specified, final String expected) throws Exception {
        final Attributes attributes = new Attributes();
        attributes.putValue(BUNDLE_VERSION, specified);

        expectations();

        verify((Task) () -> manifest.processManifest(project, attributes, policy, logger(), dependencies));

        verify(expected, attributes, BUNDLE_VERSION);
    }

    @Test
    public void testProjectMetadata() throws Exception {
        final Attributes attributes = new Attributes();

        project.setVersion("1.0-SNAPSHOT");
        project.setName("Project Name");
        project.setDescription("Project Description");
        project.setUrl("http://www.google.com");
        project.setGroupId("my.company.group");
        project.setArtifactId("my-artifact");
        project.setInceptionYear("1970");

        final License license = new License();
        license.setName("Licence X v1.1");
        project.setLicenses(Collections.singletonList(license));

        final Organization organization = new Organization();
        organization.setName("My Organization");
        organization.setUrl("http://my.company.com");
        project.setOrganization(organization);

        expectations();

        verify((Task) () -> manifest.processManifest(project, attributes, policy, logger(), dependencies));

        expect(attributes, BUNDLE_NAME, project.getName());
        expect(attributes, BUNDLE_DESCRIPTION, project.getDescription());
        expect(attributes, BUNDLE_DOCURL, project.getUrl());
        expect(attributes, BUNDLE_VENDOR, organization.getName());
        expect(attributes, BUNDLE_SYMBOLICNAME, project.getArtifactId());
        expect(attributes, BUNDLE_COPYRIGHT, String.format("Copyright %s (c) %s. All rights reserved.", organization.getName(), project.getInceptionYear()));
    }

    @Test
    public void testVersionSubstitution() throws Exception {
        final Attributes attributes = new Attributes();

        final Model model = project.getModel();
        final Properties properties = new Properties();

        properties.setProperty("xxx", "1.0");
        properties.setProperty("y.z", "1.0-beta-1");
        properties.setProperty("whatever", "1.2.3.4.whatever");
        project.setVersion("1.0-SNAPSHOT");

        model.setProperties(properties);

        final String specified = "package.bare,package.%1$s;%1$s=${%2$sxxx},package.buried;xxx=%1$s;%1$s=1.0-X;y.z=\"a\\;b;%1$s\\\"c\\\\d\";package.range1;%1$s=\"[1.0,${%2$sproject.version})\",package.range1;%1$s=\"(${%2$sy.z},${%2$swhatever}]\"";
        final String expected = "package.bare,package.%1$s;%1$s=1.0,package.buried;xxx=%1$s;%1$s=1.0-X;y.z=\"a\\;b;%1$s\\\"c\\\\d\";package.range1;%1$s=\"[1.0,1.0.0.SNAPSHOT)\",package.range1;%1$s=\"(1.0.0.beta-1,1.2.3.4-whatever]\"";

        attributes.putValue(EXPORT_PACKAGE, String.format(specified, VERSION_ATTRIBUTE, BundleJarManifest.VERSION_PREFIX));
        attributes.putValue(IMPORT_PACKAGE, String.format(specified, VERSION_ATTRIBUTE, BundleJarManifest.VERSION_PREFIX));
        attributes.putValue(DYNAMICIMPORT_PACKAGE, String.format(specified, VERSION_ATTRIBUTE, BundleJarManifest.VERSION_PREFIX));
        attributes.putValue(FRAGMENT_HOST, String.format(specified, BUNDLE_VERSION_ATTRIBUTE, BundleJarManifest.VERSION_PREFIX));

        expectations();

        verify((Task) () -> manifest.processManifest(project, attributes, policy, logger(), dependencies));

        verify(String.format(expected, VERSION_ATTRIBUTE), attributes, EXPORT_PACKAGE);
        verify(String.format(expected, VERSION_ATTRIBUTE), attributes, IMPORT_PACKAGE);
        verify(String.format(expected, VERSION_ATTRIBUTE), attributes, DYNAMICIMPORT_PACKAGE);
        verify(String.format(expected, BUNDLE_VERSION_ATTRIBUTE), attributes, FRAGMENT_HOST);
    }

    private void verify(final String expected, final Attributes attributes, final String header) {
        final String version = attributes.getValue(header);
        assert Objects.equals(expected, version) : String.format("Expected %s, got %s", expected, version);
    }

    private void expect(final Attributes attributes, final String key, final String expected) {
        final Object value = attributes.getValue(key);
        assert Objects.equals(expected, value) : value;
    }
}

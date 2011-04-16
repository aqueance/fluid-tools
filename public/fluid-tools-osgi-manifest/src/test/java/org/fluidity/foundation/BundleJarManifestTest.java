package org.fluidity.foundation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;

import org.fluidity.deployment.JarManifest;

import org.apache.maven.artifact.Artifact;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class BundleJarManifestTest {

    private final JarManifest manifest = new BundleJarManifest();

    @Test
    public void testEmptyClasspath() throws Exception {
        final Attributes attributes = new Attributes();
        final List<String> dependencies = new ArrayList<String>();

        manifest.processManifest(null, attributes, dependencies, Collections.<Artifact>emptyList());

        assert attributes.getValue(BundleJarManifest.BUNDLE_CLASSPATH) == null;

        final String version = attributes.getValue(BundleJarManifest.BUNDLE_VERSION);
        assert BundleJarManifest.DEFAULT_BUNDLE_VERSION.equals(version) : version;
    }

    @Test
    public void testClasspathWithOneEntry() throws Exception {
        final Attributes attributes = new Attributes();
        final List<String> dependencies = new ArrayList<String>();

        final String dependency = "dependency.jar";
        dependencies.add(dependency);

        manifest.processManifest(null, attributes, dependencies, Collections.<Artifact>emptyList());

        assert dependency.equals(attributes.getValue(BundleJarManifest.BUNDLE_CLASSPATH));

        final String version = attributes.getValue(BundleJarManifest.BUNDLE_VERSION);
        assert BundleJarManifest.DEFAULT_BUNDLE_VERSION.equals(version) : version;
    }

    @Test
    public void testClasspathWithMultipleEntry() throws Exception {
        final Attributes attributes = new Attributes();
        final List<String> dependencies = new ArrayList<String>();

        final String dependency1 = "dependency1.jar";
        final String dependency2 = "dependency2.jar";
        final String dependency3 = "dependency3.jar";

        dependencies.add(dependency1);
        dependencies.add(dependency2);
        dependencies.add(dependency3);

        manifest.processManifest(null, attributes, dependencies, Collections.<Artifact>emptyList());

        final StringBuilder dependencyList = new StringBuilder();
        for (final String dependency : dependencies) {
            if (dependencyList.length() > 0) {
                dependencyList.append(',');
            }

            dependencyList.append(dependency);
        }

        assert dependencyList.toString().equals(attributes.getValue(BundleJarManifest.BUNDLE_CLASSPATH));

        final String version = attributes.getValue(BundleJarManifest.BUNDLE_VERSION);
        assert BundleJarManifest.DEFAULT_BUNDLE_VERSION.equals(version) : version;
    }

    @DataProvider(name = "bundle-versions")
    public Object[][] versionNumbers() throws Exception {
        return new Object[][] {
                new Object[] { "1.0", "1.0.0" },
                new Object[] { "1.0.0", "1.0.0" },
                new Object[] { "1.2", "1.2.0" },
                new Object[] { "1.2.3", "1.2.3" },
                new Object[] { "1-beta-1", "1.0.0.beta-1" },
                new Object[] { "1.2-SNAPSHOT", "1.2.0.SNAPSHOT" },
                new Object[] { "1.2.3-SNAPSHOT", "1.2.3.SNAPSHOT" },
                new Object[] { "1.2.3.4-SNAPSHOT", "1.2.3.4-SNAPSHOT" },
                new Object[] { "1.2.3.4.whatever", "1.2.3.4-whatever" },
        };
    }

    @Test(dataProvider = "bundle-versions")
    public void testVersion(final String projectVersion, final String bundleVersion) throws Exception {
        final Attributes attributes = new Attributes();
        attributes.putValue(BundleJarManifest.BUNDLE_VERSION, projectVersion);

        manifest.processManifest(null, attributes, new ArrayList<String>(), Collections.<Artifact>emptyList());

        final String version = attributes.getValue(BundleJarManifest.BUNDLE_VERSION);
        assert bundleVersion.equals(version) : version;
    }

    // TODO: test Maven project metadata conversion
}

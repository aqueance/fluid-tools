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

package org.fluidity.foundation;

import java.util.List;
import java.util.jar.Attributes;

/**
 * Used by the maven-standalone-jar plugin after it has copied all transitive dependencies of the host project to the project artifact to modify the JAR
 * manifest of that artifact so that the host OSGi container finds those embedded JAR files and understands them to be in the bundle's class path.
 *
 * @author Tibor Varga
 */
public class BundleJarManifest implements JarManifest {

    public static final String DEFAULT_BUNDLE_VERSION = "1.0.0";
    public static final String BUNDLE_CLASSPATH = "Bundle-Classpath";
    public static final String BUNDLE_VERSION = "Bundle-Version";

    public void processManifest(final Attributes attributes, final List<String> dependencies) {
        final StringBuilder classpath = new StringBuilder();
        for (final String dependency : dependencies) {
            if (classpath.length() > 0) {
                classpath.append(',');
            }

            classpath.append(dependency);
        }

        if (classpath.length() > 0) {
            attributes.putValue(BUNDLE_CLASSPATH, classpath.toString());
        }

        // http://www.osgi.org/javadoc/r4v42/org/osgi/framework/Version.html#Version(java.lang.String)
        final String projectVersion = attributes.getValue(BUNDLE_VERSION);
        final StringBuilder bundleVersion = new StringBuilder();

        if (projectVersion != null) {
            int partCount = 0;
            for (final String part : projectVersion.split("[\\.-]")) {
                if (partCount < 3) {
                    try {
                        Integer.parseInt(part);   // just checking if part is a number
                        bundleVersion.append('.').append(part);
                    } catch (final NumberFormatException e) {

                        // part is not numeric
                        partCount = padVersion(bundleVersion, partCount);

                        bundleVersion.append('.').append(part);
                    }
                } else {
                    bundleVersion.append(partCount == 3 ? '.' : '-').append(part);
                }

                ++partCount;
            }

            padVersion(bundleVersion, partCount);

            attributes.putValue(BUNDLE_VERSION, bundleVersion.toString().substring(1));
        } else {
            attributes.putValue(BUNDLE_VERSION, DEFAULT_BUNDLE_VERSION);
        }
    }

    private int padVersion(final StringBuilder bundleVersion, int partCount) {
        for (; partCount < 3; ++partCount) {
            bundleVersion.append(".0");
        }

        return partCount;
    }
}

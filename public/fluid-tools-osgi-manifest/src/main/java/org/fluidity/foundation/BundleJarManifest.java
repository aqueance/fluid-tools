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

import java.util.jar.Attributes;

/**
 * Used by the maven-executable-jar plugin after it has copied all transitive dependencies of the host project to the project artifact to modify the JAR
 * manifest of that artifact so that the host OSGi container finds those embedded JAR files and understands them to be in the bundle's class path.
 *
 * @author Tibor Varga
 */
public class BundleJarManifest implements JarManifest {

    public static final String BUNDLE_CLASSPATH = "Bundle-Classpath";

    public void processManifest(final Attributes attributes) {
        final String[] dependencies = attributes.getValue(JarManifest.NESTED_DEPENDENCIES).split(" ");

        final StringBuilder classpath = new StringBuilder();
        for (final String dependency : dependencies) {
            if (classpath.length() > 0) {
                classpath.append(',');
            }

            classpath.append(dependency);
        }

        attributes.putValue(BUNDLE_CLASSPATH, classpath.toString());
        attributes.remove(new Attributes.Name(JarManifest.NESTED_DEPENDENCIES));
    }
}

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

package org.fluidity.tests;

import java.net.URL;
import java.util.Collection;
import java.util.HashSet;

import org.fluidity.deployment.osgi.Initialization;
import org.fluidity.foundation.Archives;

import org.osgi.framework.Bundle;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;

@SuppressWarnings("UnusedDeclaration")
final class BundleStartLevels implements Initialization {

    public void initialize(final Framework framework, final Bundle... bundles) throws Exception {

        // Start levels cannot be less than 1
        final int systemLevel = 1;

        // Allows easing the application bundles in one by one
        int applicationLevel = systemLevel;

        final URL engine = Archives.containing(framework.getClass());
        final URL packaged = Archives.containing(engine);

        // "system" is the build profile that lists the included system bundles
        final Collection<URL> system = new HashSet<URL>(Archives.Nested.dependencies(false, packaged, "system"));

        framework.adapt(FrameworkStartLevel.class).setStartLevel(systemLevel);

        for (final Bundle bundle : bundles) {
            final boolean start = system.contains(Archives.parseURL(bundle.getLocation()));
            bundle.adapt(BundleStartLevel.class).setStartLevel(start ? systemLevel : ++applicationLevel);
        }
    }
}

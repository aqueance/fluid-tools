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

import org.fluidity.deployment.osgi.Initialization;

import org.osgi.framework.Bundle;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;

@SuppressWarnings("UnusedDeclaration")
final class BundleStartLevels implements Initialization {

    public void initialize(final Framework framework, final Bundle... bundles) {
        int level = 2;
        for (final Bundle bundle : bundles) {
            bundle.adapt(BundleStartLevel.class).setStartLevel(level++);
        }

        framework.adapt(FrameworkStartLevel.class).setStartLevel(1);
    }
}

/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.tests.osgi.bundle1;

import org.fluidity.composition.Component;
import org.fluidity.deployment.osgi.BundleComponents;

/**
 * Component that depends on a local component and thus must be recognized and instantiated by the bundle component container.
 *
 * @author Tibor Varga
 */
@Component(automatic = false)
final class ComponentDependency implements BundleComponents.Managed {

    private final IndependentComponent dependency;

    ComponentDependency(final IndependentComponent dependency) {
        this.dependency = dependency;
    }

    public void start() throws Exception {
        assert dependency != null;
    }

    public void stop() throws Exception {
        // empty
    }
}

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

package org.fluidity.tests.osgi.bundle1;

import java.util.Properties;

import org.fluidity.deployment.osgi.BundleComponentContainer;
import org.fluidity.deployment.osgi.Service;

/**
 * Service that depends on a local service and thus must be recognized and instantiated by the bundle component container.
 *
 * @author Tibor Varga
 */
final class LocalServiceDependency implements BundleComponentContainer.Registration {

    private final IndependentService dependency;

    LocalServiceDependency(final @Service IndependentService dependency) {
        this.dependency = dependency;
    }

    public Class<?>[] types() {
        return new Class<?>[] { LocalServiceDependency.class };
    }

    public Properties properties() {
        return null;
    }

    public void start() throws Exception {
        assert dependency != null;
    }

    public void stop() throws Exception {
        // empty
    }
}

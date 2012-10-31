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

import org.fluidity.deployment.osgi.BundleComponentContainer;

/**
 * Independent component that must be recognized and instantiated by the bundle component container.
 *
 * @author Tibor Varga
 */
final class IndependentComponent implements BundleComponentContainer.Managed {

    public void start() throws Exception {
        // empty
    }

    public void stop() throws Exception {
        // empty
    }
}

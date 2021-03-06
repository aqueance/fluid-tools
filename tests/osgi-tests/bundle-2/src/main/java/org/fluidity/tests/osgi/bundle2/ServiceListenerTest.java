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

package org.fluidity.tests.osgi.bundle2;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.fluidity.composition.Component;
import org.fluidity.deployment.osgi.BundleComponents;
import org.fluidity.tests.osgi.BundleTest;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@Component(automatic = false)
public final class ServiceListenerTest implements BundleTest, BundleComponents.Registration.Listener {

    private final AtomicInteger bundleCount = new AtomicInteger();

    public Class type() {
        return BundleComponents.Status.class;
    }

    public void serviceAdded(final Object component, final Properties properties) {
        bundleCount.incrementAndGet();
    }

    public void serviceRemoved(final Object component) {
        bundleCount.decrementAndGet();
    }

    @Test
    public void test() throws Exception {
        assert bundleCount.get() == 2 : bundleCount.get();
    }

    public Properties properties() {
        return null;
    }

    public void start() throws Exception {
        // empty
    }

    public void stop() throws Exception {
        // empty
    }
}

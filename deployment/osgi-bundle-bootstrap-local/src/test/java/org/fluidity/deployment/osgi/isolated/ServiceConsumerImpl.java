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

package org.fluidity.deployment.osgi.isolated;

import org.fluidity.composition.Component;
import org.fluidity.deployment.osgi.BundleComponents;
import org.fluidity.deployment.osgi.Service;
import org.fluidity.deployment.osgi.impl.IsolatedClassLoader;
import org.fluidity.deployment.osgi.impl.ServiceConsumer;
import org.fluidity.deployment.osgi.impl.ServiceProvider;

/**
 * An OSGi service consumer that passes to a service the name of a class not visible to the service and expects the service to instantiate the class. This is
 * used to verify the container's ability to make services able to load classes visible only to the calling bundle.
 *
 * @author Tibor Varga
 */
@Component(automatic = false)
public final class ServiceConsumerImpl implements ServiceConsumer, BundleComponents.Managed {

    private final ServiceProvider provider;

    public ServiceConsumerImpl(final @Service ServiceProvider provider) {
        this.provider = provider;
    }

    public void start() throws Exception {
        // empty
    }

    public void stop() throws Exception {
        // empty
    }

    public String call() throws Exception {
        return provider.callback(CallbackImpl.class.getName());
    }

    /**
     * @author Tibor Varga
     */
    public static final class CallbackImpl implements ServiceProvider.Callback {

        public String invoke() {
            final ClassLoader classLoader = getClass().getClassLoader();
            assert classLoader instanceof IsolatedClassLoader : classLoader;
            return ((IsolatedClassLoader) classLoader).name();
        }
    }
}

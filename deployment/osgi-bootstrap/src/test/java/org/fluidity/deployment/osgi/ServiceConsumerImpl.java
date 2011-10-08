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

package org.fluidity.deployment.osgi;

/**
 * An OSGi service consumer that passes to a service the name of a class not visible to the service and expects the service to instantiate the class. This is
 * used to verify the whiteboard's ability to make services able to load classes visible only to the calling bundle.
 */
public final class ServiceConsumerImpl implements ServiceConsumer, Whiteboard.Managed {

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
        return provider.callback(ServiceCallbackImpl.class.getName());
    }
}

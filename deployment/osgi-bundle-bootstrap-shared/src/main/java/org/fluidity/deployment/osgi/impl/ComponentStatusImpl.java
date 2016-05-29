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

package org.fluidity.deployment.osgi.impl;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.fluidity.composition.Component;
import org.fluidity.deployment.osgi.BundleComponents;
import org.fluidity.deployment.osgi.Service;

import org.osgi.framework.BundleContext;

/**
 * This is the OSGi service implementation for the {@link BundleComponents.Status} service API. It requires a delegate to connect to the internal state
 * of the host bundle's {@link BundleComponentContainer}, which will be provided by that same container.
 * <p>
 * This is a {@linkplain BundleComponents.Managed managed} {@linkplain BundleComponents.Registration service registration} component, hence no
 * {@link org.fluidity.composition.Component @Component} annotation.
 *
 * @author Tibor Varga
 */
@Component(automatic = false)
final class ComponentStatusImpl implements BundleComponents.Status, BundleComponents.Registration {

    private final Properties registration = new Properties();
    private final ComponentStatus delegate;

    ComponentStatusImpl(final BundleContext context, final ComponentStatus delegate) {
        this.delegate = delegate;
        this.registration.setProperty(BUNDLE, context.getBundle().getSymbolicName());
    }

    public Properties properties() {
        return registration;
    }

    public Collection<Class<?>> active() {
        return delegate.active();
    }

    public Map<Class<?>, Collection<Service>> inactive() {
        return delegate.inactive();
    }

    public Collection<Class<?>> failed() {
        return delegate.failed();
    }

    public void start() throws Exception {
        // empty
    }

    public void stop() throws Exception {
        // empty
    }
}

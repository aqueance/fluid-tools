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

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ContextDefinition;
import org.fluidity.composition.Inject;
import org.fluidity.composition.spi.PlatformContainer;
import org.fluidity.foundation.logging.Log;
import org.fluidity.foundation.logging.Marker;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * Adapts the OSGi service container to a super container for the dependency injection container of a deployed bundle.
 *
 * @author Tibor Varga
 */
final class ServiceContainer implements PlatformContainer {

    @Inject
    @Marker(ServiceContainer.class)
    private Log log;

    private final BundleContext bundle;

    public ServiceContainer(final BundleContext bundle) {
        this.bundle = bundle;
    }

    public boolean containsComponent(final Class<?> api, final ContextDefinition context) {
        return service(api) != null;
    }

    @SuppressWarnings("unchecked")
    public <T> T getComponent(final Class<T> api, final ContextDefinition context) throws ComponentContainer.ResolutionException {
        final ServiceReference reference = service(api);
        return reference == null ? null : (T) bundle.getService(reference);
    }

    public boolean containsComponentGroup(final Class<?> api, final ContextDefinition context) {
        try {
            final ServiceReference[] references = services(api, context);
            return references != null && references.length > 0;
        } catch (final InvalidSyntaxException e) {
            throw new RuntimeException(api.getName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T[] getComponentGroup(final Class<T> api, final ContextDefinition context) {
        try {
            final ServiceReference[] references = services(api, context);

            if (references == null) {
                return null;
            } else {
                final T[] components = (T[]) Array.newInstance(api, references.length);

                for (int i = 0, limit = references.length; i < limit; i++) {
                    final ServiceReference reference = references[i];
                    components[i] = (T) bundle.getService(reference);
                }

                return components;
            }
        } catch (final InvalidSyntaxException e) {
            throw new RuntimeException(api.getName(), e);
        }
    }

    private ServiceReference service(final Class<?> api) {
        return lookup(api, null, bundle.getServiceReference(api.getName()))[0];
    }

    private ServiceReference[] services(final Class<?> api, final ContextDefinition context) throws InvalidSyntaxException {
        final String filter = filter(context);
        return lookup(api, filter, bundle.getServiceReferences(api.getName(), filter));
    }

    private ServiceReference[] lookup(final Class<?> api, final String filter, final ServiceReference... references) {
        final boolean found = references != null && references.length > 0 && references[0] != null;
        final boolean multiple = filter == null;
        log.info("Looking up OSGi service%s %s%s: %sfound",
                 multiple ? "" : "s",
                 api.getName(),
                 multiple ? "" : String.format(" with filter '%s'", filter),
                 found ? "" : "not ");
        return references;
    }

    private String filter(final ContextDefinition context) {
        final Annotation[] selectors = context.defined().get(Selector.class);
        return selectors != null && selectors.length > 0 ? ((Selector) selectors[selectors.length - 1]).value() : null;
    }

    public void stop() {
        log.info("container %s shut down", id());
    }

    public String id() {
        return String.format("%s@%x", getClass().getSimpleName(), System.identityHashCode(this));
    }
}

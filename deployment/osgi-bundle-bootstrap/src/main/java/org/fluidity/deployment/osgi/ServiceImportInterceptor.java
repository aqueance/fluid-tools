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

package org.fluidity.deployment.osgi;

import java.lang.reflect.Type;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.spi.ComponentInterceptor;
import org.fluidity.foundation.Generics;

/**
 * Replaces {@link Service @Service} annotated interface dependencies with the result of passing the original through {@link BundleBoundary#imported(Class,
 * Object)}.
 * <h3>Usage</h3>
 * <pre>
 * {@linkplain Component @Component}
 * public final class MyComponent implements {@linkplain BundleComponentContainer.Managed BundleComponentContainer.Managed} {
 *
 *   public MyComponent(final <span class="hl1">&#64;Service</span> <span class="hl2">SomeService</span> service) {
 *     &hellip;
 *   }
 *
 *   &hellip;
 * }
 *
 * public <b>interface</b> <span class="hl2">SomeService</span> {
 *   &hellip;
 * }
 * </pre>
 *
 * @author Tibor Varga
 */
@Component.Context(Service.class)
@SuppressWarnings("UnusedDeclaration")
final class ServiceImportInterceptor implements ComponentInterceptor {

    private final BundleBoundary border;

    public ServiceImportInterceptor(final BundleBoundary border) {
        this.border = border;
    }

    public Dependency intercept(final Type reference, final ComponentContext context, final Dependency dependency) {
        final Class<?> type = Generics.rawType(reference);

        return new Dependency() {
            @SuppressWarnings("unchecked")
            public Object create() {
                final Object instance = dependency.create();
                return type.isInterface() ? border.imported((Class) type, instance) : instance;
            }
        };
    }
}

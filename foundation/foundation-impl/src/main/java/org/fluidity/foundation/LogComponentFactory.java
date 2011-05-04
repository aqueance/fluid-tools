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

package org.fluidity.foundation;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.Context;
import org.fluidity.composition.spi.CustomComponentFactory;
import org.fluidity.foundation.logging.Log;
import org.fluidity.foundation.logging.Marker;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Creates {@link Log} objects for the marker specified by a {@link org.fluidity.foundation.logging.Marker} annotation, using the user provided implementation
 * of the {@link LogFactory} interface.
 *
 * @author Tibor Varga
 */
@Component(api = Log.class)
@Context(Marker.class)
final class LogComponentFactory implements CustomComponentFactory {

    private final LogFactory factory;

    public LogComponentFactory(final LogFactory factory) {
        this.factory = factory;
    }

    public Instance resolve(final ComponentContext context, final Resolver dependencies) throws ComponentContainer.ResolutionException {
        return new Instance() {
            public void bind(final Registry registry) throws ComponentContainer.BindingException {
                final Marker marker = context.annotation(Marker.class, Log.class);
                registry.bindInstance(factory.createLog(marker.value()), Log.class);
            }
        };
    }

}

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

package org.fluidity.composition;

import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.foundation.Log;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Creates {@link Log} objects for the class specified as the type parameter of the <code>Log</code> dependency, using the user provided implementation of the
 * {@link LogFactory} interface.
 *
 * @author Tibor Varga
 */
@Component(api = Log.class)
@Component.Qualifiers(Component.Reference.class)
final class LogComponentFactory implements ComponentFactory {

    private final LogFactory factory;

    LogComponentFactory(final LogFactory factory) {
        this.factory = factory;
    }

    @SuppressWarnings("unchecked")
    public Instance resolve(final ComponentContext context, final Resolver dependencies) throws Exception {
        return Instance.of(factory.type(), registry -> {
            final Component.Reference reference = context.qualifier(Component.Reference.class, Log.class);
            registry.bindInstance(factory.createLog(reference.parameter(0)), Log.class);
        });
    }
}

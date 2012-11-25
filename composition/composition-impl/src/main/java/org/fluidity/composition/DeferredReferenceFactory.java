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

package org.fluidity.composition;

import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.foundation.Deferred;
import org.fluidity.foundation.Generics;

/**
 * This factory makes it possible to depend on a lazy instantiated component when the deferred reference itself needs to be exposed. Use the {@link
 * Defer @Defer} annotation when the deferred reference itself is irrelevant.
 *
 * @author Tibor Varga
 */
@Component(api = Deferred.Reference.class)
@Component.Context(Component.Reference.class)
@SuppressWarnings("UnusedDeclaration")
final class DeferredReferenceFactory implements ComponentFactory {

    public Instance resolve(final ComponentContext context, final Resolver dependencies) throws Exception {
        final Dependency<?> dependency = dependencies.resolve(null, Generics.typeParameter(context.annotation(Component.Reference.class, null).type(), 0), null);

        final Deferred.Reference<Object> reference = Deferred.reference(new Deferred.Factory<Object>() {
            public Object create() {
                return dependency.instance();
            }
        });

        return new Instance() {
            @SuppressWarnings("unchecked")
            public void bind(final Registry registry) throws Exception {
                registry.bindInstance(reference, Deferred.Reference.class);
            }
        };
    }
}

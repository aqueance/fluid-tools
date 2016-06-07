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

import java.lang.reflect.Type;

import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.composition.spi.Dependency;
import org.fluidity.foundation.Deferred;
import org.fluidity.foundation.Generics;
import org.fluidity.foundation.Strings;

/**
 * This factory makes it possible to depend on a lazily instantiated component when the {@link Deferred.Reference deferred reference} itself needs to be
 * exposed. The injected deferred reference will <em>not</em> be thread safe.
 * <p>
 * Use the {@link Defer @Defer} annotation with the type parameter alone when the deferred reference itself is irrelevant.
 *
 * @author Tibor Varga
 */
@Component(api = Deferred.Reference.class)
@Component.Qualifiers(Component.Reference.class)
final class DeferredReferenceFactory implements ComponentFactory {

    @SuppressWarnings("unchecked")
    public Instance resolve(final ComponentContext context, final Container dependencies) throws Exception {
        final Type api = Generics.typeParameter(context.qualifier(Component.Reference.class, null).type(), 0);

        if (api == null) {
            throw new IllegalArgumentException(String.format("Type parameter missing from a %s dependency", Strings.formatClass(false, true, Deferred.class)));
        }

        final Dependency<?> dependency = dependencies.resolver().lookup(api);
        final Deferred.Reference reference = Deferred.local(dependency::instance);

        return Instance.of(dependency.type(), registry -> registry.bindInstance(reference, Deferred.Reference.class));
    }
}

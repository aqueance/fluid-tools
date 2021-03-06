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
import java.util.function.Function;
import java.util.function.Supplier;

import org.fluidity.composition.spi.ComponentInterceptor;
import org.fluidity.composition.spi.Dependency;
import org.fluidity.foundation.Deferred;
import org.fluidity.foundation.Generics;
import org.fluidity.foundation.Proxies;
import org.fluidity.foundation.security.Security;

/**
 * Implements the semantics of the {@link Defer @Defer} annotation.
 *
 * @author Tibor Varga
 */
@Component.Qualifiers(Defer.class)
final class DeferredReferenceInterceptor implements ComponentInterceptor {

    @SuppressWarnings("unchecked")
    public Dependency intercept(final Type reference, final ComponentContext context, final Dependency dependency) {
        final Function<Supplier, Deferred.Reference> factory = context.qualifier(Defer.class, null).shared() ? Deferred::shared : Deferred::local;
        final Deferred.Reference deferred = factory.apply(dependency::instance);

        return dependency.replace(() -> Proxies.create(Generics.rawType(reference),
                                                       (proxy, method, arguments) -> Security.access(method).invoke(deferred.get(), arguments)));
    }
}

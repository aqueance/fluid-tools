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

package org.fluidity.foundation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.spi.ComponentInterceptor;

/**
 * Implements the semantics of the {@link Component.Deferred @Component.Deferred} annotation.
 *
 * @author Tibor Varga
 */
@Component.Context(Component.Deferred.class)
@SuppressWarnings("UnusedDeclaration")
final class DeferredReferenceInterceptor implements ComponentInterceptor {

    public Dependency replace(final Type reference, final ComponentContext context, final Dependency dependency) {
        final Deferred.Reference<?> deferred = Deferred.reference(new Deferred.Factory<Object>() {
            public Object create() {
                return dependency.create();
            }
        });

        return new Dependency() {
            public Object create() {
                return Proxies.create(Generics.rawType(reference), new InvocationHandler() {
                    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                        return method.invoke(deferred.get(), args);
                    }
                });
            }
        };
    }
}

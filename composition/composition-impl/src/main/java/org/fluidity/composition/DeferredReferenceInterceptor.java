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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.fluidity.composition.spi.ComponentInterceptor;
import org.fluidity.foundation.Deferred;
import org.fluidity.foundation.Generics;
import org.fluidity.foundation.Proxies;
import org.fluidity.foundation.Security;

/**
 * Implements the semantics of the {@link Defer @Defer} annotation.
 *
 * @author Tibor Varga
 */
@Component.Context(Defer.class)
@SuppressWarnings("UnusedDeclaration")
final class DeferredReferenceInterceptor implements ComponentInterceptor {

    public Dependency intercept(final Type reference, final ComponentContext context, final Dependency dependency) {
        final Deferred.Factory<Object> factory = new Deferred.Factory<Object>() {
            public Object create() {
                return dependency.create();
            }
        };

        final Deferred.Reference<?> deferred = context.annotation(Defer.class, null).shared() ? Deferred.shared(factory) : Deferred.local(factory);

        return new Dependency() {
            public Object create() {
                return Proxies.create(Generics.rawType(reference), new InvocationHandler() {
                    public Object invoke(final Object proxy, final Method method, final Object[] arguments) throws Throwable {
                        final PrivilegedAction<Method> access = Security.setAccessible(method);
                        return (access == null ? method : AccessController.doPrivileged(access)).invoke(deferred.get(), arguments);
                    }
                });
            }
        };
    }
}

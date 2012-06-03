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

package org.fluidity.composition.spi;

import java.lang.reflect.Type;

import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.ComponentGroup;

/**
 * Replaces a component before it is injected as a dependency. The replaced component must be type compatible with the dependency reference.
 * <p/>
 * Interceptors visible to a {@linkplain org.fluidity.composition.ComponentContainer dependency injection container} are invoked at all dependency injection
 * operations performed by that container, and are the means to implement delegation chains in Fluid Tools.
 * <p/>
 * The intent is to have this process governed by the context at the dependency reference, which also determines the order in which the interceptors are
 * invoked.
 * <p/>
 * If present, the {@linkplain org.fluidity.composition.Component.Context @Component.Context} annotation of the interceptor is consulted to determine
 * what dependency references will the interceptor be invoked for: it will be invoked for those dependency references that have all listed context annotations
 * present. Context annotations used by an interceptor do not implicitly become part of the context of any component unless it is explicitly listed by that
 * component as a context annotation.
 * <p/>
 * The interceptor may also elect not to discriminate between the dependencies by their context, in which case it will be invoked for all dependency
 * references.
 * <p/>
 * The interceptor can inspect the dependency reference itself to determine what action to take and it can use the {@link org.fluidity.foundation.Generics}
 * tool to work with {@linkplain Type parameterized types}.
 * <p/>
 * <h3>Usage</h3>
 * <pre>
 * {@linkplain org.fluidity.composition.Component.Context @Component.Context}(<span class="hl2">MyAnnotation</span>.class)
 * final class MyInterceptor implements <span class="hl1">ComponentInterceptor</span> {
 *   &hellip;
 * }
 *
 * {@linkplain org.fluidity.composition.Component @Component}
 * final class MyComponent {
 *
 *   public MyComponent(final <span class="hl2">@MyAnnotation</span> MyDependency dependency) {
 *     &hellip;
 *   }
 * }
 * </pre>
 *
 * @author Tibor Varga
 */
@ComponentGroup
public interface ComponentInterceptor {

    /**
     * Replaces some dependency with a type compatible replacement. The interceptor is never invoked with a <code>null</code> dependency and returning a
     * <code>null</code> dependency will short-cut the interceptor chain and result in an unresolved dependency.
     * <p/>
     * The interceptor cannot invoke {@link ComponentInterceptor.Dependency#create()} in this method.
     *
     * @param reference  the fully specified reference to the dependency, including type parameters, if any.
     * @param context    the component context at the dependency reference.
     * @param dependency the dependency to intercept; never <code>null</code>.
     *
     * @return the replaced dependency; may be <code>null</code>.
     */
    Dependency intercept(Type reference, ComponentContext context, Dependency dependency);

    /**
     * A dependency that can be replaced by a {@link ComponentInterceptor}.
     *
     * @author Tibor Varga
     */
    interface Dependency {

        /**
         * Instantiates if necessary, and returns the dependency instance. This method may only be invoked from the same method of another object.
         *
         * @return the dependency instance; never <code>null</code>.
         */
        Object create();
    }
}

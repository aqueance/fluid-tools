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

package org.fluidity.composition.container.spi;

import java.lang.reflect.Array;
import java.lang.reflect.Type;

import org.fluidity.composition.container.api.ContextDefinition;

/**
 * Implements basic method relationships and functionality useful for dependency graph implementations.
 * <p/>
 * Implements the {@link DependencyGraph#resolveComponent(Class, ContextDefinition, Traversal)} and {@link DependencyGraph#resolveGroup(Class,
 * ContextDefinition, Traversal)} methods to delegate to the {@link DependencyGraph#resolveComponent(Class, ContextDefinition, Traversal, Type)} and
 * {@link DependencyGraph#resolveGroup(Class, ContextDefinition, Traversal, Type)} methods, respectively.
 *
 * @author Tibor Varga
 */
public abstract class EmptyDependencyGraph implements DependencyGraph {

    /**
     * Calls {@link #resolveComponent(Class, ContextDefinition, Traversal, Type)} with <code>api</code> passed to the last argument.
     * <p/>
     * {@inheritDoc}
     */
    public final Node resolveComponent(final Class<?> api, final ContextDefinition context, final Traversal traversal) {
        return resolveComponent(api, context.advance(api), traversal, api);
    }

    /**
     * Calls {@link #resolveGroup(Class, ContextDefinition, Traversal, Type)}  with <code>api</code> passed to the last argument.
     * <p/>
     * {@inheritDoc}
     */
    public final Node resolveGroup(final Class<?> api, final ContextDefinition context, final Traversal traversal) {
        return resolveGroup(api, context.advance(Array.newInstance(api, 0).getClass()), traversal, api);
    }
}

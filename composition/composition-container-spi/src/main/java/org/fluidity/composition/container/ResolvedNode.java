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

package org.fluidity.composition.container;

import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.container.spi.DependencyGraph;

/**
 * A {@linkplain org.fluidity.composition.container.spi.DependencyGraph.Node dependency graph node} for an instance that is known without further resolution,
 * or as a result of such resolution.
 * <h3>Usage</h3>
 * You don't interact with an internal interface.
 *
 * @author Tibor Varga
 */
public final class ResolvedNode implements DependencyGraph.Node {

    private final Class<?> type;
    private final Object instance;
    private final ComponentContext context;

    /**
     * @param type     the typ of the instance; useful when the instance is null thus its class cannot be queried.
     * @param instance the known component instance
     * @param context  the component context for the component instance.
     */
    public ResolvedNode(final Class<?> type, final Object instance, final ComponentContext context) {
        this.type = type;
        this.instance = instance;
        this.context = context;
    }

    public final Class<?> type() {
        return type;
    }

    public final Object instance(final DependencyGraph.Traversal traversal) {
        return instance;
    }

    public ComponentContext context() {
        return context;
    }
}

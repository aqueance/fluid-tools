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

package org.fluidity.composition.container.impl;

import java.lang.reflect.Type;

import org.fluidity.composition.container.ContextDefinition;
import org.fluidity.composition.container.spi.DependencyGraph;
import org.fluidity.composition.container.spi.DependencyResolver;

/**
 * Internal interface to handle dependency interceptors.
 *
 * @author Tibor Varga
 */
interface DependencyInterceptors {

    /**
     * Calls all available {@linkplain org.fluidity.composition.spi.ComponentInterceptor component interceptors} to process the component instantiated by the
     * given node in the given context.
     *
     * @param container the container instantiating the component.
     * @param context   the component context.
     * @param traversal the current dependency graph traversal.
     * @param reference the dependency reference to the component.
     * @param node      the node describing the component being resolved.
     *
     * @return a new node, or the original one.
     */
    DependencyGraph.Node replace(DependencyResolver container,
                                 ContextDefinition context,
                                 DependencyGraph.Traversal traversal,
                                 Type reference,
                                 DependencyGraph.Node node);
}

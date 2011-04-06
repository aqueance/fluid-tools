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

package org.fluidity.composition;

import java.util.Collection;

import org.fluidity.composition.spi.ComponentMapping;

/**
 * Extends the {@link ComponentMapping} interface with methods required by the actual container implementation.
 *
 * @author Tibor Varga
 */
interface ComponentResolver extends ComponentMapping {

    DependencyGraph.Node resolve(DependencyGraph.Traversal traversal, final SimpleContainer container, final ContextDefinition context);

    /**
     * Returns the relative priority of this mapping compared to another.
     *
     * @return the relative priority of this mapping compared to another.
     */
    int priority();

    /**
     * Tells whether the mapping is that of a {@link org.fluidity.composition.spi.ComponentVariantFactory}.
     *
     * @return <code>true</code> if the mapping is that of a {@link org.fluidity.composition.spi.ComponentVariantFactory}, <code>false</code> otherwise.
     */
    boolean isVariantMapping();

    /**
     * Tells whether this mapping has been created for an already instantiated component.
     *
     * @return <code>true</code> if this mapping represents an already instantiated component, <code>false</code> otherwise.
     */
    boolean isInstanceMapping();

    /**
     * Tells if the receiver replaces the supplied <code>resolver</code>.
     *
     * @param resolver the resolver that the receiver may need to replace.
     *
     * @return <code>true</code> if the supplied <code>resolver</code> is replaced by the receiver.
     */
    boolean replaces(ComponentResolver resolver);

    /**
     * Notifies the receiver that a previously bound resolver has been replaced by another one.
     *
     * @param api         the api for which the resolver is being replaced
     * @param previous    the old resolver.
     * @param replacement the new resolver.
     */
    void resolverReplaced(Class<?> api, ComponentResolver previous, ComponentResolver replacement);

    /**
     * Adds a list of group interfaces that the resolved component belongs to.
     *
     * @param groups list of group interfaces that the resolved component belongs to.
     */
    void addGroups(Collection<Class<?>> groups);

    /**
     * Returns the list of group interfaces that the resolved component belongs to.
     *
     * @return the list of group interfaces that the resolved component belongs to.
     */
    Collection<Class<?>> groups();
}

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

import org.fluidity.composition.spi.ComponentResolutionObserver;

/**
 * A <a href="http://code.google.com/p/fluid-tools/wiki/UserGuide#Dependency_Injection_Concept">dependency injection</a>
 * <a href="http://code.google.com/p/fluid-tools/wiki/UserGuide#Dependency_Injection_Containers">container</a> that offers traversal of static dependencies
 * without instantiating components. An <code>ObservedComponentContainer</code> instance works together with a {@link ComponentResolutionObserver} object that
 * it sends component resolution events to.
 *
 * @author Tibor Varga
 */
public interface ObservedComponentContainer extends ComponentContainer {

    /**
     * Resolves the component bound to the given interface and all dependent components without instantiating them. Dynamic dependencies, e.g., those resolved
     * in component constructors will not be picked up by this method.
     *
     * @param api the component interface.
     */
    void resolveComponent(Class<?> api);

    /**
     * Resolves the component group bound to the given interface and all dependent components without instantiating them. Dynamic dependencies, e.g., those
     * resolved in component constructors will not be picked up by this method.
     *
     * @param api the group interface.
     */
    void resolveGroup(Class<?> api);
}

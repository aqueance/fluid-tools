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

package org.fluidity.composition.tests;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.OpenComponentContainer;

/**
 * Internal interface used by {@link org.fluidity.composition.ComponentContainerAbstractTest} and its tests.
 *
 * @author Tibor Varga
 */
public interface ArtifactFactory {

    /**
     * Creates and returns a new container implementation.
     *
     * @return a new container implementation.
     */
    OpenComponentContainer createContainer();

    /**
     * Creates a component context.
     *
     * @param map the map to form the basis of the context.
     *
     * @return a newly created component context object.
     */
    ComponentContext createContext(Map<Class<? extends Annotation>, Annotation[]> map);
}

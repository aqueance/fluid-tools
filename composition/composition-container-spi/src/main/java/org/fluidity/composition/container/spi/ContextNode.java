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

import java.lang.annotation.Annotation;

/**
 * A node in a dependency graph where context annotations may be accepted and/or provided. An object implementing this interface is created for every component
 * class in a container and these objects help in the management of <a href="http://code.google.com/p/fluid-tools/wiki/UserGuide#Component_Context">component
 * context</a> when resolving dependencies.
 * <h3>Usage</h3>
 * You don't interact with an internal interface.
 *
 * @author Tibor Varga
 */
public interface ContextNode {

    /**
     * Returns the class that may accept context annotations at this node.
     *
     * @return the class that may accept context annotations at this node; may be <code>null</code>.
     */
    Class<?> contextConsumer();

    /**
     * Returns the list of annotations defined at this node that may comprise the context of some other component.
     *
     * @return the list of annotations defined at this node, or <code>null</code> if none present.
     */
    Annotation[] providedContext();
}

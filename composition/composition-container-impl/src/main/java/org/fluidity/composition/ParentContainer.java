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

import java.util.List;

/**
 * Interface to separate internal container methods from the higher level container interface.
 *
 * @author Tibor Varga
 */
interface ParentContainer extends SimpleContainer {

    /**
     * Resolves the group API to a list of implementations.
     *
     * @param api       the group API.
     * @param traversal the current graph traversal.
     * @param context   the current context.
     *
     * @return a list of objects representing the group members in this container and its parent(s), if any, starting with those in the top level container and
     *         ending with those in this one.
     */
    List<GroupResolver.Node> resolveGroup(Class<?> api, Traversal traversal, ContextDefinition context);

    /**
     * Returns the group resolver for the given interface, consulting the parent, if any, if not found in the container.
     *
     * @param api the group interface.
     *
     * @return the group resolver for the given interface; never <code>null</code>.
     */
    List<GroupResolver> groupResolvers(Class<?> api);
}

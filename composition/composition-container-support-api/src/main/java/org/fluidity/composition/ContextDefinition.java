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

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Maintains context state during dependency resolution. Context definition is expanded as we descend a dependency path and each component along the chain may
 * consume a subset of the complete context definition. That subset is the propagated upstream and used as a cache key for intermediate components.
 *
 * @author Tibor Varga
 */
public interface ContextDefinition {

    /**
     * Adds the context definitions to the context.
     *
     * @param definition the annotations potentially defining new context.
     *
     * @return the receiver.
     */
    ContextDefinition expand(Annotation[] definition);

    /**
     * Reduces the returned context annotations to those specified by the parameter.
     *
     * @param accepted the {@link Context} annotation listing the accepted annotations.
     *
     * @return the receiver.
     */
    ContextDefinition reduce(Set<Class<? extends Annotation>> accepted);

    /**
     * Adds the accepted context annotations from all supplied contexts to the accepted annotations of the receiver.
     *
     * @param contexts a list of context accepted during component instantiations downstream.
     *
     * @return the receiver.
     */
    ContextDefinition collect(Collection<ContextDefinition> contexts);

    /**
     * Adds the accepted context annotations from the supplied context to the accepted annotations of the receiver.
     *
     * @param context a context accepted during component instantiation downstream.
     */
    void collect(ContextDefinition context);

    /**
     * Returns the context definition from upstream. The more recently added annotations are at the end.
     *
     * @return the context definition from upstream.
     */
    Map<Class<? extends Annotation>, Annotation[]> defined();

    /**
     * Returns the consumed context collected upstream.
     *
     * @return the consumed context collected upstream.
     */
    Map<Class<? extends Annotation>, Annotation[]> collected();

    /**
     * Returns a new copy of the receiver.
     *
     * @return a new copy of the receiver.
     */
    ContextDefinition copy();

    /**
     * Returns a component context that can be passed to components as dependency.
     *
     * @return a component context that can be passed to components as dependency.
     */
    ComponentContext create();
}

/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.fluidity.composition;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;

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
     * @param accepted the {@link org.fluidity.composition.Context} annotation listing the accepted annotations.
     *
     * @return the receiver.
     */
    ContextDefinition reduce(Context accepted);

    /**
     * Adds the accepted context annotations from all supplied contexts to the accepted annotations of the receiver.
     *
     * @param contexts a list of context accepted during component instantiations downstream.
     *
     * @return the receiver.
     */
    ContextDefinition collect(Collection<ContextDefinition> contexts);

    /**
     * Returns the context definition from upstream.
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

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
import java.util.Map;

/**
 * Creates component context objects.
 *
 * @author Tibor Varga
 */
public interface ContextFactory {

    /**
     * Creates a new context from the given map.
     *
     * @param map the properties to turn into a {@link ComponentContext}.
     *
     * @return the new component context.
     */
    ComponentContext newContext(Map<Class<? extends Annotation>, Annotation[]> map);

    /**
     * Extracts the context from the given list of annotations.
     *
     * @param annotation the annotation to extract the context from; never <code>null</code>.
     *
     * @return the extracted context or <code>null</code> if there is no meaningful context.
     */
    ComponentContext extractContext(Annotation[] annotation);

    /**
     * Calls {@link #deriveContext(ComponentContext, ComponentContext)} using the supplied context and the one
     * extracted from the supplied class using {@link #extractContext(Annotation[])}.
     *
     * @param context         the context to use as the parent of the derived context.
     * @param contextProvider the class to extract the context to use as the child in the derived context.
     *
     * @return the derived context, one which uses the child while defaults to the parent.
     */
    ComponentContext deriveContext(ComponentContext context, Class<?> contextProvider);

    /**
     * Creates a new context that uses the child and defaults to the parent.
     *
     * @param parent the context to default to.
     * @param child  the context to use.
     *
     * @return the derived context, one which uses the child while defaults to the parent.
     */
    ComponentContext deriveContext(ComponentContext parent, ComponentContext child);

    /**
     * Returns a context from the second argument whose keys are defined by the first.
     *
     * @param filter  the context whose definitions are kept in the other
     * @param context the context whose definitions are filtered by the other.
     *
     * @return the filtered context.
     */
    ComponentContext filteredContext(ComponentContext filter, ComponentContext context);
}
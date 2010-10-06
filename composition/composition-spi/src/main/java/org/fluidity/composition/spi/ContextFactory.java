/*
 * Copyright (c) 2006-2010 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.composition.spi;

import java.util.Map;
import java.util.Properties;

import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.Context;

/**
 * Creates component context objects.
 *
 * @author Tibor Varga
 */
public interface ContextFactory {

    /**
     * Creates a new context from the given properties.
     *
     * @param properties the properties to turn into a {@link org.fluidity.composition.ComponentContext}.
     *
     * @return the new component context.
     */
    ComponentContext newContext(Properties properties);

    /**
     * Creates a new context from the given map.
     *
     * @param map the properties to turn into a {@link org.fluidity.composition.ComponentContext}.
     *
     * @return the new component context.
     */
    ComponentContext newContext(Map<String, String> map);

    /**
     * Extracts the context from the given list of annotations.
     *
     * @param annotation the annotation to extract the context from; never <code>null</code>.
     *
     * @return the extracted context; never <code>null</code>.
     */
    ComponentContext extractContext(Context annotation);

    /**
     * Calls {@link #deriveContext(org.fluidity.composition.ComponentContext, org.fluidity.composition.ComponentContext)} using the supplied context and the one
     * extracted from the supplied class using {@link #extractContext(org.fluidity.composition.Context)}.
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

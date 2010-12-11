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

package org.fluidity.composition;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import org.fluidity.composition.spi.ContextFactory;

/**
 * @author Tibor Varga
 */
final class ContextFactoryImpl implements ContextFactory {

    public ComponentContext newContext(final Map<Class<? extends Annotation>, Annotation[]> map) {
        return new ComponentContextImpl(map);
    }

    public ComponentContext extractContext(final Annotation[] context) {
        if (context == null) {
            return null;
        }

        final Map<Class<? extends Annotation>, Annotation[]> map = new HashMap<Class<? extends Annotation>, Annotation[]>();

        for (final Annotation value : context) {
            final Class<? extends Annotation> type = value.getClass();

            if (!(value instanceof Component || value instanceof ServiceProvider || value instanceof Optional || value instanceof Context)) {
                map.put(type, new Annotation[] { value });
            }
        }

        return map.isEmpty() ? null : new ComponentContextImpl(map);
    }

    public ComponentContext deriveContext(final ComponentContext context, final Class<?> contextProvider) {
        final ComponentContext extracted = extractContext(contextProvider.getAnnotations());
        return extracted == null ? context : new ComponentContextImpl(context, extracted);
    }

    public ComponentContext deriveContext(final ComponentContext parent, final ComponentContext child) {
        return new ComponentContextImpl(parent, child);
    }

    public ComponentContext filteredContext(final ComponentContext filter, final ComponentContext context) {
        final Map<Class<? extends Annotation>, Annotation[]> map = new HashMap<Class<? extends Annotation>, Annotation[]>();

        if (filter != null) {
            for (final Class<? extends Annotation> type : filter.types()) {
                if (context.defines(type)) {
                    map.put(type, context.annotations(type));
                }
            }
        }

        return new ComponentContextImpl(map);
    }
}

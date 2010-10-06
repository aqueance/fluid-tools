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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.fluidity.composition.spi.ContextFactory;

/**
 * @author Tibor Varga
 */
final class ContextFactoryImpl implements ContextFactory {

    @SuppressWarnings("unchecked")
    public ComponentContext newContext(final Properties properties) {
        return new ComponentContextImpl(properties);
    }

    public ComponentContext newContext(final Map<String, String> map) {
        return new ComponentContextImpl(map);
    }

    public ComponentContext extractContext(final Context annotation) {
        assert annotation != null;
        final Map<String, String> map = new HashMap<String, String>();

        for (final Context.Value value : annotation.value()) {
            map.put(value.name(), value.value());
        }

        return new ComponentContextImpl(map);
    }

    public ComponentContext deriveContext(final ComponentContext context, final Class<?> contextProvider) {
        final Context annotation = contextProvider.getAnnotation(Context.class);
        return annotation == null ? context : new ComponentContextImpl(context, extractContext(annotation));
    }

    public ComponentContext deriveContext(final ComponentContext parent, final ComponentContext child) {
        return new ComponentContextImpl(parent, child);
    }

    public ComponentContext filteredContext(final ComponentContext filter, final ComponentContext context) {
        final Map<String, String> map = new HashMap<String, String>();

        if (filter != null) {
            for (final String key : filter.keySet()) {
                if (context.defines(key)) {
                    map.put(key, context.value(key, null));
                }
            }
        }

        return new ComponentContextImpl(map);
    }
}

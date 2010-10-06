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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author Tibor Varga
 */
final class ComponentContextImpl implements ComponentContext {

    private final Map<String, String> map = new HashMap<String, String>();

    public ComponentContextImpl(final Properties properties) {
        for (final Map.Entry<Object, Object> entry : properties.entrySet()) {
            this.map.put((String) entry.getKey(), (String) entry.getValue());
        }
    }

    public ComponentContextImpl(final Map<String, String> map) {
        for (final Map.Entry<String, String> entry : map.entrySet()) {
            this.map.put(entry.getKey(), entry.getValue());
        }
    }

    public ComponentContextImpl(final ComponentContext parent, final ComponentContext child) {
        if (parent != null) {
            for (final String key : parent.keySet()) {
                this.map.put(key, parent.value(key, null));
            }
        }

        assert child != null;
        for (final String key : child.keySet()) {
            this.map.put(key, child.value(key, null));
        }
    }

    public String value(final String key, final String fallback) {
        return map.containsKey(key) ? map.get(key) : fallback;
    }

    public boolean defines(final String key) {
        return map.containsKey(key);
    }

    public Set<String> keySet() {
        return Collections.unmodifiableSet(map.keySet());
    }

    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        return map.equals(((ComponentContextImpl) o).map);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public String toString() {
        return map.toString();
    }
}

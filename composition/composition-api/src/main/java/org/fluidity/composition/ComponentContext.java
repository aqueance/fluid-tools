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

import java.util.Set;

/**
 * Provides a context for components. A context is a textual configuration that a component may elect to receive by listing the context names in a {@link
 * org.fluidity.composition.Context#names()} type annotation. Context values are provided by components that depend, directly or indirectly, on context
 * consuming other components using the {@link org.fluidity.composition.Context#value()} annotation.
 * <p/>
 * Context support can be added to a component not directly supporting contexts using {@link org.fluidity.composition.ComponentVariantFactory} components as
 * long as the component supports some other configuration mechanism that can be manipulated by the variant factory class.
 *
 * @author Tibor Varga
 */
public interface ComponentContext {

    /**
     * Returns the value in the context for the specified key or the given default value if the context does not define a value for the given key.
     *
     * @param key      the key to return the value for.
     * @param fallback the value to return in case there is no value for the given key.
     *
     * @return the value for the given key or the fallback value.
     *
     * @see #defines(String)
     */
    String value(String key, String fallback);

    /**
     * Tells whether the context defines a value for the given key.
     *
     * @param key the key to check the existence the value for.
     *
     * @return <code>true</code> if there is a value for the given key, <code>false</code> otherwise.
     */
    boolean defines(String key);

    /**
     * Returns the set of keys the context defines a value for.
     *
     * @return the set of keys the context defines a value for.
     */
    Set<String> keySet();
}

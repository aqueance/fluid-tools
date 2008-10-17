/*
 * Copyright (c) 2006-2008 Tibor Adam Varga (tibor.adam.varga@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Softweare"), to deal
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */
package org.fluidity.foundation;

/**
 * Application settings.
 *
 * @author Tibor Varga
 * @version $Revision$
 */
public interface Settings {

    /**
     * Returns the list of setting keys.
     *
     * @return an array of <code>String</code>s, never <code>null</code>.
     */
    String[] keys();

    /**
     * Finds a setting with the given key.
     *
     * @param key          the setting's key.
     * @param defaultValue the value to return when no such setting was found.
     *
     * @return a setting value.
     */
    String setting(final String key, final String defaultValue);

    /**
     * Finds a setting with the given key.
     *
     * @param key          the setting's key.
     * @param defaultValue the value to return when no such setting was found.
     *
     * @return a setting value.
     */
    int setting(final String key, final int defaultValue);

    /**
     * Finds a setting with the given key.
     *
     * @param key          the setting's key.
     * @param defaultValue the value to return when no such setting was found.
     *
     * @return a setting value.
     */
    boolean setting(final String key, final boolean defaultValue);

    /**
     * Returns the list of namespaces known to this object.
     *
     * @return an array of <code>String</code>s, never <code>null</code>.
     */
    String[] namespaces();

    /**
     * Returns the list of setting keys under the given namespace.
     *
     * @param namespace specifies the name space in which the keys are requested
     *
     * @return an array of <code>String</code>s, never <code>null</code>.
     */
    String[] keys(final String namespace);

    /**
     * Finds a setting with the given key.
     *
     * @param namespace    is a namespace in which the given key is unique
     * @param key          the setting's key.
     * @param defaultValue the value to return when no such setting was found.
     *
     * @return a setting value.
     */
    String setting(final String namespace, final String key, final String defaultValue);

    /**
     * Finds a setting with the given key.
     *
     * @param namespace    is a namespace in which the given key is unique
     * @param key          the setting's key.
     * @param defaultValue the value to return when no such setting was found.
     *
     * @return a setting value.
     */
    int setting(final String namespace, final String key, final int defaultValue);

    /**
     * Finds a setting with the given key.
     *
     * @param namespace    is a namespace in which the given key is unique
     * @param key          the setting's key.
     * @param defaultValue the value to return when no such setting was found.
     *
     * @return a setting value.
     */
    boolean setting(final String namespace, final String key, final boolean defaultValue);

    /**
     * Combines the given keys into a namespace.
     * @param keys a list of keys
     * @return a namespace that can then be passed to the various other methods that accept a namespace.
     */
    String asNamespace(final String... keys);
}

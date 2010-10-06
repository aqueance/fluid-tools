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
package org.fluidity.foundation;

/**
 * Provides delegating implementations of query methods with specific type.
 */
public abstract class AbstractSettings implements Settings {

    private static final char NAMESPACE_DELIMITER = '/';

    public String namespace(final String... keys) {
        final StringBuilder builder = new StringBuilder();

        for (final String key : keys) {
            assert key.charAt(0) != namespaceDelimiter() : key + " starts with " + namespaceDelimiter();
            assert key.charAt(key.length() - 1) != namespaceDelimiter() : key + " ends with " + namespaceDelimiter();

            if (builder.length() > 0) {
                builder.append(namespaceDelimiter());
            }

            builder.append(key);
        }

        return builder.toString();
    }

    public final int setting(final String key, final int defaultValue) {
        return Integer.parseInt(setting(key, String.valueOf(defaultValue)));
    }

    public final boolean setting(String key, boolean defaultValue) {
        return Boolean.parseBoolean(setting(key, String.valueOf(defaultValue)));
    }

    public final String setting(final String namespace, final String key, final String defaultValue) {
        return setting(namespace(namespace, key), defaultValue);
    }

    public final int setting(final String namespace, final String key, final int defaultValue) {
        return setting(namespace(namespace, key), defaultValue);
    }

    public final boolean setting(final String namespace, final String key, final boolean defaultValue) {
        return setting(namespace(namespace, key), defaultValue);
    }

    protected char namespaceDelimiter() {
        return NAMESPACE_DELIMITER;
    }
}

/*
 * Copyright (c) 2006-2009 Tibor Adam Varga (tibor.adam.varga on gmail)
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
 */
package org.fluidity.foundation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Exposes a {@link java.util.Properties} object through the {@link org.fluidity.foundation.Settings} interface.
 *
 * @author Tibor Varga
 */
public final class MockSettings implements Settings {

    private final Properties properties = new Properties();

    public MockSettings(final Properties properties) {
        this.properties.putAll(properties);
    }

    public String[] keys() {
        final List<String> keys = new ArrayList<String>();

        for (final Object o : properties.keySet()) {
            final String key = (String) o;

            if (key.indexOf("/") < 0) {
                keys.add(key);
            }
        }

        return keys.toArray(new String[keys.size()]);
    }

    public String setting(String key, String defaultValue) {
        final String value = properties.getProperty(key);
        return value == null ? defaultValue : value;
    }

    public int setting(String key, int defaultValue) {
        final String value = properties.getProperty(key);
        return value == null ? defaultValue : Integer.valueOf(value);
    }

    public boolean setting(String key, boolean defaultValue) {
        final String value = properties.getProperty(key);
        return value == null ? defaultValue : Boolean.valueOf(value);
    }

    public String[] namespaces() {
        final Set<String> keys = new HashSet<String>();

        for (final Object o : properties.keySet()) {
            String key = (String) o;

            final int slashIndex = key.indexOf("/");
            if (slashIndex >= 0) {
                keys.add(key.substring(0, slashIndex));
            }
        }

        return keys.toArray(new String[keys.size()]);
    }

    public String[] keys(String namespace) {
        namespace += "/";

        final List<String> keys = new ArrayList<String>();

        for (final Object o : properties.keySet()) {
            final String key = (String) o;

            if (key.startsWith(namespace)) {
                keys.add(key.substring(namespace.length()));
            }
        }

        return keys.toArray(new String[keys.size()]);
    }

    public String setting(String namespace, String key, String defaultValue) {
        return setting(namespace(namespace, key), defaultValue);
    }

    public int setting(String namespace, String key, int defaultValue) {
        return setting(namespace(namespace, key), defaultValue);
    }

    public boolean setting(String namespace, String key, boolean defaultValue) {
        return setting(namespace(namespace, key), defaultValue);
    }

    public String namespace(String... keys) {
        final StringBuilder builder = new StringBuilder();

        for (final String key : keys) {
            assert !key.startsWith("/") : key;
            assert !key.endsWith("/") : key;

            if (builder.length() > 0) {
                builder.append('/');
            }

            builder.append(key);
        }

        return builder.toString();
    }
}

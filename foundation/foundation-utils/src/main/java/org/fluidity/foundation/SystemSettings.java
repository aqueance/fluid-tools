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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Provides static access to Fluid Tools internal settings.
 */
public final class SystemSettings {

    public static final String FLUIDITY = "fluidity";
    public static final String SYSTEM_PREFIX = FLUIDITY + ":";

    private SystemSettings() {
        throw new UnsupportedOperationException("No instance allowed");
    }

    private static final Map<String, String> properties = new HashMap<String, String>();

    static {
        final Properties props = new Properties();

        try {
            final ClassLoader classloader = ClassLoaderUtils.findClassLoader(SystemSettings.class);
            final InputStream stream = classloader.getResourceAsStream(FLUIDITY + ".properties");

            if (stream != null) {
                props.load(stream);

                for (final Map.Entry<Object, Object> entry : props.entrySet()) {
                    properties.put((String) entry.getKey(), (String) entry.getValue());
                }
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void set(final String key, final String value) {
        synchronized (properties) {
            properties.put(key, value);
        }
    }

    public static boolean isSet(final String key, final String... values) {
        synchronized (properties) {
            if (!properties.containsKey(key)) {
                properties.put(key, System.getProperty(SYSTEM_PREFIX + key));
            }
        }

        final String value = get(key);

        for (final String expected : values) {
            if (expected.equals(value)) {
                return true;
            }
        }

        return false;
    }

    private static String get(String key) {
        synchronized (properties) {
            return properties.get(key);
        }
    }
}

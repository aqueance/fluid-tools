/*
 * Copyright (c) 2006-2008 Tibor Adam Varga (tibor.adam.varga on gmail)
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
package org.fluidity.foundation.settings;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.fluidity.foundation.ApplicationInfo;
import org.fluidity.foundation.logging.Log;

/**
 * In-memory settings not backed by anything. This implementation will work with or without an {@link
 * org.fluidity.foundation.ApplicationInfo} object present. When an <tt>ApplicationInfo</tt> object is present, its
 * short name will be used as a fallback namespace. What this means is that if there is a property with a key &lt;short
 * name>/&lt;key> but there is no property with the key &lt;key> then the value of the former will be returned when the
 * latter is requested.
 *
 * @author Tibor Varga
 */
final class PropertySettingsImpl implements PropertySettings {

    private final Map<URL, Map<String, String>> properties = new LinkedHashMap<URL, Map<String, String>>();

    private final String prefix;

    public PropertySettingsImpl() {
        this((String) null);
    }

    public PropertySettingsImpl(final ApplicationInfo info) {
        this(info.applicationShortName());
    }

    private PropertySettingsImpl(final String prefix) {
        this.prefix = prefix != null ? prefix.endsWith("/") ? prefix : prefix + "/" : null;
    }

    public void overrideProperties(final URL url, final Properties properties) {
        final Map<String, String> map = new HashMap<String, String>();
        for (final Map.Entry entry : properties.entrySet()) {
            map.put((String) entry.getKey(), (String) entry.getValue());
        }

        synchronized (this.properties) {
            this.properties.put(url, map);
        }

        Log.info(getClass(), "Loaded properties from " + url);
    }

    public String[] keys() {
        final Set<String> keys = new LinkedHashSet<String>();

        synchronized (this.properties) {
            for (final Map<String, String> map : properties.values()) {
                for (final String key : map.keySet()) {
                    if (prefix != null && key.startsWith(prefix)) {
                        final String subkey = key.substring(prefix.length());

                        if (subkey.indexOf('/') < 0) {
                            keys.add(subkey);
                        }
                    } else if (key.indexOf('/') < 0) {
                        keys.add(key);
                    }
                }
            }
        }

        return keys.toArray(new String[keys.size()]);
    }

    public String setting(final String spec, final String defaultValue) {
        final String key = extrapolate(spec);
        String actualKey;

        synchronized (this.properties) {
            final List<Map.Entry<URL, Map<String, String>>> entries =
                new ArrayList<Map.Entry<URL, Map<String, String>>>(properties.entrySet());

            for (final ListIterator<Map.Entry<URL, Map<String, String>>> i = entries.listIterator(entries.size());
                 i.hasPrevious();) {
                final Map.Entry<URL, Map<String, String>> entry = i.previous();
                final Map<String, String> map = entry.getValue();

                String value = prefix != null ? map.get(actualKey = prefix + key) : map.get(actualKey = key);

                if (value == null) {
                    value = map.get(actualKey = key);
                }

                if (value != null) {
                    Log.info(getClass(), "Found property " + spec + " in " + entry.getKey() + " as " + actualKey);
                    return extrapolate(value);
                }
            }
        }

        Log.info(getClass(), "Property " + spec + " not found");
        return defaultValue;
    }

    private String extrapolate(String spec) {
        final char[] chars = new char[spec.length()];
        spec.getChars(0, spec.length(), chars, 0);

        final StringBuilder value = new StringBuilder(spec.length());
        final StringBuilder name = new StringBuilder();
        final StringBuilder defaults = new StringBuilder();

        boolean escaping = false;
        boolean expectingOpenBrace = false;
        boolean collectingName = false;
        boolean collectingDefaults = false;
        boolean collectingValue = true;
        StringBuilder collect = value;
        int position = 0;

        for (final char c : chars) {
            switch (c) {
            case '\\':
                if (escaping) {
                    escaping = false;
                    collect.append(c);
                } else {
                    escaping = true;
                }

                break;
            case '$':
                if (escaping) {
                    escaping = false;
                    collect.append(c);
                } else {
                    if (!collectingValue) {
                        throw new IllegalArgumentException(spec + ": unexpected character '$' at position " + position);
                    }

                    collectingValue = false;
                    expectingOpenBrace = true;
                }

                break;

            case '{':
                if (escaping) {
                    escaping = false;
                    collect.append(c);
                } else if (expectingOpenBrace) {
                    expectingOpenBrace = false;
                    collectingName = true;
                    collect = name;
                } else {
                    collect.append(c);
                }

                break;

            case '|':
                if (escaping) {
                    escaping = false;
                    collect.append(c);
                } else if (collectingName) {
                    collectingName = false;
                    collectingDefaults = true;
                    collect = defaults;
                } else {
                    collect.append(c);
                }

                break;

            case '}':
                if (escaping) {
                    escaping = false;
                    collect.append(c);
                } else if (collectingName || collectingDefaults) {
                    collectingName = false;
                    collectingDefaults = false;
                    collectingValue = true;
                    collect = value;

                    String property = setting(name.toString(), System.getProperty(name.toString()));

                    if (property == null) {
                        final String defaultValue = defaults.toString();

                        if (defaultValue.length() == 0) {
                            Log.warning(getClass(), "Property " + name + " not found");
                        } else {
                            property = defaultValue;
                        }
                    }

                    value.append(property == null ? name : property);
                    name.setLength(0);
                    defaults.setLength(0);
                } else {
                    collect.append(c);
                }

                break;

            default:
                collect.append(c);
                break;
            }

            ++position;
        }

        return value.toString();
    }

    public int setting(final String key, final int defaultValue) {
        return Integer.parseInt(setting(key, String.valueOf(defaultValue)));
    }

    public boolean setting(String key, boolean defaultValue) {
        return Boolean.parseBoolean(setting(key, String.valueOf(defaultValue)));
    }

    public String[] namespaces() {
        final Set<String> keys = new HashSet<String>();
        final String appPrefix = prefix != null ? prefix.substring(0, prefix.length() - 1) : null;

        synchronized (this.properties) {
            for (final Map<String, String> map : properties.values()) {
                for (final String key : map.keySet()) {
                    int slashIndex = key.indexOf("/");
                    if (slashIndex >= 0) {
                        if (prefix != null && key.startsWith(prefix)) {
                            final String subkey = key.substring(prefix.length());

                            slashIndex = subkey.indexOf("/");
                            if (slashIndex >= 0) {
                                keys.add(subkey.substring(0, slashIndex));
                            }
                        } else {
                            if (appPrefix == null || !key.equals(appPrefix)) {
                                keys.add(key.substring(0, slashIndex));
                            }
                        }
                    }
                }
            }
        }

        return keys.toArray(new String[keys.size()]);
    }

    public String[] keys(String namespace) {
        final Set<String> keys = new LinkedHashSet<String>();

        namespace += "/";
        synchronized (this.properties) {
            for (final Map<String, String> map : properties.values()) {
                for (final String key : map.keySet()) {
                    if (prefix != null && key.startsWith(prefix)) {
                        final String subkey = key.substring(prefix.length());

                        if (subkey.startsWith(namespace)) {
                            keys.add(subkey.substring(namespace.length()));
                        }
                    } else if (key.startsWith(namespace)) {
                        keys.add(key.substring(namespace.length()));
                    }
                }
            }
        }

        return keys.toArray(new String[keys.size()]);
    }

    public String setting(final String namespace, final String key, final String defaultValue) {
        return setting(asNamespace(namespace, key), defaultValue);
    }

    public int setting(final String namespace, final String key, final int defaultValue) {
        return setting(asNamespace(namespace, key), defaultValue);
    }

    public boolean setting(final String namespace, final String key, final boolean defaultValue) {
        return setting(asNamespace(namespace, key), defaultValue);
    }

    public String asNamespace(final String... keys) {
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

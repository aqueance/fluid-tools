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

import org.fluidity.composition.Component;
import org.fluidity.composition.Optional;
import org.fluidity.foundation.AbstractSettings;
import org.fluidity.foundation.ApplicationInfo;
import org.fluidity.foundation.Logging;
import org.fluidity.foundation.logging.BootstrapLog;

/**
 * In-memory settings not read from anywhere. This implementation will work with or without an {@link org.fluidity.foundation.ApplicationInfo} object present.
 * When an <code>ApplicationInfo</code> object is present, its short name will be used as a fallback namespace. What this means is that if there is a property
 * with a key &lt;short name>/&lt;key> but there is no property with the key &lt;key> then the value of the former will be returned when the latter is
 * requested.
 *
 * <p/>
 *
 * This implementation also performs key expansion. A "${...}" or "${...|...}" pattern found anywhere in the key=value specification, without the quote marks of
 * course, will trigger the recursive expansion logic. Between the curly braces (first form) or between the opening curly brace and the pipe character (second
 * form) is a key. The logic first tries to find a value for that key among the key=value specifications then, and failing that, among the System properties. In
 * the second form, a default value is specified between the pipe character and the closing curly brace, which will be substituted in place of the "${...}" or
 * "${...|...}" expression if no value could be found for the given key. Short of a defaul value, the key itself will be substituted. Circular references are
 * handled gracefully by substituting the key or the default value of the last property in the circular reference chain in place of the expression, depending
 * whether a default value has been specified.
 *
 * @author Tibor Varga
 */
@Component
final class PropertySettingsImpl extends AbstractSettings implements PropertySettings {

    private final Logging log = new BootstrapLog("settings");

    private final Map<URL, Map<String, String>> properties = new LinkedHashMap<URL, Map<String, String>>();
    private final String prefix;

    public PropertySettingsImpl(@Optional final ApplicationInfo info) {
        this(info == null ? null : info.key());
    }

    private PropertySettingsImpl(final String prefix) {
        this.prefix = prefix != null ? prefix.charAt(prefix.length() - 1) == namespaceDelimiter() ? prefix : prefix + namespaceDelimiter() : null;
    }

    public void overrideProperties(final URL url, final Properties properties) {
        final Map<String, String> map = new HashMap<String, String>();
        for (final Map.Entry entry : properties.entrySet()) {
            map.put((String) entry.getKey(), (String) entry.getValue());
        }

        synchronized (this.properties) {
            this.properties.put(url, map);
        }

        log.info(getClass(), "Loaded properties from " + url);
    }

    public String[] keys() {
        final Set<String> keys = new LinkedHashSet<String>();

        synchronized (this.properties) {
            for (final Map<String, String> map : properties.values()) {
                for (final String key : map.keySet()) {
                    if (prefix != null && key.startsWith(prefix)) {
                        final String subkey = key.substring(prefix.length());

                        if (subkey.indexOf(namespaceDelimiter()) < 0) {
                            keys.add(subkey);
                        }
                    } else if (key.indexOf(namespaceDelimiter()) < 0) {
                        keys.add(key);
                    }
                }
            }
        }

        return keys.toArray(new String[keys.size()]);
    }

    public String setting(final String spec, final String defaultValue) {
        final String key = expand(null, spec);
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
                    log.info(getClass(), "Found property " + spec + " in " + entry.getKey() + " as " + actualKey);
                    return expand(actualKey, value);
                }
            }
        }

        log.info(getClass(), "Property " + spec + " not found");
        return defaultValue;
    }

    public String[] namespaces() {
        final Set<String> keys = new HashSet<String>();
        final String appPrefix = prefix != null ? prefix.substring(0, prefix.length() - 1) : null;

        synchronized (this.properties) {
            for (final Map<String, String> map : properties.values()) {
                for (final String key : map.keySet()) {
                    int slashIndex = key.indexOf(namespaceDelimiter());
                    if (slashIndex >= 0) {
                        if (prefix != null && key.startsWith(prefix)) {
                            final String subkey = key.substring(prefix.length());

                            slashIndex = subkey.indexOf(namespaceDelimiter());
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

        namespace += namespaceDelimiter();
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

    private String expand(final String key, final String spec) {
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
                    collect.append('\\');
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

                    final String nameKey = name.toString();
                    final boolean selfReference = nameKey.equals(key);
                    String property =
                            selfReference ? System.getProperty(nameKey) : setting(nameKey, System.getProperty(nameKey));

                    if (property == null) {
                        final String defaultValue = defaults.toString();

                        if (defaultValue.length() == 0 || defaultValue.equals(key)) {
                            String message = "Property " + name + " not found" + (selfReference ? " (refers to itself)" : "");
                            log.info(getClass(), message);
                        } else {
                            property = setting(defaultValue, defaultValue);
                        }
                    }

                    final String result = property == null && key == null ? nameKey : property;

                    if (result != null) {
                        value.append(result);
                    }

                    name.setLength(0);
                    defaults.setLength(0);
                } else {
                    collect.append(c);
                }

                break;

            default:
                if (escaping) {
                    escaping = false;
                    collect.append('\\');
                }

                collect.append(c);
                break;
            }

            ++position;
        }

        return value.length() == 0 ? null : value.toString();
    }

}

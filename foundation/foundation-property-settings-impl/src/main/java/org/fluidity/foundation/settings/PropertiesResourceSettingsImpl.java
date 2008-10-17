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
package org.fluidity.foundation.settings;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.fluidity.composition.Component;
import org.fluidity.foundation.ApplicationInfo;
import org.fluidity.foundation.Resources;
import org.fluidity.foundation.Settings;
import org.fluidity.foundation.logging.Log;

/**
 * Extends a properties enabled settings implementation with functionality to load properties file resources.
 *
 * @author Tibor Varga
 * @version $Revision$
 */
@Component(automatic = false)
final class PropertiesResourceSettingsImpl implements Settings {

    private final PropertySettings delegate;
    private final Resources resources;

    public PropertiesResourceSettingsImpl(final PropertySettings settings, final Resources resources) {
        this.delegate = settings;
        this.resources = resources;
        load("default");
    }

    public PropertiesResourceSettingsImpl(final PropertySettings settings,
                                          final Resources resources,
                                          final ApplicationInfo appInfo) {
        this(settings, resources);
        load(appInfo.applicationShortName());
    }

    private void load(final String name) {
        try {
            final Properties properties = new Properties();
            final String resourceName = resources.resourceName(name + ".properties");
            final InputStream stream = resources.loadResource(resourceName);

            if (stream == null) {
                Log.info(getClass(), resourceName + " not found");
            } else {
                properties.load(stream);
                this.delegate.overrideProperties(resources.locateResource(resourceName), properties);
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String[] keys() {
        return delegate.keys();
    }

    public String setting(final String key, final String defaultValue) {
        return delegate.setting(key, defaultValue);
    }

    public int setting(final String key, final int defaultValue) {
        return delegate.setting(key, defaultValue);
    }

    public boolean setting(final String key, final boolean defaultValue) {
        return delegate.setting(key, defaultValue);
    }

    public String[] namespaces() {
        return delegate.namespaces();
    }

    public String[] keys(final String namespace) {
        return delegate.keys(namespace);
    }

    public String setting(final String namespace, final String key, final String defaultValue) {
        return delegate.setting(namespace, key, defaultValue);
    }

    public int setting(final String namespace, final String key, final int defaultValue) {
        return delegate.setting(namespace, key, defaultValue);
    }

    public boolean setting(final String namespace, final String key, final boolean defaultValue) {
        return delegate.setting(namespace, key, defaultValue);
    }

    public String asNamespace(String... keys) {
        return delegate.asNamespace(keys);
    }
}

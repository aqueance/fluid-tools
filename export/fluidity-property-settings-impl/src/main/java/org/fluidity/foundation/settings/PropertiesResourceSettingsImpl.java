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

import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.fluidity.composition.Component;
import org.fluidity.composition.Optional;
import org.fluidity.foundation.AbstractSettings;
import org.fluidity.foundation.ApplicationInfo;
import org.fluidity.foundation.Exceptions;
import org.fluidity.foundation.Resources;

/**
 * Extends {@link org.fluidity.foundation.settings.PropertySettings} with functionality to load properties file resources. First all
 * <code>default.properties</code> resources are loaded from the context class loader and then if the application provides implementation for the {@link
 * org.fluidity.foundation.ApplicationInfo} interface then all <code>.properties</code> resources are loaded whose name equals to {@link
 * org.fluidity.foundation.ApplicationInfo#key()}.
 *
 * @author Tibor Varga
 */
@Component
final class PropertiesResourceSettingsImpl extends AbstractSettings {

    private final PropertySettings delegate;
    private final Resources resources;

    public PropertiesResourceSettingsImpl(final PropertySettings settings, final Resources resources, @Optional final ApplicationInfo appInfo) {
        this.delegate = settings;
        this.resources = resources;
        load("default");

        if (appInfo != null) {
            load(appInfo.key());
        }
    }

    private void load(final String name) {
        Exceptions.wrap(String.format("loading %s.properties", name), new Exceptions.Command<Void>() {
            public Void run() throws Exception {
                final Properties properties = new Properties();
                final String resourceName = resources.resourceName(String.format("%s.properties", name));
                final URL[] urls = resources.locateResources(resourceName);

                for (final URL url : urls) {
                    final InputStream stream = url.openStream();
                    assert stream != null : url;

                    properties.load(stream);
                    delegate.overrideProperties(url, properties);
                }

                return null;
            }
        });
    }

    public String[] keys() {
        return delegate.keys();
    }

    public String setting(final String key, final String defaultValue) {
        return delegate.setting(key, defaultValue);
    }

    public String[] namespaces() {
        return delegate.namespaces();
    }

    public String[] keys(final String namespace) {
        return delegate.keys(namespace);
    }

    public String namespace(final String... keys) {
        return delegate.namespace(keys);
    }
}

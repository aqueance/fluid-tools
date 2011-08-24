/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fluidity.foundation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.Context;
import org.fluidity.foundation.configuration.Configuration;
import org.fluidity.foundation.configuration.Setting;
import org.fluidity.foundation.spi.PropertyProvider;

@Component
@Context({ Configuration.Definition.class, Configuration.Context.class })
final class ConfigurationImpl<T> implements Configuration<T> {

    private final AtomicReference<T> configuration = new AtomicReference<T>();

    public ConfigurationImpl(final PropertyProvider provider, final ComponentContext context) {
        final Definition definition = context.annotation(Definition.class, getClass());

        final String prefix = propertyContext(context);

        @SuppressWarnings("unchecked")
        final Class<T> settingsApi = (Class<T>) definition.value();

        final ClassLoader loader = settingsApi.getClassLoader();
        final Class[] interfaces = { settingsApi };

        final PropertyProvider.PropertyChangeListener listener = new PropertyProvider.PropertyChangeListener() {

            @SuppressWarnings("unchecked")
            public void propertiesChanged(final PropertyProvider provider) {
                final Map<Method, Object> properties = new HashMap<Method, Object>();

                for (final Method method : settingsApi.getMethods()) {
                    final Setting setting = method.getAnnotation(Setting.class);
                    assert setting != null : String.format("No @%s specified for method %s", Setting.class.getName(), method);

                    final String property = prefix.concat(setting.key());
                    final Object value = provider.property(property);

                    properties.put(method, value == null ? convert(setting.undefined(), method.getReturnType()) : value);
                }

                configuration.set((T) Proxy.newProxyInstance(loader, interfaces, new InvocationHandler() {
                    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                        if (method.getDeclaringClass() == Object.class) {
                            return method.invoke(this, args);
                        }

                        assert method.getDeclaringClass().isAssignableFrom(settingsApi) : method;
                        return properties.get(method);
                    }
                }));
            }

            @SuppressWarnings("unchecked")
            private Object convert(final String value, final Class<?> type) {
                if (value == null || value.length() == 0) {
                    return null;
                } else if (type == String.class) {
                    return value;
                } else if (type == Boolean.TYPE || type == Boolean.class) {
                    return Boolean.valueOf(value);
                } else if (type == Byte.TYPE || type == Byte.class) {
                    return Byte.valueOf(value);
                } else if (type == Short.TYPE || type == Short.class) {
                    return Short.valueOf(value);
                } else if (type == Integer.TYPE || type == Integer.class) {
                    return Integer.valueOf(value);
                } else if (type == Long.TYPE || type == Long.class) {
                    return Long.valueOf(value);
                } else if (type == Float.TYPE || type == Float.class) {
                    return Float.valueOf(value);
                } else if (type == Double.TYPE || type == Double.class) {
                    return Double.valueOf(value);
                } else if (Enum.class.isAssignableFrom(type)) {
                    return Enum.valueOf((Class<Enum>) type, value);
                } else if (type == Class.class) {
                    try {
                        return settingsApi.getClassLoader().loadClass(value);
                    } catch (final ClassNotFoundException e) {
                        throw new IllegalArgumentException(e);
                    }
                } else {
                    throw new IllegalArgumentException(String.format("Cannot convert %s to type %s", value, type));
                }
            }
        };

        provider.addChangeListener(listener);

        if (configuration.get() == null) {
            listener.propertiesChanged(provider);
        }
    }

    private String propertyContext(final ComponentContext context) {
        final Context[] annotations = context.annotations(Context.class);
        final StringBuilder prefix = new StringBuilder();

        if (annotations != null) {
            for (final Context next : annotations) {
                prefix.append(next.value()).append('.');
            }
        }

        return prefix.toString();
    }

    public T snapshot() {
        return configuration.get();
    }
}

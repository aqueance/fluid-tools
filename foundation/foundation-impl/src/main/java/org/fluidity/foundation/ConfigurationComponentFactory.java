/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.Context;
import org.fluidity.composition.OpenComponentContainer;
import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.foundation.configuration.Configuration;
import org.fluidity.foundation.configuration.DynamicConfiguration;
import org.fluidity.foundation.configuration.Properties;
import org.fluidity.foundation.configuration.Setting;
import org.fluidity.foundation.configuration.StaticConfiguration;
import org.fluidity.foundation.spi.PropertyProvider;

/**
 * Factory for {@link DynamicConfiguration} and {@link StaticConfiguration} components. This is a context aware factory that uses the {@link @Properties}
 * annotation to decide what instance to produce.
 * <p/>
 * We use a factory instead of directly making the {@link Configuration} implementation context aware because one of its dependencies is actually defined by the
 * context: {@link Properties#provider()}. This factory is capable of reading that annotation and adding to the container the particular provider for the {@link
 * Configuration} implementation to pick up as dependency.
 */
@Component(api = Configuration.class, type = ConfigurationComponentFactory.ConfigurationImpl.class)
@Context(Properties.class)
final class ConfigurationComponentFactory implements ComponentFactory<Configuration> {

    public Configuration newComponent(final OpenComponentContainer container, final ComponentContext context) throws ComponentContainer.ResolutionException {
        final OpenComponentContainer nested = container.makeNestedContainer();
        final ComponentContainer.Registry registry = nested.getRegistry();

        final Properties properties = context.annotation(Properties.class);

        if (properties == null) {
            throw new ComponentContainer.ResolutionException("Annotation %s is missing from Configuration dependency", Properties.class);
        }

        registry.bindInstance(Properties.class, properties);
        registry.bindComponent(PropertyProvider.class, properties.provider());
        registry.bindComponent(Configuration.class, ConfigurationImpl.class);

        return nested.getComponent(Configuration.class);
    }

    static class ConfigurationImpl<T> implements StaticConfiguration<T>, DynamicConfiguration<T> {

        private final AtomicReference<T> configuration = new AtomicReference<T>();

        @SuppressWarnings( { "unchecked" })
        ConfigurationImpl(final Properties properties, final PropertyProvider provider) {
            final Class<Configuration> settingsApi = (Class<Configuration>) properties.api();

            final ClassLoader loader = settingsApi.getClassLoader();
            final Class<?>[] interfaces = { settingsApi };

            final PropertyProvider.PropertyChangeListener listener = new PropertyProvider.PropertyChangeListener() {
                public void propertiesChanged(final PropertyProvider provider) {
                    final Map<Method, Object> properties = new HashMap<Method, Object>();

                    for (final Method method : settingsApi.getMethods()) {
                        final Setting setting = method.getAnnotation(Setting.class);
                        assert setting != null : String.format("No @%s specified for method %s", Setting.class.getName(), method);

                        final String property = setting.key();
                        final Object value = provider.property(property);

                        properties.put(method, value == null ? cast(setting.fallback(), method.getReturnType()) : value);
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

                private Object cast(final String value, final Class<?> type) {
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

        public T configuration() {
            return configuration.get();
        }
    }
}

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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.ComponentFactory;
import org.fluidity.composition.Context;
import org.fluidity.composition.OpenComponentContainer;

/**
 * Factory for {@link org.fluidity.foundation.DynamicConfiguration} components. This is a context aware factory that uses the {@link @Properties} annotation to
 * decide what instance to produce.
 */
@Component(api = Configuration.class, type = ConfigurationFactory.ConfigurationImpl.class)
@Context(Properties.class)
final class ConfigurationFactory implements ComponentFactory<Configuration> {

    public Configuration newComponent(final OpenComponentContainer container, final ComponentContext context) throws ComponentContainer.ResolutionException {
        final OpenComponentContainer nested = container.makeNestedContainer();
        final ComponentContainer.Registry registry = nested.getRegistry();

        final Properties properties = context.annotation(Properties.class);

        registry.bindInstance(Properties.class, properties);
        registry.bindComponent(PropertyProvider.class, properties.provider());
        registry.bindComponent(Configuration.class, ConfigurationImpl.class);

        return nested.getComponent(Configuration.class);
    }

    static class ConfigurationImpl<T> implements StaticConfiguration<T>, DynamicConfiguration<T> {

        private final AtomicReference<T> configuration = new AtomicReference<T>();

        @SuppressWarnings( { "unchecked" })
        ConfigurationImpl(final Properties properties, final PropertyProvider provider) {
            final Class<Configuration> componentApi = (Class<Configuration>) properties.api();

            final ClassLoader loader = componentApi.getClassLoader();
            final Class<?>[] interfaces = { componentApi };

            final PropertyProvider.PropertyChangeListener listener = new PropertyProvider.PropertyChangeListener() {
                public void propertiesChanged(final PropertyProvider provider) {
                    final Map<Method, Object> properties = new HashMap<Method, Object>();
                    
                    for (final Method method : componentApi.getMethods()) {
                        final Setting setting = method.getAnnotation(Setting.class);
                        assert setting != null : String.format("No @%s specified for method %s", Setting.class.getName(), method);

                        final String property = setting.key();
                        final String value = provider.property(property);
                        final String fallback = setting.fallback();

                        properties.put(method, value == null ? (fallback.length() == 0 ? null : fallback) : value);
                    }

                    configuration.set((T) Proxy.newProxyInstance(loader, interfaces, new InvocationHandler() {
                        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                            assert componentApi.isAssignableFrom(method.getDeclaringClass()) : method;
                            return properties.get(method);
                        }
                    }));
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

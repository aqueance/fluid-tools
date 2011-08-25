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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.Context;
import org.fluidity.composition.Optional;
import org.fluidity.composition.spi.CustomComponentFactory;
import org.fluidity.foundation.configuration.Configuration;
import org.fluidity.foundation.configuration.Setting;
import org.fluidity.foundation.spi.PropertyProvider;

@Component(api = Configuration.class)
@Context( { Configuration.Definition.class, Configuration.Context.class })
final class ConfigurationFactory implements CustomComponentFactory {
    public Instance resolve(final ComponentContext context, final Resolver dependencies) throws ComponentContainer.ResolutionException {
        final Configuration.Definition definition = context.annotation(Configuration.Definition.class, Configuration.class);
        final Configuration.Context[] contexts = context.annotations(Configuration.Context.class);
        final Dependency<?> dependency = dependencies.resolve(definition.value());

        return new Instance() {
            @SuppressWarnings("unchecked")
            public void bind(final Registry registry) throws ComponentContainer.BindingException {
                final Object settings = dependency.instance();

                if (settings != null) {
                    registry.bindInstance(settings, Object.class);
                }

                if (contexts != null) {
                    registry.bindInstance(contexts, Configuration.Context[].class);
                }

                registry.bindInstance(definition, Configuration.Definition.class);
                registry.bindComponent(ConfigurationImpl.class);
            }
        };
    }

    @Component(automatic = false)
    static final class ConfigurationImpl<T> implements Configuration<T> {

        private final AtomicReference<T> configuration = new AtomicReference<T>();

        /**
         * Constructs a new configuration object.
         *
         * @param definition used to find out to what interface to adapt the properties provided by <code>provider</code>.
         * @param provider   provides properties.
         * @param defaults   when present, used as the provider of default values for properties missing from <code>provider</code>.
         * @param context    used to find the property prefix to apply to properties queried from the <code>provider</code>.
         */
        public ConfigurationImpl(final Definition definition,
                                 final @Optional PropertyProvider provider,
                                 final @Optional T defaults,
                                 final @Optional Context[] context) {
            final String[] prefixes = propertyContexts(context);

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

                        Object value = null;

                        if (provider != null) {
                            for (int i = 0, limit = prefixes.length; value == null && i < limit; i++) {
                                value = provider.property(prefixes[i].concat(setting.key()));
                            }
                        }

                        final Object property;

                        if (value == null) {
                            final Object fallback = defaults == null ? null : Exceptions.wrap(new Exceptions.Command<Object>() {
                                public Object run() throws Exception {
                                    return method.invoke(defaults);
                                }
                            });

                            final String undefined = setting.undefined();

                            property = fallback == null
                                       ? convert(undefined.length() == 0 ? null : undefined, method.getReturnType())
                                       : convert(fallback, method.getReturnType());
                        } else {
                            property = convert(value, method.getReturnType());
                        }

                        properties.put(method, property);
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

                private Object convert(final Object value, final Class<?> target) {
                    if (value == null) {
                        return null;
                    } else if (target.isAssignableFrom(value.getClass())) {
                        return value;
                    } else if (target == String.class) {
                        return String.valueOf(value);
                    } else if (value instanceof String) {
                        return stringToObject(target, (String) value);
                    } else if (value instanceof Number) {
                        return numberToPrimitive(target, (Number) value);
                    } else if (value instanceof Boolean) {
                        return booleanToNumber(target, (byte) (((Boolean) value) ? 1 : 0));
                    }

                    throw new IllegalArgumentException(String.format("Cannot convert %s to type %s", value, target));
                }

                @SuppressWarnings("unchecked")
                private Object stringToObject(final Class<?> target, final String text) {
                    try {
                        return numberToPrimitive(target, Double.valueOf(text));
                    } catch (final NumberFormatException ignore) {
                        if (String.valueOf(true).equals(text)) {
                            return booleanToNumber(target, (byte) 1);
                        } else if (String.valueOf(false).equals(text)) {
                            return booleanToNumber(target, (byte) 0);
                        } else if (target == Boolean.TYPE || target == Boolean.class) {
                            return Boolean.valueOf(text);
                        } else if (target == Byte.TYPE || target == Byte.class) {
                            return Double.valueOf(text).byteValue();
                        } else if (target == Short.TYPE || target == Short.class) {
                            return Double.valueOf(text).shortValue();
                        } else if (target == Integer.TYPE || target == Integer.class) {
                            return Double.valueOf(text).intValue();
                        } else if (target == Long.TYPE || target == Long.class) {
                            return Double.valueOf(text).longValue();
                        } else if (target == Float.TYPE || target == Float.class) {
                            return Double.valueOf(text).floatValue();
                        } else if (target == Double.TYPE || target == Double.class) {
                            return Double.valueOf(text);
                        } else if (Enum.class.isAssignableFrom(target)) {
                            return Enum.valueOf((Class<Enum>) target, text);
                        } else if (target == Class.class) {
                            try {
                                return settingsApi.getClassLoader().loadClass(text);
                            } catch (final ClassNotFoundException e) {
                                throw new IllegalArgumentException(e);
                            }
                        }

                        throw new IllegalArgumentException(String.format("Cannot convert %s to type %s", text, target));
                    }
                }

                private Object numberToPrimitive(final Class<?> target, final Number number) {
                    if (target == Boolean.TYPE || target == Boolean.class) {
                        return number.doubleValue() != 0d;
                    } else if (target == Byte.TYPE || target == Byte.class) {
                        return number.byteValue();
                    } else if (target == Short.TYPE || target == Short.class) {
                        return number.shortValue();
                    } else if (target == Integer.TYPE || target == Integer.class) {
                        return number.intValue();
                    } else if (target == Long.TYPE || target == Long.class) {
                        return number.longValue();
                    } else if (target == Float.TYPE || target == Float.class) {
                        return number.floatValue();
                    } else if (target == Double.TYPE || target == Double.class) {
                        return number.doubleValue();
                    }

                    throw new IllegalArgumentException(String.format("Cannot convert %s to type %s", number, target));
                }

                private Object booleanToNumber(final Class<?> target, final byte flag) {
                    if (target == Boolean.TYPE || target == Boolean.class) {
                        return flag != 0;
                    } else if (target == Byte.TYPE || target == Byte.class) {
                        return flag;
                    } else if (target == Short.TYPE || target == Short.class) {
                        return (short) flag;
                    } else if (target == Integer.TYPE || target == Integer.class) {
                        return (int) flag;
                    } else if (target == Long.TYPE || target == Long.class) {
                        return (long) flag;
                    } else if (target == Float.TYPE || target == Float.class) {
                        return (float) flag;
                    } else if (target == Double.TYPE || target == Double.class) {
                        return (double) flag;
                    }

                    throw new IllegalArgumentException(String.format("Cannot convert %s to type %s", flag, target));
                }
            };

            if (provider != null) {
                provider.addChangeListener(listener);
            }

            if (configuration.get() == null) {
                listener.propertiesChanged(provider);
            }
        }

        private String[] propertyContexts(final Context[] annotations) {
            final List<String> list = new ArrayList<String>((annotations == null ? 0 : annotations.length) + 1);

            final StringBuilder prefix = new StringBuilder();
            list.add(prefix.toString());

            if (annotations != null) {
                for (final Context next : annotations) {
                    prefix.append(next.value()).append('.');
                    list.add(prefix.toString());
                }
            }

            Collections.reverse(list);

            return list.toArray(new String[list.size()]);
        }

        public T snapshot() {
            return configuration.get();
        }
    }
}

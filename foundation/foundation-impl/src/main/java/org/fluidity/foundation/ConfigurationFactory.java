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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.Context;
import org.fluidity.composition.Optional;
import org.fluidity.composition.spi.CustomComponentFactory;
import org.fluidity.foundation.spi.PropertyProvider;

@Component(api = Configuration.class)
@Context({ Configuration.Interface.class, Configuration.Context.class })
final class ConfigurationFactory implements CustomComponentFactory {

    public Instance resolve(final ComponentContext context, final Resolver dependencies) throws ComponentContainer.ResolutionException {
        final Configuration.Interface definition = context.annotation(Configuration.Interface.class, Configuration.class);
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

                registry.bindInstance(definition, Configuration.Interface.class);
                registry.bindComponent(ConfigurationImpl.class);
            }
        };
    }

    @Component(automatic = false)
    static final class ConfigurationImpl<T> implements Configuration<T> {

        private final PropertyProvider provider;
        private final T configuration;

        /**
         * Constructs a new configuration object.
         *
         * @param definition used to find out to what interface to adapt the properties provided by <code>provider</code>.
         * @param provider   provides properties.
         * @param defaults   when present, used as the provider of default values for properties missing from <code>provider</code>.
         * @param context    used to find the property prefix to apply to properties queried from the <code>provider</code>.
         */
        @SuppressWarnings("unchecked")
        public ConfigurationImpl(final Interface definition,
                                 final @Optional PropertyProvider provider,
                                 final @Optional T defaults,
                                 final @Optional Context... context) {
            this.provider = provider;

            final String[] prefixes = propertyContexts(context);
            final Class<T> api = (Class<T>) definition.value();

            configuration = Proxies.create(api, new PropertyLoader<T>(api, prefixes, defaults, provider));
        }

        public T settings() {
            return configuration;
        }

        public <R> R query(final Query<T, R> query) {
            if (provider == null) {
                return query.read(configuration);
            } else {
                final AtomicReference<R> value = new AtomicReference<R>();

                provider.properties(new Runnable() {
                    public void run() {
                        value.set(query.read(configuration));
                    }
                });

                return value.get();
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

        private static class PropertyLoader<T> implements InvocationHandler {
            private final Class<T> api;
            private final String[] prefixes;
            private final T defaults;
            private final PropertyProvider provider;
            private final ClassLoader loader;

            public PropertyLoader(final Class<T> api, final String[] prefixes, final T defaults, final PropertyProvider provider) {
                this.api = api;
                this.prefixes = prefixes;
                this.defaults = defaults;
                this.provider = provider;
                this.loader = api.getClassLoader();
            }

            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                assert method.getDeclaringClass().isAssignableFrom(api) : method;

                final Property setting = method.getAnnotation(Property.class);
                assert setting != null : String.format("No @%s specified for method %s", Property.class.getName(), method);

                final Class<?> type = method.getReturnType();
                final Type genericType = method.getGenericReturnType();

                return property(setting.split(),
                                setting.grouping(),
                                String.format(setting.ids(), args),
                                String.format(setting.list(), args),
                                String.format(setting.undefined(), args),
                                type,
                                genericType,
                                prefixes,
                                String.format(setting.key(), args),
                                defaults,
                                method,
                                args);
            }

            private Object property(final String split,
                                    final String grouping,
                                    final String ids,
                                    final String list,
                                    final String undefined,
                                    final Class<?> type,
                                    final Type genericType,
                                    final String[] prefixes,
                                    final String suffix,
                                    final T defaults,
                                    final Method method,
                                    final Object[] args) {
                if (ids == null || ids.isEmpty()) {
                    Object value = null;

                    if (isComposite(type)) {
                        try {
                            return composite(type, suffix);
                        } catch (final Exception e) {
                            throw new IllegalArgumentException(String.format("Could not create objects of type %s", Strings.arrayNotation(type)), e);
                        }
                    } else {
                        if (provider != null) {
                            for (int i = 0, limit = prefixes.length; value == null && i < limit; i++) {
                                value = provider.property(prefixes[i].concat(suffix));
                            }
                        }

                        final Object property;

                        if (value == null) {
                            final Object fallback = defaults == null ? null : Exceptions.wrap(new Exceptions.Command<Object>() {
                                public Object run() throws Exception {
                                    assert method != null;
                                    return method.invoke(defaults, args);
                                }
                            });

                            property = fallback == null
                                       ? convert(undefined.length() == 0 ? null : undefined, type, genericType, split, grouping, suffix, loader)
                                       : convert(fallback, type, genericType, split, grouping, suffix, loader);
                        } else {
                            property = convert(value, type, genericType, split, grouping, suffix, loader);
                        }

                        return property;
                    }
                } else {
                    final String[] identifiers = (String[]) property(split,
                                                                     grouping,
                                                                     null,
                                                                     null,
                                                                     undefined,
                                                                     String[].class,
                                                                     null,
                                                                     prefixes,
                                                                     String.format("%s.%s", suffix, ids),
                                                                     defaults,
                                                                     null,
                                                                     null);

                    if (identifiers != null) {
                        final Map<String, Object> instances = new LinkedHashMap<String, Object>();
                        final String format = list == null || list.isEmpty() ? suffix.concat(".%s") : list;
                        final Type itemType;

                        if (Collection.class.isAssignableFrom(type)) {
                            itemType = Generics.typeParameter(genericType, 0);
                        } else if (Map.class.isAssignableFrom(type)) {
                            itemType = Generics.typeParameter(genericType, 1);
                        } else if (type.isArray()) {
                            itemType = Generics.arrayComponentType(genericType);
                        } else {
                            throw new IllegalArgumentException(String.format("Type %s is neither an array, nor a collection nor a map", Strings.arrayNotation(type)));
                        }

                        for (final String id : identifiers) {
                            final Object value = property(split,
                                                          grouping,
                                                          null,
                                                          null,
                                                          null,
                                                          Generics.rawType(itemType),
                                                          itemType,
                                                          prefixes,
                                                          String.format(format, id), null, null,
                                                          null);

                            if (value != null) {
                                instances.put(id, value);
                            }
                        }

                        if (List.class.isAssignableFrom(type)) {
                            return new ArrayList<Object>(instances.values());
                        } else if (Set.class.isAssignableFrom(type)) {
                            return new HashSet<Object>(instances.values());
                        } else if (Map.class.isAssignableFrom(type)) {
                            final Map<Object, Object> map = new LinkedHashMap<Object, Object>();

                            final Type keyType = Generics.typeParameter(genericType, 0);
                            for (final Map.Entry<String, Object> entry: instances.entrySet()) {
                                map.put(convert(entry.getKey(), Generics.rawType(keyType), keyType, null, null, null, loader), entry.getValue());
                            }

                            return map;
                        } else if (type.isArray()) {
                            final Object array = Array.newInstance(Generics.rawType(itemType), instances.size());

                            int i = 0;
                            for (final Object instance : instances.values()) {
                                Array.set(array, i++, instance);
                            }

                            return array;
                        } else {
                            throw new IllegalArgumentException(String.format("Type %s is neither an array, nor a collection nor a map", Strings.arrayNotation(type)));
                        }
                    } else {
                        return null;
                    }
                }
            }

            private boolean isComposite(final Class<?> type) {
                return !type.isArray() && !type.isPrimitive() && !Enum.class.isAssignableFrom(type) && !type.getName().startsWith("java.");
            }

            private Object composite(final Class<?> type, final String suffix) throws IllegalAccessException, InstantiationException {
                if (type.isInterface()) {
                        return Proxies.create(type, new InvocationHandler() {
                            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                                final Property setting = method.getAnnotation(Property.class);

                                if (setting == null) {
                                    throw new IllegalArgumentException(String.format("Method %s is not @%s annotated", method, Property.class));
                                }

                                return property(setting.split(),
                                                setting.grouping(),
                                                String.format(setting.ids(), args),
                                                String.format(setting.list(), args),
                                                String.format(setting.undefined(), args),
                                                method.getReturnType(),
                                                method.getGenericReturnType(),
                                                prefixes,
                                                String.format("%s.%s", suffix, String.format(setting.key(), args)),
                                                null,
                                                null,
                                                null);
                            }
                        });
                } else {
                    final Object instance = type.newInstance();

                    for (final Field field : type.getFields()) {
                        final Property setting = field.getAnnotation(Property.class);

                        if (setting != null) {
                            field.setAccessible(true);
                            field.set(instance, property(setting.split(),
                                                         setting.grouping(),
                                                         setting.ids(),
                                                         setting.list(),
                                                         setting.undefined(),
                                                         field.getType(),
                                                         field.getGenericType(),
                                                         prefixes,
                                                         String.format("%s.%s", suffix, setting.key()),
                                                         null,
                                                         null,
                                                         null));
                        }
                    }

                    return instance;
                }
            }

            Object convert(final Object value, final Class<?> target, final Type generic, final String delimiter, final String groupers, final String suffix, final ClassLoader loader) {
                if (value == null) {
                    return null;
                } else if (target.isAssignableFrom(value.getClass())) {
                    return value;
                } else if (target.isArray()) {
                    return arrayValue(value, target, generic, delimiter, groupers, suffix, loader);
                } else if (Collection.class.isAssignableFrom(target)) {
                    return collectionValue(value, target, generic, delimiter, groupers, suffix, loader);
                } else if (Map.class.isAssignableFrom(target)) {
                    return mapValue(value, generic, delimiter, groupers, suffix, loader);
                } else if (target == String.class) {
                    return String.valueOf(value);
                } else if (value instanceof String) {
                    return stringToObject(target, (String) value, loader);
                } else if (value instanceof Number) {
                    return numberToPrimitive(target, (Number) value);
                } else if (value instanceof Boolean) {
                    return booleanToNumber(target, (byte) (((Boolean) value) ? 1 : 0));
                }

                throw new IllegalArgumentException(String.format("Cannot convert %s to type %s", value, Strings.arrayNotation(target)));
            }

            Object arrayValue(final Object value, final Class<?> target, final Type generic, final String delimiter, final String groupers, final String suffix, final ClassLoader loader) {
                final Class<?> componentType = target.getComponentType();
                final List list = collectionValue(new ArrayList(), value, componentType, Generics.arrayComponentType(generic), delimiter, groupers, suffix, loader);

                final Object array = Array.newInstance(componentType, list.size());

                int i = 0;
                for (final Object element : list) {
                    Array.set(array, i++, element);
                }

                return array;
            }

            @SuppressWarnings("unchecked")
            <T extends Collection> T collectionValue(final T collection, final Object value, final Class<?> componentType, final Type generic, final String delimiter, final String groupers, final String suffix, final ClassLoader loader) {
                if (value instanceof String) {
                    for (final String item : split((String) value, delimiter, groupers)) {
                        collection.add(convert(item, componentType, generic, delimiter, groupers, suffix, loader));
                    }
                } else if (value.getClass().isArray()) {
                    for (int i = 0, limit = Array.getLength(value); i < limit; ++i) {
                        collection.add(convert(Array.get(value, i), componentType, generic, delimiter, groupers, suffix, loader));
                    }
                } else if (value instanceof Collection) {
                    for (final Object item : (Collection) value) {
                        collection.add(convert(item, componentType, generic, delimiter, groupers, suffix, loader));
                    }
                } else {
                    throw new IllegalArgumentException(String.format("Cannot convert %s to type %s collection", value, Strings.arrayNotation(componentType)));
                }

                return collection;
            }

            @SuppressWarnings("unchecked")
            Object collectionValue(final Object value, final Class<?> target, final Type generic, final String delimiter, final String groupers, final String suffix, final ClassLoader loader) {
                final Collection collection;

                if (target == Set.class) {
                    collection = new LinkedHashSet();
                } else if (target == List.class) {
                    collection = new ArrayList();
                } else {
                    throw new IllegalArgumentException(String.format("Collection type %s not supported (it is neither Set, List nor Map)", Strings.arrayNotation(target)));
                }

                final Type parameterType = Generics.typeParameter(generic, 0);

                if (value instanceof String) {
                    for (final String item : split((String) value, delimiter, groupers)) {
                        collection.add(convert(item, Generics.rawType(parameterType), parameterType, delimiter, groupers, suffix, loader));
                    }
                } else if (value.getClass().isArray()) {
                    for (int i = 0, limit = Array.getLength(value); i < limit; ++i) {
                        collection.add(convert(Array.get(value, i), Generics.rawType(parameterType), parameterType, delimiter, groupers, suffix, loader));
                    }
                } else if (value instanceof Collection) {
                    for (final Object item : (Collection) value) {
                        collection.add(convert(item, Generics.rawType(parameterType), parameterType, delimiter, groupers, suffix, loader));
                    }
                } else {
                    throw new IllegalArgumentException(String.format("Cannot convert %s to type %s<%s>", value, target, Strings.arrayNotation(Generics.rawType(parameterType))));
                }

                return collection;
            }

            @SuppressWarnings("unchecked")
            Object mapValue(final Object value, final Type generic, final String delimiter, final String groupers, final String suffix, final ClassLoader loader) {
                final Type keyType = Generics.typeParameter(generic, 0);
                final Type valueType = Generics.typeParameter(generic, 1);

                final Map map = new LinkedHashMap();

                if (value instanceof String) {
                    final AtomicReference<String> key = new AtomicReference<String>();

                    final List<String> pairs = split((String) value, delimiter, groupers);

                    if (pairs.size() % 2 != 0) {
                        throw new IllegalArgumentException(String.format("Missing a value for one of the keys in %s", value));
                    }

                    for (final String item : pairs) {
                        if (!key.compareAndSet(null, item)) {
                            map.put(convert(key.get(), Generics.rawType(keyType), keyType, delimiter, groupers, suffix, loader),
                                    convert(item, Generics.rawType(valueType), valueType, delimiter, groupers, suffix, loader));

                            key.set(null);
                        }
                    }
                }

                return map;
            }

            enum SplitState {
                REGULAR, GROUPED
            }

            /*
             * Parses a piece of text into a list of tokens. Tokens are delimited by delimiters. Tokens inside matching grouping characters are taken as
             * one
             * token. Delimiters and grouping characters may be escaped by the '\' character.
             */
            List<String> split(final String text, final String delimiters, final String grouping) {
                final StringBuilder nesting = new StringBuilder();
                boolean escaped = false;
                SplitState state = SplitState.REGULAR;

                final List<String> list = new ArrayList<String>();
                final StringBuilder collected = new StringBuilder();

                for (int i = 0, limit = text.length(); i < limit; ++i) {
                    final char c = text.charAt(i);

                    switch (c) {
                    case '\\':
                        if (escaped) {
                            collected.append(c);
                        }

                        escaped = !escaped;
                        break;

                    default:
                        if (escaped) {
                            collected.append(c);
                        } else {
                            final int group = grouping.indexOf(c);
                            final int level = nesting.length();

                            if (group > -1) {
                                if (group % 2 == 0) {   // group start
                                    nesting.append(c);

                                    if (level > 0) {
                                        collected.append(c);
                                    }
                                } else if (level > 0) {
                                    if (group - 1 == grouping.indexOf(nesting.charAt(level - 1))) { // group end character: does it match?
                                        nesting.setLength(level - 1);

                                        if (level > 1) {    // last level, which is current level + 1
                                            collected.append(c);
                                        } else {
                                            state = SplitState.GROUPED;
                                        }
                                    } else {
                                        collected.append(c);
                                    }
                                }
                            } else if (level == 0 && delimiters.indexOf(c) > -1) {
                                append(list, collected, state);
                                state = SplitState.REGULAR;
                            } else {
                                collected.append(c);
                            }
                        }
                    }
                }

                append(list, collected, state);

                return list;
            }

            void append(final List<String> list, final StringBuilder collected, final SplitState state) {
                final String piece = collected.toString().trim();

                if (state == SplitState.GROUPED || !piece.isEmpty()) {
                    list.add(piece);
                }

                collected.setLength(0);
            }

            @SuppressWarnings("unchecked")
            Object stringToObject(final Class<?> target, final String text, final ClassLoader loader) {
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
                            return loader.loadClass(text);
                        } catch (final ClassNotFoundException e) {
                            throw new IllegalArgumentException(e);
                        }
                    }

                    throw new IllegalArgumentException(String.format("Cannot convert %s to type %s", text, Strings.arrayNotation(target)));
                }
            }

            Object numberToPrimitive(final Class<?> target, final Number number) {
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

                throw new IllegalArgumentException(String.format("Cannot convert %s to type %s", number, Strings.arrayNotation(target)));
            }

            Object booleanToNumber(final Class<?> target, final byte flag) {
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

                throw new IllegalArgumentException(String.format("Cannot convert %s to type %s", flag, Strings.arrayNotation(target)));
            }
        }
    }
}

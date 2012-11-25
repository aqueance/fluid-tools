/*
 * Copyright (c) 2006-2012 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.Optional;
import org.fluidity.foundation.Command.Process;
import org.fluidity.foundation.spi.PropertyProvider;

/**
 * @author Tibor Varga
 */
@Component
@Component.Context({ Configuration.Prefix.class, Component.Reference.class })
final class ConfigurationImpl<T> implements Configuration<T> {

    private final PropertyProvider provider;
    private final T settings;

    /**
     * Constructs a new configuration object.
     *
     * @param context  used to find the property prefix to apply to properties queried from the <code>provider</code>.
     * @param provider provides properties.
     * @param defaults when found, used as the provider of default values for properties missing from <code>provider</code>.
     */
    ConfigurationImpl(final ComponentContext context, final @Optional PropertyProvider provider, final @Optional T defaults) {
        this.provider = provider;

        @SuppressWarnings("unchecked")
        final Class<T> api = (Class<T>) context.annotation(Component.Reference.class, Configuration.class).parameter(0);
        this.settings = Proxies.create(api, new PropertyLoader<T>(api, propertyContexts(context.annotations(Prefix.class)), defaults, provider));
    }

    public T settings() {
        return settings;
    }

    public <R> R query(final Query<R, T> query) {
        return Exceptions.wrap(new Process<R, Exception>() {
            public R run() throws Exception {
                if (provider == null) {
                    return query.run(settings);
                } else {
                    return provider.properties(new PropertyProvider.Query<R>() {
                        public R run() throws Exception {
                            return query.run(settings);
                        }
                    });
                }
            }
        });
    }

    /*
     * Computes the property prefixes that will be traversed for any property in order until a value is found in the underlying property provider.
     *
     * Assuming N context annotations, c1, c2, c3, &hellip;, cN-2, cN-1, cN, the property prefixes will be as follows:
     *  - c1.c2.c3 &hellip; .cN-2.cN-1.cN.
     *  - c2.c3 &hellip; .cN-2.cN-1.cN.
     *  - c3. &hellip; .cN-2.cN-1.cN.
     *  - &hellip;
     *  - cN-2.cN-1.cN.
     *  - cN-1.cN.
     *  - cN.
     *  - c1.c2.c3 &hellip; .cN-2.
     *  - &hellip;
     *  - c1.c2.c3.
     *  - c1.c2.
     *  - c1.
     *  - (empty prefix)
     */
    private String[] propertyContexts(final Prefix[] annotations) {
        final List<String> contexts = new ArrayList<String>();

        if (annotations != null) {
            for (final Prefix context : annotations) {
                contexts.add(context.value().concat("."));
            }
        }

        final List<String> list = new ArrayList<String>(contexts.size() << 1);

        final StringBuilder prefix = new StringBuilder();

        // empty prefix is added first and last prefix is not appended; will be appended below when going through the prefixes in reverse
        for (final String next : contexts) {
            list.add(prefix.toString());
            prefix.append(next);
        }

        Collections.reverse(contexts);

        prefix.setLength(0);
        for (final String next : contexts) {
            prefix.insert(0, next);
            list.add(prefix.toString());
        }

        Collections.reverse(list);

        return list.isEmpty() ? new String[] { "" } : Lists.asArray(String.class, list);
    }

    private static class PropertyLoader<T> implements InvocationHandler {
        private static final String TRUE = String.valueOf(true);
        private static final String FALSE = String.valueOf(false);

        private final Class<T> api;
        private final String[] prefixes;
        private final T defaults;
        private final PropertyProvider provider;
        private final ClassLoader loader;

        PropertyLoader(final Class<T> api, final String[] prefixes, final T defaults, final PropertyProvider provider) {
            this.api = api;
            this.prefixes = prefixes;
            this.defaults = defaults;
            this.provider = provider;

            this.loader = !Security.CONTROLLED ? api.getClassLoader() : AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return api.getClassLoader();
                }
            });
        }

        public Object invoke(final Object proxy, final Method method, final Object[] arguments) throws Throwable {
            assert method.getDeclaringClass().isAssignableFrom(api) : method;

            final Property setting = method.getAnnotation(Property.class);
            assert setting != null : String.format("No @%s specified for method %s", Property.class.getName(), method);

            final Class<?> type = method.getReturnType();
            final Type genericType = method.getGenericReturnType();

            return Exceptions.wrap(method.toGenericString(), PropertyException.class, new Process<Object, Exception>() {
                public Object run() throws Exception {
                    final PrivilegedAction<Object> action = new PrivilegedAction<Object>() {
                        public Object run() {
                            if (!method.isAccessible()) {
                                method.setAccessible(true);
                            }

                            return property(setting.split(),
                                            setting.grouping(),
                                            String.format(setting.ids(), arguments),
                                            String.format(setting.list(), arguments),
                                            String.format(setting.undefined(), arguments),
                                            type,
                                            genericType,
                                            prefixes,
                                            String.format(setting.key(), arguments),
                                            defaults,
                                            method,
                                            arguments);
                        }
                    };

                    return Security.CONTROLLED ? AccessController.doPrivileged(action) : action.run();
                }
            });
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
                                final Object[] arguments) {
            if (ids == null || ids.isEmpty()) {
                Object value = null;

                if (isComposite(type)) {
                    try {
                        return composite(type, suffix);
                    } catch (final Exception e) {
                        throw new IllegalArgumentException(String.format("Could not create objects of type %s", Strings.formatClass(false, true, type)), e);
                    }
                } else {
                    if (provider != null) {
                        for (int i = 0, limit = prefixes.length; value == null && i < limit; i++) {
                            value = provider.property(prefixes[i].concat(suffix));
                        }
                    }

                    if (value == null) {
                        final Object fallback = defaults == null ? null : Methods.invoke(method, defaults, arguments);
                        value = fallback == null ? (undefined.length() == 0 ? null : undefined) : fallback;
                    }

                    return convert(value, type, genericType, split, grouping, suffix, loader);
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
                        throw new IllegalArgumentException(String.format("Type %s is neither an array, nor a collection nor a map",
                                                                         Strings.formatClass(false, true, type)));
                    }

                    for (final String id : identifiers) {
                        final Object value = property(split, grouping,
                                                      null,
                                                      null,
                                                      null,
                                                      Generics.rawType(itemType),
                                                      itemType,
                                                      prefixes,
                                                      String.format(format, id),
                                                      null,
                                                      null,
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
                        for (final Map.Entry<String, Object> entry : instances.entrySet()) {
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
                        throw new IllegalArgumentException(String.format("Type %s is neither an array, nor a collection nor a map",
                                                                         Strings.formatClass(false, true, type)));
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
                    public Object invoke(final Object proxy, final Method method, final Object[] arguments) throws Throwable {
                        final Property setting = method.getAnnotation(Property.class);

                        if (setting == null) {
                            throw new IllegalArgumentException(String.format("Method %s is not @%s annotated", method, Property.class));
                        }

                        return property(setting.split(),
                                        setting.grouping(),
                                        String.format(setting.ids(), arguments),
                                        String.format(setting.list(), arguments),
                                        String.format(setting.undefined(), arguments),
                                        method.getReturnType(),
                                        method.getGenericReturnType(),
                                        prefixes,
                                        String.format("%s.%s", suffix, String.format(setting.key(), arguments)),
                                        null,
                                        null,
                                        null);
                    }
                });
            } else {
                final Object instance = type.newInstance();
                final Field[] fields = type.getFields();

                AccessibleObject.setAccessible(fields, true);

                for (final Field field : fields) {
                    final Property setting = field.getAnnotation(Property.class);

                    if (setting != null) {
                        field.set(instance,
                                  property(setting.split(),
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
                final PrimitiveType type = PRIMITIVE_TYPES.get(target);

                if (type != null) {
                    switch (type) {
                    case BOOLEAN:
                        return false;
                    case BYTE:
                        return (byte) 0;
                    case SHORT:
                        return (short) 0;
                    case INTEGER:
                        return 0;
                    case LONG:
                        return 0L;
                    case FLOAT:
                        return 0F;
                    case DOUBLE:
                        return 0D;
                    case CHARACTER:
                        return (char) 0;
                    }
                }

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
                return booleanToPrimitive(PRIMITIVE_TYPES.get(target), target, (Boolean) value);
            }

            throw new IllegalArgumentException(String.format("Cannot convert %s to type %s", value, Strings.formatClass(false, true, target)));
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
                throw new IllegalArgumentException(String.format("Cannot convert %s to type %s collection", value, Strings.formatClass(false, true, componentType)));
            }

            return collection;
        }

        @SuppressWarnings("unchecked")
        Object collectionValue(final Object value,
                               final Class<?> target,
                               final Type generic,
                               final String delimiter,
                               final String groupers,
                               final String suffix,
                               final ClassLoader loader) {
            final Collection collection;

            if (target == Set.class) {
                collection = new LinkedHashSet();
            } else if (target == List.class) {
                collection = new ArrayList();
            } else {
                throw new IllegalArgumentException(String.format("Collection type %s not supported (it is neither Set, List nor Map)",
                                                                 Strings.formatClass(false, true, target)));
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
                throw new IllegalArgumentException(String.format("Cannot convert %s to type %s<%s>",
                                                                 value,
                                                                 target,
                                                                 Strings.formatClass(false, true, Generics.rawType(parameterType))));
            }

            return collection;
        }

        @SuppressWarnings("unchecked")
        Object mapValue(final Object value, final Type generic, final String delimiter, final String groupers, final String suffix, final ClassLoader loader) {
            final Type keyType = Generics.typeParameter(generic, 0);
            final Type valueType = Generics.typeParameter(generic, 1);

            final Map map = new LinkedHashMap();

            if (value instanceof String) {
                final List<String> pairs = split((String) value, delimiter, groupers);

                if (pairs.size() % 2 != 0) {
                    throw new IllegalArgumentException(String.format("Missing a value for one of the keys in %s", value));
                }

                final AtomicReference<String> key = new AtomicReference<String>();

                for (final String item : pairs) {
                    if (!key.compareAndSet(null, item)) {
                        map.put(convert(key.getAndSet(null), Generics.rawType(keyType), keyType, delimiter, groupers, suffix, loader),
                                convert(item, Generics.rawType(valueType), valueType, delimiter, groupers, suffix, loader));
                    }
                }
            }

            return map;
        }

        enum SplitState {
            REGULAR, GROUPED
        }

        /*
         * Parses a piece of text into a list of tokens. Tokens are delimited by delimiters. Tokens inside matching grouping characters are taken as one
         * token. Delimiters and grouping characters may be escaped by the '\' character.
         */
        List<String> split(final String text, final String delimiters, final String grouping) {
            final StringBuilder nesting = new StringBuilder();
            boolean escaped = false;
            PropertyLoader.SplitState state = PropertyLoader.SplitState.REGULAR;

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
                                        state = PropertyLoader.SplitState.GROUPED;
                                    }
                                } else {
                                    collected.append(c);
                                }
                            }
                        } else if (level == 0 && delimiters.indexOf(c) > -1) {
                            append(list, collected, state);
                            state = PropertyLoader.SplitState.REGULAR;
                        } else {
                            collected.append(c);
                        }
                    }
                }
            }

            append(list, collected, state);

            return list;
        }

        void append(final List<String> list, final StringBuilder collected, final PropertyLoader.SplitState state) {
            final String piece = collected.toString().trim();

            if (state == PropertyLoader.SplitState.GROUPED || !piece.isEmpty()) {
                list.add(piece);
            }

            collected.setLength(0);
        }

        @SuppressWarnings("unchecked")
        Object stringToObject(final Class<?> target, final String text, final ClassLoader loader) {
            try {
                return numberToPrimitive(target, Double.valueOf(text));
            } catch (final NumberFormatException ignore) {
                if (Enum.class.isAssignableFrom(target)) {
                    return Enum.valueOf((Class<Enum>) target, text);
                } else if (target == Class.class) {
                    try {
                        return loader.loadClass(text);
                    } catch (final ClassNotFoundException e) {
                        throw new IllegalArgumentException(e);
                    }
                } else {
                    final PrimitiveType type = PRIMITIVE_TYPES.get(target);

                    if (TRUE.equals(text)) {
                        return booleanToPrimitive(type, target, true);
                    } else if (FALSE.equals(text)) {
                        return booleanToPrimitive(type, target, false);
                    } else if (type != null) {
                        switch (type) {
                        case BOOLEAN:
                            return Boolean.valueOf(text);
                        case BYTE:
                            return Double.valueOf(text).byteValue();
                        case SHORT:
                            return Double.valueOf(text).shortValue();
                        case INTEGER:
                            return Double.valueOf(text).intValue();
                        case LONG:
                            return Double.valueOf(text).longValue();
                        case FLOAT:
                            return Double.valueOf(text).floatValue();
                        case DOUBLE:
                            return Double.valueOf(text);
                        case CHARACTER:
                            if (text != null && text.length() == 1) {
                                return text.charAt(0);
                            }
                        }
                    }
                }

                throw new IllegalArgumentException(String.format("Cannot convert %s to type %s", text, Strings.formatClass(false, true, target)));
            }
        }

        Object numberToPrimitive(final Class<?> target, final Number number) {
            final PrimitiveType type = PRIMITIVE_TYPES.get(target);

            if (type != null) {
                switch (type) {
                case BOOLEAN:
                    return number.doubleValue() != 0d;
                case BYTE:
                    return number.byteValue();
                case SHORT:
                    return number.shortValue();
                case INTEGER:
                    return number.intValue();
                case LONG:
                    return number.longValue();
                case FLOAT:
                    return number.floatValue();
                case DOUBLE:
                    return number.doubleValue();
                }
            }

            throw new IllegalArgumentException(String.format("Cannot convert %s to type %s", number, Strings.formatClass(false, true, target)));
        }

        Object booleanToPrimitive(final PrimitiveType type, final Class<?> target, final boolean flag) {
            if (type != null) {
                final int value = flag ? 1 : 0;

                switch (type) {
                case BOOLEAN:
                    return flag;
                case BYTE:
                    return (byte) value;
                case SHORT:
                    return (short) value;
                case INTEGER:
                    return value;
                case LONG:
                    return (long) value;
                case FLOAT:
                    return (float) value;
                case DOUBLE:
                    return (double) value;
                }
            }

            throw new IllegalArgumentException(String.format("Cannot convert %s to type %s", flag, Strings.formatClass(false, true, target)));
        }
    }

    private static Map<Class<?>, PrimitiveType> PRIMITIVE_TYPES = new HashMap<Class<?>, PrimitiveType>();

    static {
        @SuppressWarnings({ "MismatchedReadAndWriteOfArray", "UnusedDeclaration" })
        PrimitiveType[] types = PrimitiveType.values(); // makes sure the PRIMITIVE_TYPES map gets initialized
    }

    private enum PrimitiveType {
        CHARACTER(Character.class),
        BYTE(Byte.class),
        SHORT(Short.class),
        INTEGER(Integer.class),
        LONG(Long.class),
        FLOAT(Float.class),
        DOUBLE(Double.class),
        BOOLEAN(Boolean.class);

        private PrimitiveType(final Class<?> boxing) {
            try {
                PRIMITIVE_TYPES.put(boxing, this);
                PRIMITIVE_TYPES.put((Class<?>) boxing.getField("TYPE").get(null), this);
            } catch (final Exception e) {
                assert false : e;
            }
        }
    }
}

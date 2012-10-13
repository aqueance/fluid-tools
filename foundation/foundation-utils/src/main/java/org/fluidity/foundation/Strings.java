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

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

/**
 * Common string related utilities.
 *
 * @author Tibor Varga
 */
public final class Strings extends Utility {

    private Strings() { }

    /**
     * Prints a class name. For arrays, this method adds "[]" to the class name once for each step in the depth of the array. For instance:
     * <pre>
     * assert Strings.printClass(false, Object[][][].class).equals("java.lang.Object[][][]");
     * </pre>
     *
     * @param textual if <code>true</code>, the type's string representation is used, otherwise only its fully qualified name is used.
     * @param type    the class, which may be an array.
     *
     * @return the textual representation of the given class.
     */
    public static String printClass(final boolean textual, final Class<?> type) {
        return printClass(textual, true, type);
    }

    /**
     * Prints a class name. For arrays, this method adds "[]" to the class name once for each step in the depth of the array. For instance:
     * <pre>
     * assert Strings.printClass(false, false, Object[][][].class).equals("Object[][][]");
     * </pre>
     *
     * @param textual   if <code>true</code>, the type's string representation is used, otherwise only its name is used.
     * @param qualified if <code>true</code> and <code>textual</code> is <code>false</code>, the type's fully qualified name is used, otherwise its simple name
     *                  is used.
     * @param type      the class, which may be an array.
     *
     * @return the textual representation of the given class.
     */
    public static String printClass(final boolean textual, final boolean qualified, final Class<?> type) {
        final StringBuilder builder = new StringBuilder();

        Class<?> componentType = type;
        for (; componentType.isArray(); componentType = componentType.getComponentType()) {
            builder.append("[]");
        }

        final String typeName;

        if (textual) {
            typeName = componentType.toString();
        } else if (qualified) {
            typeName = componentType.getName();
        } else {
            final String name = componentType.getName();
            typeName = name.contains(".") ? name.substring(name.lastIndexOf(".") + 1).replace('$', '.') : name;
        }

        return builder.insert(0, typeName).toString();
    }

    /**
     * Forms the simple Java-like notation or an identifier of the given annotation instance. Examples of the Java-like notation:
     * <ul>
     * <li><code>@MyAnnotation</code>: "@MyAnnotation"</li>
     * <li><code>@MyAnnotation(1000)</code>: "@MyAnnotation(1000)"</li>
     * <li><code>@MyAnnotation(id = 1000, code = "abcd")</code>: "@MyAnnotation(id=1000,code=\"abcd\")"</li>
     * <li><code>@MyAnnotation({ 1, 2, 3 })</code>: "@MyAnnotation({1,2,3})"</li>
     * </ul>
     * <p/>
     * Parameters set to their default value are not printed.
     *
     * @param identity   if <code>true</code>, return an identifier for the annotation, otherwise its Java-like form.
     * @param annotation the annotation instance to return the Java-like form of.
     *
     * @return the Java-like notation or an identifier for the given annotation.
     */
    public static String printAnnotation(final boolean identity, final Annotation annotation) {
        final StringBuilder output = new StringBuilder();

        final Class<? extends Annotation> type = annotation.annotationType();

        final String name = type.getName();
        output.append('@').append(name.substring(name.lastIndexOf(".") + 1).replace('$', '.'));

        final StringBuilder builder = new StringBuilder();
        final Method[] methods = type.getDeclaredMethods();
        try {
            if (methods.length == 1 && methods[0].getName().equals("value")) {
                methods[0].setAccessible(true);
                appendValue(identity, builder, methods[0].invoke(annotation));
            } else {
                Arrays.sort(methods, new Comparator<Method>() {
                    public int compare(final Method method1, final Method method2) {
                        return method1.getName().compareTo(method2.getName());
                    }
                });

                for (final Method method : methods) {

                    // not every Annotation class is an annotation...
                    if (method.getParameterTypes().length == 0 && method.getReturnType() != Void.TYPE) {
                        final Object fallback = method.getDefaultValue();
                        final Class<?> parameterType = method.getReturnType();

                        method.setAccessible(true);
                        final Object value = method.invoke(annotation);

                        if (fallback == null || !(parameterType.isArray() ? Arrays.equals((Object[]) fallback, (Object[]) value) : fallback.equals(value))) {
                            if (builder.length() > 0) {
                                builder.append(", ");
                            }

                            final StringBuilder parameter = new StringBuilder();
                            parameter.append(method.getName()).append('=');
                            appendValue(identity, parameter, value);
                            builder.append(parameter);
                        }
                    }
                }
            }
        } catch (final IllegalAccessException e) {
            assert false : e;
        } catch (final InvocationTargetException e) {
            assert false : e;
        }

        if (builder.length() > 0) {
            output.append('(').append(builder).append(')');
        }

        return output.toString();
    }

    private static void appendValue(final boolean identity, final StringBuilder output, final Object value) {
        if (value instanceof Class) {
            if (identity) {
                output.append(Generics.identity((Class) value));
            } else {
                output.append(printClass(false, false, (Class) value)).append(".class");
            }
        } else if (value instanceof Type) {
            if (identity) {
                output.append(Generics.identity((Type) value));
            } else {
                output.append(value);
            }
        } else if (value != null) {
            output.append(value.getClass().isArray() ? appendArray(identity, value) : value);
        } else {
            output.append((Object) null);
        }
    }

    private static String appendArray(final boolean identity, final Object value) {
        final Listing output = new Listing(",");

        for (int i = 0, length = Array.getLength(value); i < length; ++i) {
            appendValue(identity, output.next(), Array.get(value, i));
        }

        return output.surround("{}").toString();
    }

    /**
     * For proxies, it returns <code>"proxy@&lt;identity hash code>[&lt;list of interfaces>]"</code>; for ordinary classes, it returns <code>"&lt;fully
     * qualified class name>@&lt;identity hash code>"</code>; arrays are printed as described at {@link #printClass(boolean, Class)}.
     * <p/>
     * Arrays are printed as the list of individual items, surrounded with brackets.
     *
     * @param object the object to print; may be <code>null</code>.
     *
     * @return the proxy friendly run-time identity of the given object.
     */
    public static String printObjectId(final Object object) {
        if (object == null) {
            return String.valueOf(object);
        } else {
            final Class<?> type = object.getClass();

            if (type.isArray()) {
                final Listing text = delimited();

                for (int i = 0, limit = Array.getLength(object); i < limit; ++i) {
                    text.add(Strings.printObjectId(Array.get(object, i)));
                }

                return text.surround("[]").toString();
            } else {
                return Proxy.isProxyClass(type)
                       ? String.format("proxy@%x%s", System.identityHashCode(object), interfaces(type))
                       : String.format("%s@%x", printClass(false, type), System.identityHashCode(object));
            }
        }
    }

    /**
     * Returns a textual representation of the given object. In particular:<ul>
     * <li>if the object is <code>null</code>, returns <code>"null"</code>;</li>
     * <li>if the object is a proxy and has {@linkplain Proxies.Identity custom identity}, {@link Proxies.Identity#toString(Object) Proxies.Identity.toString()}
     * is invoked with the object and its result is returned;</li>
     * <li>if the object is a proxy, has no {@linkplain Proxies.Identity custom identity}, and <code>identity</code> is <code>false</code>, then
     * <code>"proxy[&lt;list of interfaces>]"</code> is returned;</li>
     * <li>if the object is a proxy, has no {@linkplain Proxies.Identity custom identity}, and <code>identity</code> is <code>true</code>, then {@link
     * #printObjectId(Object)  Strings.printObjectId()} is invoked and returned;</li>
     * <li>if the object overrides {@link Object#toString()}, the result of invoking that method is returned;</li>
     * <li>Otherwise if <code>identity</code> is <code>true</code> then {@link #printObjectId(Object) Strings.printObjectId()} is invoked and returned, else the
     * fully qualified name of the object type is returned.</li>
     * </ul>
     * <p/>
     * Arrays are printed as the list of individual items, surrounded with brackets.
     *
     * @param identify tells if object identity is to be printed in absence of a custom {@link Object#toString()} method (<code>true</code>) or just the
     *                 type identity (<code>false</code>).
     * @param object   the object to print; may be <code>null</code>.
     *
     * @return the the proxy friendly textual representation, if any, or the identity of the given object.
     */
    public static String printObject(final boolean identify, final Object object) {
        if (object == null) {
            return String.valueOf(object);
        } else {
            final Class<?> type = object.getClass();

            if (type.isArray()) {
                final Listing text = delimited();

                for (int i = 0, limit = Array.getLength(object); i < limit; ++i) {
                    text.add(Strings.printObject(identify, Array.get(object, i)));
                }

                final String array = text.surround("[]").toString();
                return identify ? String.format("%x@%s", System.identityHashCode(object), array) : array;
            } else if (Proxy.isProxyClass(type) && !Proxies.isIdentified(object)) {
                return identify ? printObjectId(object) : String.format("proxy%s", interfaces(type));
            } else {
                try {
                    return (String) type.getDeclaredMethod("toString").invoke(object);
                } catch (final Exception e) {
                    return identify ? printObjectId(object) : printClass(false, type);
                }
            }
        }
    }

    private static String interfaces(final Class<?> type) {
        final Listing listing = new Listing(",");

        for (final Class<?> api : type.getInterfaces()) {
            listing.add(printClass(false, api));
        }

        return listing.surround("[]").toString();
    }

    /**
     * Returns a comma delimited {@linkplain Strings.Listing string listing} tool.
     *
     * @return a comma delimited {@linkplain Strings.Listing string listing} tool.
     */
    public static Listing delimited() {
        return delimited(", ");
    }

    /**
     * Returns a comma delimited list of the given items.
     *
     * @return a comma delimited list of the given items.
     */
    public static String delimited(final Collection<String> items) {
        return delimited().list(items).toString();
    }

    /**
     * Returns a comma delimited list of the given items.
     *
     * @return a comma delimited list of the given items.
     */
    public static String delimited(final String[] items) {
        return delimited().list(items).toString();
    }

    /**
     * Returns {@linkplain Strings.Listing string listing} tool with the given delimiter.
     *
     * @return {@linkplain Strings.Listing string listing} tool with the given delimiter.
     */
    public static Listing delimited(final String delimiter) {
        return new Listing(delimiter);
    }

    /**
     * Returns a delimited list of the given items.
     *
     * @return a delimited list of the given items.
     */
    public static String delimited(final String delimiter, final Collection<String> items) {
        return new Listing(delimiter).list(items).toString();
    }

    /**
     * Returns a delimited list of the given items.
     *
     * @return a delimited list of the given items.
     */
    public static String delimited(final String delimiter, final String[] items) {
        return new Listing(delimiter).list(items).toString();
    }

    /**
     * A string listing tool that makes working with a {@link StringBuilder} simpler when collecting list items.
     *
     * @author Tibor Varga
     */
    public static class Listing {

        /**
         * The underlying {@link StringBuilder} object.
         */
        public final StringBuilder builder = new StringBuilder();

        private final String delimiter;

        /**
         * Creates a new string listing tool with the given delimiter.
         *
         * @param delimiter the delimiter.
         */
        protected Listing(final String delimiter) {
            this.delimiter = delimiter;
        }

        /**
         * Appends the delimiter to the string builder if it is not empty.
         *
         * @return the underlying {@link StringBuilder} object.
         */
        public final StringBuilder next() {
            return builder.length() > 0 ? builder.append(delimiter) : builder;
        }

        /**
         * Collects the given text, appending the delimiter as necessary.
         *
         * @return the underlying {@link StringBuilder} object.
         */
        public final StringBuilder add(final String text) {
            return next().append(text);
        }

        /**
         * Collects the given list of texts, appending the delimiter as necessary.
         *
         * @return the underlying {@link StringBuilder} object.
         */
        public final StringBuilder list(final Collection<String> list) {
            for (final String text : list) {
                next().append(text);
            }

            return builder;
        }

        /**
         * Collects the given list of texts, appending the delimiter as necessary.
         *
         * @return the underlying {@link StringBuilder} object.
         */
        public final StringBuilder list(final String... list) {
            for (final String text : list) {
                next().append(text);
            }

            return builder;
        }

        /**
         * Sets the value of the listing.
         *
         * @param text the new value of the list.
         *
         * @return the underlying {@link StringBuilder} object.
         */
        public StringBuilder set(final String text) {
            builder.setLength(0);
            builder.append(text);
            return builder;
        }

        /**
         * Appends some text to the list <i>without</i> the delimiter.
         *
         * @param text the text to append to the list.
         *
         * @return the underlying {@link StringBuilder} object.
         */
        public StringBuilder append(final String text) {
            return builder.append(text);
        }

        /**
         * Copies the first half of <code>bracket</code> to the beginning of the list and the second half to the end.
         *
         * @param bracket the character pairs to surround the current value with; may have odd number of characters, in which case the middle one will be
         *                copied to both ends.
         *
         * @return the underlying {@link StringBuilder} object.
         */
        public StringBuilder surround(final String bracket) {
            if (!bracket.isEmpty()) {
                final int length = bracket.length();
                final int half = length >>> 1;
                surround(bracket.substring(0, half + length % 2), bracket.substring(half));
            }

            return builder;
        }

        /**
         * Copies the <code>prefix</code> to the beginning of the list and <code>suffix</code> to the end.
         *
         * @param prefix the text to prepend to the current value.
         * @param suffix the text to append to the current value.
         *
         * @return the underlying {@link StringBuilder} object.
         */
        public StringBuilder surround(final String prefix, final String suffix) {
            return builder.insert(0, prefix).append(suffix);
        }

        /**
         * Copies the <code>prefix</code> to the beginning of the list.
         *
         * @param prefix the text to prepend to the current value.
         *
         * @return the underlying {@link StringBuilder} object.
         */
        public StringBuilder prepend(final String prefix) {
            return builder.insert(0, prefix);
        }

        /**
         * Tells if the underlying {@link StringBuilder} is empty.
         *
         * @return <code>true</code> if the underlying {@link StringBuilder} is empty; <code>false</code> otherwise.
         */
        public boolean isEmpty() {
            return builder.length() == 0;
        }

        @Override
        public String toString() {
            return builder.toString();
        }
    }
}

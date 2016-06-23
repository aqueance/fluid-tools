/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import org.fluidity.foundation.security.Security;

/**
 * Common string related utilities.
 *
 * @author Tibor Varga
 */
public final class Strings extends Utility {

    private Strings() { }

    /**
     * Returns a textual representation of the given object. In particular:<ul>
     * <li>if the object is <code>null</code>, returns <code>"null"</code>;</li>
     * <li>if the object is a proxy and has {@linkplain Proxies.Identity custom identity}, {@link Proxies.Identity#toString(Object)
     * Proxies.Identity.toString()} is invoked with the object and its result is returned;</li>
     * <li>if the object is a proxy, has no {@linkplain Proxies.Identity custom identity}, and <code>identity</code> is <code>false</code>, then
     * <code>"proxy[&lt;list of interfaces&gt;]"</code> is returned;</li>
     * <li>if the object is a proxy, has no {@linkplain Proxies.Identity custom identity}, and <code>identity</code> is <code>true</code>, then {@link
     * #formatId  Strings.formatId()} is invoked and returned;</li>
     * <li>if the object overrides {@link Object#toString()}, the result of invoking that method is returned;</li>
     * <li>Otherwise if <code>identity</code> is <code>true</code> then {@link #formatId Strings.formatId()} is invoked and returned, else the
     * fully qualified name of the object type is returned.</li>
     * </ul>
     * <p>
     * Arrays are formatted as the list of individual items, surrounded with brackets.
     *
     * @param identify  tells if object identity is to be formatted in absence of a custom {@link Object#toString()} method (<code>true</code>) or just the
     *                  type identity (<code>false</code>).
     * @param qualified specifies whether the fully qualified class name of the object should be used (<code>true</code>) or only its name
     *                  (<code>false</code>).
     * @param object    the object to format; may be <code>null</code>.
     *
     * @return the the proxy friendly textual representation, if any, or the identity of the given object.
     */
    public static String formatObject(final boolean identify, final boolean qualified, final Object object) {
        if (object == null) {
            return String.valueOf(null);
        } else {
            final Class<?> type = object.getClass();

            if (type.isArray()) {
                final Lists.Delimited text = Lists.delimited();

                for (int i = 0, limit = Array.getLength(object); i < limit; ++i) {
                    text.add(Strings.formatObject(identify, qualified, Array.get(object, i)));
                }

                final String array = text.surround("[]").toString();
                return identify ? String.format("%x@%s", System.identityHashCode(object), array) : array;
            } else if (Annotation.class.isAssignableFrom(type)) {
                return Strings.formatObject(identify, qualified, ((Annotation) object).annotationType());
            } else if (Proxy.isProxyClass(type) && !Proxies.isIdentified(object)) {
                return identify ? formatId(object) : String.format("proxy%s", interfaces(type));
            } else if (object instanceof Type) {
                return Generics.toString(qualified, (Type) object);
            } else {

                // see if object overrides Object.toString()
                final Method method = Security.invoke(() -> {
                    for (Class<?> check = type; check != Object.class; check = check.getSuperclass()) {
                        try {
                            return check.getDeclaredMethod("toString");
                        } catch (final NoSuchMethodException e) {
                            // keep searching
                        }
                    }

                    return null;
                });

                if (method == null) {

                    // nope, object did not override Object.toString()
                    return identify ? formatId(object) : Strings.formatClass(false, qualified, type);
                } else {

                    // yep, object did override Object.toString()
                    return (String) Methods.invoke(Security.access(method), object);
                }
            }
        }
    }

    /**
     * Formats a class name. For arrays, this method adds "[]" to the class name once for each step in the depth of the array. For instance:
     * <pre>
     * assert Strings.formatClass(false, false, Object[][][].class).equals("Object[][][]");
     * assert Strings.formatClass(false, true, Object[][][].class).equals("java.lang.Object[][][]");
     * </pre>
     *
     * @param kind      if <code>true</code>, the type's string representation is used, otherwise only its name is used.
     * @param qualified if <code>true</code>, the type's fully qualified name is used, otherwise its simple name is used.
     * @param type      the class, which may be an array.
     *
     * @return the textual representation of the given class.
     */
    public static String formatClass(final boolean kind, final boolean qualified, final Class<?> type) {
        final StringBuilder brackets = new StringBuilder();

        Class<?> componentType = type;
        for (; componentType.isArray(); componentType = componentType.getComponentType()) {
            brackets.append("[]");
        }

        return Strings.className(componentType, kind, qualified).concat(brackets.toString());
    }

    /**
     * For proxies, it returns <code>"proxy@&lt;identity hash code&gt;[&lt;list of interfaces&gt;]"</code>; for ordinary classes, it returns <code>"&lt;fully
     * qualified class name&gt;@&lt;identity hash code&gt;"</code>; arrays are formatted as described at {@link #formatClass(boolean, boolean, Class)}.
     * <p>
     * Arrays are formatted as the list of individual items, surrounded with brackets.
     *
     * @param object the object to format; may be <code>null</code>.
     *
     * @return the proxy friendly run-time identity of the given object.
     */
    public static String formatId(final Object object) {
        if (object == null) {
            return String.valueOf(null);
        } else {
            final Class<?> type = object.getClass();

            if (type.isArray()) {
                final Lists.Delimited text = Lists.delimited();

                for (int i = 0, limit = Array.getLength(object); i < limit; ++i) {
                    text.add(Strings.formatId(Array.get(object, i)));
                }

                return text.surround("[]").toString();
            } else {
                return Proxy.isProxyClass(type)
                       ? String.format("proxy@%x%s", System.identityHashCode(object), interfaces(type))
                       : String.format("%s@%x", Strings.formatClass(false, true, type), System.identityHashCode(object));
            }
        }
    }

    /**
     * Forms the simple Java-like notation or an identifier of the given annotation instance. Examples of the Java-like notation:
     * <ul>
     * <li><code>@MyAnnotation</code>: "@MyAnnotation"</li>
     * <li><code>@MyAnnotation(1000)</code>: "@MyAnnotation(1000)"</li>
     * <li><code>@MyAnnotation(id = 1000, code = "abcd")</code>: "@MyAnnotation(id=1000,code=\"abcd\")"</li>
     * <li><code>@MyAnnotation({ 1, 2, 3 })</code>: "@MyAnnotation({1,2,3})"</li>
     * </ul>
     * <p>
     * Parameters set to their default value are not included.
     *
     * @param identity   if <code>true</code>, return an identifier for the annotation, otherwise its Java-like form.
     * @param annotation the annotation instance to return the Java-like form of.
     *
     * @return the Java-like notation or an identifier for the given annotation.
     */
    public static String describeAnnotation(final boolean identity, final Annotation annotation) {
        final Class<? extends Annotation> type = annotation.annotationType();

        final Lists.Delimited list = Lists.delimited(", ");

        final boolean proxy = !Proxy.isProxyClass(annotation.getClass());
        final Method[] methods = Security.invoke(type::getDeclaredMethods);

        try {
            if (methods.length == 1 && methods[0].getName().equals("value")) {
                appendValue(identity, list.text, Security.access(methods[0]).invoke(annotation));
            } else if (proxy) {
                list.append(annotation);
            } else {
                Arrays.sort(methods, (method1, method2) -> method1.getName().compareTo(method2.getName()));

                for (final Method method : methods) {

                    // not every Annotation class is an annotation that has method signature restriction imposed on by the compiler...
                    if (method.getParameterTypes().length == 0 && method.getReturnType() != void.class) {
                        final Object fallback = method.getDefaultValue();
                        final Class<?> parameterType = method.getReturnType();

                        final Object value = Security.access(method).invoke(annotation);

                        if (fallback == null || !(parameterType.isArray() ? Arrays.equals((Object[]) fallback, (Object[]) value) : fallback.equals(value))) {
                            final StringBuilder parameter = new StringBuilder();
                            parameter.append(method.getName()).append('=');
                            appendValue(identity, parameter, value);
                            list.add(parameter);
                        }
                    }
                }
            }
        } catch (final IllegalAccessException | InvocationTargetException e) {
            assert false : e;
        }

        final StringBuilder output = new StringBuilder();

        if (!proxy) {
            output.append('@').append(Strings.className(type, false, false));

            if (!list.isEmpty()) {
                list.surround("()");
            }
        }

        return output.append(list).toString();
    }

    private static String className(final Class type, final boolean kind, final boolean qualified) {
        assert type != null;
        final Lists.Delimited name = Lists.delimited(".");

        Class last = type;
        for (Class enclosing = type; enclosing != null; enclosing = enclosing.getEnclosingClass()) {
            name.previous();
            name.prepend(enclosing.getSimpleName());
            last = enclosing;
        }

        if (qualified && !type.isPrimitive()) {
            final String lastName = last.getName();
            final int dot = lastName.lastIndexOf('.');

            name.previous();
            name.prepend(dot > 0 ? lastName.substring(0, dot) : lastName);
        }

        return kind ? String.format("%s %s", type.isInterface() ? Annotation.class.isAssignableFrom(type) ? "@interface" : "interface" : type.isPrimitive() ? "type": "class", name) : name.toString();
    }

    private static void appendValue(final boolean identity, final StringBuilder output, final Object value) {
        if (value instanceof Class) {
            if (identity) {
                output.append(Generics.identity((Class) value));
            } else {
                output.append(Strings.formatClass(false, false, (Class) value)).append(".class");
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
        final Lists.Delimited output = Lists.delimited(",");

        for (int i = 0, length = Array.getLength(value); i < length; ++i) {
            appendValue(identity, output.next(), Array.get(value, i));
        }

        return output.surround("{}").toString();
    }

    private static String interfaces(final Class<?> type) {
        final Lists.Delimited listing = Lists.delimited(",");

        for (final Class<?> api : type.getInterfaces()) {
            listing.add(formatClass(false, true, api));
        }

        return listing.surround("[]").toString();
    }
}

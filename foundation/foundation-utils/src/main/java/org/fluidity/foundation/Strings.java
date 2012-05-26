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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

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
     * assert Strings.printClass(Object[][][].class).equals("java.lang.Object[][][]");
     * </pre>
     *
     * @param full if <code>true</code>, the type's full string representation is used, otherwise only its fully qualified name is used.
     * @param type the class, which may be an array.
     *
     * @return the Java array notation corresponding to the given class.
     */
    public static String printClass(final boolean full, final Class<?> type) {
        return printClass(full, true, type);
    }

    /**
     * Prints a class name. For arrays, this method adds "[]" to the class name once for each step in the depth of the array. For instance:
     * <pre>
     * assert Strings.printClass(Object[][][].class).equals("java.lang.Object[][][]");
     * </pre>
     *
     * @param full      if <code>true</code>, the type's full string representation is used, otherwise only its name is used.
     * @param qualified if <code>true</code> and <code>full</code> is <code>false</code>, the type's fully qualified name is used, otherwise its simple name is
     *                  used.
     * @param type      the class, which may be an array.
     *
     * @return the Java array notation corresponding to the given class.
     */
    public static String printClass(final boolean full, final boolean qualified, final Class<?> type) {
        final StringBuilder builder = new StringBuilder();

        Class<?> componentType = type;
        for (; componentType.isArray(); componentType = componentType.getComponentType()) {
            builder.append("[]");
        }

        final String typeName;

        if (full) {
            typeName = componentType.toString();
        } else if (qualified) {
            typeName = componentType.getName();
        } else {
            final String name = componentType.getName();
            typeName = name.substring(name.lastIndexOf(".") + 1).replace('$', '.');
        }

        return builder.insert(0, typeName).toString();
    }

    /**
     * Forms the simple Java-like notation of the given annotation instance. For instance:
     * <ul>
     * <li><code>@MyAnnotation</code>: "@MyAnnotation"</li>
     * <li><code>@MyAnnotation(1000)</code>: "@MyAnnotation(1000)"</li>
     * <li><code>@MyAnnotation(id = 1000, code = "abcd")</code>: "@MyAnnotation(id=1000,code=\"abcd\")"</li>
     * <li><code>@MyAnnotation({ 1, 2, 3 })</code>: "@MyAnnotation({1,2,3})"</li>
     * </ul>
     * <p/>
     * Default values are surrounded by a pair of square brackets, i.e., "[]".
     *
     * @param annotation the annotation instance to return the Java-like form of.
     *
     * @return the Java-like notation for the given annotation.
     */
    public static String printAnnotation(final Annotation annotation) {
        final StringBuilder output = new StringBuilder();

        final Class<? extends Annotation> type = annotation.annotationType();

        final String name = type.getName();
        output.append('@').append(name.substring(name.lastIndexOf(".") + 1).replace('$', '.'));

        final StringBuilder builder = new StringBuilder();
        final Method[] methods = type.getDeclaredMethods();
        try {
            if (methods.length == 1 && methods[0].getName().equals("value")) {
                appendValue(builder, methods[0].invoke(annotation));
            } else {
                Arrays.sort(methods, new Comparator<Method>() {
                    public int compare(final Method method1, final Method method2) {
                        return method1.getName().compareTo(method2.getName());
                    }
                });

                for (final Method method : methods) {
                    if (method.getParameterTypes().length == 0) {
                        if (builder.length() > 0) {
                            builder.append(", ");
                        }

                        final StringBuilder parameter = new StringBuilder();
                        parameter.append(method.getName()).append('=');

                        final Object value = method.invoke(annotation);
                        appendValue(parameter, value);

                        final Object fallback = method.getDefaultValue();
                        final Class<?> parameterType = method.getReturnType();

                        if (fallback != null && (parameterType.isArray() ? Arrays.equals((Object[]) fallback, (Object[]) value) : fallback.equals(value))) {
                            parameter.insert(0, '[').append(']');
                        }

                        builder.append(parameter);
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

    private static void appendValue(final StringBuilder output, final Object value) {
        if (value instanceof Class) {
            output.append(printClass(false, false, (Class) value)).append(".class");
        } else if (value != null) {
            output.append(value.getClass().isArray() ? appendArray(value) : value);
        } else {
            output.append((Object) null);
        }
    }

    private static String appendArray(final Object value) {
        final StringBuilder output = new StringBuilder();

        for (int i = 0, length = Array.getLength(value); i < length; ++i) {
            if (i > 0) {
                output.append(',');
            }

            appendValue(output, Array.get(value, i));
        }

        output.insert(0, '{').append('}');
        return output.toString();
    }

    /**
     * For proxies, it returns <code>"proxy@&lt;identity hash code>[&lt;list of interfaces>]"</code>; for ordinary classes, it returns <code>"&lt;fully
     * qualified class name>@&lt;identity hash code>"</code>; arrays are printed as described at {@link #printClass(boolean, Class)}. Arrays are printed as
     * the list of individual items, surrounded with brackets.
     *
     * @param object the object to print; may be <code>null</code>.
     * @return the proxy friendly run-time identity of the given object.
     */
    public static String printObjectId(final Object object) {
        if (object == null) {
            return String.valueOf(object);
        } else {
            final Class<?> type = object.getClass();

            if (type.isArray()) {
                final StringBuilder text = new StringBuilder();

                for (int i = 0, limit = Array.getLength(object); i < limit; ++i) {
                    final Object item = Array.get(object, i);

                    if (text.length() > 0) {
                        text.append(", ");
                    }

                    text.append(Strings.printObjectId(item));
                }

                return text.insert(0, '[').append(']').toString();
            } else {
                return Proxy.isProxyClass(type)
                       ? String.format("proxy@%x%s", System.identityHashCode(object), interfaces(type))
                       : String.format("%s@%x", printClass(false, type), System.identityHashCode(object));
            }
        }
    }

    private static List<String> interfaces(final Class<?> type) {
        final List<String> list = new ArrayList<String>();

        for (final Class<?> api : type.getInterfaces()) {
            list.add(printClass(false, api));
        }

        return list;
    }
}

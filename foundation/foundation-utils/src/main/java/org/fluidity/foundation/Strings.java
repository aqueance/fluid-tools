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

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Common string related utilities.
 *
 * @author Tibor Varga
 */
public final class Strings {

    private Strings() {
        throw new UnsupportedOperationException("No instance allowed");
    }

    /**
     * Adds "[]" to the class name once for each step in the depth of the array. For instance:
     * <pre>
     * assert Strings.arrayNotation(Object[][][].class).equals("java.lang.Object[][][]");
     * </pre>
     *
     * @param type the class, which may be an array.
     *
     * @return the Java array notation corresponding to the given class.
     */
    public static String arrayNotation(final Class<?> type) {
        final StringBuilder builder = new StringBuilder();

        Class<?> componentType = type;
        for (; componentType.isArray(); componentType = componentType.getComponentType()) {
            builder.append("[]");
        }

        return builder.insert(0, componentType).toString();
    }

    /**
     * Forms the simple Java-like notation of the given annotation instance. For instance:
     * <ul>
     * <li><code>@MyAnnotation</code>: "@MyAnnotation"</li>
     * <li><code>@MyAnnotation(1000)</code>: "@MyAnnotation(1000)"</li>
     * <li><code>@MyAnnotation(id = 1000, code = "abcd")</code>: "@MyAnnotation(id=1000,code=\"1000\")"</li>
     * <li><code>@MyAnnotation({ 1, 2, 3 })</code>: "@MyAnnotation({1,2,3})"</li>
     * </ul>
     *
     * Default values are surrounded by a pair of square brackets, i.e., "[]".
     *
     * @param annotation the annotation instance to return the Java-like form of.
     *
     * @return the Java-like notation for the given annotation.
     */
    public static String simpleNotation(final Annotation annotation) {
        StringBuilder builder = new StringBuilder();
        final Class<? extends Annotation> annotationClass = annotation.getClass();

        @SuppressWarnings("unchecked")
        final Class<? extends Annotation> type = Proxy.isProxyClass(annotationClass) ? (Class<Annotation>) annotationClass.getInterfaces()[0] : annotationClass;

        builder.append('@').append(type.getSimpleName());

        final StringBuilder output = new StringBuilder();

        final Method[] methods = type.getDeclaredMethods();
        try {
            if (methods.length == 1 && methods[0].getName().equals("value")) {
                appendValue(output, methods[0].invoke(annotation));
            } else {
                Arrays.sort(methods, new Comparator<Method>() {
                    public int compare(final Method method1, final Method method2) {
                        return method1.getName().compareTo(method2.getName());
                    }
                });

                for (final Method method : methods) {
                    if (output.length() > 0) {
                        output.append(", ");
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

                    output.append(parameter);
                }
            }
        } catch (final IllegalAccessException e) {
            assert false : e;
        } catch (final InvocationTargetException e) {
            assert false : e;
        }

        if (output.length() > 0) {
            builder.append('(').append(output).append(')');
        }

        return builder.toString();
    }

    public static void appendValue(final StringBuilder output, final Object value) {
        if (value instanceof Class) {
            output.append(((Class) value).getSimpleName()).append(".class");
        } else {
            output.append(value.getClass().isArray() ? appendArray(value) : value);
        }
    }

    private static String appendArray(final Object value) {
        final StringBuilder output = new StringBuilder();

        for (int i = 0, length = Array.getLength(value); i < length; ++i) {
            if (output.length() > 0) {
                output.append(',');
            }

            appendValue(output, Array.get(value, i));
        }

        output.insert(0, '{').append('}');
        return output.toString();
    }
}

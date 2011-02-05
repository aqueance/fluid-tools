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

package org.fluidity.composition;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Tibor Varga
 */
final class ComponentContextImpl implements ComponentContext {

    private final Map<Class<? extends Annotation>, Annotation[]> annotations = new HashMap<Class<? extends Annotation>, Annotation[]>();

    public ComponentContextImpl(final Map<Class<? extends Annotation>, Annotation[]> map) {
        for (final Map.Entry<Class<? extends Annotation>, Annotation[]> entry : map.entrySet()) {
            annotations.put(entry.getKey(), entry.getValue().clone());
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Annotation> T[] annotations(final Class<T> type) {
        return (T[]) annotations.get(type);
    }

    @SuppressWarnings("unchecked")
    public <T extends Annotation> T annotation(final Class<T> type, final Class<?> reference) {
        final Annotation[] annotations = annotations(type);

        if (annotations != null && annotations.length > 0) {
            return (T) annotations[annotations.length - 1];
        } else if (reference == null) {
            return null;
        } else {
            throw new ComponentContainer.ResolutionException("Annotation %s is missing from %s dependency", type, reference);
        }
    }

    public boolean defines(final Class<? extends Annotation> type) {
        final Annotation[] annotations = annotations(type);
        return annotations != null && annotations.length > 0;
    }

    public Set<Class<? extends Annotation>> types() {
        return Collections.unmodifiableSet(annotations.keySet());
    }

    @Override
    public String toString() {
        return toString(annotations);
    }

    private static String toString(final Map<Class<? extends Annotation>, Annotation[]> map) {
        final StringBuilder builder = new StringBuilder();

        for (final Map.Entry<Class<? extends Annotation>, Annotation[]> entry : map.entrySet()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }

            for (final Annotation annotation : entry.getValue()) {
                final Class<? extends Annotation> annotationClass = annotation.getClass();

                @SuppressWarnings("unchecked")
                final Class<? extends Annotation> type = Proxy.isProxyClass(annotationClass)
                                                         ? (Class<Annotation>) annotationClass.getInterfaces()[0]
                                                         : annotationClass;

                builder.append('@').append(type.getSimpleName());

                final StringBuilder values = new StringBuilder();

                final Method[] methods = type.getDeclaredMethods();
                try {
                    if (methods.length == 1 && methods[0].getName().equals("value")) {
                        appendValue(values, methods[0].invoke(annotation));
                    } else {
                        for (final Method method : methods) {
                            if (values.length() > 0) {
                                values.append(", ");
                            }

                            values.append(method.getName()).append('=');

                            appendValue(values, method.invoke(annotation));
                        }
                    }
                } catch (final IllegalAccessException e) {
                    assert false : e;
                } catch (final InvocationTargetException e) {
                    assert false : e;
                }

                if (values.length() > 0) {
                    builder.append('(').append(values).append(')');
                }
            }
        }

        builder.insert(0, '[').append(']');
        return builder.toString();
    }

    private static void appendValue(StringBuilder values, Object value) {
        if (value instanceof Class) {
            values.append(((Class) value).getSimpleName()).append(".class");
        } else {
            values.append(value);
        }
    }

    @Override
    public boolean equals(final Object o) {
        return this == o || (o != null && getClass() == o.getClass() && annotations.equals(((ComponentContextImpl) o).annotations));
    }

    @Override
    public int hashCode() {
        return annotations.hashCode();
    }
}

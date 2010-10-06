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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Provides reflection related common functionality.
 *
 * @author Tibor Varga
 */
public abstract class Reflection {

    /**
     * Tells whether the given member is accessible by anyone. The method checks accessibility of all enclosing entities.
     *
     * @param member the member to check the accessibility of.
     *
     * @return <code>true</code> if the member is accessible, <code>false</code> otherwise.
     */
    public static boolean isAccessible(final Member member) {
        final boolean isPublic = Modifier.isPublic(member.getModifiers()) && isAccessible(member.getDeclaringClass());
        final boolean isAccessible = member instanceof AccessibleObject && ((AccessibleObject) member).isAccessible();

        return isPublic || isAccessible;
    }

    /**
     * Tells whether the given class is accessible by anyone. The method checks accessibility of all enclosing entities.
     *
     * @param type the class to check the accessibility of.
     *
     * @return <code>true</code> if the class is accessible, <code>false</code> otherwise.
     */
    public static boolean isAccessible(final Class<?> type) {
        if (!Modifier.isPublic(type.getModifiers())) {
            return false;
        }

        final Method enclosingMethod = type.getEnclosingMethod();
        if (enclosingMethod != null && !isAccessible(enclosingMethod)) {
            return false;
        }

        final Constructor enclosingConstructor = type.getEnclosingConstructor();
        if (enclosingConstructor != null && !isAccessible(enclosingConstructor)) {
            return false;
        }

        final Class enclosingClass = type.getEnclosingClass();
        return enclosingClass == null || isAccessible(enclosingClass);
    }
}

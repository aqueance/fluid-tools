/*
 * Copyright (c) 2006-2008 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Softweare"), to deal
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.fluidity.composition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * In case of simple interface to implementation mapping, this annotation can be used to specify at the implementation class what interface should be bound to
 * it.
 *
 * <p/>
 *
 * The org.fluidity.maven:maven-composition-plugin Maven plugin will generate the necessary {@link PackageBindings} object and create the appropriate service
 * provider descriptor file, unless the <code>automatic</code> is <code>false</code>.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Component {

    /**
     * Tells whether this component should be automatically bound or not. If manually bound, the developer has to implement the binding in a suitable
     * <code>PackageBindings</code> object. All other annotation properties are ignored for manually bound components.
     *
     * @return <code>true</code> if this component should be automatically bound.
     */
    boolean automatic() default true;

    /**
     * Returns the class to map the implementation class against. The property defaults to the first interface the class implements or the class itself if it
     * implements no interface.
     *
     * @return a Class object.
     */
    Class api() default Object.class;

    /**
     * Tells whether this component should be bound as a fallback if no other component has been bound to its API interface.
     *
     * @return <code>true</code> if this component should be mapped as a fallback.
     */
    boolean fallback() default false;
}

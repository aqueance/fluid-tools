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

package org.fluidity.composition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * In case of simple interface to implementation mapping, this annotation can be used to specify at the implementation class what interface it should be bound
 * to. The org.fluidity.maven:maven-composition-plugin Maven plugin will generate the necessary {@link PackageBindings} object and create the appropriate
 * service provider descriptor file, unless {@link #automatic()} is set to <code>false</code>.
 * <p/>
 * This annotation can also be used to mark instance fields of components to request field inject of dependencies. In that case the annotation is processed at
 * run-time after the component has been instantiated - either by the container or externally and in the latter case the container has to be asked to initialize
 * the externally instantiated object by calling {@link org.fluidity.composition.ComponentContainer#initialize(Object)}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD })
@SuppressWarnings({ "UnusedDeclaration" })
public @interface Component {

    /**
     * Tells whether this component should be automatically bound or not. If manually bound, the developer has to implement the binding in a suitable {@link
     * PackageBindings} object. All other properties are ignored for manually bound components.
     *
     * @return <code>true</code> if this component should be automatically bound; ignored for annotated fields.
     */
    boolean automatic() default true;

    /**
     * Returns the (interface) class to map the implementation class against. The property defaults to the first interface the class implements or the class
     * itself if it implements no interface. In case of {@link org.fluidity.composition.ComponentFactory} components, the value applies to the component the
     * factory creates, not the factory itself.
     *
     * @return a Class object.
     */
    Class api() default Object.class;

    /**
     * Tells whether this component should be bound as a fallback if no other component has been bound to its API interface.
     *
     * @return <code>true</code> if this component should be mapped as a fallback; ignored for annotated fields.
     */
    boolean fallback() default false;

    /**
     * Used by {@link org.fluidity.composition.ComponentFactory} components, this annotation specifies the component class the factory component creates
     * implementation instances of.
     *
     * @return the component class this factory component creates instances of.
     *
     * @see org.fluidity.composition.ComponentFactory
     */
    Class<?> type() default Object.class;
}

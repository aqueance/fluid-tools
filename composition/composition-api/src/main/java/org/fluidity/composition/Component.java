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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * In case of simple interface to implementation mapping, this annotation can be used to specify at the implementation class what interface it should be bound
 * to. The org.fluidity.maven:maven-composition-plugin Maven plugin will generate the necessary {@link org.fluidity.composition.spi.PackageBindings} object
 * unless {@link #automatic()} is set to <code>false</code>, and will create the appropriate service provider descriptor file even if you manually supply the
 * bindings by implementing the {@link org.fluidity.composition.spi.EmptyPackageBindings} class.
 * <p/>
 * This annotation can also be used to mark instance fields of components for dependency injection. In that case the annotation is processed at run-time after
 * the component has been instantiated - either by the container or externally and in the latter case the container has to be explicitly asked to initialize the
 * externally instantiated object by calling {@link ComponentContainer#initialize(Object)}.
 *
 * @author Tibor Varga
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.CONSTRUCTOR })
public @interface Component {

    /**
     * Returns the (interface) class to map the implementation class against. The property defaults to the first interface the class implements or the class
     * itself if it implements no interface. In case of {@link org.fluidity.composition.spi.ComponentFactory} and {@link
     * org.fluidity.composition.spi.ComponentVariantFactory} components, the value applies to the component the factory creates, not the factory itself.
     *
     * @return an array of class objects; ignored for annotated fields.
     */
    Class<?>[] api() default { };

    /**
     * Tells whether this component should be automatically bound or not. If manually bound, the developer has to implement the binding in a suitable {@link
     * org.fluidity.composition.spi.PackageBindings} object. All other properties are ignored for manually bound components.
     *
     * @return <code>true</code> if this component should be automatically bound; ignored for annotated fields.
     *
     * @see org.fluidity.composition.spi.EmptyPackageBindings
     */
    boolean automatic() default true;

    /**
     * Tells whether this component should be bound as a primary or a as fallback. As a fallback it will be used when no other component has been bound to its
     * API interface as a primary.
     *
     * @return <code>true</code> if this component should be mapped as a primary; ignored for annotated fields.
     */
    boolean primary() default true;

    /**
     * Tells whether this component should be singleton or a new instance must be created for every query or reference.
     *
     * @return <code>true</code> if a new instance should be created for every query or dependency reference; ignored for annotated fields.
     */
    boolean stateful() default false;
}

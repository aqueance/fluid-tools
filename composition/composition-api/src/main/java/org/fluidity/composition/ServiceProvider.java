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
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that the annotated class, or implementing classes of the annotated interface, are service providers in the <a
 * href="http://download.oracle.com/javase/1.5.0/docs/guide/jar/jar.html#Service Provider">JAR File Specification</a>'s sense. When used on a field or
 * constructor parameter of array type, the annotation declares that the array argument must contain an instance of each implementation of the service provider
 * that is the component type of the array.
 * <p/>
 * The org.fluidity.maven:maven-composition-plugin Maven plugin will create the appropriate service provider descriptor file.
 *
 * @author Tibor Varga
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD })
@Inherited
public @interface ServiceProvider {

    /**
     * Returns the interfaces that this service provider implements. For abstract classes or interfaces the value defaults to the annotated class itself. In
     * other cases the annotation must specify a value.
     *
     * @return an array of Class objects.
     */
    Class[] api() default { };

    /**
     * Specifies that service provider classes should be instantiable by the JDK service discovery mechanism. Defaults to Fluid Tools service discovery only.
     *
     * @return <code>true</code> if the JDK service discovery mechanism should be able to instantiate the implementing classes, <code>false</code> to indicate
     *         the Fluid Tools service discovery will be used.
     */
    boolean jdk() default false;
}

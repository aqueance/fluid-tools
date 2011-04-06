/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
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
@Internal
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
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
     * Returns the what type of service provider the annotated class is. JDK uses the default value, "services", which adds requirements to the class such as
     * for the class to be public and to have a public zero-argument constructor.
     * <p/>
     * The default type is understood by {@link java.util.ServiceLoader} while other types may be used via {@link ClassDiscovery}.
     *
     * @return the service provider type.
     */
    String type() default "services";
}

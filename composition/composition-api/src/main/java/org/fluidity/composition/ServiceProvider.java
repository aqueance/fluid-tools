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

package org.fluidity.composition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.fluidity.foundation.ServiceProviders;

/**
 * Declares that the annotated class, or implementing classes of the annotated interface, are service providers in the <a
 * href="http://download.oracle.com/javase/1.5.0/docs/guide/jar/jar.html#Service Provider">JAR File Specification</a>'s meaning of the term. As long as the
 * <code>org.fluidity.maven:maven-composition-plugin</code> Maven plugin is used in the host project, such classes can then be discovered in any given class
 * loader by the {@link org.fluidity.foundation.ClassDiscovery} component, or in case the {@link #type()} parameter is not specified, using the service
 * provider discovery mechanism built in the Java platform.
 * <h3>Usage</h3>
 * <pre>
 * <span class="hl1">&#64;ServiceProvider</span>
 * public final class MyContractImpl implements Contract {
 *   ...
 * }
 * </pre>
 *
 * @author Tibor Varga
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Component.Context(collect = Component.Context.Collection.NONE)
@SuppressWarnings("JavadocReference")
public @interface ServiceProvider {

    /**
     * Specifies the interfaces that this service provider implements. For abstract classes or interfaces the value defaults to the annotated class itself. In
     * other cases the annotation must specify a value.
     *
     * @return an array of Class objects.
     */
    Class[] api() default { };

    /**
     * Specifies the what type of service provider the annotated class is. The Java platform uses the default value, "services", which adds requirements to the
     * class such as for the class to be public and to have a public zero-argument constructor.
     * <p/>
     * The default type is understood by {@link java.util.ServiceLoader} while all types are understood by {@link org.fluidity.foundation.ClassDiscovery}.
     *
     * @return the service provider type.
     */
    String type() default ServiceProviders.TYPE;
}

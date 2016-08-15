/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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
 * href="https://docs.oracle.com/javase/8/docs/technotes/guides/jar/jar.html#Service_Provider">JAR File Specification</a>'s meaning of the term. As long as the
 * <code>org.fluidity.maven:composition-maven-plugin</code> Maven plugin is used in the host project, such classes can be discovered in any given class loader
 * by the {@link org.fluidity.composition.ComponentDiscovery} component, or in case the {@link #type()} parameter is not specified, using the service provider
 * discovery mechanism built in the Java platform.
 * <h3>Usage</h3>
 * <pre>
 * <span class="hl1">&#64;ServiceProvider</span>
 * public final class MyContractImpl implements Contract {
 *   &hellip;
 * }
 * </pre>
 *
 * @author Tibor Varga
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Qualifier(Qualifier.Composition.NONE)
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
     * Specifies what type of service provider the annotated class is. The Java platform uses the default value, "services", using which requires the class to
     * be public and to have a public zero-argument constructor.
     * <p>
     * The default type is understood by <a href="https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html"><code>ServiceLoader</code></a> while
     * all types are understood by {@link org.fluidity.composition.ComponentDiscovery}.
     *
     * @return the service provider type.
     */
    String type() default ServiceProviders.TYPE;
}

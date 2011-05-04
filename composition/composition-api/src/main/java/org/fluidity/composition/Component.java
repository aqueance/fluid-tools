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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used to specify at the implementation class what interfaces it should be bound to. The org.fluidity.maven:maven-composition-plugin
 * Maven plugin will generate the necessary {@link org.fluidity.composition.spi.PackageBindings} object unless {@link #automatic()} is set to
 * <code>false</code>, and will create the appropriate service provider descriptor file even if you manually supply the bindings by implementing the {@link
 * org.fluidity.composition.spi.EmptyPackageBindings} class.
 *
 * @author Tibor Varga
 */
@Internal
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Component {

    /**
     * Returns the (interface) class to map the implementation class against. The property defaults to the list of interfaces the class directly implements or
     * the class itself if it implements no interface. In case of {@link org.fluidity.composition.spi.CustomComponentFactory} and {@link
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

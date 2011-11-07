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

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the component interface(s) that a class implements and that should be resolved to the annotated class at run-time in a dependency injection
 * container.
 * <p/>
 * On its own, this implementation is merely a marker. However, when coupled with the use of the <code>org.fluidity.maven:maven-composition-plugin</code> Maven
 * plugin, carefree dependency injection becomes possible.
 * <p/>
 * For any class marked with this annotation and without having automatic processing disabled, i.e., without {@link #automatic() @Component(automatic = false)},
 * the <code>maven-composition-plugin</code> Maven plugin will generate at build time the necessary metadata that the dependency injection container will need
 * to pick up and process the annotated class at run-time.
 *
 * @author Tibor Varga
 */
@Internal
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Component {

    /**
     * Returns the interfaces or classes that should resolve to the implementation class at run-time.
     * <p/>
     * In case of {@link org.fluidity.composition.spi.ComponentFactory}, the value applies to the component the factory creates, not the factory itself.
     * <p/>
     * When omitted, the property defaults to the list of interfaces determined by the algorithm documented at {@link org.fluidity.composition.Components}.
     *
     * @return an array of class objects.
     */
    @SuppressWarnings("JavadocReference")
    Class<?>[] api() default { };

    /**
     * Tells whether this class can be processed by the <code>org.fluidity.maven:maven-composition-plugin</code> Maven plugin. The default value is
     * <code>true</code>, which means the class can be processed by the plugin.
     * <p/>
     * Setting this property to <code>false</code> is used when the class is manually processed or in cases when it is there to define a list of interfaces for
     * another class to pick up, such as a {@link org.fluidity.composition.spi.CustomComponentFactory} implementation.
     * <p/>
     * If manually processed, the developer either has to provide a suitable {@link org.fluidity.composition.spi.PackageBindings} object or explicitly add the
     * class to some child container at run-time.
     *
     * @return <code>true</code> if this component should be automatically processed.
     *
     * @see org.fluidity.composition.spi.EmptyPackageBindings
     */
    boolean automatic() default true;

    /**
     * Tells whether the annotated class is a primary implementation of its component interface(s) or just a fallback or default implementation. As a fallback
     * it will be used when no other class has been marked for its API interface as a primary.
     *
     * @return <code>true</code> if this component should be mapped as a primary.
     */
    boolean primary() default true;

    /**
     * Tells whether this component should be singleton or a new instance must be created for every query or reference.
     *
     * @return <code>true</code> if a new instance should be created for every query or dependency reference.
     */
    boolean stateful() default false;

    /**
     * This annotation lists the context annotations accepted by the annotated class, thereby allowing the component to specify the annotation classes that will
     * configure instances of the component at the points of dependency reference to the component. Such a configuration could, for instance, contain a database
     * identifier for a database access dependency, etc.
     *
     * @author Tibor Varga
     */
    @Internal
    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Context {

        /**
         * Returns the context annotations that configure the class annotated with {@link Context}.
         *
         * @return a list of context annotation classes.
         */
        Class<? extends Annotation>[] value();
    }
}

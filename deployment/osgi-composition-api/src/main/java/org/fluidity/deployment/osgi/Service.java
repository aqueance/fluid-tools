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

package org.fluidity.deployment.osgi;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.fluidity.composition.Component;

/**
 * Annotates method parameters of a {@link BundleComponentContainer.Managed} component that require an OSGi service instance to resolve. Parameters not so
 * annotated are resolved from the in-bundle dependency injection container(s). See {@link BundleComponentContainer} for details on managed components.
 * <p/>
 * This annotation applies to constructor parameters but not to fields as the component whose constructor has parameters annotated with this type are not
 * expected to be instantiated until all of its constructor parameters annotated with this type have been registered in the OSGi service registry.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@Component.Context(collect = Component.Context.Collection.IMMEDIATE)
public @interface Service {

    /**
     * Returns the service interface under which the required OSGi service is expected to have been registered. When not specified, the service interface
     * defaults to the type of the annotated dependency reference.
     *
     * @return the service interface under which the required OSGi service is expected to have been registered.
     */
    Class<?> api() default Object.class;

    /**
     * Returns the filter that narrows down the list of possible service implementation candidates.
     *
     * @return the filter that narrows down the list of possible service implementation candidates.
     */
    String filter() default "";
}

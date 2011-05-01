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

package org.fluidity.deployment.osgi;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Properties;

import org.fluidity.composition.ComponentGroup;

/**
 * Annotates constructor parameters or instance fields, of a component provided by {@link ServiceTracker}, that require an OSGi service instance.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER, ElementType.FIELD })
public @interface Service {

    /**
     * Returns the service interface under which the required OSGi service is expected to have been registered.
     *
     * @return the service interface under which the required OSGi service is expected to have been registered.
     */
    Class<?> api();

    /**
     * Returns the filter that narrows down the list of possible service implementation candidates.
     *
     * @return the filter that narrows down the list of possible service implementation candidates.
     */
    String filter() default "";

    /**
     * Denotes an OSGi service that will be registered when the host bundle is started. See {@link Whiteboard}.
     *
     * @author Tibor Varga
     */
    @ComponentGroup
    interface Registration<T> {

        /**
         * Returns the service registration properties to use when registering this service.
         *
         * @return the service registration properties.
         */
        Properties properties();

        /**
         * Returns the list of classes this service is to be registered against the name of.
         *
         * @return the list of classes this service is to be registered against the name of.
         */
        Class<? super T> types();
    }
}

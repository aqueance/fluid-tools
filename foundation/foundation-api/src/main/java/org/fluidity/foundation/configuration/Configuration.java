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

package org.fluidity.foundation.configuration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.fluidity.foundation.spi.PropertyProvider;

/**
 * Represents some configuration. A configuration is a group of settings consumed by some entity. The generic type of this interface is the interface defining
 * the methods that query those settings.
 * <p/>
 * For instance:
 * <pre>
 * public interface MySettings {
 *
 *   &#64;Setting(key = "setting.property.1", undefined = "default setting 1")
 *   String property1();
 *
 *   &#64;Setting(key = "setting.property.2", undefined = "default setting 2")
 *   String property2();
 * }
 * </pre>
 * <p/>
 * A settings interface like the above must have all of its methods annotated by the @{@link Setting} annotation, all methods must have a built in and non-void
 * and non-array return type and they may not have arguments. The keys specified are relative to the concatenation of each @{@link Configuration.Context}
 * annotation in the instantiation path of the configured component.
 * <p/>
 * Using the above and a suitable, <code>@Component</code> annotated implementation of {@link PropertyProvider} in the class path,
 * a component can now declare a dependency to a configuration like so:
 * <pre>
 *  &#64;Component
 *  &#64;Context(Configuration.Context.class)
 *  &#64;Configuration.Context("property.prefix")
 *  public class Configured {
 *
 *      private final String property1;
 *      private final String property2;
 *
 *      public Configured(final &#64;Configuration.Definition(MySettings.class) Configuration&lt;MySettings> settings) {
 *          final MySettings configuration = settings.snapshot();
 *          assert configuration != null;
 *
 *          property1 = configuration.property1();
 *          property2 = configuration.property2();
 *      }
 * }
 * </pre>
 * <p/>
 * The snapshot of the configuration settings above works with an optional <code>PropertyProvider</code> and an optional implementation of the settings
 * interface itself, which in the above example was <code>MySettings</code>.
 * <p/>
 * The value returned by the configuration snapshot is computed as follows:
 * <ol>
 * <li>If a <code>PropertyProvider</code> component is found, it is queried for the property key specified in the method's {@link Setting} annotation, with all
 * contextual prefixes added. If the result is not <code>null</code>, it is returned.</li>
 * <li>If a component was found for the settings interface, the method invoked on the snapshot is forwarded to it. If the result is not <code>null</code>, it is
 * returned.</li>
 * <li>The {@link Setting#undefined()} parameter is checked. If it is not empty, it is returned, otherwise <code>null</code> is returned for non primitive
 * types and the default value is returned for primitive types.</li>
 * </ol>
 * <p/>
 * The snapshot returned by {@link #snapshot()} is a consistent snapshot of the properties computed as per above even if the underlying
 * <code>PropertyProvider</code> supports run-time configuration updates. Calling the <code>snapshot()</code> method later may thus reflect a different, but
 * static and consistent, set of properties.
 *
 * @author Tibor Varga
 */
public interface Configuration<T> {

    /**
     * Returns an object implementing the settings interface. The configuration settings returned by the methods of the returned object are consistent and will
     * not reflect later changes to the underlying configuration settings.
     *
     * @return an object implementing the settings interface.
     */
    T snapshot();

    /**
     * Context annotation for {@link org.fluidity.foundation.configuration.Configuration} components.
     *
     * @author Tibor Varga
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.PARAMETER, ElementType.FIELD })
    @interface Definition {

        /**
         * Defines the interface defining the configuration methods used by the class employing this annotation.
         *
         * @return a class object.
         */
        Class<?> value();

    }

    /**
     * Provides a context for a {@link org.fluidity.foundation.configuration.Configuration} dependency.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD })
    @interface Context {

        /**
         * The property prefix to apply to all property keys defined by the {@link org.fluidity.foundation.configuration.Setting#key()} annotations for the
         * {@link org.fluidity.foundation.configuration.Configuration} dependency.
         *
         * @return a property prefix, without the trailing dot.
         */
        String value();
    }
}

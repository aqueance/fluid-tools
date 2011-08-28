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

package org.fluidity.foundation;

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
 *   &#64;Configuration.Property(key = "property.1", undefined = "default value 1")
 *   String property1();
 *
 *   &#64;Configuration.Property(key = "property.2", undefined = "default value 2")
 *   String property2();
 * }
 * </pre>
 * <p/>
 * A settings interface like the above must have all of its methods annotated by the @{@link Configuration.Property} annotation, all methods must have a built
 * in and non-void and non-array return type and they may not have arguments. The keys specified are relative to the concatenation of each @{@link
 * Configuration.Context} annotation in the instantiation path of the configured component.
 * <p/>
 * Using the above and a suitable, <code>@Component</code> annotated implementation of {@link PropertyProvider} in the class path,
 * a component can now declare a dependency to a configuration like so:
 * <pre>
 *  &#64;Component
 *  &#64;Context(Configuration.Context.class)
 *  &#64;Configuration.Context("prefix")
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
 * <li>If a <code>PropertyProvider</code> component is found, it is queried for the property key specified in the method's {@link Configuration.Property}
 * annotation, with all contextual prefixes added. If the result is not <code>null</code>, it is returned.</li>
 * <li>If a component was found for the settings interface, the method invoked on the snapshot is forwarded to it. If the result is not <code>null</code>, it is
 * returned.</li>
 * <li>The {@link Configuration.Property#undefined()} parameter is checked. If it is not empty, it is returned, otherwise <code>null</code> is returned for non
 * primitive types and the default value is returned for primitive types.</li>
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
     * Annotates a setting query method to specify what property to query and, optionally, what default value to return if the property is not defined.
     * <p/>
     * This annotation is to be used on interface methods that will be used in conjunction with either a {@link Configuration} component.
     * <p/>
     * This annotation also specify how to handle query methods with array and parameterized {@link java.util.Set}, {@link java.util.List} or {@link
     * java.util.Map} return types. The implementation understands JSON encoded arrays and maps, without the top level grouping characters, '[]' and '{}'
     * included, and can convert such property values to arrays, lists, sets and maps, nested in any complexity, as long as the parameterized return type of
     * the
     * annotated method provides adequate information as to the expected type of any item encoded in the property value. The grouping and delimiter characters
     * in the encoded property value are configurable, they aren't required to convey whether the item is a map or array since that information is already
     * encoded in the return type of the annotated method.
     * <p/>
     * For instance, with a method return value <code>Map&lt;List&lt;String>, long[]></code>, the following property values would be valid:
     * <ul>
     * <li>""</li>
     * <li>"[a, b]: [1, 2, 3]"</li>
     * <li>"[a, b]: [1, 2, 3], c: 4, [d, e, f]: "</li>
     * </ul>
     *
     * @author Tibor Varga
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Property {

        /**
         * The property key to query.
         *
         * @return the property key to query.
         */
        String key();

        /**
         * Returns the default value for the property.
         *
         * @return the default value for the property.
         */
        String undefined() default "";

        /**
         * For array or collection valued properties, this parameter defines the characters delimiting the items. The value is ignored for non collection type
         * properties. If not specified, it defaults to ",:".
         *
         * @return the list of characters that delimit items of an array, collection or map valued property.
         */
        String list() default ",:";

        /**
         * For map or multidimensional collection valued properties, this string specifies the character pairs that enclose elements of one dimension. If not
         * specified, it defaults to "[]{}".
         * <p/>
         * For instance, <code>@Configuration.Property(key = "..." list = ',' grouping="()")</code> with property value "(1, 2, 3), (4, 5, 6), (7, 8, 9)"
         * results in a 2-dimensional array that is equivalent to the following Java array initializer:  <code>{ {1, 2, 3}, {4, 5, 6}, {7, 8, 9} }</code>.
         *
         * @return the grouping characters that surround collection elements.
         */
        String grouping() default "[]{}";
    }

    /**
     * Context annotation for {@link Configuration} components.
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
     * Provides a context for a {@link Configuration} dependency.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD })
    @interface Context {

        /**
         * The property prefix to apply to all property keys defined by the {@link Configuration.Property#key()} annotations for the {@link Configuration}
         * dependency.
         *
         * @return a property prefix, without the trailing dot.
         */
        String value();
    }
}

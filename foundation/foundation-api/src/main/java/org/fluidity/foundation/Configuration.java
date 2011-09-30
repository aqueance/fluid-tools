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
 * Represents some configuration.
 * <h2>Usage</h2>
 * A configuration is a group of settings consumed by some entity. The type parameter of this interface is the interface defining
 * the methods that query those settings.
 * <p/>
 * For instance:
 * <pre>
 * public interface MySettings {
 *
 *   &#64;Configuration.Property(key = "property.1", undefined = "default value 1")
 *   String property1();
 *
 *   &#64;Configuration.Property(key = "property.%d.value", undefined = "default value %d")
 *   String property2(int item);
 * }
 * </pre>
 * <h3>Query Methods</h3>
 * A settings interface like the above must have all of its methods annotated by the @{@link Configuration.Property} annotation, all methods must have a
 * supported return type and they may have any number of arguments. The given {@link Configuration.Property#key()}s are understood to be relative to the
 * concatenation of each @{@link Configuration.Context} annotation in the instantiation path of the configured component.
 * <p/>
 * Query methods may have parameters that provide values to placeholders in the property key, as in the method definition for {@code property2} above.
 * <h3>Property Provider</h3>
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
 * <h3>Property Values</h3>
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
 * <h2>Supported Return Types</h2>
 * <ul>
 * <li>{@code String}</li>
 * <li>Primitive Java type, such as {@code int}</li>
 * <li>Boxed primitive Java type, such as {@code Long}</li>
 * <li>{@link Enum} type</li>
 * <li>Array of any supported type</li>
 * <li>{@link java.util.List} of any supported type</li>
 * <li>{@link java.util.Set} of any supported type</li>
 * <li>{@link java.util.Map} of any supported types</li>
 * </ul>
 *
 * @author Tibor Varga
 */
public interface Configuration<T> {

    /**
     * Returns an object implementing the settings interface.
     *
     * @return an object implementing the settings interface.
     */
    T settings();

    /**
     * Provides access to an object that implements the settings interface. The configuration settings returned by the methods of the provided object are
     * consistent and will not reflect changes to the underlying configuration settings while the method executes.
     *
     * @param query the object to supply the settings implementation to.
     *
     * @return whatever the supplied {@code query} returns.
     */
    <R> R query(Query<T, R> query);

    /**
     * Groups property queries. Properties read in the {@link #read(Object)} method will be consistent in that no property change will take place
     * during the execution of that method. Subject to {@link PropertyProvider} support.
     *
     * @param <T> the settings interface type.
     * @param <R> the return type of the {@code read} method.
     */
    interface Query<T, R> {

        /**
         * Allows access to the settings. If the underlying {@link PropertyProvider} supports it, properties will not be updated dynamically while this method
         * executes.
         *
         * @param settings an object implementing the settings interface.
         *
         * @return whatever the caller wants {@link Configuration#query(Configuration.Query)} to return.
         */
        R read(T settings);
    }

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
     * in the encoded property value are configurable, they aren't required to tell whether the item is a map or array since that information is already
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

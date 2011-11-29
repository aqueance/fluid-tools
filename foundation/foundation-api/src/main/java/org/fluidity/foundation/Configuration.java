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
 * Represents some configuration of a component. The component receives its configuration, which will be an object implementing this interface, as an injected
 * dependency. A component may have any number of such configurations, each for a different context if desired.
 * <p/>
 * The configuration is normally backed by a {@link PropertyProvider property provider} object that the configuration object queries for data
 * and provides type conversion of values returned. The properties queried are determined by {@link Configuration.Context annotations} and the component
 * context of the configured component.
 * <h2>Usage</h2>
 * A configuration is a group of settings consumed by some entity. The settings are defined by the methods of a custom <em>settings interface</em>, which will
 * be used as the type parameter of the <code>Configuration</code> interface.
 * <p/>
 * For instance:
 * <pre>
 * interface MySettings {
 *
 *   &#64;Configuration.Property(key = "property.1", undefined = "default value 1")
 *   String property1();
 *
 *   &#64;Configuration.Property(key = "property.%d.value", undefined = "default value %d")
 *   String property2(int item);
 * }
 *
 * &#64;Component
 * final class ConfiguredComponent {
 *
 *     public ConfiguredComponent(final Configuration&lt;MySettings> configuration) {
 *         ...
 *         final MySettings settings = configuration.settings();
 *         ...
 *     }
 * }
 * </pre>
 * <h3>Query Methods</h3>
 * A settings interface like the above must have all of its methods annotated with {@link Configuration.Property @Configuration.Property}, must have a
 * <a href="#supported_types">supported return type</a>, and the methods may have any number of arguments.
 * <p/>
 * The given {@link Configuration.Property#key() property key}s are understood to be relative to the concatenation of the value of each {@link
 * Configuration.Context @Configuration.Context} annotation in the instantiation path of the configured component.
 * <p/>
 * If the computed property has no value in the underlying property provider, the last context is stripped and the new property is queried, and this process
 * is repeated until the property provider returns a value or there is no more context to strip.
 * <p/>
 * For instance if the the instantiation path contains <code>@Configuration.Context("a")</code>, <code>@Configuration.Context("b")</code> and
 * <code>@Configuration.Context("c")</code>, then the method <code>@Configuration.Property(key = "property") String property()</code> will query the
 * <code>"a.b.c.property"</code> from its underlying property provider, and then <code>"a.b.property"</code>, <code>"a.property"</code> and
 * <code>"property"</code> until the property has a value or there is no more context to strip.
 * <p/>
 * Query methods may have parameters that provide values to placeholders in the property key, as shown in the method definition for <code>property2</code>
 * above.
 * <p/>
 * <h3>Property Values</h3>
 * The snapshot of the configuration settings above works with an optional {@link PropertyProvider} and an optional implementation of the settings interface
 * itself, which in the above example was <code>MySettings</code>.
 * <p/>
 * The value returned by the configuration snapshot is computed as follows:
 * <ol>
 * <li>If a <code>PropertyProvider</code> component is found, it is queried for the property key specified in the method's {@link Configuration.Property}
 * annotation, with contextual prefixes added. If the result is not <code>null</code>, it is returned.</li>
 * <li>If a component bound to the settings interface was provided, the method invoked on the snapshot is forwarded to the settings implementation. If the
 * result is not <code>null</code>, it is returned.</li>
 * <li>The {@link Configuration.Property#undefined()} parameter is checked. If it is not empty, it is returned, otherwise <code>null</code> is returned for non
 * primitive types and the default value is returned for primitive types.</li>
 * </ol>
 * <a name="supported_types"><h2>Supported Return Types</h2></a>
 * <ul>
 * <li><code>String</code></li>
 * <li>Primitive Java type, such as <code>int</code></li>
 * <li>Boxed primitive Java type, such as <code>Long</code></li>
 * <li>{@link Enum} type</li>
 * <li>Array of any supported type</li>
 * <li>{@link java.util.List} of any supported type</li>
 * <li>{@link java.util.Set} of any supported type</li>
 * <li>{@link java.util.Map} of any supported types</li>
 * <li>Any accessible interface with methods annotated with @<code>Configuration.Property</code></li>
 * <li>Any accessible class with fields annotated with @<code>Configuration.Property</code></li>
 * <li>Any combination of the above</li>
 * </ul>
 * <h3>Custom Types</h3>
 * The configuration implementation supports both interfaces with query methods and classes with fields as return values from the settings interface. There are
 * a few key difference between the two and you need to choose the one that fits your use of the property data.
 * <p/>
 * The implementation for a custom interface:
 * <ul>
 * <li>allows method parameters to vary the property queried for a given method,</li>
 * <li>provides only object identity implementation for the {@link Object#equals(Object)} method,</li>
 * <li>may return a different value at different times if accessed outside the {@link Configuration.Query#read(Object) Configuration.Query.read(T)}
 * method.</li>
 * </ul>
 * In contrast, your custom class:
 * <ul>
 * <li>provides no means to vary at run-time the property queried for a given field,</li>
 * <li>may provide whatever equality in its {@link Object#equals(Object)} method,</li>
 * <li>will have the same property value in its fields regardless of when they are accessed (until you change those values, of course).</li>
 * </ul>
 *
 * @param <T> the settings interface.
 *
 * @author Tibor Varga
 */
public interface Configuration<T> {

    /**
     * Returns an object implementing the settings interface. Methods of the returned settings implementation go straight to the underlying {@link
     * PropertyProvider}, if any, to resolve property values and thus may return different values at different times.
     *
     * @return an object implementing the settings interface.
     */
    T settings();

    /**
     * Provides isolated access to an object that implements the settings interface. The configuration settings returned by the methods of the provided object
     * are consistent and will not reflect changes to the underlying configuration settings while the method executes.
     *
     * @param query the object to supply the settings implementation to.
     *
     * @return whatever the supplied <code>query</code> returns.
     */
    <R> R query(Query<T, R> query);

    /**
     * Groups property queries to provide settings consistency. Properties read in the {@link #read(Object) read(T)} method will be consistent in that no
     * property change will take place during the execution of that method. Stashing the <code>read</code> method parameter and invoking its methods
     * outside the <code>read</code> method will not have the same effect.
     * <p/>
     * This feature is subject to {@link PropertyProvider#properties(Runnable) property provider} support.
     *
     * @param <T> the settings interface type.
     * @param <R> the return type of the <code>read</code> method.
     */
    interface Query<T, R> {

        /**
         * Allows access to the settings. If the underlying {@link PropertyProvider} supports it, properties will not be updated dynamically while this method
         * executes. However, stashing the <code>read</code> method parameter and invoking its methods outside the <code>read</code> method will not have the
         * same guarantee.
         *
         * @param settings an object implementing the settings interface.
         *
         * @return whatever the caller wants {@link Configuration#query(Configuration.Query)} to return.
         */
        R read(T settings);
    }

    /**
     * Configuration that is periodically updated. This is a wrapper around {@link Configuration} that periodically refreshes the snapshot of the settings
     * object. If used with this variant of the configuration mechanism, the methods of the settings interface may not have any parameters.
     *
     * @param <T> the settings interface.
     */
    interface Updated<T> {

        /**
         * Returns an object containing the most recent snapshot of the settings.
         *
         * @param period the period during which at most one refresh will take place.
         *
         * @return an object containing the most recent snapshot of the settings.
         */
        Updates.Snapshot<T> snapshot(long period);
    }

    /**
     * Annotates a configuration query method to specify what property to query, what default value to return if the property is not defined, and how to
     * process list and map typed configuration values.
     * <p/>
     * This annotation is used on interface methods that parameterize a {@link Configuration} dependency is some component.
     * <p/>
     * To handle query methods with array and parameterized {@link java.util.Set}, {@link java.util.List} or {@link java.util.Map} return types, the
     * implementation understands JSON encoded arrays and maps, without the top level grouping characters included ('[]' and '{}'), and can convert such
     * property values to arrays, lists, sets and maps, nested in any complexity, as long as the parameterized return type of the annotated method provides
     * adequate information as to the expected type of any item encoded in the property value. Although the grouping and delimiter characters in the encoded
     * property value are configurable, they aren't required to tell whether the item is a map or array since that information is already encoded in the return
     * type of the annotated method.
     * <p/>
     * For instance, with a method returning <code>Map&lt;List&lt;String>, long[]></code>, the following property values would be valid:
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
    @Target({ ElementType.METHOD, ElementType.FIELD })
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
         * properties.
         *
         * @return the list of characters that delimit items of an array, collection or map valued property.
         */
        String split() default ",:";

        /**
         * For map or multidimensional collection valued properties, this string specifies the character pairs that enclose elements of one dimension.
         * <p/>
         * For instance, <code>@Configuration.Property(key = "..." split = ',' grouping="()")</code> with property value "(1, 2, 3), (4, 5, 6), (7, 8, 9)"
         * results in a 2-dimensional array that is equivalent to the following Java array initializer:  <code>{ {1, 2, 3}, {4, 5, 6}, {7, 8, 9} }</code>.
         *
         * @return the grouping characters that surround collection elements.
         */
        String grouping() default "[]{}";

        /**
         * Returns the property key that defines a list of other items. This is attribute is useful in cases where one property provides a list from which
         * a list of other properties are derived, and the values of those properties should be returned as an array, collection, or map. For instance,
         * consider the following properties:
         * <pre>
         * item.list=1,2,3
         * item.1=item1
         * item.2=item1
         * item.3=item1
         * </pre>
         * In the above case, you would need the following method to read the list of items as an array of <code>String</code> objects:
         * <pre>
         * interface Settings {
         *     ...
         *     &#64;Configuration.Property(key = "item", ids="list")
         *     String[] items();
         *     ...
         * }
         * </pre>
         *
         * @return the property key that defines a list of other items.
         */
        String ids() default "";

        /**
         * Works in conjunction with {@link #ids()} to override the prefix for the individual items. For instance, let's consider the following properties:
         * <pre>
         * item.list=1,2,3
         * item.1.value=item1
         * item.2.value=item1
         * item.3.value=item1
         * </pre>
         * In the above case, you would need the following method to read the list of items as an array of <code>String</code> objects:
         * <pre>
         * interface Settings {
         *     ...
         *     &#64;Configuration.Property(key = "item", ids="list", list="item.%%s.value")
         *     String[] items();
         *     ...
         * }
         * </pre>
         * </p>
         * <b>Note</b>: the placeholder used in this parameter is <code>"%%s"</code> to allow for interpolating the method parameters. For instance:
         * <pre>
         * interface Settings {
         *     ...
         *     &#64;Configuration.Property(key = "item.%s", ids="list", list="item.%s.%%s.value")
         *     String[] items(String type);
         *     ...
         * }
         * </pre>
         * The above would allow one to query either the <code>item.long</code> or <code>item.short</code> list by calling <code>items("long")</code> or
         * <code>items("short")</code>, respectively.
         * <pre>
         * item.long.list=1,2,3
         * item.long.1.value=item1
         * item.long.2.value=item1
         * item.long.3.value=item1
         *
         * item.short.list=1,2
         * item.short.1.value=item1
         * item.short.2.value=item1
         * </pre>
         * In the above case, you would need the following method to read the list of items as an array of <code>String</code> objects:
         *
         * @return the property key pattern for individual items in a list; defaults to {@link #key()}.<code>concat(".%%s")</code>.
         */
        String list() default "";
    }

    /**
     * Provides a context for a {@link Configuration} dependency.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD })
    @interface Context {

        /**
         * The property prefix to apply to all property keys specified by {@link Configuration.Property#key()} annotated methods in the scope of this context.
         *
         * @return a property prefix, without the trailing dot.
         */
        String value();
    }

    /**
     * Adds a more descriptive error message to exceptions that occur while loading properties and converting them to settings.
     */
    class PropertyException extends RuntimeException {

        /**
         * Creates a new instance.
         *
         * @param cause  the original exception to decorate.
         * @param format the message format.
         * @param args   the message parameters.
         */
        public PropertyException(final Exception cause, final String format, final Object... args) {
            super(String.format(format, args), cause);
        }
    }
}

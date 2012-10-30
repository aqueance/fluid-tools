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

package org.fluidity.foundation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.fluidity.foundation.spi.PropertyProvider;

/**
 * Dependency injected component configuration. A component may have any number of such configurations, each for a different context and / or for a different
 * settings interface.
 * <p/>
 * The configuration is backed by a {@linkplain PropertyProvider property provider} component that, if implemented, will be queried for data. The properties
 * queried are determined by {@linkplain Configuration.Prefix context annotations} of the configured component up to the point of its dependency reference.
 * <h3>Usage</h3>
 * A configuration is a group of settings consumed by the configured component. The settings are defined by the methods of a custom <em>settings
 * interface</em>, which is specified as the type parameter of the <code>Configuration</code> interface.
 * <p/>
 * For instance:
 * <pre>
 * // The settings interface
 * interface <span class="hl2">MySettings</span> {
 *
 *   <span class="hl1">{@linkplain Property @Configuration.Property}</span>(<span class="hl1">key</span> = "<span class="hl3">property.%d</span>", <span class="hl1">undefined</span> = "default value <span class="hl3">%d</span>")
 *   String <span class="hl2">property</span>(<span class="hl3">int</span> item);
 *
 *   &hellip;
 * }
 * </pre>
 * <pre>
 * // The configured component
 * {@linkplain org.fluidity.composition.Component @Component}
 * <span class="hl1">{@linkplain Configuration.Prefix @Configuration.Prefix}</span>(<span class="hl3">"some.property"</span>)
 * public final class MyComponent {
 *
 *   MyComponent(final <span class="hl1">{@linkplain Configuration.Prefix @Configuration.Prefix}</span>(<span class="hl3">"prefix"</span>) <span class="hl1">Configuration</span><span class="hl2">&lt;MySettings></span> configuration) {
 *     final <span class="hl2">MySettings</span> settings = configuration<span class="hl1">.settings()</span>;
 *
 *     // query <span class="hl3">"some.property</span>.<span class="hl3">prefix</span>.<span class="hl3">property.123"</span> from the optional {@linkplain PropertyProvider} component
 *     final String property123 = settings.<span class="hl2">property</span>(<span class="hl3">123</span>);
 *
 *     &hellip;
 *   }
 *
 *   &hellip;
 * }
 * </pre>
 * <h4>Query Methods</h4>
 * A settings interface like the above must have all of its methods annotated with {@link Configuration.Property @Configuration.Property}, must have a
 * <a href="#supported_types">supported return type</a>, and the methods may have any number of arguments.
 * <p/>
 * The given {@link Configuration.Property#key() property keys} are understood to be relative to the dot delimited concatenation of the value of each {@link
 * Configuration.Prefix @Configuration.Prefix} annotation in the dependency path of the configured component.
 * <p/>
 * If the computed property has no value in the underlying property provider, the first context is stripped and the new property is queried, and this process
 * is repeated until the property provider returns a value or there is no more context to strip, then the last context is stripped from the original property
 * and the new property is queried, and the process is repeated again.
 * <p/>
 * For instance, if the the dependency path contains <code>@Configuration.Prefix("a")</code>, <code>@Configuration.Prefix("b")</code>, and
 * <code>@Configuration.Prefix("c")</code>, then the method <code>@Configuration.Property(key = "property") String property()</code> will query the following
 * properties, in the given order, from its underlying property provider until it returns a value or there is no more context to strip:
 * <ul>
 * <li><code>a.b.c.property</code></li>
 * <li><code>b.c.property</code></li>
 * <li><code>c.property</code></li>
 * <li><code>a.b.property</code></li>
 * <li><code>a.property</code></li>
 * <li><code>property</code></li>
 * </ul>
 * <p/>
 * Query methods may have parameters that provide values to placeholders in the property key, as shown in the method definition for <code>property</code>
 * above.
 * <p/>
 * <h4>Property Values</h4>
 * The snapshot of the configuration settings above works with an optional {@link PropertyProvider} and an optional implementation of the settings interface
 * itself, which in the above example was <code>MySettings</code>.
 * <p/>
 * The value returned by the configuration snapshot is computed as follows, in the given order:
 * <ol>
 * <li>If a <code>PropertyProvider</code> component is found, it is queried for the property key specified in the method's {@link
 * Configuration.Property @Configuration.Property} annotation, with contextual prefixes added and stripped as described above. If the result is not
 * <code>null</code>, it is returned.</li>
 * <li>If a component bound to the settings interface has been implemented, the method invoked on the snapshot is forwarded to the settings implementation. If
 * a result is not <code>null</code>, it is returned.</li>
 * <li>The {@link Configuration.Property#undefined() undefined} parameter is checked. If it is not empty, it is returned, otherwise <code>null</code> is
 * returned for non primitive types and the default value is returned for primitive types.</li>
 * </ol>
 * <a name="supported_types"><h3>Supported Return Types</h3></a>
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
 * <h4>Custom Types</h4>
 * The configuration implementation supports both interfaces with query methods and classes with fields as return values from the settings interface. There are
 * a few key differences between the two and you need to choose the one that fits your use of the property data.
 * <p/>
 * A custom interface:
 * <ul>
 * <li>allows method parameters to vary the property queried for a given method,</li>
 * <li>is given the default {@linkplain Object#equals(Object) object identity},</li>
 * <li>may return a different value at different times if accessed outside the {@link Configuration.Query#run Configuration.Query.read()}
 * method.</li>
 * </ul>
 * In contrast, a custom class:
 * <ul>
 * <li>provides no means to vary at run time the property queried for a given field,</li>
 * <li>honours the equality defined by its {@link Object#equals(Object) equals()} method,</li>
 * <li>will have the same property value in its fields regardless of when they are accessed (until you change those values, of course).</li>
 * </ul>
 *
 * @param <T> the settings interface.
 *
 * @author Tibor Varga
 */
@SuppressWarnings("JavadocReference")
public interface Configuration<T> {

    /**
     * Returns an object implementing the settings interface. Methods of the returned settings implementation go straight to the underlying {@link
     * PropertyProvider}, if any, to resolve property values and thus may return different values at different times.
     *
     * @return an object implementing the settings interface.
     */
    T settings();

    /**
     * Provides consistent access to the configuration settings. The methods of the settings object sent to the provided <code>query</code> will return the same
     * value from multiple invocations during the execution of this method as long as the {@link PropertyProvider#properties(PropertyProvider.Query)
     * PropertyProvider.properties()} method of the underlying property provider, if found, properly implements that consistency.
     *
     * @param query the object to supply the settings implementation to.
     *
     * @return whatever the supplied <code>query</code> returns.
     */
    <R> R query(Query<R, T> query);

    /**
     * Groups {@link Configuration configuration} queries to provide settings consistency. Properties read in the {@link #run read()} method will be
     * consistent in that no property change will take place during the execution of that method. Stashing the <code>read</code> method parameter and invoking
     * its methods outside the <code>read</code> method will not have the same effect.
     * <p/>
     * This feature is subject to {@linkplain PropertyProvider#properties(PropertyProvider.Query) property provider} support.
     * <h3>Usage</h3>
     * <pre>
     * {@linkplain org.fluidity.composition.Component @Component}
     * public final class MyComponent {
     *
     *   MyComponent(final {@linkplain Configuration}&lt;<span class="hl2">MySettings</span>> configuration) {
     *     final int data = configuration.<span class="hl1">query</span>(new <span class="hl1">Configuration.Query</span>&lt;<span class="hl2">MySettings</span>, Integer>() {
     *       public final Integer <span class="hl1">read</span>(final <span class="hl2">MySettings</span> settings) {
     *         return settings.<span class="hl2">property1</span>() + settings.<span class="hl2">property2</span>();
     *       }
     *     });
     *
     *     &hellip;
     *   }
     *
     *   interface <span class="hl2">MySettings</span> {
     *
     *       <span class="hl1">{@linkplain Property @Configuration.Property}</span>(<span class="hl1">key</span> = "property1", <span class="hl1">undefined</span> = "default value 1")
     *       int <span class="hl2">property1</span>();
     *
     *       <span class="hl1">{@linkplain Property @Configuration.Property}</span>(<span class="hl1">key</span> = "property2", <span class="hl1">undefined</span> = "default value 2")
     *       int <span class="hl2">property2</span>();
     *   }
     * }
     * </pre>
     *
     * @param <P> the settings interface type.
     * @param <R> the return type of the <code>read</code> method.
     */
    interface Query<R, P> {

        /**
         * Atomic access to the settings. If the underlying {@link PropertyProvider} {@linkplain PropertyProvider#properties(PropertyProvider.Query) supports}
         * it, properties will not be updated dynamically while this method executes. However, stashing the method parameter and invoking its methods outside
         * this method will circumvent consistency afforded to property access within the method.
         *
         * @param settings an object implementing the settings interface.
         *
         * @return whatever the caller wants {@link Configuration#query(Configuration.Query) Configuration.query()} returned.
         */
        R run(P settings) throws Exception;
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
         * For instance, <code>@Configuration.Property(key = "&hellip;" split = ',' grouping="()")</code> with property value "(1, 2, 3), (4, 5, 6), (7, 8, 9)"
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
         *     &hellip;
         *     &#64;Configuration.Property(key = "item", ids="list")
         *     String[] items();
         *     &hellip;
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
         *     &hellip;
         *     &#64;Configuration.Property(key = "item", ids="list", list="item.%%s.value")
         *     String[] items();
         *     &hellip;
         * }
         * </pre>
         * </p>
         * <b>Note</b>: the placeholder used in this parameter is <code>"%%s"</code> to allow for interpolating the method parameters. For instance:
         * <pre>
         * interface Settings {
         *     &hellip;
         *     &#64;Configuration.Property(key = "item.%s", ids="list", list="item.%s.%%s.value")
         *     String[] items(String type);
         *     &hellip;
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
     * Provides a component context for a {@link Configuration} dependency.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD })
    @interface Prefix {

        /**
         * The property prefix to apply to all property keys specified by {@link Configuration.Property @Configuration.Property} annotated methods in the scope
         * of this context.
         *
         * @return a property prefix, without the trailing dot.
         */
        String value();
    }

    /**
     * Adds a more descriptive error message to exceptions that occur while loading properties and converting them to settings.
     */
    final class PropertyException extends RuntimeException {

        @SuppressWarnings("UnusedDeclaration")
        PropertyException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}

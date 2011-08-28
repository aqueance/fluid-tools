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
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Annotates a setting query method to specify what property to query and, optionally, what default value to return if the property is not defined.
 * <p/>
 * This annotation is to be used on interface methods that will be used in conjunction with either a {@link Configuration}
 * component.
 * <p/>
 * This annotation also specify how to handle query methods with array and parameterized {@link Set}, {@link List} or {@link Map} return types. The
 * implementation understands JSON encoded arrays and maps, without the top level grouping characters, '[]' and '{}' included, and can convert such property
 * values to arrays, lists, sets and maps, nested in any complexity, as long as the parameterized return type of the annotated method provides adequate
 * information as to the expected type of any item encoded in the property value. The grouping and delimiter characters in the encoded property value are
 * configurable, they aren't required to convey whether the item is a map or array since that information is already encoded in the return type of the
 * annotated method.
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
public @interface Setting {

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
     * For map or multidimensional collection valued properties, this string specifies the character pairs that enclose elements of one dimension. If not specified, it defaults to "[]{}".
     * <p/>
     * For instance, <code>@Setting(key = "..." list = ',' grouping="()")</code> with property value "(1, 2, 3), (4, 5, 6), (7, 8, 9)" results in a
     * 2-dimensional array that is equivalent to the following Java array initializer:  <code>{ {1, 2, 3}, {4, 5, 6}, {7, 8, 9} }</code>.
     *
     * @return the grouping characters that surround collection elements.
     */
    String grouping() default "[]{}";
}

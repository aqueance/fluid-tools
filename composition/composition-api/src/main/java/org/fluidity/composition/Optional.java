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

package org.fluidity.composition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a {@linkplain Component component} reference as optional dependency. An optional dependency will receive a <code>null</code> value upon dependency
 * resolution if the dependency cannot be satisfied. Without this annotation, a {@link ComponentContainer.ResolutionException} is
 * thrown in the same case.
 * <h3>Usage</h3>
 * <pre>
 * {@linkplain Component @Component}
 * public final class MyComponent {
 *
 *   MyComponent(final <span class="hl1">&#64;Optional</span> SomeDependency optional) {
 *     &hellip;
 *   }
 *
 *   &hellip;
 * }
 * </pre>
 *
 * @author Tibor Varga
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER, ElementType.FIELD })
@Component.Context(collect = Component.Context.Collection.NONE)
public @interface Optional { }

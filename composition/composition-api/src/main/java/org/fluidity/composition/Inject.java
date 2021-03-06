/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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
 * This annotation is used to mark instance fields and/or constructors of a {@linkplain Component component} for
 * <a href="https://github.com/aqueance/fluid-tools/wiki/User-Guide---Overview#dependency-injection">dependency injection</a>. In case a constructor is marked,
 * that particular constructor will be used by the dependency injection container to instantiate the class. If there is only one (public) constructor then that
 * needs <em>not</em> be marked with this annotation.
 * <p>
 * The dependency injection container handles the annotated fields as well. If the component was instantiated by the container, no further action is necessary
 * on the part of the developer. To inject the fields of a manually instantiated component, call {@link ComponentContainer#initialize(Object)
 * ComponentContainer.initialize()} on a suitable container.
 * <h3>Usage</h3>
 * <pre>
 * {@linkplain Component @Component}
 * public final class <span class="hl2">MyComponent</span> {
 *
 *   <span class="hl1">&#64;Inject</span>
 *   <span class="hl2">MyComponent</span>(final SomeDependency dependency) {
 *     &hellip;
 *   }
 *
 *   // used for testing
 *   <span class="hl2">MyComponent</span>() {
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
@Target({ ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.PARAMETER })
@Qualifier(Qualifier.Composition.NONE)
public @interface Inject { }

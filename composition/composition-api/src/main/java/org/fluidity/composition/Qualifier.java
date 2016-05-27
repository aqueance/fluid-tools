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
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applied to an annotation type, the {@link #value()} parameter determines how multiple instances of the qualifier are handled.
 * <h3>Usage</h3>
 * <pre>
 * {@linkplain Qualifier @Qualifier(Qualifier.Composition.IMMEDIATE)}
 * public &#64;interface <span class="hl1">MyQualifier</span> {}
 *
 *   &hellip;
 * }
 * </pre>
 * <pre>
 * {@linkplain Component @Component}
 * <span class="hl2">&#64;MyQualifier</span>("1")
 * public final class MyComponent {
 *
 *   MyComponent(final <span class="hl2">&#64;MyQualifier</span>("2") <span class="hl3">MyDependency</span> dependency) {
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
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.ANNOTATION_TYPE, ElementType.TYPE })
@Qualifier(Qualifier.Composition.NONE)
public @interface Qualifier {

    /**
     * When applied to a qualifier annotation type, this parameter determines how multiple instances of the qualifier are handled.
     *
     * @return the qualifier annotation's composition specifier.
     */
    Composition value() default Composition.ALL;

    /**
     * Defines how a chain of occurrences of the same qualifiers along an instantiation path is handled. Used in {@link Qualifier#value @Qualifier(&hellip;)}.
     * <h3>Usage</h3>
     * <pre>
     * {@linkplain Documented @Documented}
     * {@linkplain Retention @Retention}(RetentionPolicy.RUNTIME)
     * {@linkplain Target @Target}({ ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD })
     * {@linkplain Qualifier @Qualifier}(<span class="hl1">Qualifier.Composition.DIRECT</span>)
     * public &#64;interface <span class="hl2">SomeQualifier</span> { }
     * </pre>
     */
    enum Composition {

        /**
         * All unique instances of the qualifier along the instantiation path are passed to the component that accepts that qualifier. Identical instances
         * of a qualifier are collapsed into the first occurrence.
         */
        ALL,

        /**
         * Only the last instance of the qualifier is passed to the component that accepts that qualifier.
         */
        LAST,

        /**
         * The only instance of the qualifier passed to the component that accepts that qualifier is the one annotating the class, the injected constructor
         * or method, the injected dependency reference to that component and is closest among these to the dependency reference. Further restrictions can
         * be made using the {@link Target @Target} annotation.
         */
        IMMEDIATE,

        /**
         * No instance of this qualifier annotation will ever form the context of a components.
         */
        NONE
    }
}

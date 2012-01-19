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

package org.fluidity.composition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation marks interfaces or classes that are expected to have several implementations, which are used together as a group by some component. All
 * classes implementing or extending such an interface or class will be provided as a whole, in the form of an array of the marked type, to components that
 * depend on such an array or that use {@link ComponentContainer#getComponentGroup(Class)} to get one.
 * <p/>
 * When the group interface or class is not available to annotate, actual implementations may also be annotated with this class but in that case only those
 * implementations that are actually annotated will be found and provided as a component group.
 * <p/>
 * Components, i.e., classes annotated with the {@link Component @Component} annotation, may, in addition to being a component, implement or extend an
 * interface or class marked with this annotation. In such a case, however, this annotation will only be recognized if inherited via a component interface,
 * i.e., one by which the component can be depended upon. The algorithm for computing the set of component interfaces is described at {@link
 * ComponentContainer.Registry#bindComponent(Class, Class[])}.
 * <h3>Usage</h3>
 * <pre>
 * <span class="hl1">&#64;ComponentGroup</span>
 * public interface <span class="hl2">ImageFilter</span> { ... }
 *
 * &#64;Component
 * final class ImageProcessor {
 *
 *   public ImageProcessor(final <span class="hl1">&#64;ComponentGroup</span> <span class="hl2">ImageFilter</span>[] filters) {
 *     assert filters != null : ImageFilter.class;
 *     ...
 *   }
 * }
 *
 * final class ImageFilter1 implements <span class="hl2">ImageFilter</span> { ... }
 *
 * final class ImageFilter2 implements <span class="hl2">ImageFilter</span> { ... }
 *
 * final class ImageFilter3 implements <span class="hl2">ImageFilter</span> { ... }
 * </pre>
 *
 * @author Tibor Varga
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER })
@Inherited
@Component.Context(series = Component.Context.Series.NONE)
public @interface ComponentGroup {

    /**
     * Specifies the (interface) class of the component group. The property defaults to the list of interfaces the class directly implements or the class
     * itself if it implements no interface.
     *
     * @return an array of class objects; ignored for annotated fields.
     */
    Class<?>[] api() default { };
}

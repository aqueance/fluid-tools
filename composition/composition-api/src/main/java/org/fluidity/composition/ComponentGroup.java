/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.fluidity.composition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation marks interfaces or classes that are expected to have several implementations, which are all used by some component. Any class implementing
 * or extending such an interface or class will be provided to other components as a whole, in the form of an array of the marked type, to components that
 * depend on such an array or that use {@link ComponentContainer#getComponentGroup(Class)}.
 *
 * When the group interface or class is not available to annotate, actual implementations may also be annotated with this class but in that case only those
 * implementations that are actually annotated will be found and proved to components that depend on an array of the group interface.
 *
 * Example of use:
 * <pre>
 * &#64;ComponentGroup
 * public interface ImageFilter { ... }
 *
 * &#64;Component
 * final class ImageProcessor {
 *
 *   public ImageProcessor(final &#64;ComponentGroup ImageFilter[] filters) {
 *      // do something with the list of ImageFilter implementations...
 *   }
 * }
 *
 * final class ImageFilter1 implements ImageFilter { ... }
 *
 * final class ImageFilter2 implements ImageFilter { ... }
 *
 * final class ImageFilter3 implements ImageFilter { ... }
 * </pre>
 *
 * @author Tibor Varga
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER })
@Inherited
public @interface ComponentGroup {

    /**
     * Returns the (interface) class of the component group(s). The property defaults to the list of interfaces the class directly implements or the class
     * itself if it implements no interface.
     *
     * @return an array of class objects; ignored for annotated fields.
     */
    Class<?>[] api() default { };
}

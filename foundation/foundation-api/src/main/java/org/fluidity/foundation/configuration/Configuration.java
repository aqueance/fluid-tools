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

package org.fluidity.foundation.configuration;

/**
 * Represents some configuration. A configuration is a group of settings consumed by some entity. The generic type of this interface is the interface defining
 * the methods that query those settings.
 * <p/>
 * For instance:
 * <pre>
 * public interface MySettings {
 *
 *   &#64;Setting(key = "setting.property.1", fallback = "default setting 1")
 *   String property1();
 *
 *   &#64;Setting(key = "setting.property.2", fallback = "default setting 2")
 *   String property2();
 * }
 * </pre>
 * <p/>
 * A settings interface like the above must have all of its methods annotated by the {@link @Setting} annotation.
 * <p/>
 * Using the above and a suitable implementation of {@link org.fluidity.foundation.spi.PropertyProvider}, <code>MyPropertyProvider</code>, a component can now
 * declare a dependency to a configuration, either static or dynamic, like so:
 * <pre>
 *  &#64;Component
 *  public class Configured {
 *
 *      private final String property1;
 *      private final String property2;
 *
 *      public Configured(final &#64;Properties(api = MySettings.class, provider = MyPropertyProvider.class) Configuration&lt;MySettings> settings) {
 *          final MySettings configuration = settings.configuration();
 *          assert configuration != null;
 *
 *          property1 = configuration.property1();
 *          property2 = configuration.property2();
 *      }
 * }
 * </pre>
 * <p/>
 * The value offered by the above is that you do not need to implement the <code>MySettings</code> interface, it will be done for you automatically. You only
 * need to provide an implementation for the {@link org.fluidity.foundation.spi.PropertyProvider} for each of the various ways you have your application
 * configured - that is have property names mapped to configuration setting values -, once.
 *
 * @author Tibor Varga
 */
public interface Configuration<T> {

    /**
     * Returns an object implementing the settings interface.
     *
     * @return an object implementing the settings interface.
     */
    T configuration();
}

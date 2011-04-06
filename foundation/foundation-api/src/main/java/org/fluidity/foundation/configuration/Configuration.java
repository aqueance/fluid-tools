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

/**
 * Represents some configuration. A configuration is a group of settings consumed by some entity. The generic type of this interface is the interface defining
 * the methods that query those settings.
 * <p/>
 * For instance:
 * <pre>
 * public interface MySettings {
 *
 *   &#64;Setting(key = "setting.property.1", undefined = "default setting 1")
 *   String property1();
 *
 *   &#64;Setting(key = "setting.property.2", undefined = "default setting 2")
 *   String property2();
 * }
 * </pre>
 * <p/>
 * A settings interface like the above must have all of its methods annotated by the {@link @Setting} annotation.
 * <p/>
 * Using the above and a suitable implementation of {@link org.fluidity.foundation.spi.PropertyProvider}, <code>@Component(api = MyPropertyProvider.class)
 * MyPropertyProvider</code>, a component can now
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

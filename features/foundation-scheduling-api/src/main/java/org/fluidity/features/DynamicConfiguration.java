/*
 * Copyright (c) 2006-2018 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.features;

import java.util.function.Supplier;

/**
 * Configuration that is periodically updated. This is a wrapper around {@link org.fluidity.foundation.Configuration} that periodically refreshes the snapshot
 * of the settings object. The methods of the settings interface, if used with this variant of the configuration mechanism, may not have any parameters.
 * <p>
 * The period of the updates can be configured by implementing a {@link org.fluidity.foundation.spi.PropertyProvider} component that returns a valid
 * number for the {@link #CONFIGURATION_REFRESH_PERIOD} key. The default period is 30 seconds.
 * <p>
 * See {@link org.fluidity.foundation.Configuration} for details on configuration.
 * <h3>Usage</h3>
 * <pre>
 * {@linkplain org.fluidity.composition.Component @Component}
 * public final class <span class="hl3">MyComponent</span> {
 *
 *   private final <span class="hl1">{@linkplain Supplier}</span>&lt;<span class="hl2">Settings</span>&gt; configuration;
 *
 *   <span class="hl3">MyComponent</span>(final <span class="hl1">ReloadingConfiguration</span>&lt;<span class="hl2">Settings</span>&gt; configuration) {
 *     this.configuration = configuration.<span class="hl1">snapshot</span>();
 *   }
 *
 *   public void someMethod() {
 *     final <span class="hl2">Settings</span> settings = configuration.<span class="hl1">get</span>();
 *     &hellip;
 *   }
 *
 *   interface <span class="hl2">Settings</span> {
 *     &hellip;
 *   }
 * }
 * </pre>
 *
 * @param <T> the settings interface type to implement.
 *
 * @author Tibor Varga
 */
@SuppressWarnings("JavadocReference")
public interface DynamicConfiguration<T> {

    String CONFIGURATION_REFRESH_PERIOD = "org.fluidity.features.configuration-refresh-period-ms";

    /**
     * Returns an an automatically refreshing snapshot of the settings.
     *
     * @return an an automatically refreshing snapshot of the settings.
     */
    Supplier<T> snapshot();
}

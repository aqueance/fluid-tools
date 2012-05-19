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

package org.fluidity.features;

/**
 * Configuration that is periodically updated. This is a wrapper around {@link org.fluidity.foundation.Configuration} that periodically refreshes the snapshot
 * of the settings object. The methods of the settings interface, if used with this variant of the configuration mechanism, may not have any parameters.
 * <p/>
 * The granularity of the updates can be configured by implementing a {@link org.fluidity.foundation.spi.PropertyProvider} component that returns a valid
 * number for the {@link #CONFIGURATION_REFRESH_PERIOD} key. The default period granularity is 30 seconds.
 *
 * @param <T> the settings interface.
 *
 * @author Tibor Varga
 */
public interface ReloadingConfiguration<T> {

    String CONFIGURATION_REFRESH_PERIOD = "org.fluidity.features.configuration-refresh-period-ms";

    /**
     * Returns an an automatically refreshing snapshot of the settings.
     *
     * @return an an automatically refreshing snapshot of the settings.
     */
    Updates.Snapshot<T> snapshot();
}

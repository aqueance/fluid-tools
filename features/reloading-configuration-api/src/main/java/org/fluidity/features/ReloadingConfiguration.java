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

package org.fluidity.features;

import org.fluidity.foundation.Configuration;

/**
 * Configuration that is periodically updated. This is a wrapper around {@link org.fluidity.foundation.Configuration} that periodically refreshes the snapshot
 * of the settings object. If used with this variant of the configuration mechanism, the methods of the settings interface may not have any parameters.
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


    /**
     * Configuration refresh period settings.
     */
    interface Settings {

        /**
         * Returns the period in milliseconds during which at most one log level check takes place per logger. May
         * be 0 or negative, in which case no periodic log level check will take place.
         *
         * @return the period in milliseconds.
         */
        @Configuration.Property(key = ReloadingConfiguration.CONFIGURATION_REFRESH_PERIOD, undefined = "30000")
        long period();
    }
}

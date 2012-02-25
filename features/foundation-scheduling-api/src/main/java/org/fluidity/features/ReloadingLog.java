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

import org.fluidity.foundation.Log;

/**
 * A {@link org.fluidity.foundation.Log} implementation wrapper that periodically refreshes log level permissions.
 * <p/>
 * The granularity of the updates can be configured by implementing a {@link org.fluidity.foundation.spi.PropertyProvider} component that returns a valid
 * number for the {@link #LOG_LEVEL_REFRESH_PERIOD} key. The default period granularity is 30 seconds.
 * <p/>
 * See {@link org.fluidity.foundation.Configuration} for details on configuration.
 *
 * @param <T> the underlying log implementation's class.
 *
 * @author Tibor Varga
 */
public interface ReloadingLog<T> extends Log<T> {

    /**
     * The configuration property that specifies the number of milliseconds during which at most one log level
     * check is performed per logger.
     */
    String LOG_LEVEL_REFRESH_PERIOD = "org.fluidity.features.log-level-refresh-period-ms";
}
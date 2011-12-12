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

/**
 * Configuration that is periodically updated. This is a wrapper around {@link org.fluidity.foundation.Configuration} that periodically refreshes the snapshot
 * of the settings object. If used with this variant of the configuration mechanism, the methods of the settings interface may not have any parameters.
 *
 * @param <T> the settings interface.
 *
 * @author Tibor Varga
 */
public interface ReloadingConfiguration<T> {

    /**
     * Returns an object containing the most recent snapshot of the settings.
     *
     * @param period the period during which at most one refresh will take place.
     *
     * @return an object containing the most recent snapshot of the settings.
     */
    Updates.Snapshot<T> snapshot(long period);
}

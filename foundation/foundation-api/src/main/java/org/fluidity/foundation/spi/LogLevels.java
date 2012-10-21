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

package org.fluidity.foundation.spi;

/**
 * Allows triggering dynamic reconfiguration of log levels. Log levels are loaded for each {@linkplain org.fluidity.foundation.Log logger} the first time it is
 * used and then the levels are cached until {@link #updated()} is invoked.
 *
 * @author Tibor Varga
 */
public final class LogLevels {

    /**
     * Used internally to trigger reloading log levels.
     */
    static volatile long updated;

    /**
     * Records the fact that log levels may have changed. The next time a log message is emitted through any {@linkplain org.fluidity.foundation.Log logger},
     * its log levels will be queried from the underlying logging framework and cached until this method is invoked again.
     */
    public static void updated() {
        updated = System.currentTimeMillis();
    }

    private LogLevels() {
        throw new UnsupportedOperationException(String.format("No instance allowed of %s", getClass()));
    }
}

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

package org.fluidity.foundation;

import org.fluidity.composition.Component;
import org.fluidity.composition.ServiceProvider;
import org.fluidity.foundation.spi.AbstractLog;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Log factory to emit messages to the standard output stream.
 *
 * @author Tibor Varga
 */
@ServiceProvider
@Component(primary = false)
final class StandardOutLogFactory implements LogFactory {

    /**
     * {@inheritDoc}
     */
    public Log createLog(final Class<?> source) {
        return new StandardOutLog(source);
    }

    public Log createLog(final Class<?> source, final AbstractLog.Levels.Snapshots levels) {
        return createLog(source);
    }
}

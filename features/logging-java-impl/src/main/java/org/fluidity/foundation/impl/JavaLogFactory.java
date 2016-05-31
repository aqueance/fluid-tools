/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.foundation.impl;

import java.util.logging.LogManager;

import org.fluidity.composition.Component;
import org.fluidity.composition.ServiceProvider;
import org.fluidity.foundation.Log;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Log factory backed by <code>java.util.logging</code>. The log levels are mapped as follows:<ul>
 * <li>{@linkplain org.fluidity.foundation.Log#trace(String, Object...) trace}: {@linkplain java.util.logging.Level#FINEST FINEST}</li>
 * <li>{@linkplain org.fluidity.foundation.Log#debug(String, Object...) debug}: {@linkplain java.util.logging.Level#FINE FINE}</li>
 * <li>{@linkplain org.fluidity.foundation.Log#info(String, Object...) info}: {@linkplain java.util.logging.Level#INFO INFO}</li>
 * <li>{@linkplain org.fluidity.foundation.Log#warning(String, Object...) warning}: {@linkplain java.util.logging.Level#WARNING WARNING}</li>
 * <li>{@linkplain org.fluidity.foundation.Log#error(String, Object...) error}: {@linkplain java.util.logging.Level#SEVERE SEVERE}</li>
 * </ul>
 *
 * @author Tibor Varga
 */
@Component
@ServiceProvider
final class JavaLogFactory implements LogFactory {

    private final LogManager loggers = LogManager.getLogManager();

    @Override
    public Class<?> type() {
        return JavaLogImpl.class;
    }

    /**
     * {@inheritDoc}
     */
    public <T> Log<T> createLog(final Class<T> source) {
        return new JavaLogImpl<>(loggers, source);
    }
}

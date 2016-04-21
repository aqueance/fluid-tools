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

import org.fluidity.composition.Component;
import org.fluidity.composition.ServiceProvider;
import org.fluidity.foundation.Log;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Log factory backed by Apache commons-logging.
 *
 * @author Tibor Varga
 */
@Component
@ServiceProvider
final class CommonsLogFactory implements LogFactory {

    /**
     * {@inheritDoc}
     */
    public <T> Log<T> createLog(final Class<T> source) {
        return new CommonsLogImpl<>(source);
    }
}

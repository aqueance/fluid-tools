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

package org.fluidity.foundation;

import org.fluidity.composition.Component;
import org.fluidity.features.DynamicConfiguration;
import org.fluidity.features.Updates;
import org.fluidity.foundation.spi.PropertyProvider;

/**
 * @author Tibor Varga
 */
@Component
public class EchoPropertyProviderImpl implements PropertyProvider {

    static final String UNKNOWN = "unknown-";

    private static final String[] known = new String[] {
            Updates.UPDATE_PERIOD,
            DynamicConfiguration.CONFIGURATION_REFRESH_PERIOD
    };

    public Object property(final String key) {
        for (final String name : known) {
            if (key.equals(name) || key.endsWith(".".concat(name))) {
                return "0";
            }
        }

        return key.startsWith(UNKNOWN) ? null : key;
    }
}

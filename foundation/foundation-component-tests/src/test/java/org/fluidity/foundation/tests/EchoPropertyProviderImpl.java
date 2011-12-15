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

package org.fluidity.foundation.tests;

import java.util.Properties;

import org.fluidity.composition.Component;
import org.fluidity.features.ReloadingConfiguration;
import org.fluidity.features.ReloadingLog;
import org.fluidity.features.Updates;
import org.fluidity.foundation.spi.PropertyProvider;

/**
 * @author Tibor Varga
 */
@Component
public class EchoPropertyProviderImpl implements PropertyProvider {

    private static final String[] known = new String[] {
            Updates.UPDATE_GRANULARITY,
            ReloadingLog.LOG_LEVEL_REFRESH_PERIOD,
            ReloadingConfiguration.CONFIGURATION_REFRESH_PERIOD
    };

    private final Properties properties = new Properties();

    public EchoPropertyProviderImpl() {
        for (final String name : known) {
            properties.setProperty(name, "0");
        }
    }

    public Object property(final String key) {
        for (final String name : known) {
            if (key.endsWith(name)) {
                return properties.getProperty(key);
            }
        }

        return key;
    }

    public void properties(final Runnable reader) {
        reader.run();
    }
}

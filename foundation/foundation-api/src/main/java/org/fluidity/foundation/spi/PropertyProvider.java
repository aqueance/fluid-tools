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

package org.fluidity.foundation.spi;

/**
 * Maps from property keys to property values. You provide an implementation that suits your configuration strategy.
 *
 * @author Tibor Varga
 */
public interface PropertyProvider {

    /**
     * Returns the configured value for the given property or <code>null</code> if no property was configured with the given name. Default values are assigned
     * to missing property values at higher levels.
     *
     * @param key the property name, or key.
     *
     * @return the configured value for the given property or <code>null</code> if no property was configured with the given name.
     */
    Object property(String key);

    /**
     * Registers an object to call when properties change. The receiver must accept multiple listeners and invoke each when properties change. The listener will
     * reload all properties used by it so it should not be notified for each property change individually.
     *
     * @param listener an object to call when properties change.
     */
    void addChangeListener(PropertyChangeListener listener);

    /**
     * Callback interface to get notified of changes to the property set of this provider.
     */
    interface PropertyChangeListener {

        /**
         * Notifies the receiver that the set of properties have changed.
         *
         * @param provider the provider whose properties have changed.
         */
        void propertiesChanged(PropertyProvider provider);
    }
}

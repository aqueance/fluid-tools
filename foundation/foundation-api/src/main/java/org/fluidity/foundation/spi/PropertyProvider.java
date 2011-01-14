/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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

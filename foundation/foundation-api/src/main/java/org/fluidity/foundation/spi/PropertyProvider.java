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
 * Maps property keys to property values. You provide an implementation that suits your configuration strategy. As long as the implementation is annotated as
 * {@link org.fluidity.composition.Component @Component}, Fluid Tools will find and use it.
 *
 * @author Tibor Varga
 */
@SuppressWarnings("JavadocReference")
public interface PropertyProvider {

    /**
     * Returns the configured value for the given property or <code>null</code> if no property was configured for the given key. Default values are assigned
     * to missing property values at higher levels.
     *
     * @param key the property name, or key.
     *
     * @return the configured value for the given property or <code>null</code> if no property was configured for the given key.
     */
    Object property(String key);

    /**
     * Runs the given command to read properties and if supported, guarantees that no property update takes place while the command executes. If properties are
     * read from a database, this method must open an isolated transaction before running the given command and then close the transaction afterwards. Other
     * implementation may use locking to prevent changes to the properties while this method executes.
     *
     * @param reader the command that reads properties and expects no changes in the property values while doing so.
     */
    void properties(Runnable reader);
}

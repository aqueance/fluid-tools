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

import java.util.List;
import java.util.jar.Attributes;

/**
 * Interface that a manifest handler must implement. In practice this is a service provider but since no composition
 * functionality is available when implementations of this interface are resolved, we can't use the @ServiceProvider
 * annotation here and neither can we use it on the implementations.
 *
 * @author Tibor Varga
 */
public interface JarManifest {

    String NESTED_DEPENDENCIES = "Nested-Dependencies";

    /**
     * Set main manifest attributes to invoke this launcher.
     *
     * @param attributes   the main manifest attributes.
     * @param dependencies the list of dependency paths pointing to the embedded JAR files relative to the JAR file root.
     */
    void processManifest(Attributes attributes, final List<String> dependencies);
}

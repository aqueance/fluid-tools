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

package org.fluidity.composition.spi;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * Component details used by container services.
 *
 * @author Tibor Varga
 */
public interface ComponentDescriptor {

    /**
     * Returns the list of context annotation accepted by the component.
     *
     * @return the list of context annotation accepted by the component.
     */
    Set<Class<? extends Annotation>> acceptedContext();

    /**
     * Returns the list of annotations that may comprise the context of some other component. Factories do not provide context annotations.
     *
     * @return the list of annotations that may comprise the context of some other component or <code>null</code> if none present.
     */
    Annotation[] annotations();

    /**
     * Converts the dependency descriptor to a (more or less) human readable form.
     *
     * @return the {@link String} representation of the dependency descriptor.
     */
    String toString();
}

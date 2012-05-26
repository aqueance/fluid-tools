/*
 * Copyright (c) 2006-2012 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.composition.container;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.fluidity.composition.Component;

/**
 * State object to hold a dependency type and context. Used by {@link ContextDefinition#filter(ComponentContextDescriptor[])}.
 *
 * @author Tibor Varga
 */
public class ComponentContextDescriptor<T> {

    public final Class<T> type;
    public final Collection<Class<? extends Annotation>> context;

    /**
     * Constructs an instance using the {@link org.fluidity.composition.Component.Context @Component.Context} annotation of the given class, if present.
     *
     * @param type the dependency type.
     */
    public ComponentContextDescriptor(final Class<T> type) {
        this.type = type;
        final Component.Context annotation = type.getAnnotation(Component.Context.class);
        this.context = annotation == null ? Collections.<Class<? extends Annotation>>emptyList() : Arrays.asList(annotation.value());
    }
}

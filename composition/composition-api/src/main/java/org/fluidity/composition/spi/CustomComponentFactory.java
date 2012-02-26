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

/**
 * Poses as a factory for a particular component, taking over component instantiation from the host container. You only need to provide an implementation of
 * this interface for a given component if that component requires instantiation logic more complex than {@link
 * java.lang.reflect.Constructor#newInstance(Object...)} with dependency injected parameters.
 * <p/>
 * The interface of the component is specified in the {@link org.fluidity.composition.Component#api()} annotation of the the factory implementation class,
 * which must be present.
 * <p/>
 * If the component is context dependent, the factory class must specify on behalf of the components the valid context annotation classes using the {@link
 * org.fluidity.composition.Component.Context @Component.Context} class annotation.
 * <h3>Usage</h3>
 * See {@link ComponentFactory}.
 *
 * @author Tibor Varga
 */
public interface CustomComponentFactory extends ComponentFactory { }

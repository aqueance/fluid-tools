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
 * A variant factory offers context dependent instance variants of an otherwise singleton component that itself can in some way be configured to adapt to
 * various externally defined contexts. The variant factory lists the context annotations it understands in its {@link
 * org.fluidity.composition.Component.Context @Component.Context} class annotation.
 * <p/>
 * A <code>ComponentVariantFactory</code> works in conjunction with an otherwise singleton component independently registered in a dependency injection
 * container and accessible to the factory with a binding that allows new instances to be created, i.e., the component has not been bound by {@link
 * org.fluidity.composition.ComponentContainer.Registry#bindInstance(Object, Class[])}.
 *
 * @author Tibor Varga
 */
public interface ComponentVariantFactory extends ComponentFactory { }

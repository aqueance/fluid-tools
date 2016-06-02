/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import java.util.function.Supplier;

import org.fluidity.composition.ComponentContext;

/**
 * A dependency that can be replaced by a {@link ComponentInterceptor}, and can be injected to a component when created by a {@link ComponentFactory}.
 *
 * @param <T> the optional type of the component represented by this dependency.
 *
 * @author Tibor Varga
 */
public interface Dependency<T> {

    /**
     * Returns the class of the component that resolves this dependency.
     *
     * @return a class object; never <code>null</code>.
     */
    Class<? extends T> type();

    /**
     * Instantiates, if necessary, and returns the dependency instance.
     * <p>
     * <b>Note</b>: When used in a {@link ComponentInterceptor}, this method may only be invoked from the same method of another {@link Dependency dependency}.
     * <p>
     * <b>Note</b>: When used in a {@link ComponentFactory}, this method is intended to be invoked only from the {@link
     * ComponentFactory.Instance#bind(ComponentFactory.Registry) bind()} method of the <code>Instance</code> returned from the factory's {@link
     * ComponentFactory#resolve(ComponentContext, ComponentFactory.Container) resolve()} method.
     *
     * @return the dependency instance; never <code>null</code>.
     */
    T instance();

    /**
     * Replaces the component factory with the supplied one.
     *
     * @param factory the new component factory.
     *
     * @return a new {@link Dependency} dependency object.
     */
    default Dependency<T> replace(final Supplier<T> factory) {
        return Dependency.to(this::type, factory);
    }

    /**
     * Creates a new {@link Dependency} with the given type and component factory.
     *
     * @param type    the component class; never <code>null</code>.
     * @param factory creates the component instance; never <code>null</code>.
     * @param <T>     the type of the component represented by the returned dependency.
     *
     * @return a new {@link Dependency} object.
     */
    static <T> Dependency<T> to(final Class<? extends T> type, final Supplier<T> factory) {
        return to(() -> type, factory);
    }

    /**
     * Creates a new {@link Dependency} with the given type and component factories.
     *
     * @param type    supplies the component class; never <code>null</code>.
     * @param factory creates the component instance; never <code>null</code>.
     * @param <T>     the type of the component represented by the returned dependency.
     *
     * @return a new {@link Dependency} object.
     */
    static <T> Dependency<T> to(final Supplier<Class<? extends T>> type, final Supplier<T> factory) {
        return new Dependency<T>() {
            @Override
            public Class<? extends T> type() {
                return type.get();
            }

            @Override
            public T instance() {
                return factory.get();
            }
        };
    }
}

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

/**
 * Lazy loading utilities.
 *
 * @author Tibor Varga
 */
public final class Deferred extends Utilities {

    private Deferred() { }

    /**
     * Returns a lazy loading reference to some object. The object is returned by the given factory's {@link Factory#create()} method, which will be invoked
     * the first time the returned object's {@link Reference#get()} method is invoked, and then its return value will be cached for use in subsequent
     * invocations.
     * <p/>
     * This reference implements the double-check locking logic with volatile acquire/release semantics to lazily create the referenced object. If the factory
     * returns <code>null</code> instead of an object then the <code>null</code> value will be cached and returned in subsequent queries by the returned
     * reference.
     * <p/>
     * Note: the returned object will maintain a strong reference to the provided factory until the first time its {@link Reference#get()} method is invoked.
     *
     * @param factory the factory to create the referred to object.
     *
     * @return a deferred reference to the object created by the given factory.
     */
    public static <T> Reference<T> reference(final Factory<T> factory) {
        return new ReferenceImpl<T>(factory);
    }

    /**
     * A factory of some object to be lazily instantiated. This is used by {@link Deferred#reference(Factory)}.
     */
    public interface Factory<T> {

        /**
         * Creates the object represented by this factory.
         *
         * @return a new instance.
         */
        T create();
    }

    /**
     * A reference to some object that is lazily instantiated. This is returned by {@link Deferred#reference(Factory)}.
     */
    public interface Reference<T> {

        /**
         * Returns the referred to object after creating it if necessary.
         *
         * @return the referred to object.
         */
        T get();
    }

    /**
     * Double-check locking implementation.
     */
    private static class ReferenceImpl<T> implements Reference<T> {
        private Factory<T> factory;
        private volatile T delegate;

        public ReferenceImpl(final Factory<T> factory) {
            assert factory != null;
            this.factory = factory;
        }

        public final T get() {
            T cache = delegate;

            if (factory != null && delegate == null) {
                synchronized (this) {
                    cache = delegate;

                    if (cache == null) {
                        delegate = cache = factory.create();
                        factory = null;
                    }
                }
            }

            return cache;
        }
    }
}

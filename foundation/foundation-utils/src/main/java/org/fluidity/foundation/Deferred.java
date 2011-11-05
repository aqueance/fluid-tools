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
 */
public class Deferred extends Utilities {

    /**
     * Returns a deferred reference to some object. The object is returned by the given factory's {@link Factory#create()} method, which will be invoked the
     * first time the reference's {@link Reference#get()} method is invoked and then its return value will be cached for use in subsequent invocations.
     *
     * @param factory the factory to create the referred to object.
     *
     * @return a deferred reference to the object created by the given factory.
     */
    public static <T> Reference<T> defer(final Factory<T> factory) {
        return new Reference<T>() {
            private volatile T delegate;

            public final T get() {
                T cache = delegate;

                if (delegate == null) {
                    synchronized (this) {
                        cache = delegate;

                        if (cache == null) {
                            delegate = cache = factory.create();
                        }
                    }
                }

                return cache;
            }
        };
    }

    /**
     * A factory of some object.
     */
    public interface Factory<T> {

        /**
         * Creates some object.
         *
         * @return the object.
         */
        T create();
    }

    /**
     * A reference to some object.
     */
    public interface Reference<T> {

        /**
         * Returns the referred to object after creating it if necessary.
         *
         * @return the referred to objcet.
         */
        T get();
    }
}

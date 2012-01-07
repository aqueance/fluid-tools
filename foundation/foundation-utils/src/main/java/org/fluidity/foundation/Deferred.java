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
 * Double-checked locking with <code>volatile</code> acquire/release semantics.
 * <h3>Usage</h3>
 * <pre>
 * final class LightObject {
 *
 *   private final <b>Deferred.Reference</b>&lt;HeavyObject> reference = <b>Deferred.reference</b>(new <b>Deferred.Factory</b>&lt;HeavyObject>() {
 *     public HeavyObject <b>create()</b> {
 *       return new HeavyObject(...);
 *     }
 *   });
 *
 *    ...
 *
 *    private void someMethod() {
 *        final HeavyObject object = reference.<b>get()</b>;
 *        assert object != null;
 *        assert object == reference.<b>get()</b>;
 *    }
 * }
 * </pre>
 *
 * @author Tibor Varga
 */
public final class Deferred extends Utilities {

    private Deferred() { }

    /**
     * Returns a lazy loading reference to some object. The object is instantiated by the given factory's {@link Factory#create() create()} method, which will
     * be invoked the first time the returned object's {@link Reference#get() get()} method is invoked, and then its return value will be cached for use in
     * subsequent invocations.
     * <p/>
     * This reference implements the double-check locking logic with volatile acquire/release semantics to lazily create the referenced object. If the factory
     * returns <code>null</code> instead of an object then the <code>null</code> value will be cached and returned in subsequent queries by the returned
     * reference.
     * <p/>
     * Note: the returned object will maintain a strong reference to the provided factory until the first time its {@link Reference#get() get()} method is
     * invoked.
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
     * A reference to some object that is lazily instantiated. Instances are created by {@link Deferred#reference(Factory)}.
     */
    public interface Reference<T> {

        /**
         * Returns the referred to object after creating it if necessary.
         *
         * @return the referred to object.
         */
        T get();

        /**
         * Tells if the reference has been resolved; i.e., if the object has been instantiated.
         *
         * @return <code>true</code> if the object has already been instantiated, <code>false</code> otherwise.
         */
        boolean resolved();
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

            if (factory != null && cache == null) {
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

        public boolean resolved() {
            return delegate != null;
        }
    }
}

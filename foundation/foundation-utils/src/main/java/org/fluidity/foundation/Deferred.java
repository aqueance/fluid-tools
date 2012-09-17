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

package org.fluidity.foundation;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Double-checked locking with <code>volatile</code> acquire/release semantics.
 * <h3>Usage</h3>
 * <pre>
 * final class LightObject {
 *
 *   private final <span class="hl1">Deferred.Factory</span> factory = new <span class="hl1">Deferred.Factory</span><span class="hl2">&lt;HeavyObject></span>() {
 *     public <span class="hl2">HeavyObject</span> <span class="hl1">create()</span> {
 *       return new <span class="hl2">HeavyObject</span>(&hellip;);
 *     }
 *   }
 *
 *   private final <span class="hl1">Deferred.Reference</span><span class="hl2">&lt;HeavyObject></span> reference = <span class="hl1">Deferred.reference</span>(factory);
 *
 *   &hellip;
 *
 *   private void someMethod() {
 *     final <span class="hl2">HeavyObject</span> object = reference.<span class="hl1">get()</span>;
 *     assert object != null : HeavyObject.class;
 *     assert object == reference.<span class="hl1">get()</span>;
 *   }
 * }
 * </pre>
 *
 * @author Tibor Varga
 */
public final class Deferred extends Utility {

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
     * @param <T>     the class of the lazily instantiated object.
     *
     * @return a deferred reference to the object created by the given factory.
     */
    public static <T> Reference<T> reference(final Factory<T> factory) {
        return new ReferenceImpl<T>(factory);
    }

    /**
     * Returns a lazy loading reference to some object. The object is instantiated by the given factory's {@link Factory#create() create()} method, which will
     * be invoked the first time the returned object's {@link Reference#get() get()} method is invoked, and then its return value will be cached for use in
     * subsequent invocations until the {@link Deferred.Reference.State#invalidate()} method is called to throw away the cached value.
     * <p/>
     * This reference implements the double-check locking logic with volatile acquire/release semantics to lazily create the referenced object. If the factory
     * returns <code>null</code> instead of an object then the <code>null</code> value will be cached and returned in subsequent queries by the returned
     * reference.
     * <p/>
     * Note: the returned object will maintain a strong reference to the provided factory until it itself is garbage collected.
     *
     * @param factory the factory to create the referred to object.
     * @param <T>     the class of the lazily instantiated object.
     *
     * @return a deferred reference to the object created by the given factory.
     */
    public static <T> Reference.State<T> state(final Factory<T> factory) {
        return new StateImpl<T>(factory);
    }

    /**
     * A factory of some object to be {@link Deferred lazily} instantiated. This is used by {@link Deferred#reference(Factory)}.
     * <h3>Usage</h3>
     * See {@link Deferred}.
     *
     * @param <T> the class of the created object.
     *
     * @author Tibor Varga
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
     * A reference to some object that is {@link Deferred lazily} instantiated. Instances are created by {@link Deferred#reference(Factory)}.
     * <h3>Usage</h3>
     * See {@link Deferred}.
     *
     * @param <T> the class of the cached object.
     *
     * @author Tibor Varga
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

        /**
         * Extends the {@link Deferred.Reference} interface with facilities to invalidate the lazily instantiated object to force its lazy instantiation the
         * next time it is requested by a call to {@link #get()}.
         * <h3>Usage</h3>
         * <pre>
         * final class LightObject {
         *
         *   private final <span class="hl1">Deferred.Reference.State</span><span class="hl2">&lt;HeavyObject></span> state = <span class="hl1">Deferred.state</span>(new <span class="hl1">Deferred.Factory</span><span class="hl2">&lt;HeavyObject></span>() {
         *     public <span class="hl2">HeavyObject</span> <span class="hl1">create()</span> {
         *       return new HeavyObject(&hellip;);
         *     }
         *   });
         *
         *   &hellip;
         *
         *   private void someMethod() {
         *     final <span class="hl2">HeavyObject</span> object = reference.<span class="hl1">get()</span>;
         *     assert object != null : HeavyObject.class;
         *     assert object == reference.<span class="hl1">get()</span>;
         *   }
         * }
         * </pre>
         *
         * @param <T> the class of the cached object.
         *
         * @author Tibor Varga
         */
        interface State<T> extends Reference<T> {

            /**
             * Forces the re-instantiation of the deferred object the next time it is {@linkplain Deferred.Reference#get() de-referenced}.
             *
             * @return the cached object; may be <code>null</code> if the object has not yet been {@linkplain #resolved() resolved}.
             */
            T invalidate();
        }
    }

    /**
     * Double-check locking implementation.
     */
    private static class ReferenceImpl<T> implements Reference<T> {

        private Factory<T> factory;
        private volatile T delegate;

        ReferenceImpl(final Factory<T> factory) {
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

    private static class StateImpl<T> implements Reference.State<T> {

        private final Factory<T> factory;
        private final AtomicReference<Reference<T>> state = new AtomicReference<Reference<T>>();

        StateImpl(final Factory<T> factory) {
            this.factory = factory;
            this.state.set(Deferred.reference(this.factory));
        }

        public T get() {
            return state.get().get();
        }

        public boolean resolved() {
            return state.get().resolved();
        }

        public T invalidate() {
            final Reference<T> reference = state.getAndSet(Deferred.reference(factory));
            return reference.resolved() ? reference.get() : null;
        }
    }
}

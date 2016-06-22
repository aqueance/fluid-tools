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

package org.fluidity.foundation;

import java.util.function.Supplier;

/**
 * Lazy instantiation with or without thread safety.
 * <h3>Usage</h3>
 * <pre>
 * final class LightObject {
 *
 *   private final <span class="hl1">Deferred.Factory</span> factory = new <span class="hl1">Deferred.Factory</span><span class="hl2">&lt;HeavyObject&gt;</span>() {
 *     public <span class="hl2">HeavyObject</span> <span class="hl1">create()</span> {
 *       return new <span class="hl2">HeavyObject</span>(&hellip;);
 *     }
 *   }
 *
 *   private final <span class="hl1">Deferred.Reference</span><span class="hl2">&lt;HeavyObject&gt;</span> reference = <span class="hl1">Deferred.local</span>(factory);
 *
 *   &hellip;
 *
 *   private void someMethod() {
 *     assert reference.<span class="hl1">invalidate</span>() == null;
 *
 *     final <span class="hl2">HeavyObject</span> object = reference.<span class="hl1">get</span>();
 *     assert object != null : HeavyObject.class;
 *
 *     assert object == reference.<span class="hl1">get</span>();
 *
 *     assert reference.<span class="hl1">invalidate</span>() == object;
 *     assert object != reference.<span class="hl1">get</span>();
 *   }
 * }
 * </pre>
 *
 * @author Tibor Varga
 */
@SuppressWarnings("WeakerAccess")
public final class Deferred extends Utility {

    private Deferred() { }

    /**
     * Returns a lazy loading reference to some object. The object is instantiated by the given factory's {@link Supplier#get() create()} method, which will
     * be invoked the first time the returned object's {@link Reference#get() get()} method is invoked, and then its return value will be cached for use in
     * subsequent invocations. If the factory returns <code>null</code> instead of an object then the <code>null</code> value will be cached and returned in
     * subsequent queries by the returned reference.
     * <p>
     * This reference implements the double-check locking logic with volatile acquire/release semantics to lazily create the referenced object.
     * <p>
     * Note: the returned object will maintain a strong reference to the provided factory.
     *
     * @param <T>     the type of the lazily instantiated object.
     *
     * @param factory the factory to create the referred to object.
     * @return a deferred reference to the object created by the given factory.
     */
    public static <T> Reference<T> shared(final Supplier<T> factory) {
        return new SafeReference<>(factory);
    }

    /**
     * Returns a lazy loading reference to some object. The object is instantiated by the given factory's {@link Supplier#get() create()} method, which will
     * be invoked the first time the returned object's {@link Reference#get() get()} method is invoked, and then its return value will be cached for use in
     * subsequent invocations. If the factory returns <code>null</code> instead of an object then the <code>null</code> value will be cached and returned in
     * subsequent queries by the returned reference.
     * <p>
     * This reference implements uses no locking when invoking the given <code>factory</code>.
     * <p>
     * Note: the returned object will maintain a strong reference to the provided factory.
     *
     * @param <T>     the type of the lazily instantiated object.
     *
     * @param factory the factory to create the referred to object.
     * @return a deferred reference to the object created by the given factory.
     */
    public static <T> Reference<T> local(final Supplier<T> factory) {
        return new BareReference<>(factory);
    }

    /**
     * Creates a lazy-initialized label from the given <code>format</code> and <code>arguments</code>. The arguments are evaluated every time {@link
     * Deferred.Label#toString()} is invoked.
     *
     * @param format    the Java format specification.
     * @param arguments the details to format.
     *
     * @return a lazy initialized {@link Label} object; never <code>null</code>.
     */
    public static Label label(final String format, final Object... arguments) {
        return new Label() {
            public String toString() {
                return String.format(format, arguments);
            }
        };
    }

    /**
     * Creates a lazy-initialized label that is produced by the given <code>factory</code>. The factory is invoked at most once when
     * {@link Deferred.Label#toString()} is invoked.
     *
     * @param factory the factory to produce the label.
     *
     * @return a lazily initialized {@link Label} object; never <code>null</code>.
     */
    public static Label label(final Supplier<String> factory) {
        return new Label() {
            private final Deferred.Reference<String> label = Deferred.local(factory);

            public String toString() {
                return label.get();
            }
        };
    }

    /**
     * A reference to some object that is {@link Deferred lazily} instantiated. Instances are created by {@link Deferred#shared(Supplier)} and
     * {@link Deferred#local(Supplier)}.
     * <h3>Usage</h3>
     * See {@link Deferred}.
     *
     * @param <T> the type of the object cached by this reference.
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
         * Forces the re-instantiation of the deferred object the next time it is {@linkplain Deferred.Reference#get() de-referenced}.
         *
         * @return the cached object; may be <code>null</code> if the object has not yet been {@linkplain #resolved() resolved}.
         */
        T invalidate();
    }

    /**
     * A lazy-initialized label. Labels are essentially objects with a single {@link Object#toString()} method that returns some text that is expensive to
     * compute.
     *
     * @author Tibor Varga
     * @see Deferred#label(Supplier)
     * @see Deferred#label(String, Object...)
     */
    public interface Label {

        /**
         * Initializes and returns the label's value.
         *
         * @return the label value.
         */
        String toString();
    }

    /**
     * A thread safe implementation of {@link Deferred.Reference}.
     *
     * @author Tibor Varga
     */
    private static class SafeReference<T> implements Reference<T> {

        private final Supplier<T> factory;
        private volatile DCL<T> state;

        SafeReference(final Supplier<T> factory) {
            this.factory = factory;
            this.state = new DCL<>(factory);
        }

        public T get() {
            return state.get();
        }

        public boolean resolved() {
            return state.resolved();
        }

        public T invalidate() {
            final DCL<T> reference = state;

            if (reference.resolved()) {
                state = new DCL<>(factory);
                return reference.get();
            } else {
                return null;
            }
        }

        /**
         * Double-check locking implementation based on the acquire/release semantics of volatile read/write.
         *
         * @author Tibor Varga
         */
        private static class DCL<T> {

            private volatile Supplier<T> factory;
            private T object;

            DCL(final Supplier<T> factory) {
                assert factory != null;
                this.factory = factory;
            }

            public final T get() {
                if (factory != null) {
                    synchronized (this) {
                        final Supplier<T> cache = factory;  // avoid excessive volatile access

                        if (cache != null) {
                            object = cache.get();
                            factory = null;
                        }
                    }
                }

                return object;
            }

            public boolean resolved() {
                return factory == null;
            }
        }
    }

    /**
     * A thread unsafe implementation of {@link Deferred.Reference}.
     *
     * @author Tibor Varga
     */
    private static class BareReference<T> implements Reference<T> {

        private final Supplier<T> factory;
        private T object;

        private boolean pristine = true;

        BareReference(final Supplier<T> factory) {
            this.factory = factory;
        }

        public T get() {
            if (pristine) {
                object = factory.get();
                pristine = false;
            }

            return object;
        }

        public boolean resolved() {
            return !pristine;
        }

        public T invalidate() {
            if (pristine) return null;
            pristine = true;

            final T previous = object;
            object = null;

            return previous;
        }
    }
}

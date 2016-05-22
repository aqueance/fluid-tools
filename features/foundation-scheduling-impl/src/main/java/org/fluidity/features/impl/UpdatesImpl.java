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

package org.fluidity.features.impl;

import java.util.concurrent.atomic.AtomicBoolean;

import org.fluidity.composition.Component;
import org.fluidity.features.Scheduler;
import org.fluidity.features.Updates;
import org.fluidity.foundation.Configuration;

/**
 * @author Tibor Varga
 */
@Component
final class UpdatesImpl implements Updates {

    private volatile long timestamp;

    private final long delay;

    UpdatesImpl(final Scheduler scheduler, @Component.Qualifiers(ignore = Configuration.Prefix.class) final Configuration<Settings> configuration) {
        delay = configuration.settings().period();

        if (delay > 0) {
            scheduler.invoke(delay, delay, new Scheduler.Task() {
                public void run() {
                    timestamp = System.currentTimeMillis();
                }
            });
        }
    }

    public synchronized <T> Snapshot<T> snapshot(final long period, final Snapshot<T> loader) {
        final long interval = period > 0 ? delay : period;  // period set: delay determines special behavior, period does otherwise

        if (interval < 0) {

            // transparent snapshot
            return new Snapshot<T>() {
                public T get() {
                    return loader.get();
                }
            };
        } else if (interval == 0) {

            // static snapshot
            return new Snapshot<T>() {
                private final T snapshot = loader.get();

                public T get() {
                    return snapshot;
                }
            };
        } else {

            // periodic snapshot
            return new Snapshot<T>() {

                private long loaded = System.currentTimeMillis();   // not volatile: snapshot assignment is a write barrier, timestamp query is a read barrier
                private volatile T snapshot = loader.get();

                private final AtomicBoolean loading = new AtomicBoolean(false);

                public T get() {

                    // "timestamp" query is a read barrier; must happen before reading "loaded"
                    if (timestamp - loaded >= period && loading.compareAndSet(false, true)) {
                        loaded = System.currentTimeMillis();
                        snapshot = loader.get();                    // "snapshot" assignment is a write barrier; must come after assignment to "loaded"
                        loading.set(false);
                    }

                    return snapshot;
                }
            };
        }
    }

    /**
     * Provides settings to the {@link org.fluidity.features.Updates} component.
     *
     * @author Tibor Varga
     */
    interface Settings {

        /**
         * The minimum number in milliseconds between subsequent calls to {@link org.fluidity.features.Updates.Snapshot#get()} of a loader passed to
         * {@link org.fluidity.features.Updates#snapshot(long, Updates.Snapshot)}.
         *
         * @return a number greater than 0.
         */
        @Configuration.Property(key = Updates.UPDATE_GRANULARITY, undefined = "1000")
        long period();
    }
}

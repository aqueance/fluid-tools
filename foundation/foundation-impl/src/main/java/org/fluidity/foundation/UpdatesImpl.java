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

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.Component;

/**
 * @author Tibor Varga
 */
@Component(automatic = false)
@Component.Context(ignore = Configuration.Context.class)
final class UpdatesImpl implements Updates {

    private final AtomicBoolean stopped = new AtomicBoolean();
    private final AtomicLong timestamp = new AtomicLong();

    private final Deferred.Reference<Timer> timer;

    public UpdatesImpl(final Configuration<Settings> configuration) {
        final long period = configuration.settings().period();

        this.timer = Deferred.reference(new Deferred.Factory<Timer>() {
            public Timer create() {
                if (period > 0) {
                    final Timer timer = new Timer(Updates.class.getName(), true);

                    final TimerTask task = new TimerTask() {
                        @Override
                        public void run() {
                            timestamp.set(System.currentTimeMillis());
                        }
                    };

                    timer.scheduleAtFixedRate(task, period, period);

                    return timer;
                } else {
                    return null;
                }
            }
        });
    }

    public synchronized <T> Snapshot<T> register(final long period, final Snapshot<T> loader) {
        if (stopped.get()) {
            throw new IllegalStateException("Updates have been stopped");
        }

        if (timer.get() == null || period <= 0) {
            return new Snapshot<T>() {
                private final T snapshot = loader.get();

                public T get() {
                    return snapshot;
                }
            };
        } else {
            return new Snapshot<T>() {
                private final AtomicLong loaded = new AtomicLong(System.currentTimeMillis());
                private final AtomicReference<T> snapshot = new AtomicReference<T>(loader.get());
                private final AtomicBoolean loading = new AtomicBoolean(false);

                public T get() {
                    if (timestamp.get() - loaded.get() >= period && loading.compareAndSet(false, true)) {
                        loaded.set(System.currentTimeMillis());
                        snapshot.set(loader.get());
                        loading.set(false);
                    }

                    return snapshot.get();
                }
            };
        }
    }

    public synchronized void stop() {
        if (stopped.compareAndSet(false, true) && timer.resolved()) {
            final Timer timer = this.timer.get();

            if (timer != null) {
                timer.cancel();
            }
        }
    }
}

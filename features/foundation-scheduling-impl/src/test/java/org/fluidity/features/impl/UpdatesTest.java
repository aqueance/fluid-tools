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

package org.fluidity.features.impl;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.fluidity.features.Scheduler;
import org.fluidity.features.Updates;
import org.fluidity.foundation.testing.MockConfiguration;
import org.fluidity.testing.Simulator;

import org.easymock.EasyMock;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class UpdatesTest extends Simulator {

    private final MockObjects dependencies = dependencies();
    private final MockConfiguration.Direct<UpdatesImpl.Settings> configuration = MockConfiguration.direct(UpdatesImpl.Settings.class, dependencies);

    @SuppressWarnings("unchecked")
    private final Supplier<Object> loader = dependencies.normal(Supplier.class);
    private final Scheduler scheduler = dependencies.normal(Scheduler.class);
    private final Scheduler.Task.Control control = dependencies.normal(Scheduler.Task.Control.class);

    private Updates updates;

    private Scheduler.Task setPeriod(final boolean schedule) throws Exception {
        assert updates == null;
        final long period = schedule ? 10 : 0;

        configuration.expectSettings(settings -> EasyMock.expect(settings.period()).andReturn(period));

        final AtomicReference<Scheduler.Task> task = new AtomicReference<>();

        if (period > 0) {
            EasyMock.expect(scheduler.invoke(EasyMock.eq(period), EasyMock.eq(period), EasyMock.anyObject()))
                    .andAnswer(() -> {
                        task.set((Scheduler.Task) EasyMock.getCurrentArguments()[2]);
                        return control;
                    });
        }

        updates = verify((Work<Updates>) () -> new UpdatesImpl(scheduler, configuration.get()));

        return task.get();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        updates = null;
    }

    @Test
    public void testUpdates() throws Exception {
        final Scheduler.Task timer = setPeriod(true);
        assert timer != null;

        final Object context = new Object();

        // initialization
        final Supplier<Object> snapshot = test(() -> {
            EasyMock.expect(loader.get()).andReturn(context);

            return verify(() -> updates.snapshot(100, loader));
        });

        verify(() -> {
            assert context == snapshot.get();
            assert context == snapshot.get();
        });

        Thread.sleep(150);
        timer.run();

        test(() -> {
            EasyMock.expect(loader.get()).andReturn(new Object());

            assert context != verify(snapshot::get);
        });
    }

    @Test
    public void testNoUpdates() throws Exception {
        final Scheduler.Task timer = setPeriod(false);
        assert timer == null;

        final Object context = new Object();

        // initialization
        final Supplier<Object> snapshot = test(() -> {
            EasyMock.expect(loader.get()).andReturn(context);

            return verify(() -> updates.snapshot(100, loader));
        });

        verify(() -> {
            assert context == snapshot.get();
            assert context == snapshot.get();
        });

        Thread.sleep(150);

        assert context == verify(snapshot::get);
    }

    @Test
    public void testNoPeriod() throws Exception {
        final Scheduler.Task timer = setPeriod(true);
        assert timer != null;

        final Object context = new Object();

        // initialization
        final Supplier<Object> snapshot = test(() -> {
            EasyMock.expect(loader.get()).andReturn(context);

            return verify(() -> updates.snapshot(0, loader));
        });

        verify(() -> {
            assert context == snapshot.get();
            assert context == snapshot.get();
        });

        Thread.sleep(150);

        assert context == verify(snapshot::get);
    }

    @Test
    public void testTransparentSnapshot() throws Exception {
        final Scheduler.Task timer = setPeriod(true);
        assert timer != null;

        final Object context1 = new Object();
        final Object context2 = new Object();

        final Supplier<Object> snapshot = verify(() -> updates.snapshot(-1, loader));

        test(() -> {
            EasyMock.expect(loader.get()).andReturn(context1);

            assert context1 == verify(snapshot::get);
        });

        test(() -> {
            EasyMock.expect(loader.get()).andReturn(context2);

            assert context2 == verify(snapshot::get);
        });
    }
}

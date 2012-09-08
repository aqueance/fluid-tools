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

import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.features.Scheduler;
import org.fluidity.features.Updates;
import org.fluidity.foundation.Configuration;
import org.fluidity.testing.MockGroup;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class UpdatesTest extends MockGroup {

    @SuppressWarnings("unchecked")
    private final Configuration<UpdatesImpl.Settings> configuration = mock(Configuration.class);
    @SuppressWarnings("unchecked")
    private final Updates.Snapshot<Object> loader = mock(Updates.Snapshot.class);

    private final Scheduler scheduler = mock(Scheduler.class);
    private final Scheduler.Control control = mock(Scheduler.Control.class);
    private final UpdatesImpl.Settings settings = mock(UpdatesImpl.Settings.class);

    private Updates updates;

    public Runnable setPeriod(final boolean schedule) throws Exception {
        assert updates == null;
        final long period = schedule ? 10 : 0;

        EasyMock.expect(configuration.settings()).andReturn(settings);
        EasyMock.expect(settings.period()).andReturn(period);

        final AtomicReference<Runnable> task = new AtomicReference<Runnable>();

        if (period > 0) {
            EasyMock.expect(scheduler.invoke(EasyMock.eq(period), EasyMock.eq(period), EasyMock.<Runnable>anyObject()))
                    .andAnswer(new IAnswer<Scheduler.Control>() {
                        public Scheduler.Control answer() throws Throwable {
                            task.set((Runnable) EasyMock.getCurrentArguments()[2]);
                            return control;
                        }
                    });
        }

        replay();
        updates = new UpdatesImpl(scheduler, configuration);
        verify();

        return task.get();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        updates = null;
    }

    @Test
    public void testUpdates() throws Exception {
        final Runnable timer = setPeriod(true);
        assert timer != null;

        final Object context = new Object();

        // initialization
        EasyMock.expect(loader.get()).andReturn(context);

        replay();
        final Updates.Snapshot<Object> snapshot = updates.register(100, loader);
        verify();

        replay();
        assert context == snapshot.get();
        assert context == snapshot.get();
        verify();

        Thread.sleep(150);
        timer.run();

        EasyMock.expect(loader.get()).andReturn(new Object());

        replay();
        assert context != snapshot.get();
        verify();
    }

    @Test
    public void testNoUpdates() throws Exception {
        final Runnable timer = setPeriod(false);
        assert timer == null;

        final Object context = new Object();

        // initialization
        EasyMock.expect(loader.get()).andReturn(context);

        replay();
        final Updates.Snapshot<Object> snapshot = updates.register(100, loader);
        verify();

        replay();
        assert context == snapshot.get();
        assert context == snapshot.get();
        verify();

        Thread.sleep(150);

        replay();
        assert context == snapshot.get();
        verify();
    }

    @Test
    public void testNoPeriod() throws Exception {
        final Runnable timer = setPeriod(true);
        assert timer != null;

        final Object context = new Object();

        // initialization
        EasyMock.expect(loader.get()).andReturn(context);

        replay();
        final Updates.Snapshot<Object> snapshot = updates.register(0, loader);
        verify();

        replay();
        assert context == snapshot.get();
        assert context == snapshot.get();
        verify();

        Thread.sleep(150);

        replay();
        assert context == snapshot.get();
        verify();
    }

    @Test
    public void testTransparentSnapshot() throws Exception {
        final Runnable timer = setPeriod(true);
        assert timer != null;

        final Object context1 = new Object();
        final Object context2 = new Object();

        replay();
        final Updates.Snapshot<Object> snapshot = updates.register(-1, loader);
        verify();

        EasyMock.expect(loader.get()).andReturn(context1);
        EasyMock.expect(loader.get()).andReturn(context2);

        replay();
        assert context1 == snapshot.get();
        assert context2 == snapshot.get();
        verify();
    }
}

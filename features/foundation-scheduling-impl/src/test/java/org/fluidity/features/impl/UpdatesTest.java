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
    private final Scheduler.Task.Control control = mock(Scheduler.Task.Control.class);
    private final UpdatesImpl.Settings settings = mock(UpdatesImpl.Settings.class);

    private Updates updates;

    public Scheduler.Task setPeriod(final boolean schedule) throws Exception {
        assert updates == null;
        final long period = schedule ? 10 : 0;

        EasyMock.expect(configuration.settings()).andReturn(settings);
        EasyMock.expect(settings.period()).andReturn(period);

        final AtomicReference<Scheduler.Task> task = new AtomicReference<Scheduler.Task>();

        if (period > 0) {
            EasyMock.expect(scheduler.invoke(EasyMock.eq(period), EasyMock.eq(period), EasyMock.<Scheduler.Task>anyObject()))
                    .andAnswer(new IAnswer<Scheduler.Task.Control>() {
                        public Scheduler.Task.Control answer() throws Throwable {
                            task.set((Scheduler.Task) EasyMock.getCurrentArguments()[2]);
                            return control;
                        }
                    });
        }

        updates = verify(new Work<Updates>() {
            public Updates run() throws Exception {
                return new UpdatesImpl(scheduler, configuration);
            }
        });

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
        final Updates.Snapshot<Object> snapshot = test(new Work<Updates.Snapshot<Object>>() {
            public Updates.Snapshot<Object> run() throws Exception {
                EasyMock.expect(loader.get()).andReturn(context);

                return verify(new Work<Updates.Snapshot<Object>>() {
                    public Updates.Snapshot<Object> run() throws Exception {
                        return updates.register(100, loader);
                    }
                });
            }
        });

        verify(new Task() {
            public void run() throws Exception {
                assert context == snapshot.get();
                assert context == snapshot.get();
            }
        });

        Thread.sleep(150);
        timer.run();

        test(new Task() {
            public void run() throws Exception {
                EasyMock.expect(loader.get()).andReturn(new Object());

                assert context != verify(new Work<Object>() {
                    public Object run() throws Exception {
                        return snapshot.get();
                    }
                });
            }
        });
    }

    @Test
    public void testNoUpdates() throws Exception {
        final Scheduler.Task timer = setPeriod(false);
        assert timer == null;

        final Object context = new Object();

        // initialization
        final Updates.Snapshot<Object> snapshot = test(new Work<Updates.Snapshot<Object>>() {
            public Updates.Snapshot<Object> run() throws Exception {
                EasyMock.expect(loader.get()).andReturn(context);

                return verify(new Work<Updates.Snapshot<Object>>() {
                    public Updates.Snapshot<Object> run() throws Exception {
                        return updates.register(100, loader);
                    }
                });
            }
        });

        verify(new Task() {
            public void run() throws Exception {
                assert context == snapshot.get();
                assert context == snapshot.get();
            }
        });

        Thread.sleep(150);

        assert context == verify(new Work<Object>() {
            public Object run() throws Exception {
                return snapshot.get();
            }
        });
    }

    @Test
    public void testNoPeriod() throws Exception {
        final Scheduler.Task timer = setPeriod(true);
        assert timer != null;

        final Object context = new Object();

        // initialization
        final Updates.Snapshot<Object> snapshot = test(new Work<Updates.Snapshot<Object>>() {
            public Updates.Snapshot<Object> run() throws Exception {
                EasyMock.expect(loader.get()).andReturn(context);

                return verify(new Work<Updates.Snapshot<Object>>() {
                    public Updates.Snapshot<Object> run() throws Exception {
                        return updates.register(0, loader);
                    }
                });
            }
        });

        verify(new Task() {
            public void run() throws Exception {
                assert context == snapshot.get();
                assert context == snapshot.get();
            }
        });

        Thread.sleep(150);

        assert context == verify(new Work<Object>() {
            public Object run() throws Exception {
                return snapshot.get();
            }
        });
    }

    @Test
    public void testTransparentSnapshot() throws Exception {
        final Scheduler.Task timer = setPeriod(true);
        assert timer != null;

        final Object context1 = new Object();
        final Object context2 = new Object();

        final Updates.Snapshot<Object> snapshot = verify(new Work<Updates.Snapshot<Object>>() {
            public Updates.Snapshot<Object> run() throws Exception {
                return updates.register(-1, loader);
            }
        });

        test(new Task() {
            public void run() throws Exception {
                EasyMock.expect(loader.get()).andReturn(context1);

                assert context1 == verify(new Work<Object>() {
                    public Object run() throws Exception {
                        return snapshot.get();
                    }
                });
            }
        });

        test(new Task() {
            public void run() throws Exception {
                EasyMock.expect(loader.get()).andReturn(context2);

                assert context2 == verify(new Work<Object>() {
                    public Object run() throws Exception {
                        return snapshot.get();
                    }
                });
            }
        });
    }
}

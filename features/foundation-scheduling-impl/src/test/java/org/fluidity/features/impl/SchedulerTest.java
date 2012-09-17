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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fluidity.composition.spi.ContainerTermination;
import org.fluidity.features.Scheduler;
import org.fluidity.foundation.Command;
import org.fluidity.testing.MockGroup;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class SchedulerTest extends MockGroup {

    private final Runnable task = mock(Runnable.class);

    private Scheduler scheduler;
    private Command.Job<Exception> stop;

    @BeforeMethod
    public void setUp() throws Exception {
        assert stop == null;
        final ContainerTermination termination = localMock(ContainerTermination.class);

        termination.add(EasyMock.<Command.Job<Exception>>notNull());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {
            @SuppressWarnings("unchecked")
            public Void answer() throws Throwable {
                stop = (Command.Job<Exception>) EasyMock.getCurrentArguments()[0];
                return null;
            }
        });

        verify(new Task() {
            public void run() throws Exception {
                scheduler = new SchedulerImpl(termination);
            }
        });

        assert stop != null;
    }

    @AfterMethod
    public void tearDown() throws Exception {
        assert scheduler != null;
        stop.run();
        stop = null;
    }

    @Test
    public void testScheduler() throws Exception {
        final Scheduler.Control control = verify(new Work<Scheduler.Control>() {
            public Scheduler.Control run() throws Exception {
                return scheduler.invoke(100, 100, task);
            }
        });

        test(new Task() {
            public void run() throws Exception {
                task.run();

                verify(new Task() {
                    public void run() throws Exception {
                        Thread.sleep(150);
                    }
                });
            }
        });

        test(new Task() {
            public void run() throws Exception {
                control.cancel();

                verify(new Task() {
                    public void run() throws Exception {
                        Thread.sleep(250);
                    }
                });
            }
        });
    }

    @Test
    public void testThreading() throws Exception {
        test(new Task() {
            public void run() throws Exception {
                final AtomicBoolean running = new AtomicBoolean(false);

                scheduler.invoke(0, new Runnable() {
                    public void run() {
                        running.set(true);
                    }
                });

                verify(new Task() {
                    public void run() throws Exception {
                        Thread.sleep(50);
                    }
                });

                assert running.get();
            }
        });

        final ThreadMXBean threadControl = ManagementFactory.getThreadMXBean();

        if (threadControl != null) {
            final String name = Scheduler.class.getName();

            test(new Task() {
                public void run() throws Exception {
                    final ThreadInfo thread = thread(threadControl, name);

                    assert thread != null : name;
                    assert thread.getThreadState() != Thread.State.TERMINATED : thread.getThreadState();
                }
            });

            stop.run();

            test(new Task() {
                public void run() throws Exception {
                    Thread.sleep(10);

                    final ThreadInfo thread = thread(threadControl, name);

                    assert thread == null || thread.getThreadState() == Thread.State.TERMINATED : thread.getThreadState();
                }
            });
        }
    }

    private ThreadInfo thread(final ThreadMXBean threadControl, final String name) {
        assert name != null;

        for (final ThreadInfo info : threadControl.getThreadInfo(threadControl.getAllThreadIds())) {
            if (name.equals(info.getThreadName())) {
                return info;
            }
        }

        return null;
    }
}

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
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fluidity.composition.spi.ContainerTermination;
import org.fluidity.features.Scheduler;
import org.fluidity.foundation.Command;
import org.fluidity.foundation.Log;
import org.fluidity.foundation.NoLogFactory;
import org.fluidity.foundation.testing.MockConfiguration;
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

    private final MockConfiguration<SchedulerImpl.Settings> configuration = MockConfiguration.create(this, SchedulerImpl.Settings.class);

    private final Scheduler.Task task = mock(Scheduler.Task.class);
    private final Log<SchedulerImpl> log = NoLogFactory.consume(SchedulerImpl.class);

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
                scheduler = new SchedulerImpl(termination, configuration.get(), log);
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

    private void settings(final int errorLimit, final long errorPenalty) throws Exception {
        configuration.expectQuery(new Command.Operation<SchedulerImpl.Settings, Exception>() {
            public void run(final SchedulerImpl.Settings settings) throws Exception {
                EasyMock.expect(settings.defaultErrorLimit()).andReturn(errorLimit);
                EasyMock.expect(settings.defaultErrorPenalty()).andReturn(errorPenalty);
            }
        });
    }

    @Test
    public void testScheduler() throws Exception {
        final CyclicBarrier barrier = new CyclicBarrier(2);
        final Threads threads = newThreads("Concurrency");

        final Scheduler.Task.Control control = verify(new Work<Scheduler.Task.Control>() {
            public Scheduler.Task.Control run() throws Exception {
                return scheduler.invoke(50, 100, task);
            }
        });

        settings(3, 60000);

        task.run();
        EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {
            public Void answer() throws Throwable {
                threads.lineup(barrier, 50);
                Thread.sleep(50);
                return null;
            }
        });

        verify(new Task() {
            public void run() throws Exception {
                threads.lineup(barrier, 100);
            }
        });

        assert !control.canceled();
        assert control.cancel();
        assert control.canceled();
    }

    @Test
    public void testThreading() throws Exception {
        test(new Task() {
            public void run() throws Exception {
                settings(3, 60000);

                final AtomicBoolean running = new AtomicBoolean(false);

                verify(new Task() {
                    public void run() throws Exception {
                        scheduler.invoke(0, new Scheduler.Task() {
                            public void run() {
                                running.set(true);
                            }
                        });

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

    @Test
    public void testTaskError() throws Exception {
        final CyclicBarrier barrier = new CyclicBarrier(2);
        final Threads threads = newThreads("Concurrency");

        final Scheduler.Task.Control control = verify(new Work<Scheduler.Task.Control>() {
            public Scheduler.Task.Control run() throws Exception {
                return scheduler.invoke(50, 100, task);
            }
        });

        settings(3, 60000);

        task.run();
        EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {
            public Void answer() throws Throwable {
                threads.lineup(barrier, 50);
                throw new Error();
            }
        });

        assert !control.canceled();

        verify(new Task() {
            public void run() throws Exception {
                threads.lineup(barrier, 100);
                Thread.yield();
            }
        });

        assert control.canceled();
    }

    @Test
    public void testSuspension() throws Exception {
        final CyclicBarrier barrier = new CyclicBarrier(2);
        final Threads threads = newThreads("Concurrency");

        final Scheduler.Task.Control control = verify(new Work<Scheduler.Task.Control>() {
            public Scheduler.Task.Control run() throws Exception {
                return scheduler.invoke(50, 100, task);
            }
        });

        settings(3, 50);

        task.run();
        EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {
            public Void answer() throws Throwable {
                threads.lineup(barrier, 50);
                return null;
            }
        });

        assert !control.suspended();

        verify(new Task() {
            public void run() throws Exception {
                threads.lineup(barrier, 100);
            }
        });

        assert !control.canceled();
        assert !control.suspended();

        control.suspend();

        assert control.suspended();

        verify(new Task() {
            public void run() throws Exception {
                Thread.sleep(350);   // at least 3 invocations to be ignored
            }
        });

        assert control.suspended();

        // nothing should happen, only setting some variables
        verify(new Task() {
            public void run() throws Exception {
                control.resume();
            }
        });

        assert !control.suspended();

        task.run();
        EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {
            public Void answer() throws Throwable {
                threads.lineup(barrier, 50);
                return null;
            }
        });
        task.run();
        EasyMock.expectLastCall().anyTimes();

        verify(new Task() {
            public void run() throws Exception {
                threads.lineup(barrier, 100);

                assert !control.canceled();
                assert !control.suspended();
            }
        });
    }

    @Test
    public void testDelayedInvocation() throws Exception {
        final Scheduler.Task.Control control = verify(new Work<Scheduler.Task.Control>() {
            public Scheduler.Task.Control run() throws Exception {
                final Scheduler.Task.Control control = scheduler.invoke(50, task);
                control.suspend();
                return control;
            }
        });

        assert control.suspended();

        settings(3, 50);

        // suspended task should not be invoked, only settings should be checked
        verify(new Task() {
            public void run() throws Exception {
                Thread.sleep(100);
            }
        });

        task.run();

        // task should be invoked at resume
        verify(new Task() {
            public void run() throws Exception {
                control.resume();
            }
        });

        assert !control.suspended();
    }

    @Test
    public void testTaskExceptionsAutoResume() throws Exception {
        final CyclicBarrier barrier = new CyclicBarrier(2);
        final Threads threads = newThreads("Concurrency");

        final Scheduler.Task.Control control = verify(new Work<Scheduler.Task.Control>() {
            public Scheduler.Task.Control run() throws Exception {
                return scheduler.invoke(10, 10, task);
            }
        });

        final IAnswer<Void> exception = new IAnswer<Void>() {
            public Void answer() throws Throwable {
                threads.lineup(barrier, 20);
                throw new Exception();
            }
        };

        settings(3, 50);

        // waiting out the penalty

        task.run();
        EasyMock.expectLastCall().andAnswer(exception).times(3);

        assert !control.suspended();

        final long timestamp = verify(new Work<Long>() {
            public Long run() throws Exception {
                threads.lineup(barrier, 40);
                threads.lineup(barrier, 40);
                threads.lineup(barrier, 40);

                return System.currentTimeMillis();
            }
        });

        Thread.sleep(10);

        assert !control.canceled();
        assert control.suspended();

        task.run();
        EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {
            public Void answer() throws Throwable {
                threads.lineup(barrier, 100);
                return null;
            }
        });
        task.run();
        EasyMock.expectLastCall().anyTimes();

        verify(new Task() {
            public void run() throws Exception {
                threads.lineup(barrier, 100);

                final long elapsed = System.currentTimeMillis() - timestamp;
                assert elapsed >= 30 : elapsed; // 50 ms but with some allowance for variations in timing

                assert !control.canceled();
                assert !control.suspended();
            }
        });
    }

    @Test
    public void testTaskExceptionsExplicitResume() throws Exception {
        final CyclicBarrier barrier = new CyclicBarrier(2);
        final Threads threads = newThreads("Concurrency");

        final Scheduler.Task.Control control = verify(new Work<Scheduler.Task.Control>() {
            public Scheduler.Task.Control run() throws Exception {
                return scheduler.invoke(10, 10, task);
            }
        });

        final IAnswer<Void> exception = new IAnswer<Void>() {
            public Void answer() throws Throwable {
                threads.lineup(barrier, 20);
                throw new Exception();
            }
        };

        settings(3, 50);

        // explicit resume

        task.run();
        EasyMock.expectLastCall().andAnswer(exception).times(3);

        assert !control.canceled();
        assert !control.suspended();

        verify(new Task() {
            public void run() throws Exception {
                threads.lineup(barrier, 40);
                threads.lineup(barrier, 40);
                threads.lineup(barrier, 40);
            }
        });

        Thread.sleep(10);

        assert !control.canceled();
        assert control.suspended();

        // nothing else should happen, only setting some variables
        verify(new Task() {
            public void run() throws Exception {
                control.resume();
            }
        });

        assert !control.suspended();

        task.run();
        EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {
            public Void answer() throws Throwable {
                threads.lineup(barrier, 100);
                return null;
            }
        });
        task.run();
        EasyMock.expectLastCall().anyTimes();

        verify(new Task() {
            public void run() throws Exception {
                threads.lineup(barrier, 20);

                assert !control.canceled();
                assert !control.suspended();
            }
        });
    }

    @Test
    public void testTaskExceptionsOverLimit() throws Exception {
        final CyclicBarrier barrier = new CyclicBarrier(2);
        final Threads threads = newThreads("Concurrency");

        final Scheduler.Task.Control control = verify(new Work<Scheduler.Task.Control>() {
            public Scheduler.Task.Control run() throws Exception {
                return scheduler.invoke(10, 10, task);
            }
        });

        final IAnswer<Void> exception = new IAnswer<Void>() {
            public Void answer() throws Throwable {
                threads.lineup(barrier, 40);
                throw new Exception();
            }
        };

        settings(3, 50);

        task.run();
        EasyMock.expectLastCall().andAnswer(exception).times(3);

        assert !control.canceled();
        assert !control.suspended();

        verify(new Task() {
            public void run() throws Exception {
                threads.lineup(barrier, 40);
                threads.lineup(barrier, 40);
                threads.lineup(barrier, 40);
            }
        });

        Thread.sleep(10);

        assert !control.canceled();
        assert control.suspended();

        task.run();
        EasyMock.expectLastCall().andAnswer(exception);

        verify(new Task() {
            public void run() throws Exception {
                threads.lineup(barrier, 100);
                Thread.yield();
            }
        });

        assert !control.canceled();
        assert control.suspended();
    }

    private ThreadInfo thread(final ThreadMXBean threads, final String name) {
        assert name != null;

        for (final ThreadInfo info : threads.getThreadInfo(threads.getAllThreadIds())) {
            if (name.equals(info.getThreadName())) {
                return info;
            }
        }

        return null;
    }
}

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

package org.fluidity.testing;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeoutException;

import org.fluidity.foundation.Command;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class SimulatorTest extends Simulator {

    @Test
    public void testVarargsMatching() throws Exception {
        final VarargsComponent component = dependencies().normal(VarargsComponent.class);

        final String expected = "x";
        final String prefix = "0";
        final String[] arguments = {"1", "2", "3"};

        EasyMock.expect(component.method(EasyMock.<String>notNull(), varEq(arguments))).andReturn(expected);

        final String actual1 = verify(new Work<String>() {
            public String run() throws Exception {
                return component.method(prefix, arguments);
            }
        });

        EasyMock.expect(component.method(EasyMock.<String>notNull(), varSame(arguments))).andReturn(expected);

        final String actual2 = verify(new Work<String>() {
            public String run() throws Exception {
                return component.method(prefix, arguments);
            }
        });

        EasyMock.expect(component.method(EasyMock.<String>notNull(), varNotNull(arguments))).andReturn(expected);
        final String actual3 = verify(new Work<String>() {
            public String run() throws Exception {
                return component.method(prefix, arguments);
            }
        });

        assert expected.equals(actual1) : String.format("%s != %s", expected, actual1);
        assert expected.equals(actual2) : String.format("%s != %s", expected, actual2);
        assert expected.equals(actual3) : String.format("%s != %s", expected, actual3);
    }

    public interface VarargsComponent {

        String method(final String prefix, final String... values);
    }

    @Test(expectedExceptions = TimeoutException.class)
    public void testBareDeadLock() throws Exception {
        final DeadLock deadLock = new DeadLock();

        final Threads threads = newThreads("dead-lock");

        final CyclicBarrier barrier = new CyclicBarrier(2);

        threads.concurrent(new Task.Concurrent() {
            public Task run(final MockObjects.Concurrent ignored) throws Exception {
                return new Task() {
                    public void run() throws Exception {
                        deadLock.lock12(new Command.Job<Exception>() {
                            public void run() throws Exception {
                                threads.lineup(barrier, threads.time(5));
                            }
                        });
                    }
                };
            }
        });

        threads.concurrent(new Task.Concurrent() {
            public Task run(final MockObjects.Concurrent ignored) throws Exception {
                return new Task() {
                    public void run() throws Exception {
                        deadLock.lock21(new Command.Job<Exception>() {
                            public void run() throws Exception {
                                threads.lineup(barrier, threads.time(5));
                            }
                        });
                    }
                };
            }
        });

        threads.verify(threads.time(10), (Task) null);
    }

    @Test(expectedExceptions = TimeoutException.class)
    public void testMockedDeadLock() throws Exception {
        final Threads threads = newThreads("dead-lock");

        final CyclicBarrier barrier = new CyclicBarrier(2);

        final IAnswer<Void> lineup = new IAnswer<Void>() {
            public Void answer() throws Throwable {
                threads.lineup(barrier, threads.time(10));
                return null;
            }
        };

        final DeadLock deadLock = new DeadLock();

        threads.concurrent(new Task.Concurrent() {
            public Task run(final MockObjects.Concurrent factory) throws Exception {

                @SuppressWarnings("unchecked")
                final Command.Job<Exception> command = (Command.Job<Exception>) factory.normal(Command.Job.class);

                command.run();
                EasyMock.expectLastCall().andAnswer(lineup);

                return new Task() {
                    public void run() throws Exception {
                        deadLock.lock12(command);
                    }
                };
            }
        });

        threads.concurrent(new Task.Concurrent() {
            public Task run(final MockObjects.Concurrent factory) throws Exception {

                @SuppressWarnings("unchecked")
                final Command.Job<Exception> command = (Command.Job<Exception>) factory.normal(Command.Job.class);

                command.run();
                EasyMock.expectLastCall().andAnswer(lineup);

                return new Task() {
                    public void run() throws Exception {
                        deadLock.lock21(command);
                    }
                };
            }
        });

        threads.guarantee(threads.time(20), (Task) null);
    }

    private static class DeadLock {

        private final Object lock1 = new Object();
        private final Object lock2 = new Object();

        public void lock12(final Command.Job<Exception> checkpoint) throws Exception {
            synchronized(lock1) {
                checkpoint.run();

                synchronized(lock2) {
                    System.out.println("lock12 - should not be printed");
                }
            }
        }

        public void lock21(final Command.Job<Exception> checkpoint) throws Exception  {
            synchronized(lock2) {
                checkpoint.run();

                synchronized(lock1) {
                    System.out.println("lock21 - should not be printed");
                }
            }
        }
    }

    @Test
    public void testConcurrentMocks() throws Exception {
        final Composite<Runnable> composite = composite(Runnable.class, arguments().normal(Runnable.class));
        final ConcurrentDelegate component = new ConcurrentDelegate(composite.dependency());

        final Threads threads = newThreads("concurrent");

        final Task.Concurrent task = new Task.Concurrent() {
            public Task run(final MockObjects.Concurrent factory) throws Exception {
                final Runnable dependency = factory.normal(Runnable.class, composite);

                dependency.run();
                EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
                    public Object answer() throws Throwable {
                        Thread.sleep(threads.time(10));
                        return null;
                    }
                });

                return new Task() {
                    public void run() throws Exception {
                        component.run();
                    }
                };
            }
        };

        for (int i = 0, count = Runtime.getRuntime().availableProcessors(); i < count; ++i) {
            threads.concurrent(task);
        }

        threads.guarantee(threads.time(15), (Task) null);
    }

    private static class ConcurrentDelegate implements Runnable {

        private final Runnable delegate;

        private ConcurrentDelegate(final Runnable delegate) {
            this.delegate = delegate;
        }

        public void run() {
            delegate.run();
        }
    }
}

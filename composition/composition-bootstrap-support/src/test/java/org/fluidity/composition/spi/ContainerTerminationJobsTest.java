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

package org.fluidity.composition.spi;

import java.util.concurrent.CyclicBarrier;

import org.fluidity.foundation.Command;
import org.fluidity.foundation.NoLogFactory;
import org.fluidity.testing.MockGroup;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class ContainerTerminationJobsTest extends MockGroup {

    private final ContainerTermination.Jobs list = new ContainerTerminationJobs(NoLogFactory.consume(ContainerTerminationJobs.class));
    @SuppressWarnings("unchecked")
    private final Command.Job<Exception>[] jobs = new Command.Job[] {
            mock(Command.Job.class),
            mock(Command.Job.class),
            mock(Command.Job.class),
            mock(Command.Job.class)
    };

    @Test
    public void testAddition() throws Exception {
        list.add(jobs[0]);
        list.add(jobs[1]);

        // jobs should be invoked
        test(new Task() {
            public void run() throws Exception {
                jobs[1].run();
                jobs[0].run();

                verify(new Task() {
                    public void run() throws Exception {
                        list.flush();
                    }
                });
            }
        });

        // jobs should not be invoked
        verify(new Task() {
            public void run() throws Exception {
                list.flush();
            }
        });
    }

    @Test
    public void testRemoval() throws Exception {
        list.add(jobs[0]);
        list.add(jobs[1]);

        list.remove(jobs[1]);
        list.remove(jobs[0]);

        // jobs should not be invoked
        verify(new Task() {
            public void run() throws Exception {
                list.flush();
            }
        });
    }

    @Test
    public void testRepeatedCycle() throws Exception {
        final Task cycle = new Task() {
            public void run() throws Exception {
                list.add(jobs[0]);
                list.add(jobs[1]);

                jobs[1].run();
                jobs[0].run();

                verify(new Task() {
                    public void run() throws Exception {
                        list.flush();
                    }
                });
            }
        };

        test(cycle);
        test(cycle);
    }

    @Test
    public void testConcurrency() throws Exception {
        list.add(jobs[1]);
        list.add(jobs[0]);

        final Threads threads = newThreads("Concurrency");

        final CyclicBarrier barrier = new CyclicBarrier(2);

        // job 0 is invoked, forces the thread to wait
        jobs[0].run();
        EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {
            public Void answer() throws Throwable {
                threads.lineup(barrier, 100);
                // just wait for the concurrent task to execute
                threads.lineup(barrier, 100);
                return null;
            }
        });

        // this task will be executed while jobs[0] is waiting in execution, releasing jobs[0] at completion
        threads.concurrent(new Task() {
            public void run() throws Exception {
                threads.lineup(barrier, 100);

                list.remove(jobs[1]);
                list.add(jobs[2]);
                list.add(jobs[3]);
                list.remove(jobs[3]);

                threads.lineup(barrier, 100);
            }
        });

        // job 2 is invoked
        jobs[2].run();

        // no other job is invoked

        threads.verify(500, new Task() {
            public void run() throws Exception {
                list.flush();
            }
        });

        // verify that none remained added/removed
        test(new Task() {
            public void run() throws Exception {
                list.add(jobs[0]);
                list.add(jobs[1]);
                list.add(jobs[2]);
                list.add(jobs[3]);

                // all jobs should be invoked once
                jobs[3].run();
                jobs[2].run();
                jobs[1].run();
                jobs[0].run();

                verify(new Task() {
                    public void run() throws Exception {
                        list.flush();
                    }
                });
            }
        });
    }
}

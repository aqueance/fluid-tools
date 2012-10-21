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
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class MockGroupTest extends MockGroup {

    @Test
    public void testVarargsMatching() throws Exception {
        final VarargsComponent component = mock(VarargsComponent.class);

        final String expected = "x";
        final String prefix = "0";
        final String[] arguments = {"1", "2", "3"};

        EasyMock.expect(component.method(EasyMock.<String>notNull(), varEq(arguments))).andReturn(expected);
        EasyMock.expect(component.method(EasyMock.<String>notNull(), varSame(arguments))).andReturn(expected);
        EasyMock.expect(component.method(EasyMock.<String>notNull(), varNotNull(arguments))).andReturn(expected);

        replay();
        final String actual1 = component.method(prefix, arguments);
        final String actual2 = component.method(prefix, arguments);
        final String actual3 = component.method(prefix, arguments);
        verify();

        assert expected.equals(actual1) : String.format("%s != %s", expected, actual1);
        assert expected.equals(actual2) : String.format("%s != %s", expected, actual2);
        assert expected.equals(actual3) : String.format("%s != %s", expected, actual3);
    }

    public interface VarargsComponent {

        String method(final String prefix, final String... values);
    }

    @Test(expectedExceptions = TimeoutException.class)
    public void testDeadLock() throws Exception {
        final DeadLock deadLock = new DeadLock();

        final Threads threads = newThreads("dead-lock");

        final CyclicBarrier barrier = new CyclicBarrier(2);

        threads.concurrent(new Task() {
            public void run() throws Exception {
                deadLock.lock12(new Command.Job<Exception>() {
                    public void run() throws Exception {
                        threads.lineup(barrier, threads.time(5));
                    }
                });
            }
        });

        threads.concurrent(new Task() {
            public void run() throws Exception {
                deadLock.lock21(new Command.Job<Exception>() {
                    public void run() throws Exception {
                        threads.lineup(barrier, threads.time(5));
                    }
                });
            }
        });

        threads.release(threads.time(10));
    }

    private static final class DeadLock {

        private final Object lock1 = new Object();
        private final Object lock2 = new Object();

        public void lock12(final Command.Job<Exception> checkpoint) throws Exception {
            synchronized(lock1) {
                checkpoint.run();

                synchronized(lock2) {
                    System.out.println("lock12");
                }
            }
        }

        public void lock21(final Command.Job<Exception> checkpoint) throws Exception  {
            synchronized(lock2) {
                checkpoint.run();

                synchronized(lock1) {
                    System.out.println("lock21");
                }
            }
        }
    }
}

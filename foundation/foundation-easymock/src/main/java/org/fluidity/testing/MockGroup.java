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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fluidity.foundation.Command;
import org.fluidity.foundation.Strings;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.easymock.internal.matchers.Equals;
import org.easymock.internal.matchers.NotNull;
import org.easymock.internal.matchers.Same;
import org.testng.annotations.AfterMethod;

/**
 * Facilitates the use of multiple <a href="http://www.easymock.org/">EasyMock</a> control objects. There are two ways to use this class: as a super class
 * (preferred mode) or as a delegate.
 * <p/>
 * To use it as a super class, the subclass must use <a href="http://testng.org">TestNG</a> annotations. If TestNG or subclassing is not an option, an
 * instance of this class can be used as a delegate. In that case, the actual test case must call {@link #clear() clear} method on the instance after each test
 * method.
 * <p/>
 * Mock objects are created by any of the<ul>
 * <li>{@link #mock(Class) mock}, {@link #niceMock(Class) niceMock}, {@link #strictMock(Class) strictMock}, {@link #mockAll(Class, Class[]) mockAll}, and</li>
 * <li>{@link #localMock(Class) localMock}, {@link #localNiceMock(Class) localNiceMock}, {@link #localStrictMock(Class) localStrictMock}, {@link
 * #localMockAll(Class, Class[]) localMockAll}</li>
 * </ul> methods.
 * <p/>
 * The <i>first group</i> of methods create mock objects that will be valid in any test method and <i>should be invoked to initialize instance fields</i>,
 * while the <i>second group</i> of methods create mock objects that will be valid only in the calling method and <i>should only be called from test
 * methods</i>.
 * <h3>Usage</h3>
 * <h4>As a super class</h4>
 * <pre>
 * public class MyComponentTest extends <span class="hl1">MockGroup</span> {
 *
 *   private final Dependency <span class="hl2">dependency</span> = <span class="hl1">{@linkplain #mock(Class) mock}</span>(Dependency.class);
 *   private final MyComponent component = new MyComponent(<span class="hl2">dependency</span>);
 *
 *   &#64;Test
 *   public void testSomething() throws Exception {
 *     final Action <span class="hl2">action</span> = <span class="hl1">{@linkplain #localMock(Class) localMock}</span>(Action.class);
 *
 *     {@linkplain EasyMock}.{@linkplain EasyMock#expect(Object) expect}(dependency.<span class="hl2">getSomeAction</span>()).{@linkplain org.easymock.IExpectationSetters#andReturn(Object) andReturn}(<span class="hl2">action</span>);
 *     {@linkplain EasyMock}.{@linkplain EasyMock#expect(Object) expect}(action.<span class="hl2">perform</span>()).{@linkplain org.easymock.IExpectationSetters#andReturn(Object) andReturn}(true);
 *
 *     final boolean success = <span class="hl1">{@linkplain #verify() verify}</span>(new {@linkplain Work Work}&lt;Boolean>() {
 *       public Boolean run() throws Exception {
 *         return component.doSomething();
 *       }
 *     });
 *
 *     assert success;
 *   }
 * }
 * </pre>
 * <h4>As a delegate</h4>
 * <pre>
 * public class MyComponentTest {
 *
 *   private final <span class="hl1">MockGroup</span> group = new <span class="hl1">MockGroup</span>();
 *   private final Dependency <span class="hl2">dependency</span> = group.<span class="hl1">{@linkplain #mock(Class) mock}</span>(Dependency.class);
 *   private final MyComponent component = new MyComponent(<span class="hl2">dependency</span>);
 *
 *   &#64;AfterMethod
 *   public void tearDown() throws Exception {
 *     group.<span class="hl1">{@linkplain #clear() clear}</span>();
 *   }
 *
 *   &#64;Test
 *   public void testSomething() throws Exception {
 *     final Action <span class="hl2">action</span> = group.<span class="hl1">{@linkplain #localMock(Class) localMock}</span>(Action.class);
 *
 *     {@linkplain EasyMock}.{@linkplain EasyMock#expect(Object) expect}(dependency.<span class="hl2">getSomeAction</span>()).{@linkplain org.easymock.IExpectationSetters#andReturn(Object) andReturn}(<span class="hl2">action</span>);
 *     {@linkplain EasyMock}.{@linkplain EasyMock#expect(Object) expect}(action.<span class="hl2">perform</span>()).{@linkplain org.easymock.IExpectationSetters#andReturn(Object) andReturn}(true);
 *
 *     final boolean success = group.<span class="hl1">{@linkplain #verify() verify}</span>(new {@linkplain Work}&lt;Boolean>() {
 *       public Boolean run() throws Exception {
 *         return component.doSomething();
 *       }
 *     });
 *
 *     assert success;
 *   }
 * }
 * </pre>
 *
 * @author Tibor Varga
 */
@SuppressWarnings("UnusedDeclaration")
public class MockGroup {

    private final ControlGroup globalGroup = new ControlGroup();
    private ControlGroup localGroup = new ControlGroup();

    /**
     * Default constructor.
     */
    public MockGroup() { }

    @AfterMethod
    public final void clear() throws Exception {
        localGroup = new ControlGroup();
        reset();
    }

    /**
     * Creates an {@link EasyMock#createMock(Class) ordinary mock object}. The set of mock objects are controlled collectively by the {@link #replay()}, {@link
     * #verify()}, and {@link #reset()} methods.
     *
     * @param interfaceClass the interface to mock.
     * @param <T>            the interface class.
     *
     * @return the mock object for the interface.
     */
    public final <T> T mock(final Class<T> interfaceClass) {
        return globalGroup.mock(interfaceClass);
    }

    /**
     * Creates a {@link EasyMock#createNiceMock(Class) nice mock object}. The set of mock objects are controlled collectively by the {@link #replay()}, {@link
     * #verify()}, and {@link #reset()} methods.
     *
     * @param interfaceClass the interface to mock.
     * @param <T>            the interface class.
     *
     * @return the mock object for the interface.
     */
    public final <T> T niceMock(final Class<T> interfaceClass) {
        return globalGroup.niceMock(interfaceClass);
    }

    /**
     * Creates a {@link EasyMock#createStrictMock(Class) strict mock object}. The set of mock objects are controlled collectively by the {@link #replay()},
     * {@link #verify()}, and {@link #reset()} methods.
     *
     * @param interfaceClass the interface to mock.
     * @param <T>            the interface class.
     *
     * @return the mock object for the interface.
     */
    public final <T> T strictMock(final Class<T> interfaceClass) {
        return globalGroup.strictMock(interfaceClass);
    }

    /**
     * Creates a method local {@link EasyMock#createMock(Class) ordinary mock object} that will be deleted after the method. The set of mock objects are
     * controlled collectively by the {@link #replay()}, {@link #verify()} and {@link #reset()} methods.
     *
     * @param interfaceClass the interface to mock.
     * @param <T>            the interface class.
     *
     * @return the mock object for the interface.
     */
    public final <T> T localMock(final Class<T> interfaceClass) {
        return globalGroup.mock(interfaceClass);
    }

    /**
     * Creates a method local {@link EasyMock#createNiceMock(Class) nice mock object} that will be deleted after the method. The set of mock objects are
     * controlled collectively by the {@link #replay()}, {@link #verify()} and {@link #reset()} methods.
     *
     * @param interfaceClass the interface to mock.
     * @param <T>            the interface class.
     *
     * @return the mock object for the interface.
     */
    public final <T> T localNiceMock(final Class<T> interfaceClass) {
        return globalGroup.niceMock(interfaceClass);
    }

    /**
     * Creates a method local {@link EasyMock#createStrictMock(Class) strict mock object} that will be deleted after the method. The set of mock objects are
     * controlled collectively by the {@link #replay()}, {@link #verify()} and {@link #reset()} methods.
     *
     * @param interfaceClass the interface to mock.
     * @param <T>            the interface class.
     *
     * @return the mock object for the interface.
     */
    public final <T> T localStrictMock(final Class<T> interfaceClass) {
        return globalGroup.strictMock(interfaceClass);
    }

    /**
     * Creates several {@link EasyMock#createMock(Class) ordinary mock objects}, one for each interface given, and combines them into one mock object that
     * implements all of the given interfaces. The returned mock object can be cast to any of the interface types that were given as method arguments.
     * <p/>
     * The set of mock objects are controlled collectively by the {@link #replay()}, {@link #verify()} and {@link #reset()} methods.
     * <p/>
     * Note: interface visibility may make it impossible to combine the mock objects.
     *
     * @param <T>             the interface class.
     * @param mainInterface   the interface to use as a reference and also the on that determines the return type.
     * @param otherInterfaces the other interfaces to mock.
     *
     * @return the mock object for the interface.
     */
    @SuppressWarnings("unchecked")
    public final <T> T mockAll(final Class<T> mainInterface, final Class<?>... otherInterfaces) {
        return globalGroup.mockAll(mainInterface, otherInterfaces);
    }

    /**
     * Creates several method local {@link EasyMock#createMock(Class) ordinary mock objects}, one for each interface given, and combines them into one mock
     * object that implements all of the given interfaces. The returned mock object can be cast to any of the interface types that were given as method
     * argument. The mock objects will be deleted at the end of the calling method.
     * <p/>
     * The set of mock objects are controlled collectively by the {@link #replay()}, {@link #verify()} and {@link #reset()} methods.
     * <p/>
     * Note: interface visibility may make it impossible to combine the mock objects.
     *
     * @param <T>             the interface class.
     * @param mainInterface   the interface to use as a reference and also the on that determines the return type.
     * @param otherInterfaces the other interfaces to mock.
     *
     * @return the mock object for the interface.
     */
    @SuppressWarnings("unchecked")
    public final <T> T localMockAll(final Class<T> mainInterface, final Class<?>... otherInterfaces) {
        return globalGroup.mockAll(mainInterface, otherInterfaces);
    }

    /**
     * Command to feed to {@link MockGroup#test(MockGroup.Work)}, {@link MockGroup#verify(MockGroup.Work)}, or {@link MockGroup.Threads#verify(long,
     * MockGroup.Work)}.
     * <h3>Usage</h3>
     * See {@link MockGroup}.
     *
     * @author Tibor Varga
     */
    public interface Work<T> extends Command.Process<T, Exception> { }

    /**
     * Command to feed to {@link MockGroup#test(MockGroup.Task)}, {@link MockGroup#verify(MockGroup.Task)}, or {@link MockGroup.Threads#verify(long,
     * MockGroup.Task)}, {@link MockGroup.Threads#serial(MockGroup.Task)}, and {@link MockGroup.Threads#concurrent(MockGroup.Task)}.
     * <h3>Usage</h3>
     * See {@link MockGroup}.
     *
     * @author Tibor Varga
     */
    public interface Task extends Command.Job<Exception> { }

    /**
     * Executes the given block and returns whatever it returns. Useful for segmenting steps in a sequence of tests.
     *
     * @param block the block to execute.
     * @param <T>   the type of the block's return value.
     *
     * @return whatever the <code>block</code> returns.
     *
     * @throws Exception when some error occurs.
     */
    public final <T> T test(final Work<T> block) throws Exception {
        return block.run();
    }

    /**
     * Executes the given block and returns whatever it returns. Useful for segmenting steps in a sequence of tests.
     *
     * @param block the block to execute.
     *
     * @throws Exception when some error occurs.
     */
    public final void test(final Task block) throws Exception {
        block.run();
    }

    /**
     * Invokes {@link #replay()}, executes the block, and then, unless the block throws an exception, invokes {@link #verify()}, and returns whatever the block
     * returns.
     *
     * @param block the block to execute.
     * @param <T>   the type of the block's return value.
     *
     * @return whatever the <code>block</code> returns.
     *
     * @throws Exception when some error occurs.
     */
    public final <T> T verify(final Work<T> block) throws Exception {
        replay();
        final T result = block.run();
        verify();
        return result;
    }
    /**
     * Invokes {@link #replay()}, executes the block, and then, unless the block throws an exception, invokes {@link #verify()}.
     *
     * @param block the block to execute.
     *
     * @throws Exception when some error occurs.
     */
    public final void verify(final Task block) throws Exception {
        replay();
        block.run();
        verify();
    }

    /**
     * Calls {@link IMocksControl#replay()} on all mock objects added to the group. Mock objects are added by calling {@link #mock(Class)}, {@link
     * #niceMock(Class)}, {@link #strictMock(Class)}, and {@link #mockAll(Class, Class[])}, and their <code>local</code> counterparts.
     */
    public final void replay() {
        globalGroup.replay();
        localGroup.replay();
    }

    /**
     * Calls {@link IMocksControl#verify()} on all mock objects added to the group. Mock objects are added by calling {@link #mock(Class)}, {@link
     * #niceMock(Class)}, {@link #strictMock(Class)}, and {@link #mockAll(Class, Class[])}, and their <code>local</code> counterparts.
     */
    public final void verify() {
        globalGroup.verify();
        localGroup.verify();
    }

    /**
     * Calls {@link IMocksControl#reset()} on all mock objects added to the group. Mock objects are added by calling {@link #mock(Class)}, {@link
     * #niceMock(Class)}, {@link #strictMock(Class)}, and {@link #mockAll(Class, Class[])}, and their <code>local</code> counterparts.
     */
    public final void reset() {
        globalGroup.reset();
        localGroup.reset();
    }

    /**
     * Argument matcher that understands variadic arguments and asserts that the given expected values are {@link EasyMock#eq(Object) equal} to the method
     * parameters at the matched invocation.
     *
     * @param expected the array of expected values.
     *
     * @return the method parameter.
     */
    public final <T> T[] varEq(final T... expected) {
        for (final T value : expected) {
            EasyMock.reportMatcher(new Equals(value));
        }

        return expected;
    }

    /**
     * Argument matcher that understands variadic arguments and asserts that the given expected values are the {@link EasyMock#same(Object) same} as the method
     * parameters at the matched invocation.
     *
     * @param expected the array of expected values.
     *
     * @return the method parameter.
     */
    public final <T> T[] varSame(final T... expected) {
        for (final T value : expected) {
            EasyMock.reportMatcher(new Same(value));
        }

        return expected;
    }

    /**
     * Argument matcher that understands variadic arguments and asserts that the method parameter list at the matched invocation consists of
     * <code>expected.length</code> number of non-<code>null</code> values.
     *
     * @param expected an array of <code>null</code> values, of the expected size.
     *
     * @return the method parameter.
     */
    public final <T> T[] varNotNull(final T... expected) {
        for (final T value : expected) {
            EasyMock.reportMatcher(NotNull.NOT_NULL);
        }

        return expected;
    }

    /**
     * Creates a new {@link Threads} object. The returned object can be used to create concurrent test cases.
     *
     * @param name the text to use in thread names to distinguish threads created by the returned {@link Threads} instance from other instances.
     *
     * @return a new {@link Threads} instance; never <code>null</code>.
     */
    public final Threads newThreads(final String name) {
        return new ThreadsImpl(name);
    }

    /**
     * Allows test cases to manage interacting threads. Threads are created by the {@link #concurrent(MockGroup.Task) concurrent()} method and started /
     * stopped by the {@link #verify(long, MockGroup.Task)} and {@link #verify(long, MockGroup.Work)} methods. Threads synchronize on {@linkplain CyclicBarrier
     * barriers} using the {@link #coalesce(CyclicBarrier, long) coalesce()}.
     * <p/>
     * {@link EasyMock} calls <b>must</b> always run in the main thread.
     * <h3>Usage</h3>
     * <pre>
     * &#64;Test
     * public void testConcurrency() throws Exception {
     *   final <span class="hl1">Threads</span> threads = {@linkplain MockGroup#newThreads(String) newThreads}("Concurrency");
     *
     *   // barrier initialized with number of threads to synchronize on:
     *   final {@linkplain CyclicBarrier} barrier = new {@linkplain CyclicBarrier#CyclicBarrier(int) CyclicBarrier}(2);
     *
     *   &hellip;
     *
     *   threads.<span class="hl1">serial</span>(new {@linkplain MockGroup.Task Task}() {
     *     public void run() throws Exception {
     *       &hellip;
     *       thread.<span class="hl1">coalesce</span>(barrier, 100);
     *       &hellip;
     *       thread.<span class="hl1">coalesce</span>(barrier, 100);
     *       &hellip;
     *     }
     *   });
     *
     *   &hellip;
     *
     *   threads.<span class="hl1">concurrent</span>(new {@linkplain MockGroup.Task Task}() {
     *     public void run() throws Exception {
     *       &hellip;
     *       thread.<span class="hl1">coalesce</span>(barrier, 100);
     *       &hellip;
     *       thread.<span class="hl1">coalesce</span>(barrier, 100);
     *       &hellip;
     *     }
     *   });
     *
     *   &hellip;
     *
     *   threads.<span class="hl1">verify</span>(500, new {@linkplain MockGroup.Task Task}() {
     *     &hellip;
     *   });
     * }
     * </pre>
     *
     * @author Tibor Varga
     */
    public interface Threads {

        /**
         * Executes in the calling thread the given task. This is just a convenience method to visually separate the execution of the <code>task</code> from
         * other code sequences.
         * <p/>
         * This method may be invoked from a task submitted to {@link #concurrent(MockGroup.Task)}.
         *
         * @param task    the task to execute; if <code>null</code>, no code is executed between the two barriers, if any.
         *
         * @throws Exception whatever the <code>task</code> throws.
         */
        void serial(Task task) throws Exception;

        /**
         * Creates a new thread to execute the given task with.
         * <p/>
         * This method may <i>not</i> be invoked from a task submitted to another invocation of the same method.
         *
         * @param task the task to execute in a new thread.
         *
         * @throws Exception whatever the <code>task</code> throws.
         */
        void concurrent(Task task) throws Exception;

        /**
         * Causes all threads calling the same method with the same barrier to synchronize at the point of calling this method. Calls to this method must be
         * paired with one or more invocation in different threads with the same barrier.
         *
         * @param barrier the barrier to synchronize on.
         * @param timeout the maximum time to wait for other threads to coalesce.
         *
         * @return the arrival index of the calling thread.
         *
         * @throws Exception when something goes wrong during synchronization.
         */
        int coalesce(CyclicBarrier barrier, long timeout) throws Exception;

        /**
         * Releases all threads created by {@link #concurrent(MockGroup.Task)}, invokes {@link MockGroup#verify(MockGroup.Task)}, and then waits, with the
         * given timeout, for all threads to complete.
         * <p/>
         * This method may <i>not</i> be invoked from a task submitted to {@link #concurrent(MockGroup.Task)}.
         *
         * @param timeout the timeout to wait for all threads to complete.
         * @param task    the task to verify.
         *
         * @throws Exception whatever the <code>task</code> throws.
         */
        void verify(long timeout, Task task) throws Exception;

        /**
         * Releases all threads created by {@link #concurrent(MockGroup.Task)}, invokes {@link MockGroup#verify(MockGroup.Work)}, waits, with the given
         * timeout, for all threads to complete, and returns whatever the <code>task</code> returned.
         * <p/>
         * This method may <i>not</i> be invoked from a task submitted to {@link #concurrent(MockGroup.Task)}.
         *
         * @param timeout the timeout to wait for all threads to complete.
         * @param task    the task to verify.
         *
         * @return whatever the <code>task</code> returns.
         *
         * @throws Exception whatever the <code>task</code> throws.
         */
        <T> T verify(long timeout, Work<T> task) throws Exception;
    }

    /**
     * @author Tibor Varga
     */
    private final class ThreadsImpl implements Threads {

        private final AtomicBoolean error = new AtomicBoolean();
        private final List<Thread> threads = new ArrayList<Thread>();
        private final List<Exception> errors = new ArrayList<Exception>();
        private CountDownLatch latch;

        private ThreadsImpl(final String name) {
            this.name = name;
        }

        private final String name;

        public void serial(final Task task) throws Exception {
            task.run();
        }

        public int coalesce(final CyclicBarrier barrier, final long timeout) throws Exception {
            int result = -1;

            try {
                result = barrier.await(timeout, TimeUnit.MILLISECONDS);
            } finally {
                error.compareAndSet(false, result < 0);
            }


            return result;
        }

        public synchronized void concurrent(final Task task) throws Exception {
            assert task != null;
            assert latch == null : String.format("Calls to %s.concurrent(...) may not be nested", Strings.printClass(false, false, MockGroup.Threads.class));
            threads.add(new Thread(String.format("%s %s [%d]", MockGroup.class.getSimpleName(), name, threads.size())) {
                public void run() {
                    try {
                        task.run();
                    } catch (final Exception e) {
                        synchronized (errors) {
                            errors.add(e);
                        }
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }

        private synchronized void release() {
            latch = new CountDownLatch(threads.size());
            errors.clear();

            try {
                for (final Thread thread : threads) {
                    thread.start();
                }
            } finally {
                threads.clear();
            }
        }

        private synchronized void join(final long timeout) {
            try {
                try {
                    latch.await(timeout, TimeUnit.MILLISECONDS);
                } catch (final Exception e) {
                    synchronized (errors) {
                        errors.add(e);
                    }
                }

                for (final Exception error : errors) {
                    error.printStackTrace();
                }

                assert errors.isEmpty();
            } finally {
                errors.clear();
            }
        }

        public final void verify(final long timeout, final Task task) throws Exception {
            assert latch == null : String.format("Calls to %s.verify(...) may not be nested", Strings.printClass(false, false, MockGroup.Threads.class));
            release();

            try {
                MockGroup.this.verify(task);
            } finally {
                join(timeout);
            }

            assert !error.get() : "Concurrency/timing error";
        }

        public <T> T verify(final long timeout, final Work<T> task) throws Exception {
            assert latch == null : String.format("Calls to %s.verify(...) may not be nested", Strings.printClass(false, false, MockGroup.Threads.class));
            release();

            final T result;
            try {
                result = MockGroup.this.verify(task);
            } finally {
                join(timeout);
            }

            assert !error.get() : "Concurrency/timing error";

            return result;
        }
    }

    /**
     * A group of mock objects.
     *
     * @author Tibor Varga
     */
    private static class ControlGroup {

        public final IMocksControl group = EasyMock.createControl();
        public final IMocksControl niceGroup = EasyMock.createNiceControl();
        public final IMocksControl strictGroup = EasyMock.createStrictControl();

        public final List<IMocksControl> groups = new ArrayList<IMocksControl>(Arrays.asList(strictGroup, group, niceGroup));

        ControlGroup() {
            for (final IMocksControl control : groups) {
                control.makeThreadSafe(true);
            }
        }

        /**
         * Adds an {@link EasyMock#createMock(Class) ordinary mock object} to the group. The set of mock objects added to the group are controlled collectively
         * by the {@link #replay()}, {@link #verify()} and {@link #reset()} methods.
         *
         * @param interfaceClass the interface to mock.
         * @param <T>            the interface class.
         *
         * @return the mock object for the interface.
         */
        protected final <T> T mock(final Class<T> interfaceClass) {
            return group.createMock(interfaceClass);
        }

        /**
         * Adds a {@link EasyMock#createNiceMock(Class) nice mock object} to the group. The set of mock objects added to the group are controlled collectively
         * by the {@link #replay()}, {@link #verify()} and {@link #reset()} methods.
         *
         * @param interfaceClass the interface to mock.
         * @param <T>            the interface class.
         *
         * @return the mock object for the interface.
         */
        protected final <T> T niceMock(final Class<T> interfaceClass) {
            return niceGroup.createMock(interfaceClass);
        }

        /**
         * Adds a {@link EasyMock#createStrictMock(Class) strict mock object} to the group. The set of mock objects added to the group are controlled
         * collectively by the {@link #replay()}, {@link #verify()} and {@link #reset()} methods.
         *
         * @param interfaceClass the interface to mock.
         * @param <T>            the interface class.
         *
         * @return the mock object for the interface.
         */
        protected final <T> T strictMock(final Class<T> interfaceClass) {
            return strictGroup.createMock(interfaceClass);
        }

        /**
         * Creates several {@link EasyMock#createMock(Class) ordinary mock objects}, one for each interface given and combines them into one mock object that
         * implements all of the given interfaces. The returned mock object can be cast to any of the interface types that were given as method argument. The
         * mock object is then added to the group.
         * <p/>
         * The set of mock objects added to the group are controlled collectively by the {@link #replay()}, {@link #verify()} and {@link #reset()} methods.
         * <p/>
         * Note: interface visibility may make it impossible to combine the mock objects.
         *
         * @param <T>             the interface class.
         * @param mainInterface   the interface to use as a reference and also the on that determines the return type.
         * @param otherInterfaces the other interfaces to mock.
         *
         * @return the mock object for the interface.
         */
        @SuppressWarnings("unchecked")
        protected final <T> T mockAll(final Class<T> mainInterface, final Class<?>... otherInterfaces) {
            if (otherInterfaces.length == 0) {
                return mock(mainInterface);
            } else {
                final Class<?> interfaces[] = new Class<?>[otherInterfaces.length + 1];

                interfaces[0] = mainInterface;
                System.arraycopy(otherInterfaces, 0, interfaces, 1, otherInterfaces.length);

                final Map<Class<?>, Object> proxyMap = new LinkedHashMap<Class<?>, Object>();

                for (Class<?> api : interfaces) {
                    assert !proxyMap.containsKey(api) : api;
                    proxyMap.put(api, mock(api));
                }

                return (T) Proxy.newProxyInstance(mainInterface.getClassLoader(), interfaces, new InvocationHandler() {
                    public Object invoke(final Object object, final Method method, final Object[] args) throws Throwable {
                        final Class<?> owner = method.getDeclaringClass();
                        Object proxy = proxyMap.get(owner);

                        if (proxy == null) {
                            for (final Class<?> api : interfaces) {
                                if (owner.isAssignableFrom(api)) {
                                    proxyMap.put(owner, proxy = proxyMap.get(api));
                                }
                            }
                        }

                        assert proxy != null : method;
                        try {
                            return method.invoke(proxy, args);
                        } catch (final InvocationTargetException e) {
                            throw e.getCause();
                        }
                    }
                });
            }
        }

        /**
         * Calls {@link IMocksControl#replay()} on all mock objects added to the group. Mock objects are added by calling {@link #mock(Class)}, {@link
         * #niceMock(Class)}, {@link #strictMock(Class)} and {@link #mockAll(Class, Class[])}.
         */
        protected final void replay() {
            for (final IMocksControl group : groups) {
                group.replay();
            }
        }

        /**
         * Calls {@link IMocksControl#verify()} on all mock objects added to the group. Mock objects are added by calling {@link #mock(Class)}, {@link
         * #niceMock(Class)}, {@link #strictMock(Class)} and {@link #mockAll(Class, Class[])}.
         */
        protected final void verify() {
            for (final IMocksControl group : groups) {
                try {
                    group.verify();
                } finally {
                    try {
                        group.reset();
                    } catch (final Throwable e) {
                        // ignore
                    }
                }
            }
        }

        /**
         * Calls {@link IMocksControl#reset()} on all mock objects added to the group. Mock objects are added by calling {@link #mock(Class)}, {@link
         * #niceMock(Class)}, {@link #strictMock(Class)} and {@link #mockAll(Class, Class[])}.
         */
        protected final void reset() {
            for (final IMocksControl group : groups) {
                group.reset();
            }
        }
    }
}

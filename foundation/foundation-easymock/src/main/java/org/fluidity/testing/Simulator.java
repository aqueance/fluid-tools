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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.foundation.Command;
import org.fluidity.foundation.Proxies;
import org.fluidity.foundation.Strings;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.easymock.internal.matchers.Equals;
import org.easymock.internal.matchers.NotNull;
import org.easymock.internal.matchers.Same;
import org.testng.annotations.AfterMethod;

/**
 * Facilitates the use of multiple <a href="http://www.easymock.org/">EasyMock</a> control objects. There are two ways to use this class: as a super class
 * (preferred mode), or as a delegate if inheritance is not an option.
 * <p/>
 * When used as a super class but with a test harness other than TestNG, or when used as a delegate, the test class must call the {@link #clear()}
 * method after every test method. When used as a super class of a TestNG based test class, that method will be <i>automatically</i> invoked after every test
 * method.
 * <p/>
 * Mock objects are produced by instances of the {@link Simulator.MockObjects} interface. There are three types of mock objects:<ul>
 * <li>{@linkplain MockObjects#normal(Class) normal} mock objects that accept only expected methods,</li>
 * <li>{@linkplain MockObjects#lenient(Class) lenient} mock objects that accept any method, expected or not, and returns default values from unexpected
 * ones,</li>
 * <li>{@linkplain MockObjects#ordered(Class) ordered} mock objects that accept only expected methods in the expected order</li>
 * </ul>
 * Mock objects can be used in two distinct contexts:<ul>
 * <li>as dependencies of the object under test, created by the {@link MockObjects} factory returned by {@link #dependencies()}, or</li>
 * <li>as temporary method local objects as, e.g., method arguments of the object under test, created by the {@link MockObjects} factory returned by
 * {@link #arguments()}.</li>
 * </ul>
 * <p/>
 * The factory returned by the {@link #dependencies()} method creates mock objects that will be valid in any test method and <i>should be invoked to initialize
 * instance fields</i> of the test and passed to the object under test, while the factory returned by the {@link #arguments()} method creates mock objects that
 * will be valid only in the calling method and thus <i>can only be used in that method</i>.
 * <h3>Usage</h3>
 * <h4>As a super class</h4>
 * <pre>
 * public class MyComponentTest extends <span class="hl1">Simulator</span> {
 *
 *   private final Dependency <span class="hl2">dependency</span> = <span class="hl1">{@linkplain #dependencies() dependencies}</span>().<span class="hl1">{@linkplain MockObjects#normal(Class) normal}</span>(Dependency.class);
 *   private final MyComponent component = new MyComponent(<span class="hl2">dependency</span>);
 *
 *   &#64;Test
 *   public void testSomething() throws Exception {
 *     final Action <span class="hl2">action</span> = <span class="hl1">{@linkplain #arguments() arguments}</span>().<span class="hl1">{@linkplain MockObjects#normal(Class) normal}</span>(Action.class);
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
 *     assert success : "No success";
 *   }
 * }
 * </pre>
 * <h4>As a delegate</h4>
 * <pre>
 * public class MyComponentTest {
 *
 *   private final <span class="hl1">Simulator</span> simulator = new <span class="hl1">Simulator</span>();
 *   private final Dependency <span class="hl2">dependency</span> = simulator.<span class="hl1">{@linkplain #dependencies() dependencies}</span>().<span class="hl1">{@linkplain MockObjects#normal(Class) normal}</span>(Dependency.class);
 *   private final MyComponent component = new MyComponent(<span class="hl2">dependency</span>);
 *
 *   &#64;AfterMethod
 *   public void tearDown() throws Exception {
 *     simulator.<span class="hl1">{@linkplain #clear() clear}</span>();
 *   }
 *
 *   &#64;Test
 *   public void testSomething() throws Exception {
 *     final Action <span class="hl2">action</span> = simulator.<span class="hl1">{@linkplain #arguments() arguments}</span>().<span class="hl1">{@linkplain MockObjects#normal(Class) normal}</span>(Action.class);
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
 *     assert success : "No success";
 *   }
 * }
 * </pre>
 *
 * @author Tibor Varga
 */
@SuppressWarnings("UnusedDeclaration")
public class Simulator {

    private final MockControl dependencies = new ControlGroup();
    private final Collection<MockControl> transients = new ArrayList<MockControl>();

    private MockControl arguments = newLocalObjects();    // must be defined after groups have been initialized

    final AtomicReference<CompositeFactory> locals = new AtomicReference<CompositeFactory>();

    /**
     * Default constructor; does nothing.
     */
    public Simulator() { }

    /**
     * Returns a {@link MockObjects} factory to create mock objects to be used as constructor/field dependencies of the object under test. Mock objects created
     * by the returned factory will be {@linkplain EasyMock#reset(Object...) reset} after each method.
     *
     * @return a {@link MockObjects} object.
     */
    public final MockObjects dependencies() {
        return dependencies;
    }

    /**
     * Returns a {@link MockObjects} factory to create method local mock objects to be used, e.g., as method arguments. Method local mock objects will be
     * discarded after the test method in which they were created completes.
     *
     * @return a {@link MockObjects} object.
     */
    public final MockObjects arguments() {
        return arguments;
    }

    /**
     * Returns a new {@link MockObjects} factory to create mock objects with for concurrent access. Independent mock objects will be discarded after the test
     * method in which they were created completes.
     *
     * @return a {@link MockObjects} object.
     */
    public final MockObjects concurrent() {
        return newLocalObjects();
    }

    /**
     * Called after each test method to clear method and thread local mock objects.
     *
     * @throws Exception
     */
    @AfterMethod
    public final synchronized void clear() throws Exception {
        dependencies.reset();
        transients.clear();
        arguments = newLocalObjects();
    }

    /**
     * Encapsulates a sequence of code, to execute as part of some test case, that returns some value.
     * <h3>Usage</h3>
     * See {@link Simulator}.
     *
     * @param <T> the type of the object returned by the implementing closure.
     *
     * @author Tibor Varga
     */
    public interface Work<T> extends Command.Process<T, Exception> { }

    /**
     * Encapsulates a sequence of code to execute as part of some test case.
     * <h3>Usage</h3>
     * See {@link Simulator}.
     *
     * @author Tibor Varga
     */
    public interface Task extends Command.Job<Exception> {

        /**
         * A factory of concurrent tasks, to be used with {@link Threads#concurrent(Simulator.Task.Concurrent)}.
         *
         * @author Tibor Varga
         */
        interface Concurrent extends Command.Function<Task, MockObjects, Exception> {

            /**
             * Creates mock objects using the supplied <code>factory</code>, sets up expectations therewith, and then returns a task that will, in a separate
             * thread, exercise the object under test to meet those expectations.
             *
             * @param factory is the mock object factory to create mock objects with.
             *
             * @return a task to run concurrently.
             */
            Task run(MockObjects factory) throws Exception;
        }
    }

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
     * Invokes {@link #replay()}, executes the block, and then, unless the block throws an exception, invokes {@link #verify()}.
     *
     * @param block the block to execute.
     *
     * @throws Exception when some error occurs.
     */
    public final void verify(final Task block) throws Exception {
        replay();
        if (block != null) {
            block.run();
        }
        verify();
    }

    /**
     * Invokes {@link #replay()}, executes the block, and then, regardless of whether the block throws an exception or not, invokes {@link #verify()}.
     *
     * @param block the block to execute.
     *
     * @throws Exception when some error occurs.
     */
    public final void guarantee(final Task block) throws Exception {
        replay();

        try {
            if (block != null) {
                block.run();
            }
        } finally {
            verify();
        }
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
        final T result = block == null ? null : block.run();
        verify();
        return result;
    }

    /**
     * Invokes {@link #replay()}, executes the block, and then, regardless of whether the block throws an exception or not, invokes {@link #verify()}, and,
     * unless the block threw an exception, returns whatever the block returns.
     *
     * @param block the block to execute.
     * @param <T>   the type of the block's return value.
     *
     * @return whatever the <code>block</code> returns.
     *
     * @throws Exception when some error occurs.
     */
    public final <T> T guarantee(final Work<T> block) throws Exception {
        replay();
        try {
            return block == null ? null : block.run();
        } finally {
            verify();
        }
    }

    private synchronized void replay() {
        dependencies.replay();
        for (final MockControl control : transients) {
            control.replay();
        }
    }

    private synchronized void verify() {
        dependencies.verify();
        for (final MockControl control : transients) {
            control.verify();
        }
    }

    private synchronized void reset() {
        dependencies.reset();
        for (final MockControl control : transients) {
            control.reset();
        }
    }

    /**
     * Argument matcher that understands variadic arguments and asserts that the given expected values are {@link EasyMock#eq(Object) equal} to the method
     * parameters at the matched invocation.
     *
     * @param expected the array of expected values.
     * @param <T>      the component type of the expected array.
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
     * @param <T>      the component type of the expected array.
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
     * @param <T>      the component type of the expected array.
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
     * Creates a composite mock object using the given mock object for the <i>main</i> thread, which must be the current thread. The composite mock object
     * can then be used to create thread local mock objects for the same dependency using the {@link Simulator.Composite#normal()}, {@link
     * Simulator.Composite#lenient()}, and {@link Simulator.Composite#ordered()} methods.
     *
     * @param type the dependency class to mock.
     * @param mock the mock object created for the main thread.
     * @param <T>  the type of the mocked dependency.
     *
     * @return a composite mock object suitable for multi-threaded access.
     */
    public final <T> Composite<T> composite(final Class<T> type, final T mock) {
        final ThreadLocal<T> local = new ThreadLocal<T>();

        local.set(mock);

        final T proxy = Proxies.create(type, new InvocationHandler() {
            public Object invoke(final Object proxy, final Method method, final Object[] arguments) throws Throwable {
                final T delegate = local.get();

                if (delegate == null) {
                    throw new IllegalStateException(String.format("No concurrent mock created for %1$s; use MockObjects.Concurrent.*mock(Class<%1$s>, Composite<%1$s>) or Composite<%1$s>.use(%1$s)",
                                                                  Strings.formatClass(false, false, type)));
                } else {
                    return method.invoke(delegate, arguments);
                }
            }
        });

        return new Composite<T>() {
            public void use(final T mock) {
                local.set(mock);
            }

            public T normal() {
                return factory("normal").normal(type, this);
            }

            public T lenient() {
                return factory("lenient").lenient(type, this);
            }

            public T ordered() {
                return factory("ordered").ordered(type, this);
            }

            public T dependency() {
                return proxy;
            }

            private CompositeFactory factory(final String method) {
                final CompositeFactory factory = locals.get();

                if (factory == null) {
                    throw new IllegalStateException(String.format("%s.%s() can only be invoked from %s.concurrent(...)",
                                                                  Strings.formatClass(false, true, Composite.class),
                                                                  method,
                                                                  Strings.formatClass(false, true, Threads.class)));
                }

                return factory;
            }
        };
    }

    private synchronized MockControl newLocalObjects() {
        final MockControl control = new ControlGroup();
        transients.add(control);
        return control;
    }

    /**
     * Creates mock objects.
     *
     * @author Tibor Varga
     */
    public interface MockObjects {

        /**
         * Creates a {@link EasyMock#createMock(Class) normal mock object}.
         *
         * @param interfaceClass the interface to mock.
         * @param <T>            the type to mock.
         *
         * @return the mock object for the interface.
         */
        <T> T normal(Class<T> interfaceClass);

        /**
         * Creates a {@link EasyMock#createNiceMock(Class) nice mock object}.
         *
         * @param interfaceClass the interface to mock.
         * @param <T>            the type to mock.
         *
         * @return the mock object for the interface.
         */
        <T> T lenient(Class<T> interfaceClass);

        /**
         * Creates a {@link EasyMock#createStrictMock(Class) strict mock object}.
         *
         * @param interfaceClass the interface to mock.
         * @param <T>            the type to mock.
         *
         * @return the mock object for the interface.
         */
        <T> T ordered(Class<T> interfaceClass);
    }

    /**
     * Allows test cases to manage interacting threads. Threads are created by the {@link #concurrent(Simulator.Task.Concurrent) concurrent()} method and
     * started / stopped by the {@link #verify(long, Simulator.Task)}, {@link #verify(long, Simulator.Work)}, {@link #guarantee(long, Simulator.Task)}, or
     * {@link #guarantee(long, Simulator.Work)} method.
     * <p/>
     * Threads synchronize on {@linkplain CyclicBarrier barriers} using the {@link #lineup lineup()} method and relative time can be given using {@link
     * #time(float)}.
     * <h3>Usage</h3>
     * <pre>
     * &#64;Test
     * public void testConcurrency() throws Exception {
     *   final <span class="hl1">Threads</span> threads = {@linkplain Simulator#newThreads(String) newThreads}("Concurrency");
     *
     *   // barrier initialized with number of threads to synchronize on:
     *   final {@linkplain CyclicBarrier} barrier = new {@linkplain CyclicBarrier#CyclicBarrier(int) CyclicBarrier}(2);
     *
     *   &hellip;
     *
     *   threads.<span class="hl1">concurrent</span>(new {@linkplain Simulator.Task.Concurrent Task.Concurrent}() {
     *     public {@linkplain Simulator.Task Task} run(final {@link Simulator.MockObjects MockObjects} factory) throws Exception {
     *       return new {@linkplain Simulator.Task Task}() {
     *         public void run() throws Exception {
     *           &hellip;
     *           thread.<span class="hl1">lineup</span>(barrier, threads.<span class="hl1">time</span>(10));
     *           &hellip;
     *           thread.<span class="hl1">lineup</span>(barrier, threads.<span class="hl1">time</span>(10));
     *           &hellip;
     *         }
     *       }
     *     }
     *   });
     *
     *   &hellip;
     *
     *   threads.<span class="hl1">verify</span>(threads.<span class="hl1">time</span>(30), new {@linkplain Simulator.Task Task}() {
     *     public void run() throws Exception {
     *       &hellip;
     *       thread.<span class="hl1">lineup</span>(barrier, threads.<span class="hl1">time</span>(10));
     *       &hellip;
     *       thread.<span class="hl1">lineup</span>(barrier, threads.<span class="hl1">time</span>(10));
     *       &hellip;
     *     }
     *   });
     * }
     * </pre>
     *
     * @author Tibor Varga
     */
    public interface Threads {

        /**
         * The system variable that forms the base value used for {@linkplain #time(float) adjusting} {@linkplain Threads thread} synchronization timing. The
         * default base value is 10 ms.
         */
        String TIMING_BASE = "timing.base.ms";

        /**
         * Returns an adjusted value to be used for timing. The value can be adjusted by the {@link #TIMING_BASE} system variable.
         *
         * @param factor the time in milliseconds to adjust.
         *
         * @return the adjusted time value.
         */
        long time(float factor);

        /**
         * Executes in the calling thread the given task. This method is merely a visual aid to separate the execution of the <code>task</code> from other code
         * sequences.
         * <p/>
         * This method may be invoked from a task submitted to {@link #concurrent(Simulator.Task.Concurrent)}.
         *
         * @param task    the task to execute; if <code>null</code>, no code is executed between the two barriers, if any.
         *
         * @throws Exception whatever the <code>task</code> throws.
         */
        void serial(Task task) throws Exception;

        /**
         * Creates a new thread to execute a concurrent task with, and a new {@link Simulator.MockObjects} to create mock objects with for use by the concurrent
         * task. The specified <code>task</code> creates and sets up mock objects and returns a {@link Simulator.Task} that uses those mock objects.
         * <p/>
         * This method may <i>not</i> be invoked from a task submitted to another invocation of the <code>concurrent(...)</code>, <code>verify(...)</code>, or
         * <code>guarantee(...)</code> methods.
         *
         *
         * @param task the task to execute in a new thread.
         *
         * @throws Exception whatever the <code>task</code> throws.
         */
        void concurrent(Task.Concurrent task) throws Exception;

        /**
         * Causes all threads calling the same method with the same barrier to synchronize at the point of calling this method. Invocations of method must be
         * accompanied with {@link CyclicBarrier#getParties()} number of invocation in all different threads with the same barrier for the caller threads to
         * pass the call without error.
         *
         * @param barrier the barrier to synchronize on.
         * @param timeout the maximum time to wait for other threads to coalesce.
         *
         * @return the arrival index of the calling thread.
         *
         * @throws Exception when something goes wrong during synchronization.
         */
        int lineup(CyclicBarrier barrier, long timeout) throws Exception;

        /**
         * Releases all threads created by {@link #concurrent(Simulator.Task.Concurrent)}, invokes {@link Simulator#verify(Simulator.Task)}, and then waits, for the
         * given timeout, for all threads to complete.
         * <p/>
         * This method may <i>not</i> be invoked from a task submitted to {@link #concurrent(Simulator.Task.Concurrent)}.
         *
         * @param timeout the timeout to wait for all threads to complete.
         * @param task    the task to verify.
         *
         * @throws Exception whatever the <code>task</code> throws.
         */
        void verify(long timeout, Task task) throws Exception;

        /**
         * Releases all threads created by {@link #concurrent(Simulator.Task.Concurrent)}, invokes {@link Simulator#guarantee(Simulator.Task)}, and then waits, with the
         * given timeout, for all threads to complete.
         * <p/>
         * This method may <i>not</i> be invoked from a task submitted to {@link #concurrent(Simulator.Task.Concurrent)}.
         *
         * @param timeout the timeout to wait for all threads to complete.
         * @param task    the task to verify.
         *
         * @throws Exception whatever the <code>task</code> throws.
         */
        void guarantee(long timeout, Task task) throws Exception;

        /**
         * Releases all threads created by {@link #concurrent(Simulator.Task.Concurrent)}, invokes {@link Simulator#verify(Simulator.Work)}, waits, for the given
         * timeout, for all threads to complete, and returns whatever the <code>block</code> returned.
         * <p/>
         * This method may <i>not</i> be invoked from a block submitted to {@link #concurrent(Simulator.Task.Concurrent)}.
         *
         * @param timeout the timeout to wait for all threads to complete.
         * @param block   the block containing the calls to verify.
         * @param <T>     the type returned by the block.
         *
         * @return whatever the <code>task</code> returns.
         *
         * @throws Exception whatever the <code>task</code> throws.
         */
        <T> T verify(long timeout, Work<T> block) throws Exception;

        /**
         * Releases all threads created by {@link #concurrent(Simulator.Task.Concurrent)}, invokes {@link Simulator#guarantee(Simulator.Work)}, waits, for the given
         * timeout, for all threads to complete, and returns whatever the <code>block</code> returned.
         * <p/>
         * This method may <i>not</i> be invoked from a block submitted to {@link #concurrent(Simulator.Task.Concurrent)}.
         *
         * @param timeout the timeout to wait for all threads to complete.
         * @param block   the block containing the calls to verify.
         * @param <T>     the type returned by the block.
         *
         * @return whatever the <code>block</code> returns.
         *
         * @throws Exception whatever the <code>block</code> throws.
         */
        <T> T guarantee(long timeout, Work<T> block) throws Exception;
    }

    /**
     * Composite mock object. A composite is used in multi-threaded tests to create thread specific mock objects of some dependency of the object under test.
     * <p/>
     * The composite is created in the <i>main</i> thread for each dependency of the object under test that will be invoked concurrently, and a composite mock
     * object, returned by the {@link #dependency()} method, is passed to the object under test instead of a mock object as would be in a serial test case.
     * <p/>
     * Each concurrent thread must create its own mock object for each dependency it sets up expectations on, using one of the {@link #normal()}, {@link
     * #lenient()}, and {@link #ordered()} methods of this interface.
     * <h3>Usage</h3>
     * Assuming <code>SomComponent</code> with a <code>Dependency</code> dependency, the following example shows the use of composites and thread local mock
     * objects.
     * <pre>
     * public class SomeComponentTest extends {@linkplain Simulator} {
     *
     *   private final <span class="hl1">Composite</span>&lt;<span class="hl2">Dependency</span>> composite = <span class="hl1">{@linkplain Simulator#composite(Class, Object) composite}</span>(<span class="hl2">Dependency</span>.class, dependencies().normal(<span class="hl2">Dependency</span>.class));
     *   private final SomeComponent component = new SomeComponent(<span class="hl1">composite</span>.{@linkplain #dependency() dependency}());
     *
     *   &hellip;
     *
     *   &#64;Test
     *   public void testConcurrency() throws Exception {
     *     final {@linkplain Simulator.Threads Threads} threads = {@linkplain Simulator#newThreads(String) newThreads}("concurrency");
     *
     *     threads.{@linkplain Simulator.Threads#concurrent(Simulator.Task.Concurrent) concurrent}(new {@linkplain Simulator.Task.Concurrent Task.Concurrent}() {
     *       public {@linkplain Simulator.Task Task} run(final {@linkplain Simulator.MockObjects MockObjects} factory) throws Exception {
     *         final <span class="hl2">Dependency</span> dependency = <span class="hl1">composite</span>.{@linkplain #normal() normal}():
     *
     *         &hellip;
     *
     *         return new {@linkplain Simulator.Task Task}() {
     *           &hellip;
     *         }
     *       }
     *     });
     *
     *     &hellip;
     *
     *     threads.{@linkplain Simulator.Threads#guarantee(long, org.fluidity.testing.Simulator.Task) guarantee}(&hellip;);
     *   }
     * }
     * </pre>
     *
     * @param <T> the type of the mock object.
     */
    public interface Composite<T> {

        /**
         * Returns the composite object to pass to the object under test. The returned object is not a mock object, it cannot be used to set up expectations.
         * To set up expectations, use the original object passed to {@link Simulator#composite(Class, Object)} when creating this object in the main thread,
         * and create new mock objects in other threads using one of the {@link #normal()}, {@link #lenient()}, and {@link #ordered()} methods.
         *
         * @return the composite object to pass to the object under test.
         */
        T dependency();

        /**
         * Creates a new normal mock object using the {@link MockObjects#normal(Class)} method passed to the concurrent task this method is invoked from, and
         * adds it to this composite object to be used in the thread corresponding to that concurrent task.
         *
         * @return a new normal mock object.
         */
        T normal();

        /**
         * Creates a new lenient mock object using the {@link MockObjects#lenient(Class)} method passed to the concurrent task this method is invoked from, and
         * adds it to this composite object to be used in the thread corresponding to that concurrent task.
         *
         * @return a new lenient mock object.
         */
        T lenient();

        /**
         * Creates a new ordered mock object using the {@link MockObjects#ordered(Class)} method passed to the concurrent task this method is invoked from, and
         * adds it to this composite object to be used in the thread corresponding to that concurrent task.
         *
         * @return a new ordered mock object.
         */
        T ordered();

        /**
         * Tells this composite object to use the given mock object in the calling thread. You <i>only</i> use this method if the calling thread was <i>not</i>
         * created by a call to {@link Simulator.Threads#concurrent(Simulator.Task.Concurrent)}.
         *
         * @param mock the mock object to use in the calling thread.
         */
        void use(T mock);
    }

    /**
     * Used to create thread local mock objects for {@linkplain Composite composites}.
     *
     * @author Tibor Varga
     */
    private interface CompositeFactory {

        /**
         * Creates an {@link org.easymock.EasyMock#createMock(Class) normal mock object} for a concurrent thread, for the given <code>composite</code>.
         *
         * @param type      the interface to mock.
         * @param composite the composite object the new mock object is a thread local delegate of.
         * @param <T>       the type to mock.
         *
         * @return the mock object for the interface.
         */
        <T> T normal(Class<T> type, Composite<T> composite);

        /**
         * Creates a {@link org.easymock.EasyMock#createNiceMock(Class) nice mock object} for a concurrent thread, for the given <code>composite</code>.
         *
         * @param type      the interface to mock.
         * @param composite the composite object the new mock object is a thread local delegate of.
         * @param <T>       the type to mock.
         *
         * @return the mock object for the interface.
         */
        <T> T lenient(Class<T> type, Composite<T> composite);

        /**
         * Creates a {@link org.easymock.EasyMock#createStrictMock(Class) strict mock object} for a concurrent thread, for the given <code>composite</code>.
         *
         * @param type      the interface to mock.
         * @param composite the composite object the new mock object is a thread local delegate of.
         * @param <T>       the type to mock.
         *
         * @return the mock object for the interface.
         */
        <T> T ordered(Class<T> type, Composite<T> composite);
    }

    /**
     * @author Tibor Varga
     */
    private final class ThreadsImpl implements Threads {

        private final int TIMING_BASE_MS = Integer.getInteger(TIMING_BASE, 10);

        private final List<Thread> threads = new ArrayList<Thread>();
        private final List<Exception> errors = new ArrayList<Exception>();
        private CountDownLatch latch;

        private final String name;

        private ThreadsImpl(final String name) {
            this.name = name;
        }

        public long time(final float factor) {
            return (long) ((float) TIMING_BASE_MS * factor);
        }

        public void serial(final Task task) throws Exception {
            task.run();
        }

        public synchronized void concurrent(final Task.Concurrent tasks) throws Exception {
            assert tasks != null;

            final String method = "concurrent";
            if (locals.get() != null) {
                throw new IllegalStateException(String.format("%s.%s(...) cannot be called recursively",
                                                              Strings.formatClass(false, true, Threads.class),
                                                              method));
            }
            checkNesting(method);

            final List<Task> composites = new ArrayList<Task>();
            final ConcurrentFactory factory = new ConcurrentFactory(composites, Simulator.this.concurrent());

            final Task task;

            try {
                locals.set(factory);
                task = tasks.run(factory);
                assert task != null;
            } finally {
                locals.set(null);
            }

            threads.add(new Thread(String.format("%s %s [%d]", Simulator.class.getSimpleName(), name, threads.size())) {
                public void run() {
                    try {
                        for (final Task init : composites) {
                            init.run();
                        }

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

        public int lineup(final CyclicBarrier barrier, final long timeout) throws Exception {
            return barrier.await(timeout, TimeUnit.MILLISECONDS);
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

        private synchronized void join(final long timeout) throws Exception {
            try {
                boolean completed = false;

                try {
                    completed = latch.await(timeout, TimeUnit.MILLISECONDS);
                } catch (final Exception e) {
                    synchronized (errors) {
                        errors.add(e);
                    }
                }

                if (errors.isEmpty()) {
                    if (!completed) {
                        throw new TimeoutException("Thread completion");
                    }
                } else {
                    for (final Exception error : errors) {
                        error.printStackTrace();
                    }
                }

                assert errors.isEmpty();
            } finally {
                errors.clear();
            }
        }

        public final void verify(final long timeout, final Task task) throws Exception {
            checkNesting("verify");

            Simulator.this.verify(new Task() {
                public void run() throws Exception {
                    release();

                    try {
                        if (task != null) {
                            task.run();
                        }
                    } finally {
                        join(timeout);
                    }
                }
            });
        }

        public final void guarantee(final long timeout, final Task task) throws Exception {
            checkNesting("guarantee");

            Simulator.this.guarantee(new Task() {
                public void run() throws Exception {
                    release();

                    try {
                        if (task != null) {
                            task.run();
                        }
                    } finally {
                        join(timeout);
                    }
                }
            });
        }

        public <T> T verify(final long timeout, final Work<T> block) throws Exception {
            assert block != null;
            checkNesting("verify");

            return Simulator.this.verify(new Work<T>() {
                public T run() throws Exception {
                    release();

                    try {
                        return block.run();
                    } finally {
                        join(timeout);
                    }
                }
            });
        }

        public <T> T guarantee(final long timeout, final Work<T> block) throws Exception {
            assert block != null;
            checkNesting("guarantee");

            return Simulator.this.guarantee(new Work<T>() {
                public T run() throws Exception {
                    release();

                    try {
                        return block.run();
                    } finally {
                        join(timeout);
                    }
                }
            });
        }

        private void checkNesting(final String invoked) {
            assert latch == null : String.format("Calls to %1$s.%2$s(...) may not be nested with calls to %1$s.concurrent(...), %1$s.verify(...), or %1$s.guarantee(...)", Strings.formatClass(false, false, Threads.class), invoked);
        }
    }

    private static class ConcurrentFactory implements CompositeFactory, MockObjects {

        private final List<Task> composites;
        private final MockObjects factory;

        public ConcurrentFactory(final List<Task> composites, final MockObjects factory) {
            this.composites = composites;
            this.factory = factory;
        }

        private <T> T init(final Composite<T> composite, final T mock) {
            composites.add(new Task() {
                public void run() throws Exception {
                    composite.use(mock);
                }
            });

            return mock;
        }

        public <T> T normal(final Class<T> type, final Composite<T> composite) {
            return init(composite, factory.normal(type));
        }

        public <T> T lenient(final Class<T> type, final Composite<T> composite) {
            return init(composite, factory.lenient(type));
        }

        public <T> T ordered(final Class<T> type, final Composite<T> composite) {
            return init(composite, factory.ordered(type));
        }

        public <T> T normal(final Class<T> interfaceClass) {
            return factory.normal(interfaceClass);
        }

        public <T> T lenient(final Class<T> interfaceClass) {
            return factory.lenient(interfaceClass);
        }

        public <T> T ordered(final Class<T> interfaceClass) {
            return factory.ordered(interfaceClass);
        }
    }

    /**
     * @author Tibor Varga
     */
    private interface MockControl extends MockObjects {

        /**
         * Calls {@link org.easymock.IMocksControl#replay()} on all mock objects created by the factory. Mock objects are created by calling {@link
         * #normal}, {@link #lenient}, and {@link #ordered}.
         */
        void replay();

        /**
         * Calls {@link org.easymock.IMocksControl#verify()} on all mock objects created by the factory. Mock objects are created by calling {@link
         * #normal}, {@link #lenient}, and {@link #ordered}.
         */
        void verify();

        /**
         * Calls {@link org.easymock.IMocksControl#reset()} on all mock objects created by the factory. Mock objects are created by calling {@link
         * #normal}, {@link #lenient}, and {@link #ordered}.
         */
        void reset();
    }

    /**
     * A group of mock objects.
     *
     * @author Tibor Varga
     */
    private static class ControlGroup implements MockControl {

        public final IMocksControl strictGroup = EasyMock.createStrictControl();
        public final IMocksControl normalGroup = EasyMock.createControl();
        public final IMocksControl niceGroup = EasyMock.createNiceControl();

        public final IMocksControl[] groups = { strictGroup, normalGroup, niceGroup };

        ControlGroup() {
            for (final IMocksControl control : groups) {
                control.makeThreadSafe(true);
            }
        }

        public final <T> T normal(final Class<T> interfaceClass) {
            final T mock = normalGroup.createMock(interfaceClass);
            EasyMock.makeThreadSafe(mock, false);
            return mock;
        }

        public final <T> T lenient(final Class<T> interfaceClass) {
            final T mock = niceGroup.createMock(interfaceClass);
            EasyMock.makeThreadSafe(mock, false);
            return mock;
        }

        public final <T> T ordered(final Class<T> interfaceClass) {
            final T mock = strictGroup.createMock(interfaceClass);
            EasyMock.makeThreadSafe(mock, false);
            return mock;
        }

        public final void replay() {
            for (final IMocksControl group : groups) {
                group.replay();
            }
        }

        public final void verify() {
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

        public final void reset() {
            for (final IMocksControl group : groups) {
                group.reset();
            }
        }
    }
}

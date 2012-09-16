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
 *     private final Dependency <span class="hl2">dependency</span> = <span class="hl1">{@linkplain #mock(Class) mock}</span>(Dependency.class);
 *     private final MyComponent component = new MyComponent(<span class="hl2">dependency</span>);
 *
 *     &#64;Test
 *     public void testSomething() throws Exception {
 *         final Action <span class="hl2">action</span> = <span class="hl1">{@linkplain #localMock(Class) localMock}</span>(Action.class);
 *
 *         {@linkplain EasyMock}.expect(dependency.<span class="hl2">getSomeAction</span>()).andReturn(<span class="hl2">action</span>);
 *         {@linkplain EasyMock}.expect(action.<span class="hl2">perform</span>()).andReturn(true);
 *
 *         <span class="hl1">{@linkplain #replay() replay}</span>();
 *         final boolean success = component.doSomething();
 *         <span class="hl1">{@linkplain #verify() verify}</span>();
 *
 *         assert success;
 *     }
 * }
 * </pre>
 * <h4>As a delegate</h4>
 * <pre>
 * public class MyComponentTest {
 *
 *     private final <span class="hl1">MockGroup</span> group = new <span class="hl1">MockGroup</span>();
 *     private final Dependency <span class="hl2">dependency</span> = group.<span class="hl1">{@linkplain #mock(Class) mock}</span>(Dependency.class);
 *     private final MyComponent component = new MyComponent(<span class="hl2">dependency</span>);
 *
 *     &#64;AfterMethod
 *     public void tearDown() throws Exception {
 *         group.<span class="hl1">{@linkplain #clear() clear}</span>();
 *     }
 *
 *     &#64;Test
 *     public void testSomething() throws Exception {
 *         final Action <span class="hl2">action</span> = group.<span class="hl1">{@linkplain #localMock(Class) localMock}</span>(Action.class);
 *
 *         {@linkplain EasyMock}.expect(dependency.<span class="hl2">getSomeAction</span>()).andReturn(<span class="hl2">action</span>);
 *         {@linkplain EasyMock}.expect(action.<span class="hl2">perform</span>()).andReturn(true);
 *
 *         group.<span class="hl1">{@linkplain #replay() replay}</span>();
 *         final boolean success = component.doSomething();
 *         group.<span class="hl1">{@linkplain #verify() verify}</span>();
 *
 *         assert success;
 *     }
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
    public MockGroup() {
    }

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

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

package org.fluidity.tests;

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
 * Abstract test cases that facilitates the use of a {@link IMocksControl}.
 *
 * @author Tibor Varga
 */
@SuppressWarnings("UnusedDeclaration")
public abstract class MockGroupAbstractTest {

    private final ControlGroup globalGroup = new ControlGroup();
    private ControlGroup localGroup = new ControlGroup();

    @AfterMethod
    public void methodDone() throws Exception {
        localGroup = new ControlGroup();
        reset();
    }

    /**
     * Adds an ordinary mock object to the test case. The set of mock objects added to the test case are controlled collectively by the {@link #replay()},
     * {@link #verify()} and {@link #reset()} methods.
     *
     * @param interfaceClass the interface to mock.
     * @param <T>            the interface class.
     * @return the mock object for the interface.
     */
    protected final <T> T mock(final Class<T> interfaceClass) {
        return globalGroup.mock(interfaceClass);
    }

    /**
     * Adds a nice mock object to the test case. The set of mock objects added to the test case are controlled collectively by the {@link #replay()}, {@link
     * #verify()} and {@link #reset()} methods.
     *
     * @param interfaceClass the interface to mock.
     * @param <T>            the interface class.
     * @return the mock object for the interface.
     */
    protected final <T> T niceMock(final Class<T> interfaceClass) {
        return globalGroup.niceMock(interfaceClass);
    }

    /**
     * Adds a strict mock object to the test case. The set of mock objects added to the test case are controlled collectively by the {@link #replay()}, {@link
     * #verify()} and {@link #reset()} methods.
     *
     * @param interfaceClass the interface to mock.
     * @param <T>            the interface class.
     * @return the mock object for the interface.
     */
    protected final <T> T strictMock(final Class<T> interfaceClass) {
        return globalGroup.strictMock(interfaceClass);
    }

    /**
     * Adds an ordinary mock object to the test method and will be deleted for the next method. The set of mock objects added to the test case are controlled
     * collectively by the {@link #replay()}, {@link #verify()} and {@link #reset()} methods.
     *
     * @param interfaceClass the interface to mock.
     * @param <T>            the interface class.
     * @return the mock object for the interface.
     */
    protected final <T> T localMock(final Class<T> interfaceClass) {
        return globalGroup.mock(interfaceClass);
    }

    /**
     * Adds a nice mock object to the test method and will be deleted for the next method. The set of mock objects added to the test case are controlled
     * collectively by the {@link #replay()}, {@link #verify()} and {@link #reset()} methods.
     *
     * @param interfaceClass the interface to mock.
     * @param <T>            the interface class.
     * @return the mock object for the interface.
     */
    protected final <T> T localNiceMock(final Class<T> interfaceClass) {
        return globalGroup.niceMock(interfaceClass);
    }

    /**
     * Adds a strict mock object to the test method and will be deleted for the next method. The set of mock objects added to the test case are controlled
     * collectively by the {@link #replay()}, {@link #verify()} and {@link #reset()} methods.
     *
     * @param interfaceClass the interface to mock.
     * @param <T>            the interface class.
     * @return the mock object for the interface.
     */
    protected final <T> T localStrictMock(final Class<T> interfaceClass) {
        return globalGroup.strictMock(interfaceClass);
    }

    /**
     * Creates several ordinary mock objects, one for each interface given and combines them into one mock object that implements all of the given interfaces.
     * The returned mock object can be cast to any of the interface types that were given as method argument. The mock object is then added to the test case.
     * <p/>
     * The set of mock objects added to the test case are controlled collectively by the {@link #replay()}, {@link #verify()} and {@link #reset()} methods.
     * <p/>
     * Note: interface visibility may make it impossible to combine the mock objects.
     *
     * @param <T>             the interface class.
     * @param mainInterface   the interface to use as a reference and also the on that determines the return type.
     * @param otherInterfaces the other interfaces to mock.
     * @return the mock object for the interface.
     */
    @SuppressWarnings("unchecked")
    public final <T> T mockAll(final Class<T> mainInterface, final Class<?>... otherInterfaces) {
        return globalGroup.mockAll(mainInterface, otherInterfaces);
    }

    /**
     * Creates several ordinary mock objects, one for each interface given and combines them into one mock object that implements all of the given interfaces.
     * The returned mock object can be cast to any of the interface types that were given as method argument. The mock object is then added to the test method
     * and will be deleted for the next method.
     * <p/>
     * The set of mock objects added to the test case are controlled collectively by the {@link #replay()}, {@link #verify()} and {@link #reset()} methods.
     * <p/>
     * Note: interface visibility may make it impossible to combine the mock objects.
     *
     * @param <T>             the interface class.
     * @param mainInterface   the interface to use as a reference and also the on that determines the return type.
     * @param otherInterfaces the other interfaces to mock.
     * @return the mock object for the interface.
     */
    @SuppressWarnings("unchecked")
    public final <T> T localMockAll(final Class<T> mainInterface, final Class<?>... otherInterfaces) {
        return globalGroup.mockAll(mainInterface, otherInterfaces);
    }

    /**
     * Calls {@link IMocksControl#replay()} on all mock objects added to the test. Mock objects are added by calling {@link #mock(Class)}, {@link
     * #niceMock(Class)}, {@link #strictMock(Class)} and {@link #mockAll(Class, Class[])}.
     */
    protected final void replay() {
        globalGroup.replay();
        localGroup.replay();
    }

    /**
     * Calls {@link IMocksControl#verify()} on all mock objects added to the test. Mock objects are added by calling {@link #mock(Class)}, {@link
     * #niceMock(Class)}, {@link #strictMock(Class)} and {@link #mockAll(Class, Class[])}.
     */
    protected final void verify() {
        globalGroup.verify();
        localGroup.verify();
    }

    /**
     * Calls {@link IMocksControl#reset()} on all mock objects added to the test. Mock objects are added by calling {@link #mock(Class)}, {@link
     * #niceMock(Class)}, {@link #strictMock(Class)} and {@link #mockAll(Class, Class[])}.
     */
    protected final void reset() {
        globalGroup.reset();
        localGroup.reset();
    }

    /**
     * Argument matcher that understands variadic arguments.
     *
     * @param expected the array of expected values.
     * @return the matcher.
     */
    protected final <T> T[] varEq(final T[] expected) {
        for (final T value : expected) {
            EasyMock.reportMatcher(new Equals(value));
        }

        return expected;
    }

    /**
     * Argument matcher that understands variadic arguments.
     *
     * @param expected the array of expected values.
     * @return the matcher.
     */
    protected final <T> T[] varSame(final T[] expected) {
        for (final T value : expected) {
            EasyMock.reportMatcher(new Same(value));
        }

        return expected;
    }

    /**
     * Argument matcher that understands variadic arguments.
     *
     * @param expected an array with the expected size.
     * @return the matcher.
     */
    protected final <T> T[] varNotNull(final T[] expected) {
        for (final T value : expected) {
            EasyMock.reportMatcher(NotNull.NOT_NULL);
        }

        return expected;
    }

    private static class ControlGroup {

        public final IMocksControl group = EasyMock.createControl();
        public final IMocksControl niceGroup = EasyMock.createNiceControl();
        public final IMocksControl strictGroup = EasyMock.createStrictControl();

        public final List<IMocksControl> groups = new ArrayList<IMocksControl>(Arrays.asList(strictGroup, group, niceGroup));

        public ControlGroup() {
            for (final IMocksControl control : groups) {
                control.makeThreadSafe(true);
            }
        }

        /**
         * Adds an ordinary mock object to the test case. The set of mock objects added to the test case are controlled collectively by the {@link #replay()},
         * {@link #verify()} and {@link #reset()} methods.
         *
         * @param interfaceClass the interface to mock.
         * @param <T>            the interface class.
         * @return the mock object for the interface.
         */
        protected final <T> T mock(final Class<T> interfaceClass) {
            return group.createMock(interfaceClass);
        }

        /**
         * Adds a nice mock object to the test case. The set of mock objects added to the test case are controlled collectively by the {@link #replay()}, {@link
         * #verify()} and {@link #reset()} methods.
         *
         * @param interfaceClass the interface to mock.
         * @param <T>            the interface class.
         * @return the mock object for the interface.
         */
        protected final <T> T niceMock(final Class<T> interfaceClass) {
            return niceGroup.createMock(interfaceClass);
        }

        /**
         * Adds a strict mock object to the test case. The set of mock objects added to the test case are controlled collectively by the {@link #replay()},
         * {@link #verify()} and {@link #reset()} methods.
         *
         * @param interfaceClass the interface to mock.
         * @param <T>            the interface class.
         * @return the mock object for the interface.
         */
        protected final <T> T strictMock(final Class<T> interfaceClass) {
            return strictGroup.createMock(interfaceClass);
        }

        /**
         * Creates several ordinary mock objects, one for each interface given and combines them into one mock object that implements all of the given
         * interfaces. The returned mock object can be cast to any of the interface types that were given as method argument. The mock object is then added to
         * the test case.
         * <p/>
         * The set of mock objects added to the test case are controlled collectively by the {@link #replay()}, {@link #verify()} and {@link #reset()} methods.
         * <p/>
         * Note: interface visibility may make it impossible to combine the mock objects.
         *
         * @param <T>             the interface class.
         * @param mainInterface   the interface to use as a reference and also the on that determines the return type.
         * @param otherInterfaces the other interfaces to mock.
         * @return the mock object for the interface.
         */
        @SuppressWarnings("unchecked")
        public final <T> T mockAll(final Class<T> mainInterface, final Class<?>... otherInterfaces) {
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
         * Calls {@link IMocksControl#replay()} on all mock objects added to the test. Mock objects are added by calling {@link #mock(Class)}, {@link
         * #niceMock(Class)}, {@link #strictMock(Class)} and {@link #mockAll(Class, Class[])}.
         */
        protected final void replay() {
            for (final IMocksControl group : groups) {
                group.replay();
            }
        }

        /**
         * Calls {@link IMocksControl#verify()} on all mock objects added to the test. Mock objects are added by calling {@link #mock(Class)}, {@link
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
         * Calls {@link IMocksControl#reset()} on all mock objects added to the test. Mock objects are added by calling {@link #mock(Class)}, {@link
         * #niceMock(Class)}, {@link #strictMock(Class)} and {@link #mockAll(Class, Class[])}.
         */
        protected final void reset() {
            for (final IMocksControl group : groups) {
                group.reset();
            }
        }
    }
}

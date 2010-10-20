/*
 * Copyright (c) 2006-2010 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
import org.testng.annotations.BeforeMethod;

/**
 * Abstract test cases that facilitates the use of a <code>IMocksControl</code>.
 *
 * @author Tibor Varga
 */
public abstract class MockGroupAbstractTest {

    private final IMocksControl group = EasyMock.createControl();
    private final IMocksControl niceGroup = EasyMock.createNiceControl();
    private final IMocksControl strictGroup = EasyMock.createStrictControl();

    private final List<IMocksControl> groups = new ArrayList<IMocksControl>(Arrays.asList(strictGroup, group, niceGroup));

    @BeforeMethod
    public void setup() throws Exception {
        reset();
    }

    /**
     * Returns the mock control for ordinary mock objects.
     *
     * @return the mock control for ordinary mock objects.
     */
    protected final IMocksControl group() {
        return group;
    }

    /**
     * Returns the mock control for nice mock objects.
     *
     * @return the mock control for nice mock objects.
     */
    protected final IMocksControl niceGroup() {
        return niceGroup;
    }

    /**
     * Returns the mock control for strict mock objects.
     *
     * @return the mock control for strict mock objects.
     */
    protected final IMocksControl strictGroup() {
        return niceGroup;
    }

    /**
     * Adds an ordinary mock object to the test case. The set of mock objects added to the test case are controlled collectively by the {@link #replay()},
     * {@link #verify()} and {@link #reset()} methods.
     *
     * @param interfaceClass the interface to mock.
     * @param <T>            the interface class.
     *
     * @return the mock object for the interface.
     */
    protected final <T> T addControl(final Class<T> interfaceClass) {
        return group.createMock(interfaceClass);
    }

    /**
     * Adds a nice mock object to the test case. The set of mock objects added to the test case are controlled collectively by the {@link #replay()}, {@link
     * #verify()} and {@link #reset()} methods.
     *
     * @param interfaceClass the interface to mock.
     * @param <T>            the interface class.
     *
     * @return the mock object for the interface.
     */
    protected final <T> T addNiceControl(final Class<T> interfaceClass) {
        return niceGroup.createMock(interfaceClass);
    }

    /**
     * Adds a strict mock object to the test case. The set of mock objects added to the test case are controlled collectively by the {@link #replay()}, {@link
     * #verify()} and {@link #reset()} methods.
     *
     * @param interfaceClass the interface to mock.
     * @param <T>            the interface class.
     *
     * @return the mock object for the interface.
     */
    protected final <T> T addStrictControl(final Class<T> interfaceClass) {
        return strictGroup.createMock(interfaceClass);
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
     *
     * @return the mock object for the interface.
     */
    @SuppressWarnings({ "unchecked" })
    public final <T> T addControls(final Class<T> mainInterface, final Class<?>... otherInterfaces) {
        if (otherInterfaces.length == 0) {
            return addControl(mainInterface);
        } else {
            final Class<?> interfaces[] = new Class<?>[otherInterfaces.length + 1];

            interfaces[0] = mainInterface;
            System.arraycopy(otherInterfaces, 0, interfaces, 1, otherInterfaces.length);

            final Map<Class<?>, Object> proxyMap = new LinkedHashMap<Class<?>, Object>();

            for (Class<?> api : interfaces) {
                assert !proxyMap.containsKey(api) : api;
                proxyMap.put(api, addControl(api));
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
                        method.setAccessible(true);
                        return method.invoke(proxy, args);
                    } catch (final InvocationTargetException e) {
                        throw e.getCause();
                    }
                }
            });
        }
    }

    /**
     * Calls {@link org.easymock.IMocksControl#replay()} on all mock objects added to the test. Mock objects are added by calling {@link #addControl(Class)},
     * {@link #addNiceControl(Class)}, {@link #addStrictControl(Class)} and {@link #addControls(Class, Class[])}.
     */
    protected final void replay() {
        for (final IMocksControl group : groups) {
            group.replay();
        }
    }

    /**
     * Calls {@link org.easymock.IMocksControl#verify()} on all mock objects added to the test. Mock objects are added by calling {@link #addControl(Class)},
     * {@link #addNiceControl(Class)}, {@link #addStrictControl(Class)} and {@link #addControls(Class, Class[])}.
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
     * Calls {@link org.easymock.IMocksControl#reset()} on all mock objects added to the test. Mock objects are added by calling {@link #addControl(Class)},
     * {@link #addNiceControl(Class)}, {@link #addStrictControl(Class)} and {@link #addControls(Class, Class[])}.
     */
    protected final void reset() {
        for (final IMocksControl group : groups) {
            group.reset();
        }
    }
}

/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.foundation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.fluidity.tests.MockGroupAbstractTest;

import org.easymock.EasyMock;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class ProxiesTest extends MockGroupAbstractTest {

    private final TestObject mock = mock(TestObject.class);

    interface TestObject {

        int id();
    }

    @Test
    public void testIntrospection() throws Exception {
        final InvocationHandler handler = new InvocationHandler() {
            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                return method.invoke(mock, args);
            }
        };

        final TestObject proxy = Proxies.create(TestObject.class, handler);

        assert Proxies.invocationHandler(proxy) == handler;

        final Class<?>[] types = proxy.getClass().getInterfaces();
        assert types != null;
        assert types.length == 1 : types.length;
        assert types[0] == TestObject.class : types[0];
    }

    @Test
    public void testDefaultIdentity() throws Exception {
        final TestObject instance1 = Proxies.create(TestObject.class, new InvocationHandler() {
            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                return method.invoke(mock, args);
            }
        });

        final TestObject instance2 = Proxies.create(TestObject.class, new InvocationHandler() {
            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                return method.invoke(mock, args);
            }
        });

        replay();
        assert instance1.equals(instance1);
        assert instance2.equals(instance2);
        assert !instance1.equals(instance2);
        assert !instance2.equals(instance1);
        verify();
    }

    @Test
    public void testCustomIdentity() throws Exception {
        final Proxies.ObjectIdentity<TestObject> identity = new Proxies.ObjectIdentity<TestObject>() {
            public int hashCode(final TestObject instance) {
                return instance.id();
            }

            public boolean equals(final Object other, final TestObject instance) {
                if (other == instance) {
                    return true;
                } else if (!(other instanceof TestObject)) {
                    return false;
                }

                return instance.id() == ((TestObject) other).id();
            }

            public String toString(final TestObject instance) {
                return String.valueOf(instance.id());
            }
        };

        final TestObject instance1 = Proxies.create(TestObject.class, identity, new InvocationHandler() {
            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                return method.invoke(mock, args);
            }
        });

        final TestObject instance2 = Proxies.create(TestObject.class, identity, new InvocationHandler() {
            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                return method.invoke(mock, args);
            }
        });

        EasyMock.expect(mock.id()).andReturn(1234).anyTimes();

        replay();
        assert instance1.id() == 1234;
        assert instance2.id() == 1234;
        assert instance1.equals(instance1);
        assert instance2.equals(instance2);
        assert instance1.equals(instance2);
        assert instance2.equals(instance1);
        verify();
    }
}

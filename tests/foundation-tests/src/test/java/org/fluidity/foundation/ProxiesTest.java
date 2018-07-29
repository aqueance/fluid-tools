/*
 * Copyright (c) 2006-2018 Tibor Adam Varga (tibor.adam.varga on gmail)
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
import java.util.Objects;

import org.fluidity.testing.Simulator;

import org.easymock.EasyMock;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("EqualsWithItself")
public class ProxiesTest extends Simulator {

    private final TestObject mock = dependencies().normal(TestObject.class);
    private final InvocationHandler invocations = (proxy, method, arguments) -> method.invoke(mock, arguments);

    private interface TestObject {

        int id();
    }

    @Test
    public void testIntrospection() throws Exception {
        final TestObject proxy = Proxies.create(TestObject.class, invocations);

        assert Proxies.handler(proxy) == invocations;

        final Class<?>[] types = proxy.getClass().getInterfaces();
        assert types != null;
        assert types.length == 1 : types.length;
        assert types[0] == TestObject.class : types[0];
    }

    @Test
    public void testDefaultIdentity() throws Exception {
        final TestObject instance1 = Proxies.create(TestObject.class, invocations);
        final TestObject instance2 = Proxies.create(TestObject.class, invocations);

        verify(() -> {
            assert Objects.equals(instance1, instance1);
            assert Objects.equals(instance2, instance2);
            assert !Objects.equals(instance1, instance2);
            assert !Objects.equals(instance2, instance1);
        });
    }

    @Test
    public void testCustomIdentity() throws Exception {
        final Proxies.Identity<TestObject> identity = new Proxies.Identity<TestObject>() {
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

        final TestObject instance1 = Proxies.create(TestObject.class, identity, invocations);
        final TestObject instance2 = Proxies.create(TestObject.class, identity, invocations);

        EasyMock.expect(mock.id()).andReturn(1234).anyTimes();

        verify(() -> {
            assert instance1.id() == 1234;
            assert instance2.id() == 1234;
            assert Objects.equals(instance1, instance1);
            assert Objects.equals(instance2, instance2);
            assert Objects.equals(instance1, instance2);
            assert Objects.equals(instance2, instance1);
        });
    }
}

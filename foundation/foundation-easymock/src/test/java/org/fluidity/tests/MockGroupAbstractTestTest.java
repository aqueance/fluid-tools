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

package org.fluidity.tests;

import org.easymock.EasyMock;
import org.testng.annotations.Test;

public class MockGroupAbstractTestTest extends MockGroupAbstractTest {

    private final VarargsComponent component = mock(VarargsComponent.class);

    @Test
    public void testVarargsMatching() throws Exception {
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

    public static interface VarargsComponent {

        String method(final String prefix, final String... values);
    }
}

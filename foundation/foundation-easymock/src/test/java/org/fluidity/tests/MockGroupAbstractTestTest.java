package org.fluidity.tests;

import org.easymock.EasyMock;
import org.testng.annotations.Test;

public class MockGroupAbstractTestTest extends MockGroupAbstractTest {

    private final VarargsComponent component = addControl(VarargsComponent.class);

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

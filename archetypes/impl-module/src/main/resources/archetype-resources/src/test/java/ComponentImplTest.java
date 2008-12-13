#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import org.fluidity.tests.MockGroupAbstractTest;
import org.testng.annotations.Test;
import org.testng.Assert;
import org.easymock.EasyMock;

public final class ComponentImplTest extends MockGroupAbstractTest {

    private final ComponentApi.Dependency dependency = addControl(ComponentApi.Dependency.class);

    private final ComponentApi testee = new ComponentImpl(dependency);

    @Test
    public void sendsText() {
        final String accepted = "accepted";
        final String rejected = "rejected";

        EasyMock.expect(dependency.receiveText(accepted)).andReturn(true);
        EasyMock.expect(dependency.receiveText(rejected)).andReturn(false);

        replay();

        Assert.assertTrue(testee.sendText(accepted));
        Assert.assertFalse(testee.sendText(rejected));

        verify();
    }
}

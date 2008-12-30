#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import org.easymock.EasyMock;
import org.fluidity.deployment.DeployedComponent;
import org.fluidity.tests.MockGroupAbstractTest;
import org.testng.Assert;
import org.testng.annotations.Test;

public final class DeploymentTest extends MockGroupAbstractTest {

    private final DeployedComponent deployment = new Deployment();

    @Test
    public void metadata() throws Exception {
        replay();
        deployment.key();
        deployment.name();
        verify();
    }

    @Test
    public void deployment() throws Exception {
        replay();
        deployment.start();
        verify();

        replay();
        deployment.stop();
        verify();
    }
}

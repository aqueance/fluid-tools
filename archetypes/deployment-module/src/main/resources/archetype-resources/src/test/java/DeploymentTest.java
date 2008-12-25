#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import org.easymock.EasyMock;
import org.fluidity.composition.DeployedComponent;
import org.fluidity.tests.MockGroupAbstractTest;
import org.testng.Assert;
import org.testng.annotations.Test;

public final class DeploymentTest extends MockGroupAbstractTest {

    private final DeployedComponent deployment = new Deployment();

    @Test
    public void basics() throws Exception {
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

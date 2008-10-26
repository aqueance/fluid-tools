package org.fluidity.composition;

import org.easymock.EasyMock;
import org.fluidity.foundation.Logging;
import org.fluidity.foundation.logging.StandardOutLogging;
import org.fluidity.tests.MockGroupAbstractTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public final class DeploymentBootstrapImplTest extends MockGroupAbstractTest {

    private final Logging log = new StandardOutLogging("test");
    private final ComponentContainer container = addControl(ComponentContainer.class);
    private final ComponentDiscovery discovery = addControl(ComponentDiscovery.class);

    private final DeployedComponent component1 = addStrictControl(DeployedComponent.class);
    private final DeployedComponent component2 = addStrictControl(DeployedComponent.class);
    private final DeployedComponent component3 = addStrictControl(DeployedComponent.class);

    private final DeploymentObserver observer1 = addStrictControl(DeploymentObserver.class);
    private final DeploymentObserver observer2 = addStrictControl(DeploymentObserver.class);

    private DeploymentBootstrap bootstrap;

    @BeforeMethod
    public void setupBootstrap() {
        EasyMock.expect(discovery.findComponentInstances(container, DeployedComponent.class)).andReturn(new DeployedComponent[] {
                component1,
                component2,
                component3,
        });
        EasyMock.expect(discovery.findComponentInstances(container, DeploymentObserver.class)).andReturn(new DeploymentObserver[] {
                observer1,
                observer2,
        });
        replay();
        bootstrap = new DeploymentBootstrapImpl(log, container, discovery);
        verify();
    }

    @Test
    public void forceTestOrdering() throws Exception {
        testLoading();
        testUnloading();
    }

    public void testLoading() throws Exception {
        EasyMock.expect(component1.name()).andReturn("Component 1");
        component1.start();
        EasyMock.expect(component1.name()).andReturn("Component 1");

        EasyMock.expect(component2.name()).andReturn("Component 2");
        component2.start();
        EasyMock.expect(component2.name()).andReturn("Component 2");

        EasyMock.expect(component3.name()).andReturn("Component 3");
        component3.start();
        EasyMock.expect(component3.name()).andReturn("Component 3");

        observer1.started();
        observer2.started();

        replay();
        bootstrap.load();
        verify();
    }

    public void testUnloading() throws Exception {
        EasyMock.expect(component3.name()).andReturn("Component 3");
        component3.stop();
        EasyMock.expect(component3.name()).andReturn("Component 3");

        EasyMock.expect(component2.name()).andReturn("Component 2");
        component2.stop();
        EasyMock.expect(component2.name()).andReturn("Component 2");

        EasyMock.expect(component1.name()).andReturn("Component 1");
        component1.stop();
        EasyMock.expect(component1.name()).andReturn("Component 1");

        observer2.stopped();
        observer1.stopped();

        replay();
        bootstrap.unload();
        verify();
    }
}

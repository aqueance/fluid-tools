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

package org.fluidity.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.fluidity.foundation.NoLogFactory;
import org.fluidity.foundation.logging.Log;
import org.fluidity.tests.MockGroupAbstractTest;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public final class DeploymentBootstrapImplTest extends MockGroupAbstractTest {

    private final Log log = new NoLogFactory().createLog(null);
    private final DeploymentControl deployments = addControl(DeploymentControl.class);

    private final DeployedComponent component1 = addStrictControl(DeployedComponent.class);
    private final DeployedComponent component2 = addStrictControl(DeployedComponent.class);
    private final DeployedComponent component3 = addStrictControl(DeployedComponent.class);

    private final DeploymentObserver observer1 = addStrictControl(DeploymentObserver.class);
    private final DeploymentObserver observer2 = addStrictControl(DeploymentObserver.class);

    private DeploymentBootstrap bootstrap;

    @Test
    public void forceTestOrdering() throws Exception {
        testLoading();
        testUnloading();
    }

    private void deployments(final DeployedComponent[] components, final DeploymentObserver[] observers) {
        bootstrap = new DeploymentBootstrapImpl(log, components, observers, deployments);
    }

    @Test
    public void testAutoStoppingSome() throws Exception {
        deployments(new DeployedComponent[] { component1, component2, component3 }, new DeploymentObserver[] { observer1, observer2 });

        final List<DeployedComponent.Context> contexts = new ArrayList<DeployedComponent.Context>();
        final CaptureContext capture = new CaptureContext(contexts);

        final String name1 = "Component 1";
        final String name2 = "Component 2";
        final String name3 = "Component 3";

        EasyMock.expect(component1.name()).andReturn(name1);
        component1.start(EasyMock.<DeployedComponent.Context>notNull());
        EasyMock.expectLastCall().andAnswer(capture);
        EasyMock.expect(component1.name()).andReturn(name1);

        EasyMock.expect(component2.name()).andReturn(name2);
        component2.start(EasyMock.<DeployedComponent.Context>notNull());
        EasyMock.expectLastCall().andAnswer(capture);
        EasyMock.expect(component2.name()).andReturn(name2);

        EasyMock.expect(component3.name()).andReturn(name3);
        component3.start(EasyMock.<DeployedComponent.Context>notNull());
        EasyMock.expectLastCall().andAnswer(capture);
        EasyMock.expect(component3.name()).andReturn(name3);

        observer1.started();
        observer2.started();

        deployments.completed();

        replay();
        bootstrap.load();

        // there are 3 components so we need 3 contexts
        Assert.assertEquals(contexts.size(), 3);

        verify();

        // let's remove the observer for the 1st component, meaning that will not stop automatically
        contexts.remove(0);

        // because component1 did not stop automatically, it has to be stopped externally
        EasyMock.expect(component1.name()).andReturn(name1);
        component1.stop();
        EasyMock.expect(component1.name()).andReturn(name1);

        observer2.stopped();
        observer1.stopped();

        replay();

        for (final DeployedComponent.Context observer : contexts) {
            observer.complete();
        }

        bootstrap.unload();
        verify();
    }

    @DataProvider(name = "standalone")
    public Object[][] testAutoStoppingAll() throws Exception {
        return new Object[][] {
                new Object[] { false }, new Object[] { true }
        };
    }

    @Test(dataProvider = "standalone")
    public void testAutoStoppingAll(final boolean standalone) throws Exception {
        deployments(new DeployedComponent[] { component1, component2, component3 }, new DeploymentObserver[] { observer1, observer2 });

        // one for each component (and that's 3)
        final List<DeployedComponent.Context> contexts = new ArrayList<DeployedComponent.Context>();

        final String name1 = "Component 1";
        final String name2 = "Component 2";
        final String name3 = "Component 3";

        EasyMock.expect(component1.name()).andReturn(name1);
        component1.start(EasyMock.<DeployedComponent.Context>notNull());
        EasyMock.expectLastCall().andAnswer(new CaptureContext(contexts));
        EasyMock.expect(component1.name()).andReturn(name1);

        EasyMock.expect(component2.name()).andReturn(name2);
        component2.start(EasyMock.<DeployedComponent.Context>notNull());
        EasyMock.expectLastCall().andAnswer(new CaptureContext(contexts));
        EasyMock.expect(component2.name()).andReturn(name2);

        EasyMock.expect(component3.name()).andReturn(name3);
        component3.start(EasyMock.<DeployedComponent.Context>notNull());
        EasyMock.expectLastCall().andAnswer(new CaptureContext(contexts));
        EasyMock.expect(component3.name()).andReturn(name3);

        observer1.started();
        observer2.started();

        deployments.completed();

        replay();
        bootstrap.load();
        verify();

        EasyMock.expect(deployments.isStandalone()).andReturn(standalone);

        if (!standalone) {
            deployments.stop();
        }

        observer2.stopped();
        observer1.stopped();

        replay();

        for (final DeployedComponent.Context observer : contexts) {
            observer.complete();
        }

        bootstrap.unload();
        verify();
    }

    @Test
    public void testNothingToDeploy() throws Exception {
        deployments(new DeployedComponent[0], new DeploymentObserver[0]);

        // because no components were found to deploy, runtime.deploymentsComplete() is called
        deployments.completed();

        replay();
        bootstrap.load();
        verify();

        replay();
        bootstrap.unload();
        verify();
    }

    public void testLoading() throws Exception {
        deployments(new DeployedComponent[] { component1, component2, component3 }, new DeploymentObserver[] { observer1, observer2 });

        EasyMock.expect(component1.name()).andReturn("Component 1");
        component1.start(EasyMock.<DeployedComponent.Context>notNull());
        EasyMock.expect(component1.name()).andReturn("Component 1");

        EasyMock.expect(component2.name()).andReturn("Component 2");
        component2.start(EasyMock.<DeployedComponent.Context>notNull());
        EasyMock.expect(component2.name()).andReturn("Component 2");

        EasyMock.expect(component3.name()).andReturn("Component 3");
        component3.start(EasyMock.<DeployedComponent.Context>notNull());
        EasyMock.expect(component3.name()).andReturn("Component 3");

        observer1.started();
        observer2.started();

        deployments.completed();

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

    private static class CaptureContext implements IAnswer<Void> {

        private final Collection<DeployedComponent.Context> contexts;

        public CaptureContext(final Collection<DeployedComponent.Context> contexts) {
            this.contexts = contexts;
        }

        public Void answer() throws Throwable {
            contexts.add((DeployedComponent.Context) EasyMock.getCurrentArguments()[0]);
            return null;
        }
    }
}

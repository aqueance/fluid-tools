/*
 * Copyright (c) 2006-2012 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.management;

import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.fluidity.composition.OpenContainer;
import org.fluidity.composition.spi.ContainerTermination;
import org.fluidity.foundation.Command;
import org.fluidity.testing.Simulator;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class LoggingMBeanBootstrapTest extends Simulator {

    private final MockObjects dependencies = dependencies();

    private final OpenContainer container = dependencies.normal(OpenContainer.class);
    private final ContainerTermination shutdown = dependencies.normal(ContainerTermination.class);

    private final LoggingMBeanBootstrap bootstrap = new LoggingMBeanBootstrap();

    @Test
    public void testMBeanLifeCycle() throws Exception {
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

        if (server != null) {
            final ObjectName name = new ObjectName(LoggingMBeanBootstrap.MBEAN_NAME);

            final Task unregistered = new Task() {
                public void run() throws Exception {
                    try {
                        server.getMBeanInfo(name);
                        assert false : "MBean should not be found";
                    } catch (final InstanceNotFoundException e) {
                        // expected
                    }
                }
            };

            test(unregistered);

            final Command.Job<Exception> job = test(new Work<Command.Job<Exception>>() {
                public Command.Job<Exception> run() throws Exception {
                    final AtomicReference<Command.Job<Exception>> job = new AtomicReference<Command.Job<Exception>>();

                    EasyMock.expect(container.instantiate(LoggingMBeanImpl.class)).andReturn(new LoggingMBeanImpl());

                    shutdown.add(EasyMock.<Command.Job<Exception>>anyObject());
                    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
                        @SuppressWarnings("unchecked")
                        public Object answer() throws Throwable {
                            job.set((Command.Job<Exception>) EasyMock.getCurrentArguments()[0]);
                            return null;
                        }
                    });

                    verify(new Task() {
                        public void run() throws Exception {
                            bootstrap.initialize(container, shutdown);
                        }
                    });

                    return job.get();
                }
            });

            final MBeanInfo info = server.getMBeanInfo(name);
            assert info.getOperations().length == 1 : info.getOperations();

            verify(new Task() {
                public void run() throws Exception {
                    job.run();
                }
            });

            test(unregistered);
        }
    }
}

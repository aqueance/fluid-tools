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

package org.fluidity.foundation;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.fluidity.composition.OpenContainer;
import org.fluidity.composition.spi.ContainerTermination;
import org.fluidity.composition.spi.EmptyPackageBindings;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("UnusedDeclaration")
final class PackageBindingsImpl extends EmptyPackageBindings {

    @Override
    public void initializeComponents(final OpenContainer container) throws Exception {
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

        if (server != null) {
            final ObjectName name = new ObjectName(String.format("org.fluidity.management:type=%s", Strings.printClass(false, false, LoggingMBean.class)));

            server.registerMBean(container.instantiate(LoggingMBeanImpl.class), name);

            container.getComponent(ContainerTermination.class).add(new Command.Job<Exception>() {
                public void run() throws Exception {
                    server.unregisterMBean(name);
                }
            });
        }
    }
}

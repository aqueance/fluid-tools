/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.fluidity.composition.OpenContainer;
import org.fluidity.composition.spi.ContainerTermination;
import org.fluidity.composition.spi.EmptyPackageBindings;
import org.fluidity.foundation.Strings;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("UnusedDeclaration")
final class LoggingMBeanBootstrap extends EmptyPackageBindings {

    static final String MBEAN_NAME = String.format("org.fluidity.management:type=%s", Strings.formatClass(false, false, LoggingMBean.class));

    @Override
    public void initialize(final OpenContainer container, final ContainerTermination shutdown) throws Exception {
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

        if (server != null) {
            final ObjectName name = new ObjectName(MBEAN_NAME);

            server.registerMBean(container.instantiate(LoggingMBeanImpl.class), name);

            shutdown.add(() -> server.unregisterMBean(name));
        }
    }
}

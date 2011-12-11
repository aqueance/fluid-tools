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

package org.fluidity.foundation.tests;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ContainerBoundary;
import org.fluidity.foundation.Log;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class RefreshedLogFactoryTest {

    private final ComponentContainer container = new ContainerBoundary();

    @Test
    public void testRefreshedLogging() throws Exception {
        assert container.getComponent(Logging.class) != null : Logging.class;
    }

    @Component
    private static class Logging {

        @SuppressWarnings("UnusedDeclaration")
        public Logging(final Log.Refreshed<Logging> log) {
            assert log != null;
        }
    }
}

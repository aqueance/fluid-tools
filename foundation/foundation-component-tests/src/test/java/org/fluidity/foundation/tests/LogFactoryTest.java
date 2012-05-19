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

package org.fluidity.foundation.tests;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ContainerBoundary;
import org.fluidity.foundation.Log;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class LogFactoryTest {

    private final ComponentContainer container = new ContainerBoundary();

    @Test
    public void testLogInjection() throws Exception {
        assert container.getComponent(Logger.class) != null;
    }

    @Component
    private static class Logger {

        @SuppressWarnings("UnusedDeclaration")
        private Logger(final Log<Logger> log) {
            assert log != null;
        }
    }
}

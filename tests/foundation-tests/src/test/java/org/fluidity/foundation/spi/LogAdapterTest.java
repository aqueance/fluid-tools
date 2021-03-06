/*
 * Copyright (c) 2006-2018 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.foundation.spi;

import org.fluidity.testing.Simulator;

import org.easymock.EasyMock;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class LogAdapterTest extends Simulator {

    private final LogAdapter.Levels levels = dependencies().normal(LogAdapter.Levels.class);
    private final Logger logger = new Logger();

    @Test
    public void testDynamic() throws Exception {
        final Adapter log = verify(() -> new Adapter(logger));

        test(() -> {
            EasyMock.expect(levels.trace()).andReturn(false);
            EasyMock.expect(levels.debug()).andReturn(true);
            EasyMock.expect(levels.info()).andReturn(false);
            EasyMock.expect(levels.warning()).andReturn(true);

            final Task assertions = () -> {
                assert !log.isTraceEnabled();
                assert log.isDebugEnabled();
                assert !log.isInfoEnabled();
                assert log.isWarningEnabled();
            };

            verify(assertions);
            verify(assertions);
        });

        LogLevels.updated();

        test(() -> {
            EasyMock.expect(levels.trace()).andReturn(true);
            EasyMock.expect(levels.debug()).andReturn(false);
            EasyMock.expect(levels.info()).andReturn(true);
            EasyMock.expect(levels.warning()).andReturn(false);

            verify(() -> {
                assert log.isTraceEnabled();
                assert !log.isDebugEnabled();
                assert log.isInfoEnabled();
                assert !log.isWarningEnabled();
            });
        });
    }

    private static final class Logger { }

    private final class Adapter extends LogAdapter<Logger, Object> {

        Adapter(final Logger log) {
            super(log);
        }

        @Override
        protected Levels levels() {
            return levels;
        }

        public void trace(final String format, final Object... arguments) {
            // empty
        }

        public void debug(final String format, final Object... arguments) {
            // empty
        }

        public void info(final String format, final Object... arguments) {
            // empty
        }

        public void warning(final String format, final Object... arguments) {
            // empty
        }

        public void error(final String format, final Object... arguments) {
            // empty
        }

        public void trace(final Throwable exception, final String format, final Object... arguments) {
            // empty
        }

        public void debug(final Throwable exception, final String format, final Object... arguments) {
            // empty
        }

        public void info(final Throwable exception, final String format, final Object... arguments) {
            // empty
        }

        public void warning(final Throwable exception, final String format, final Object... arguments) {
            // empty
        }

        public void error(final Throwable exception, final String format, final Object... arguments) {
            // empty
        }
    }
}

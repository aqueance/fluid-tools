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

package src.fluidity.features.impl;

import org.fluidity.tests.MockGroupAbstractTest;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class SchedulerTest extends MockGroupAbstractTest {

    private final Runnable task = mock(Runnable.class);

    private SchedulerImpl scheduler = new SchedulerImpl();

    @AfterMethod
    public void tearDown() throws Exception {
        assert scheduler != null;
        scheduler.stop();
        scheduler = null;
    }

    @Test
    public void testScheduler() throws Exception {
        replay();
        final Scheduler.Control control = scheduler.invoke(100, 100, task);
        verify();

        task.run();

        replay();
        Thread.sleep(150);
        verify();

        control.cancel();

        replay();
        Thread.sleep(250);
        verify();
    }
}

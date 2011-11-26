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

package org.fluidity.foundation;

import org.fluidity.tests.MockGroupAbstractTest;

import org.easymock.EasyMock;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class UpdatesTest extends MockGroupAbstractTest {

    @SuppressWarnings("unchecked") private final Configuration<Updates.Settings> configuration = mock(Configuration.class);
    @SuppressWarnings("unchecked") private final Updates.Snapshot<Object> loader = mock(Updates.Snapshot.class);

    private final Updates.Settings settings = mock(Updates.Settings.class);
    private UpdatesImpl updates;

    public void setPeriod(final long value) throws Exception {
        assert updates == null;
        EasyMock.expect(configuration.settings()).andReturn(settings);
        EasyMock.expect(settings.period()).andReturn(value);

        replay();
        updates = new UpdatesImpl(configuration);
        verify();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        assert updates != null;
        updates.stop();
        updates = null;
    }

    @Test
    public void testUpdates() throws Exception {
        setPeriod(10L);

        final Object context = new Object();

        // initialization
        EasyMock.expect(loader.get()).andReturn(context);

        replay();
        final Updates.Snapshot<Object> snapshot = updates.register(100, loader);
        verify();

        replay();
        assert context == snapshot.get();
        assert context == snapshot.get();
        verify();

        Thread.sleep(200);

        EasyMock.expect(loader.get()).andReturn(new Object());

        replay();
        assert context != snapshot.get();
        verify();
    }

    @Test
    public void testNoUpdates() throws Exception {
        setPeriod(0);

        final Object context = new Object();

        // initialization
        EasyMock.expect(loader.get()).andReturn(context);

        replay();
        final Updates.Snapshot<Object> snapshot = updates.register(100, loader);
        verify();

        replay();
        assert context == snapshot.get();
        assert context == snapshot.get();
        verify();

        Thread.sleep(200);

        replay();
        assert context == snapshot.get();
        verify();
    }
}

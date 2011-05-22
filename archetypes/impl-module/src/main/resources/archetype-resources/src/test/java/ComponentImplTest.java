#*
Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*##set( $symbol_pound = '#' )#*
*##set( $symbol_dollar = '$' )#*
*##set( $symbol_escape = '\' )#*
*#package ${package};

import org.easymock.EasyMock;
import org.fluidity.tests.MockGroupAbstractTest;
import org.testng.Assert;
import org.testng.annotations.Test;

public final class ComponentImplTest extends MockGroupAbstractTest {

    private final ComponentApi.MessageSink dependency = mock(ComponentApi.MessageSink.class);

    private final ComponentApi subject = new ComponentImpl(dependency);

    @Test
    public void sendsText() throws Exception {
        final String accepted = "accepted";
        final String rejected = "rejected";

        EasyMock.expect(dependency.receiveText(accepted)).andReturn(true);
        EasyMock.expect(dependency.receiveText(rejected)).andReturn(false);

        replay();

        Assert.assertTrue(subject.sendText(accepted));
        Assert.assertFalse(subject.sendText(rejected));

        verify();
    }
}

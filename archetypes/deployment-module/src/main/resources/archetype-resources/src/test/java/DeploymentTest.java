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
*#
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import org.easymock.EasyMock;
import org.fluidity.deployment.DeployedComponent;
import org.fluidity.tests.MockGroupAbstractTest;
import org.testng.Assert;
import org.testng.annotations.Test;

public final class DeploymentTest extends MockGroupAbstractTest {

    private final DeployedComponent deployment = new Deployment();

    @Test
    public void metadata() throws Exception {
        replay();
        deployment.key();
        deployment.name();
        verify();
    }

    @Test
    public void deployment() throws Exception {
        replay();
        deployment.start();
        verify();

        replay();
        deployment.stop();
        verify();
    }
}

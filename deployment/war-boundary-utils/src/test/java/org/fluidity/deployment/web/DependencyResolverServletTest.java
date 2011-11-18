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
package org.fluidity.deployment.web;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.fluidity.tests.MockGroupAbstractTest;

import org.easymock.EasyMock;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class DependencyResolverServletTest extends MockGroupAbstractTest {

    private final Servlet servlet = mock(Servlet.class);
    private final ServletConfig config = mock(ServletConfig.class);
    private final DependencyResolver resolver = mock(DependencyResolver.class);
    private final ServletRequest request = mock(ServletRequest.class);
    private final ServletResponse response = mock(ServletResponse.class);

    @SuppressWarnings("StringEquality")
    @Test
    public void delegateAcquisition() throws Exception {
        final String componentKey = "key";
        final String servletInfo = "info";

        EasyMock.expect(config.getInitParameter(DependencyResolver.COMPONENT_KEY)).andReturn(componentKey);
        EasyMock.expect(resolver.findComponent(componentKey)).andReturn(servlet);
        servlet.init(config);
        EasyMock.expect(servlet.getServletConfig()).andReturn(config);
        EasyMock.expect(servlet.getServletInfo()).andReturn(servletInfo);
        servlet.service(request, response);
        servlet.destroy();

        replay();

        final DependencyResolverServlet resolverServlet = new DependencyResolverServlet();
        resolverServlet.init(config, resolver);
        assert resolverServlet.getServletConfig() == config;
        assert resolverServlet.getServletInfo() == servletInfo;
        resolverServlet.service(request, response);
        resolverServlet.destroy();

        verify();
    }
}

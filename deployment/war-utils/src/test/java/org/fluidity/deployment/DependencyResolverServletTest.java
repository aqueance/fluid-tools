/*
 * Copyright (c) 2006-2008 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Softweare"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.fluidity.deployment;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.easymock.EasyMock;
import org.fluidity.tests.MockGroupAbstractTest;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class DependencyResolverServletTest extends MockGroupAbstractTest {

    private final Servlet servlet = addControl(Servlet.class);
    private final ServletConfig config = addControl(ServletConfig.class);
    private final DependencyResolver resolver = addControl(DependencyResolver.class);
    private final ServletRequest request = addControl(ServletRequest.class);
    private final ServletResponse response = addControl(ServletResponse.class);

    @SuppressWarnings({ "StringEquality" })
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

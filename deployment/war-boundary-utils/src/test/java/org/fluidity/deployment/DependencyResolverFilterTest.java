/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.fluidity.deployment;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.fluidity.tests.MockGroupAbstractTest;

import org.easymock.EasyMock;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class DependencyResolverFilterTest extends MockGroupAbstractTest {

    private final Filter filter = addControl(Filter.class);
    private final FilterConfig config = addControl(FilterConfig.class);
    private final DependencyResolver resolver = addControl(DependencyResolver.class);
    private final ServletRequest request = addControl(ServletRequest.class);
    private final ServletResponse response = addControl(ServletResponse.class);
    private final FilterChain chain = addControl(FilterChain.class);

    @Test
    public void delegateAcquisition() throws Exception {
        final String componentKey = "key";

        EasyMock.expect(config.getInitParameter(DependencyResolver.COMPONENT_KEY)).andReturn(componentKey);

        EasyMock.expect(resolver.findComponent(componentKey)).andReturn(filter);
        filter.init(config);
        filter.doFilter(request, response, chain);
        filter.destroy();

        replay();

        final DependencyResolverFilter resolverFilter = new DependencyResolverFilter();
        resolverFilter.init(config, resolver);
        resolverFilter.doFilter(request, response, chain);
        resolverFilter.destroy();

        verify();
    }
}

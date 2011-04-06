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

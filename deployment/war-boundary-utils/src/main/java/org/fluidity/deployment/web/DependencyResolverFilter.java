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

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * A filter decorator that enables its delegate to have its dependencies injected. The delegate must be another filter, obviously. The delegate is
 * instantiated in the {@link #init(FilterConfig)}  method and discarded in the {@link #destroy()} method..
 * <p/>
 * Usage in <code>web.xml</code>:<pre>
 * &lt;web-app>
 *   ...
 *   &lt;filter>
 *     ...
 *     &lt;filter-class>org.fluidity.deployment.web.DependencyResolverFilter&lt;/filter-class>
 *     ...
 *     &lt;init-param>
 *       &lt;param-name>component-key&lt;/param-name>
 *       &lt;param-value><i>the class name of this filter's delegate</i>&lt;/param-value>
 *     &lt;/init-param>
 *   &lt;/filter>
 * &lt;/web-app>
 * </pre>
 *
 * @author Tibor Varga
 */
public final class DependencyResolverFilter implements Filter {

    private final DependencyResolver resolver = new DependencyResolverImpl();

    private Filter delegate;

    public void init(final FilterConfig config) throws ServletException {
        init(config, resolver);
    }

    /*
     * Package visible to make accessible to test cases.
     */
    void init(final FilterConfig config, final DependencyResolver resolver) throws ServletException {
        assert resolver != null;
        delegate = (Filter) resolver.findComponent(config.getInitParameter(DependencyResolver.COMPONENT_KEY));
        assert delegate != null;
        delegate.init(config);
    }

    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        delegate.doFilter(request, response, chain);
    }

    public void destroy() {
        delegate.destroy();
        delegate = null;
    }
}

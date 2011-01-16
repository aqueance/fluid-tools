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

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * A filter decorator that enables its delegate to have its dependencies injected.
 *
 * <p/>
 *
 * Usage:<pre>
 * &lt;web-app>
 *   ...
 *   &lt;filter>
 *     ...
 *     &lt;filter-class>org.fluidity.deployment.DependencyResolverFilter&lt;/filter-class>
 *     ...
 *     &lt;init-param>
 *       &lt;param-name>component-key&lt;/param-name>
 *       &lt;param-value><i>the requested component's key in the container that resolves to this filter's
 * delegate</i>&lt;/param-value>
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

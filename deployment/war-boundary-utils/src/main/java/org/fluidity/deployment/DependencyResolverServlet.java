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

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * A servlet decorator that enables the decorated servlet to have its dependencies injected.
 *
 * <p/>
 *
 * Usage:<pre>
 * &lt;web-app>
 *   ...
 *   &lt;servlet>
 *     ...
 *     &lt;servlet-class>org.fluidity.deployment.DependencyResolverServlet&lt;/servlet-class>
 *     ...
 *     &lt;init-param>
 *       &lt;param-name>component-key&lt;/param-name>
 *       &lt;param-value><i>the class name of this servlet's delegate</i>&lt;/param-value>
 *     &lt;/init-param>
 *   &lt;/servlet>
 * &lt;/web-app>
 * </pre>
 *
 * @author Tibor Varga
 */
public final class DependencyResolverServlet implements Servlet {

    private final DependencyResolver resolver = new DependencyResolverImpl();

    private Servlet delegate;

    public void init(final ServletConfig config) throws ServletException {
        init(config, resolver);
    }

    /*
     * Package visible to make accessible to test cases.
     */
    void init(final ServletConfig config, final DependencyResolver resolver) throws ServletException {
        assert resolver != null;
        delegate = (Servlet) resolver.findComponent(config.getInitParameter(DependencyResolver.COMPONENT_KEY));
        assert delegate != null;
        delegate.init(config);
    }

    public ServletConfig getServletConfig() {
        return delegate.getServletConfig();
    }

    public void service(final ServletRequest request, final ServletResponse response)
            throws ServletException, IOException {
        delegate.service(request, response);
    }

    public String getServletInfo() {
        return delegate.getServletInfo();
    }

    public void destroy() {
        delegate.destroy();
        delegate = null;
    }
}

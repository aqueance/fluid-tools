#*
Copyright (c) 2006-2012 Tibor Adam Varga (tibor.adam.varga on gmail)

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

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fluidity.composition.Component;
import org.fluidity.composition.Inject;
import org.fluidity.composition.Containers;

public final class MyServlet extends HttpServlet {

    @Inject
    private ComponentApi sink;

    public MyServlet() {
        Containers.global().initialize(this);
    }

    public void init(final ServletConfig config) throws ServletException {
        sink.sendText("--- Servlet initialized. Press Ctrl-C to terminate it.");
    }

    public void destroy() {
        sink.sendText("--- Servlet destroyed.");
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        sink.sendText("--- Servlet received a GET request");
    }

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        sink.sendText("--- Servlet received a POST request");
    }
}

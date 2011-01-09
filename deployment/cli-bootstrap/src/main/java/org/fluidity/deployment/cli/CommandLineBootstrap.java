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
package org.fluidity.deployment.cli;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainerAccess;
import org.fluidity.composition.spi.ShutdownHook;
import org.fluidity.deployment.DeploymentBootstrap;

/**
 * A command line main class that bootstraps the application's dependency injection container, invokes {@link org.fluidity.deployment.DeploymentBootstrap} to
 * load and control deployment units and to invokes the application supplied main loop, {@link MainLoop}. Any component can access the command line parameters
 * by having a constructor with, among other dependencies, a <code>final String[] args</code> parameter.
 */
@Component(api = CommandLineBootstrap.class)
public final class CommandLineBootstrap implements Runnable {

    private final MainLoop main;

    public CommandLineBootstrap(final DeploymentBootstrap bootstrap, final MainLoop main, final ShutdownHook shutdown) throws Exception {
        this.main = main;

        shutdown.addTask("bootstrap", new Runnable() {
            public void run() {
                bootstrap.unload();
            }
        });

        bootstrap.load();
    }

    public void run() {
        main.run();
    }

    public static void main(final String[] args) throws Exception {
        final ComponentContainerAccess container = new ComponentContainerAccess();
        container.bindBootComponent(String[].class, args);
        container.getComponent(CommandLineBootstrap.class).run();
    }
}
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
package org.fluidity.composition.cli;

import org.fluidity.composition.DeploymentBootstrap;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContainerAccess;
import org.fluidity.composition.ShutdownHook;

/**
 * A command line main class that bootstraps and controls all {@link org.fluidity.composition.DeployedComponent} and {@link
 * org.fluidity.composition.DeploymentObserver} objects in the application. The application must have a {@link Main} component that is initialized ({@link
 * Main#initialize(String[])}) and run ({@link org.fluidity.composition.cli.Main#run()}).
 */
public final class BootstrapMain {

    public BootstrapMain(final ShutdownHook shutdown, final DeploymentBootstrap bootstrap) throws Exception {
        shutdown.addTask("bootstrap", new Runnable() {
            public void run() {
                bootstrap.unload();
            }
        });

        bootstrap.load();
    }

    public static void main(final String[] args) throws Exception {
        final ComponentContainer container = new ComponentContainerAccess();

        final ShutdownHook shutdown = container.getComponent(ShutdownHook.class);
        final DeploymentBootstrap bootstrap = container.getComponent(DeploymentBootstrap.class);

        final Main main = container.getComponent(Main.class);
        assert main != null : Main.class;

        main.initialize(args);

        new BootstrapMain(shutdown, bootstrap);

        main.run();
    }
}
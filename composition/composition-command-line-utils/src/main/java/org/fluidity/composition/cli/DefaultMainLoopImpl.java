package org.fluidity.composition.cli;

import org.fluidity.composition.Component;
import org.fluidity.composition.DeploymentBootstrap;

/**
 * This default implementation expects the application code to be in a {@link org.fluidity.composition.DeployedComponent}, which when done invokes {@link
 * org.fluidity.composition.cli.MainLoop#stop()}.
 */
@Component(fallback = true)
final class DefaultMainLoopImpl implements MainLoop {

    private final Object lock = new Object();

    private final DeploymentBootstrap bootstrap;

    public DefaultMainLoopImpl(final DeploymentBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    public void run() {
        if (bootstrap.deploymentCount() > 0) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (final InterruptedException e) {
                    // ignore
                }
            }
        }
    }

    public void stop() {
        synchronized (lock) {
            lock.notify();
        }
    }
}

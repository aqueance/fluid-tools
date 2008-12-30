package org.fluidity.deployment.cli;

import java.util.concurrent.atomic.AtomicBoolean;

import org.fluidity.composition.Component;
import org.fluidity.composition.DeploymentBootstrap;

/**
 * This default implementation expects the application code to be in a {@link org.fluidity.composition.DeployedComponent}, which when done invokes {@link
 * org.fluidity.deployment.RuntimeControl#stop()}.
 */
@Component(fallback = true)
final class DefaultMainLoopImpl implements MainLoop {

    private final Object lock = new Object();

    private final DeploymentBootstrap bootstrap;
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    public DefaultMainLoopImpl(final DeploymentBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    public void run() {
        if (bootstrap.deploymentCount() > 0 && !stopped.get()) {
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
        if (!stopped.compareAndSet(false, true)) {
            synchronized (lock) {
                lock.notify();
            }
        }
    }
}

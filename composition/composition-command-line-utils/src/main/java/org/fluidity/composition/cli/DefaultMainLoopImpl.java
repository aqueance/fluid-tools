package org.fluidity.composition.cli;

import org.fluidity.composition.Component;

/**
 * This default implementation expects the application code to be in a {@link org.fluidity.composition.DeployedComponent}, which when done invokes {@link
 * org.fluidity.composition.cli.MainLoop#stop()}.
 */
@Component(fallback = true)
final class DefaultMainLoopImpl implements MainLoop {

    private final Object lock = new Object();

    public boolean run(final int componentCount) {
        final boolean started = componentCount > 0;

        if (started) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (final InterruptedException e) {
                    // ignore
                }
            }
        }

        return started;
    }

    public void stop() {
        synchronized (lock) {
            lock.notify();
        }
    }
}

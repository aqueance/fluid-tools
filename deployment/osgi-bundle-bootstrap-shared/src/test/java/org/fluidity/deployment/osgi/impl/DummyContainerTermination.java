package org.fluidity.deployment.osgi.impl;

import org.fluidity.composition.Component;
import org.fluidity.composition.spi.ContainerTermination;
import org.fluidity.foundation.Command;

/**
 * @author Tibor Varga
 */
@Component
public class DummyContainerTermination implements ContainerTermination {

    public void add(final Command.Job<Exception> job) {
        // empty
    }

    public void remove(final Command.Job<Exception> job) {
        // empty
    }
}

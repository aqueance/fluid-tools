package org.fluidity.composition.web;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentDiscovery;
import org.fluidity.composition.ComponentContainerAccess;

/**
 * Finds all implementations of the {@link javax.servlet.ServletContextListener} that have been marked with the
 *
 * @{@link org.fluidity.composition.ServiceProvider} annotation and dispatches the listener events to all.
 */
public final class WebApplicationLifecycleListener implements ServletContextListener {

    private final ServletContextListener listeners[];

    public WebApplicationLifecycleListener() {
        final ComponentContainer container = new ComponentContainerAccess();
        final ComponentDiscovery discovery = container.getComponent(ComponentDiscovery.class);

        this.listeners = discovery.findComponentInstances(container, ServletContextListener.class);
    }

    public void contextInitialized(final ServletContextEvent event) {
        for (final ServletContextListener listener : listeners) {
            listener.contextInitialized(event);
        }
    }

    public void contextDestroyed(final ServletContextEvent event) {
        for (final ServletContextListener listener : listeners) {
            listener.contextDestroyed(event);
        }
    }
}

package org.fluidity.composition.spi;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ContextDefinition;

/**
 * Parent container of the root dependency injection container of an application. When available, the implementation is mapped to some dependency injection
 * mechanism of the application container itself.
 */
public interface PlatformContainer {

    /**
     * Tells, without any reservation on the part of the receiver, whether the given component interface can be resolved by the receiver.
     *
     * @param api     the component interface to check.
     * @param context the current component context.
     *
     * @return <code>true</code> if the component interface can be resolved, <code>false</code> otherwise.
     */
    boolean containsComponent(Class<?> api, final ContextDefinition context);

    /**
     * See {@link ComponentContainer#getComponent(Class)}.
     *
     * @param api     see {@link ComponentContainer#getComponent(Class)}.
     * @param context the current component context.
     *
     * @return see {@link ComponentContainer#getComponent(Class)}.
     *
     * @throws ComponentContainer.ResolutionException
     *          see {@link ComponentContainer#getComponent(Class)}
     */
    <T> T getComponent(Class<T> api, ContextDefinition context) throws ComponentContainer.ResolutionException;

    /**
     * Tells, without any reservation on the part of the receiver, whether the given group interface can be resolved by the receiver.
     *
     * @param api     the component interface to check.
     * @param context the current component context.
     *
     * @return <code>true</code> if the group interface can be resolved, <code>false</code> otherwise.
     */
    boolean containsComponentGroup(Class<?> api, final ContextDefinition context);

    /**
     * See {@link ComponentContainer#getComponentGroup(Class)}.
     *
     * @param api     see {@link ComponentContainer#getComponentGroup(Class)}.
     * @param context the component context prevalent at the group reference site.
     *
     * @return see {@link ComponentContainer#getComponentGroup(Class)}.
     */
    <T> T[] getComponentGroup(Class<T> api, ContextDefinition context);

    /**
     * Returns a textual identifier for the container.
     *
     * @return a textual identifier for the container.
     */
    String id();

    /**
     * Releases any resources acquired from the platform.
     */
    void stop();
}

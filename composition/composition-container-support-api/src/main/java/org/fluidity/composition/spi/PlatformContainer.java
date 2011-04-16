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
     *
     * @param api the component interface to check.
     * @param context
     * @return <code>true</code> if the component interface can be resolved, <code>false</code> otherwise.
     */
    boolean containsComponent(Class<?> api, final ContextDefinition context);

    /**
     * Returns a component by interface or (super)class. This method is provided for boundary objects (objects created outside the container by third party
     * tools) to acquire their dependencies.
     * <p/>
     * If there is no component bound to the given class itself, an attempt is made to locate a single component that implements the given interface or is an
     * instance of the given class or any of its subclasses.
     *
     *
     * @param api is a class object that was used to bind a component against; never <code>null</code>.
     * @param context
     * @return the component bound against the give class or <code>null</code> when none was found.
     * @throws ComponentContainer.ResolutionException
     *          when dependency resolution fails
     */
    <T> T getComponent(Class<T> api, ContextDefinition context) throws ComponentContainer.ResolutionException;

    /**
     * Tells, without any reservation on the part of the receiver, whether the given group interface can be resolved by the receiver.
     *
     *
     * @param api the component interface to check.
     * @param context
     * @return <code>true</code> if the group interface can be resolved, <code>false</code> otherwise.
     */
    boolean containsComponentGroup(Class<?> api, final ContextDefinition context);

    /**
     * Returns the list of components implementing the given interface, provided that they each, or the given interface itself, has been marked with the
     * {@link org.fluidity.composition.ComponentGroup} annotation.
     *
     * @param api     the group interface class.
     * @param context the component context prevalent at the group reference site.
     * @return an array of components that belong to the given group; may be <code>null</code>.
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

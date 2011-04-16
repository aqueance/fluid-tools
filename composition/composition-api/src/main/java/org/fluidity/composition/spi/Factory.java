package org.fluidity.composition.spi;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.OpenComponentContainer;

/**
 * Defines the behaviour of component factories.
 */
public interface Factory {

    /**
     * Binds in the provided container the dependencies of the component that this is a factory for. Once this methods is called, the given container must be
     * able to successfully return an instance of the API this is a factory for.
     * <p/>
     * It is recommended not to instantiate components but add proper bindings for the container to instantiate them, including the component this is a factory
     * for.
     *
     * @param container is the container to add bindings to and, if necessary, resolve dependencies of the component from.
     * @param context   is the context for the instance to create. When this is null or empty, the default instance must be returned. The key set in the
     *                  context is taken from the list of annotation classes in the {@link org.fluidity.composition.Context} annotation of this factory.
     *
     * @throws ComponentContainer.ResolutionException
     *          if a component cannot be created.
     */
    void newComponent(OpenComponentContainer container, ComponentContext context) throws ComponentContainer.ResolutionException;
}

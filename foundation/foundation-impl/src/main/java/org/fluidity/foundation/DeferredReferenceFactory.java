package org.fluidity.foundation;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.spi.CustomComponentFactory;

/**
 * This factory makes it possible to depend on a lazy instantiated component.
 *
 * @author Tibor Varga
 */
@Component(api = Deferred.Reference.class)
@Component.Context(Component.Reference.class)
@SuppressWarnings("UnusedDeclaration")
final class DeferredReferenceFactory implements CustomComponentFactory {

    public Instance resolve(final ComponentContext context, final Resolver dependencies) throws ComponentContainer.ResolutionException {
        final Dependency<?> dependency = dependencies.resolve(null, Generics.typeParameter(context.annotation(Component.Reference.class, null).type(), 0));

        final Deferred.Reference<Object> reference = Deferred.reference(new Deferred.Factory<Object>() {
            public Object create() {
                return dependency.instance();
            }
        });

        return new Instance() {
            @SuppressWarnings("unchecked")
            public void bind(final Registry registry) throws ComponentContainer.BindingException {
                registry.bindInstance(reference, Deferred.Reference.class);
            }
        };
    }
}

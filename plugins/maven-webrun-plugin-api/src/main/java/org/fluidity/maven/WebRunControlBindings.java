package org.fluidity.maven;

import java.util.Map;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.EmptyPackageBindings;

public final class WebRunControlBindings extends EmptyPackageBindings {

    private final WebRunControl control;

    public WebRunControlBindings(final Map properties) {
        control = (WebRunControl) properties.get(WebRunControl.class);
        assert control != null : WebRunControl.class;
    }

    @Override
    public void registerComponents(ComponentContainer.Registry registry) {
        if (control != null) {
            registry.bind(WebRunControl.class, control);
        }
    }
}

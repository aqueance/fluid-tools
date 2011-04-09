package org.fluidity.foundation;

import java.util.jar.Attributes;

/**
 * Interface that a manifest handler must implement. In practice this is a service provider but since no composition
 * functionality is available when implementations of this interface are resolved, we can't use the @ServiceProvider
 * annotation here and neither can we use it on the implementations.
 *
 * @author Tibor Varga
 */
public interface JarManifest {

    String NESTED_DEPENDENCIES = "Nested-Dependencies";

    /**
     * Set main manifest attributes to invoke this launcher.
     *
     * @param attributes the main manifest attributes.
     */
    void processManifest(Attributes attributes);
}

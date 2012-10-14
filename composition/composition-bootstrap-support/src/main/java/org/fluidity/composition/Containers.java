/*
 * Copyright (c) 2006-2012 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fluidity.composition;

import org.fluidity.foundation.Utility;

/**
 * Static access to component containers.
 *
 * @author Tibor Varga
 */
@SuppressWarnings("UnusedDeclaration")
public final class Containers extends Utility {

    private Containers() { }

    /**
     * Returns the component container populated for the <i>current</i> class loader. The current class loader is either the context class loader for the
     * calling thread or, if that is <code>null</code>, the class loader that loaded this class.
     *
     * @return the component container populated for the current class loader.
     */
    public static ComponentContainer global() {
        return new ContainerBoundary();
    }

    /**
     * Returns the component container populated for the given class loader.
     *
     * @param loader the class loader.
     *
     * @return the component container populated for the given class loader.
     */
    public static ComponentContainer global(final ClassLoader loader) {
        return new ContainerBoundary(loader);
    }

    /**
     * Returns the component container to be populated for the current class loader. The current class loader is either the context class loader for the
     * calling thread or, if that is <code>null</code>, the class loader that loaded this class.
     *
     * @return the component container to be populated for the current class loader.
     */
    public static ContainerBoundary prepare() {
        return new ContainerBoundary();
    }

    /**
     * Returns the component container to be populated for the given class loader.
     *
     * @param loader the class loader.
     *
     * @return the component container to be populated for the given class loader.
     */
    public static ContainerBoundary prepare(final ClassLoader loader) {
        return new ContainerBoundary(loader);
    }

    /**
     * Creates a new empty component container. The returned container can be populated via the {@link ComponentContainer.Registry registry} returned by its
     * {@link ExposedComponentContainer#getRegistry()} method.
     *
     * @param loader the class loader to find the container implementation in; may be <code>null</code>, in which case the class loader that loaded this class
     *               will be used.
     * @return a new empty component container.
     */
    public static ExposedComponentContainer create(final ClassLoader loader) {
        return new ContainerBoundary(loader == null ? Containers.class.getClassLoader() : loader).create();
    }
}

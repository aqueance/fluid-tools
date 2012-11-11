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

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.fluidity.foundation.Security;

/**
 * An abstract class to extend by components at the edge of the application to enjoy field based dependency injection. The class extending this one must
 * declare its dependencies as {@link Inject @Inject} annotated non <code>final</code> instance fields, and call one of the constructors of this class.
 * <p/>
 * In cases where class extension is not possible, simply use the following code in the constructor, or something similar:
 * <pre>
 * {@linkplain Containers Containers}.{@linkplain org.fluidity.composition.Containers#global(ClassLoader) global}(getClass().getClassLoader()).{@linkplain ComponentContainer#initialize(Object) initialize}(this);
 * </pre>
 *
 * @author Tibor Varga
 */
@SuppressWarnings("UnusedDeclaration")
public abstract class BoundaryComponent {

    private final ComponentContainer container;

    /**
     * Resolves the component dependency fields from components visible to the class loader that loaded the subclass.
     */
    protected BoundaryComponent() {
        this.container = Containers.global(!Security.CONTROLLED ? getClass().getClassLoader() : AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            public ClassLoader run() {
                return BoundaryComponent.this.getClass().getClassLoader();
            }
        }));

        this.container.initialize(this);
    }

    /**
     * Resolves the component dependency fields from components visible to the given class loader.
     *
     * @param loader the class loader to use.
     */
    protected BoundaryComponent(final ClassLoader loader) {
        this.container = Containers.global(loader);
        this.container.initialize(this);
    }

    /**
     * Resolves the component dependency fields from the given dependency injection container.
     *
     * @param container the container to use.
     */
    protected BoundaryComponent(final ComponentContainer container) {
        this.container = container;
        this.container.initialize(this);
    }

    /**
     * Returns the container for this boundary component.
     *
     * @return the container for this boundary component.
     */
    public final ComponentContainer container() {
        return container;
    }
}

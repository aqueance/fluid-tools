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

package org.fluidity.deployment.osgi;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.fluidity.foundation.Lists;

/**
 * @author Tibor Varga
 */
final class ComponentDescriptor extends Descriptor {

    private boolean failed = false;

    private final Set<ServiceDescriptor> dependencies = new HashSet<ServiceDescriptor>();
    private final Class<? super BundleComponentContainer.Managed>[] api;

    @SuppressWarnings("unchecked")
    ComponentDescriptor(final Class<BundleComponentContainer.Managed> type, final Collection<Class<? super BundleComponentContainer.Managed>> api) {
        super(type);
        this.api = Lists.asArray(Class.class, api);
    }

    public Set<ServiceDescriptor> dependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

    public void dependencies(final Set<ServiceDescriptor> services) {
        assert dependencies().isEmpty() : type;
        dependencies.addAll(services);
    }

    public Class<?>[] interfaces() {
        return api;
    }

    public void failed(final boolean flag) {
        failed = flag;
    }

    public boolean failed() {
        return failed;
    }
}

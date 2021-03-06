/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.deployment.osgi.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.fluidity.deployment.osgi.BundleComponents;
import org.fluidity.foundation.Lists;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("WeakerAccess")
final class ComponentDescriptor extends Descriptor {

    private boolean failed = false;

    private final Set<ServiceDescriptor> dependencies = new HashSet<>();
    private final Class<? extends BundleComponents.Managed>[] api;

    @SuppressWarnings("unchecked")
    ComponentDescriptor(final Class<? extends BundleComponents.Managed> type, final Collection<Class<? extends BundleComponents.Managed>> api) {
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

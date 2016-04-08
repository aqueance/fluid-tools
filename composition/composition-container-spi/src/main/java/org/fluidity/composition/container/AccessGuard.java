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

package org.fluidity.composition.container;

import java.util.concurrent.atomic.AtomicBoolean;

import org.fluidity.composition.ComponentContainer;

/**
 * Guards against inadvertent access to some component. Guards are used by the {@link DependencyInjector} to prevent container use in constructors, for
 * instance.
 * <h3>Usage</h3>
 * <pre>
 * final {@linkplain DependencyInjector} injector = &hellip;
 * final <span class="hl1">AccessGuard</span>&lt;{@linkplain ComponentContainer}&gt; <span class="hl2">access</span> = injector.{@linkplain DependencyInjector#containerGuard() containerGuard}();
 *
 * final {@linkplain org.fluidity.composition.container.spi.DependencyGraph.Node DependencyGraph.Node} node = injector.{@linkplain DependencyInjector#resolve(Class, AccessGuard, DependencyInjector.Resolution) resolve}(&hellip;, <span class="hl2">access</span>, &hellip;);
 * &hellip;
 * final Object component = node.{@linkplain org.fluidity.composition.container.spi.DependencyGraph.Node#instance(org.fluidity.composition.container.spi.DependencyGraph.Traversal) instance}(&hellip;);
 * &hellip;
 * <span class="hl2">access</span>.<span class="hl1">{@linkplain #enable() enable}</span>();
 * </pre>
 *
 * @author Tibor Varga
 */
public final class AccessGuard<T> {

    private String rejected;
    private final AtomicBoolean enabled = new AtomicBoolean();

    public AccessGuard(final String rejected) {
        this.rejected = rejected;
    }

    public T access(final T valid) {
        if (enabled.get()) {
            return valid;
        } else {
            throw new ComponentContainer.ResolutionException(rejected);
        }
    }

    public boolean enabled() {
        return enabled.get();
    }

    public void enable() {
        enabled.set(true);
    }
}

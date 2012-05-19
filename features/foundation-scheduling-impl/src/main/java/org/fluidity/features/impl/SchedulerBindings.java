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

package org.fluidity.features.impl;

import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.spi.EmptyPackageBindings;
import org.fluidity.features.Scheduler;

/**
 * Binds a {@link Scheduler} implementation and makes sure it is stopped when the container is shut down.
 *
 * @author Tibor Varga
 */
@SuppressWarnings("UnusedDeclaration")
final class SchedulerBindings extends EmptyPackageBindings {

    private final AtomicReference<SchedulerImpl> scheduler = new AtomicReference<SchedulerImpl>();

    @Override
    @SuppressWarnings("unchecked")
    public void bindComponents(final ComponentContainer.Registry registry) {
        registry.bindComponent(SchedulerImpl.class);
    }

    @Override
    public void initializeComponents(final ComponentContainer container) {
        scheduler.set((SchedulerImpl) container.getComponent(Scheduler.class));
    }

    @Override
    public void shutdownComponents(final ComponentContainer container) {
        scheduler.getAndSet(null).stop();
    }
}

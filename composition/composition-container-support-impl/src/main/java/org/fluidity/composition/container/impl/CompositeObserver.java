/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.composition.container.impl;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.DependencyPath;

/**
 * A composite component resolution observer.
 *
 * @author Tibor Varga
 */
final class CompositeObserver implements ComponentContainer.Observer {

    private final Set<ComponentContainer.Observer> observers = new LinkedHashSet<ComponentContainer.Observer>();

    public static ComponentContainer.Observer combine(final ComponentContainer.Observer... observers) {
        final Set<ComponentContainer.Observer> list = new LinkedHashSet<ComponentContainer.Observer>(observers.length, (float) 1.0);

        for (final ComponentContainer.Observer observer : observers) {
            if (observer != null) {
                list.add(observer);
            }
        }

        switch (list.size()) {
        case 0:
            return null;
        case 1:
            return list.iterator().next();
        default:
            return new CompositeObserver(list);
        }
    }

    private CompositeObserver(final Collection<ComponentContainer.Observer> observers) {
        for (final ComponentContainer.Observer observer : observers) {
            add(observer);
        }
    }

    public void resolving(final Class<?> declaringType,
                          final Class<?> dependencyType,
                          final Annotation[] typeAnnotations,
                          final Annotation[] referenceAnnotations) {
        for (final ComponentContainer.Observer observer : observers) {
            observer.resolving(declaringType, dependencyType, typeAnnotations, referenceAnnotations);
        }
    }

    public void resolved(final DependencyPath path, final Class<?> type) {
        for (final ComponentContainer.Observer observer : observers) {
            observer.resolved(path, type);
        }
    }

    public void instantiated(final DependencyPath path, final AtomicReference<?> reference) {
        for (final ComponentContainer.Observer observer : observers) {
            observer.instantiated(path, reference);
        }
    }

    private void add(final ComponentContainer.Observer observer) {
        if (observer instanceof CompositeObserver) {
            observers.addAll(((CompositeObserver) observer).observers);
        } else {
            observers.add(observer);
        }
    }
}

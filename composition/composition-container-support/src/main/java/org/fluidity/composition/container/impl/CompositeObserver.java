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
 * <h3>Usage</h3>
 * <pre>
 * final {@linkplain ComponentContainer} <span class="hl2">container</span> = &hellip;
 * final {@linkplain ComponentContainer.Observer}[] <span class="hl3">observers</span> = &hellip;
 * &hellip;
 * final {@linkplain org.fluidity.composition.ObservedContainer ObservedContainer} observed = <span class="hl2">container</span>.{@linkplain ComponentContainer#observed(ComponentContainer.Observer) observe}(<span class="hl1">{@linkplain CompositeObserver#combine(ComponentContainer.Observer...) CompositeObserver.combine}</span>(<span class="hl3">observers</span>));
 * &hellip;
 * </pre>
 *
 * @author Tibor Varga
 */
@SuppressWarnings("unused")
public final class CompositeObserver implements ComponentContainer.Observer {

    private final Set<ComponentContainer.Observer> observers = new LinkedHashSet<>();

    /**
     * Combines zero, one, or more observers into one, which can then be fed to {@link ComponentContainer#observed(ComponentContainer.Observer)
     * ComponentContainer.observed(…)}. Composite observer in the given list will have its individual observers added to this one. Missing (<code>null</code>)
     * elements will be omitted from the composite.
     *
     * @param observers the list of observers to combine.
     *
     * @return an observer that broadcasts observed events to the given list of observers, or <code>null</code> if the given argument list is empty.
     */
    public static ComponentContainer.Observer combine(final ComponentContainer.Observer... observers) {
        final Set<ComponentContainer.Observer> list = new LinkedHashSet<>(observers.length);

        for (final ComponentContainer.Observer observer : observers) {
            if (observer != null) {
                list.add(observer);
            }
        }

        return _cases(list);
    }

    /**
     * Combines zero, one, or more observers into one, which can then be fed to {@link ComponentContainer#observed(ComponentContainer.Observer)
     * ComponentContainer.observed(…)}. Composite observer in the given list will have its individual observers added to this one. Missing (<code>null</code>)
     * elements will be omitted from the composite.
     *
     * @param observers the list of observers to combine.
     *
     * @return an observer that broadcasts observed events to the given list of observers, or <code>null</code> if the given argument list is empty.
     */
    public static ComponentContainer.Observer combine(final Collection<? extends ComponentContainer.Observer> observers) {
        final Set<ComponentContainer.Observer> list = new LinkedHashSet<>(observers.size());

        for (final ComponentContainer.Observer observer : observers) {
            if (observer != null) {
                list.add(observer);
            }
        }

        return _cases(list);
    }

    private static ComponentContainer.Observer _cases(final Collection<? extends ComponentContainer.Observer> list) {
        switch (list.size()) {
        case 0:
            return null;
        case 1:
            return list.iterator().next();
        default:
            return new CompositeObserver(list);
        }
    }

    private CompositeObserver(final Collection<? extends ComponentContainer.Observer> observers) {
        observers.forEach(this::add);
    }

    public void descending(final Class<?> declaringType,
                           final Class<?> dependencyType,
                           final Annotation[] typeAnnotations,
                           final Annotation[] referenceAnnotations) {
        for (final ComponentContainer.Observer observer : observers) {
            observer.descending(declaringType, dependencyType, typeAnnotations, referenceAnnotations);
        }
    }

    public void ascending(final Class<?> declaringType, final Class<?> dependencyType) {
        for (final ComponentContainer.Observer observer : observers) {
            observer.ascending(declaringType, dependencyType);
        }
    }

    public void circular(final DependencyPath path) {
        for (final ComponentContainer.Observer observer : observers) {
            observer.circular(path);
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

/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.fluidity.composition;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.fluidity.composition.spi.ComponentResolutionObserver;
import org.fluidity.composition.spi.DependencyPath;

/**
 * A composite component resolution observer.
 *
 * @author Tibor Varga
 */
final class CompositeObserver implements ComponentResolutionObserver {

    private final Set<ComponentResolutionObserver> observers = new LinkedHashSet<ComponentResolutionObserver>();

    public static ComponentResolutionObserver combine(final ComponentResolutionObserver... observers) {
        return combine(Arrays.asList(observers));
    }

    public static ComponentResolutionObserver combine(final Collection<ComponentResolutionObserver> observers) {
        final Set<ComponentResolutionObserver> list = new LinkedHashSet<ComponentResolutionObserver>(observers.size(), (float) 1.0);

        for (final ComponentResolutionObserver observer : observers) {
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

    private CompositeObserver(final Collection<ComponentResolutionObserver> observers) {
        for (final ComponentResolutionObserver observer : observers) {
            add(observer);
        }
    }

    public void resolved(final DependencyPath path, final Class<?> type) {
        for (final ComponentResolutionObserver observer : observers) {
            observer.resolved(path, type);
        }
    }

    public void instantiated(final DependencyPath path) {
        for (final ComponentResolutionObserver observer : observers) {
            observer.instantiated(path);
        }
    }

    private void add(final ComponentResolutionObserver observer) {
        if (observer instanceof CompositeObserver) {
            observers.addAll(((CompositeObserver) observer).observers);
        } else {
            observers.add(observer);
        }
    }
}

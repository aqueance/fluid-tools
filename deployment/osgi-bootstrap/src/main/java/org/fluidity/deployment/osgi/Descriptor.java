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

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Tibor Varga
 */
abstract class Descriptor {

    public final Class<?> type;

    private final AtomicReference<Object> instance = new AtomicReference<Object>();

    protected Descriptor(final Class<?> type) {
        this.type = type;
    }

    public void failed(final boolean flag) {
        // empty
    }

    public final void started(final Object instance) {
        this.instance.set(instance);
        failed(false);
    }

    public final Object stopped(final boolean failed) {
        try {
            return instance.getAndSet(null);
        } finally {
            failed(failed);
        }
    }

    public final Object instance() {
        return instance.get();
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        return this == o || !(o == null || getClass() != o.getClass()) && type.equals(((Descriptor) o).type);
    }

    @Override
    public String toString() {
        return type.toString();
    }
}

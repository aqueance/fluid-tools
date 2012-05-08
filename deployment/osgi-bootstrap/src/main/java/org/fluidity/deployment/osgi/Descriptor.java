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

    public void failed(final boolean successful) {
        // empty
    }

    public final void started(final Object instance) {
        this.instance.set(instance);
        failed(false);
    }

    public final Object stopped() {
        try {
            return instance.getAndSet(null);
        } finally {
            failed(false);
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

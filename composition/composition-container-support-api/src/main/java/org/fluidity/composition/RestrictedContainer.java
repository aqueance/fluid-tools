package org.fluidity.composition;

/**
 * Wraps another container and denies access to it until {@link #enable()} is invoked.
 *
 * @author Tibor Varga
 */
public interface RestrictedContainer extends ComponentContainer {

    /**
     * Enables the wrapped container.
     */
    void enable();
}
